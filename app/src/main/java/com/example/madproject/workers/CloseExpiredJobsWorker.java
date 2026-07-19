package com.example.madproject.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.madproject.firebase.JobManager;
import com.example.madproject.models.Job;
import com.google.firebase.firestore.DocumentSnapshot;

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
            closeExpiredJobs();
            Log.d(TAG, "Expired jobs cleanup completed successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error closing expired jobs: " + e.getMessage(), e);
            return Result.retry(); // Retry on failure
        }
    }

    private void closeExpiredJobs() {
        CountDownLatch latch = new CountDownLatch(1);
        final int[] closedCount = {0};

        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime - TEN_DAYS_IN_MILLIS;

        // Get all open jobs
        JobManager.getInstance()
                .getOpenJobs()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Job job = doc.toObject(Job.class);

                        if (job != null && job.getPostedDate() <= expirationTime) {
                            // Job is older than 10 days, close it
                            closeJob(job);
                            closedCount[0]++;
                        }
                    }

                    Log.d(TAG, "Closed " + closedCount[0] + " expired jobs");
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching open jobs: " + e.getMessage());
                    latch.countDown();
                });

        // Wait for completion (max 30 seconds)
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted: " + e.getMessage());
        }
    }

    private void closeJob(Job job) {
        JobManager.getInstance()
                .updateJobStatus(job.getJobId(), "closed")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Closed job: " + job.getJobId() + " - " + job.getTitle());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to close job: " + job.getJobId() + " - " + e.getMessage());
                });
    }
}
