package com.example.madproject.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.madproject.firebase.JobManager;
import com.example.madproject.models.Job;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Background worker that automatically closes jobs that are 10 days old
 * Runs daily to check and close expired jobs
 */
public class CloseExpiredJobsWorker extends Worker {

    private static final String TAG = "CloseExpiredJobsWorker";
    private static final long TEN_DAYS_IN_MILLIS = 10 * 24 * 60 * 60 * 1000L; // 10 days

    public CloseExpiredJobsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting expired jobs cleanup...");

        try {
            boolean allClosedSuccessfully = closeExpiredJobs();
            if (!allClosedSuccessfully) {
                Log.w(TAG, "Not all expired jobs were closed successfully - retrying");
                return Result.retry();
            }
            Log.d(TAG, "Expired jobs cleanup completed successfully");
            return Result.success();
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Error closing expired jobs: " + e.getMessage(), e);
            return Result.retry(); // Retry on failure
        }
    }

    /** @return true only if the query and every close-write actually completed successfully. */
    private boolean closeExpiredJobs() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime - TEN_DAYS_IN_MILLIS;

        CountDownLatch queryLatch = new CountDownLatch(1);
        List<Job> expiredJobs = new ArrayList<>();
        boolean[] queryFailed = {false};

        JobManager.getInstance()
                .getOpenJobs()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Job job = doc.toObject(Job.class);
                        if (job != null && job.getPostedDate() <= expirationTime) {
                            expiredJobs.add(job);
                        }
                    }
                    queryLatch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching open jobs: " + e.getMessage());
                    queryFailed[0] = true;
                    queryLatch.countDown();
                });

        if (!queryLatch.await(30, TimeUnit.SECONDS)) {
            Log.e(TAG, "Timed out fetching open jobs");
            return false;
        }
        if (queryFailed[0]) return false;
        if (expiredJobs.isEmpty()) {
            Log.d(TAG, "No expired jobs to close");
            return true;
        }

        // Wait for every individual close-write to actually complete (success or failure),
        // not just for the initial query - the old version counted a job as "closed" the
        // moment updateJobStatus() was *called*, regardless of whether that write ever landed.
        CountDownLatch writeLatch = new CountDownLatch(expiredJobs.size());
        int[] closedCount = {0};
        int[] failedCount = {0};

        for (Job job : expiredJobs) {
            JobManager.getInstance()
                    .updateJobStatus(job.getJobId(), "closed")
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Closed job: " + job.getJobId() + " - " + job.getTitle());
                        closedCount[0]++;
                        writeLatch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to close job: " + job.getJobId() + " - " + e.getMessage());
                        failedCount[0]++;
                        writeLatch.countDown();
                    });
        }

        boolean completed = writeLatch.await(30, TimeUnit.SECONDS);
        Log.d(TAG, "Closed " + closedCount[0] + "/" + expiredJobs.size() + " expired jobs"
                + (failedCount[0] > 0 ? " (" + failedCount[0] + " failed)" : "")
                + (completed ? "" : " (timed out waiting for remaining writes)"));

        return completed && failedCount[0] == 0;
    }
}
