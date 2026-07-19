package com.example.madproject;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.madproject.workers.CloseExpiredJobsWorker;

import java.util.concurrent.TimeUnit;

public class MadProjectApplication extends Application {

    private static final String TAG = "MadProjectApp";
    private static final String CLOSE_EXPIRED_JOBS_WORK = "close_expired_jobs_work";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application started");

        // Apply status-bar inset to every toolbar in the app (edge-to-edge fix for Android 15+)
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) {}

            @Override
            public void onActivityStarted(@NonNull Activity a) {
                // Push toolbar below status bar (runs after setContentView & window attach)
                View toolbar = a.findViewById(R.id.toolbar);
                if (toolbar != null) {
                    ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
                        Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                        ViewGroup.MarginLayoutParams lp =
                                (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                        lp.topMargin = insets.top;
                        v.setLayoutParams(lp);
                        return windowInsets;
                    });
                    ViewCompat.requestApplyInsets(toolbar);
                }
                // Push bottom nav above gesture bar
                View bottomNav = a.findViewById(R.id.bottomNavigation);
                if (bottomNav == null) bottomNav = a.findViewById(R.id.bottomNav);
                if (bottomNav != null) {
                    final View nav = bottomNav;
                    ViewCompat.setOnApplyWindowInsetsListener(nav, (v, windowInsets) -> {
                        Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
                        v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                                v.getPaddingRight(), insets.bottom);
                        return windowInsets;
                    });
                    ViewCompat.requestApplyInsets(nav);
                }
            }
            @Override public void onActivityResumed(@NonNull Activity a) {}
            @Override public void onActivityPaused(@NonNull Activity a) {}
            @Override public void onActivityStopped(@NonNull Activity a) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}
            @Override public void onActivityDestroyed(@NonNull Activity a) {}
        });

        // Schedule background jobs
        scheduleBackgroundJobs();
    }

    private void scheduleBackgroundJobs() {
        // Schedule job to close expired jobs (runs once daily)
        scheduleExpiredJobsCleanup();
    }

    private void scheduleExpiredJobsCleanup() {
        // Define constraints - only run when connected to network
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create periodic work request (runs every 24 hours)
        PeriodicWorkRequest closeExpiredJobsWork = new PeriodicWorkRequest.Builder(
                CloseExpiredJobsWorker.class,
                24, TimeUnit.HOURS,  // Repeat every 24 hours
                15, TimeUnit.MINUTES // Flex interval (can run within 15 min window)
        )
                .setConstraints(constraints)
                .build();

        // Enqueue the work (KEEP will not replace existing work)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                CLOSE_EXPIRED_JOBS_WORK,
                ExistingPeriodicWorkPolicy.KEEP, // Don't cancel existing work
                closeExpiredJobsWork
        );

        Log.d(TAG, "Scheduled: Close expired jobs worker (runs daily)");
    }
}
