package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.JobAdapter;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.helpers.FCMHelper;
import com.example.madproject.helpers.NameFormatter;
import com.example.madproject.models.Job;
import com.example.madproject.models.User;
import com.example.madproject.views.ShimmerLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContractorDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ContractorDashboard";

    private TextView tvContractorName, tvCategory, tvRating, tvReviews;
    private TextView tvActiveProjectsCount, tvCompletedCount, tvTotalEarnings;
    private TextView tvViewAllJobs;
    private ShimmerLayout shimmerProfile, shimmerStats, shimmerJobs;
    private CircleImageView ivProfileImage;
    private RecyclerView rvAvailableJobs;
    private LinearLayout emptyState;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAIChat;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private User currentUser;

    private JobAdapter jobAdapter;
    private List<Job> jobList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contractor_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        Log.d(TAG, "Current Contractor ID: " + currentUserId);

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup listeners
        setupClickListeners();

        // Load data
        loadContractorData();
        loadAvailableJobs();

        // Register FCM token for push notifications
        FCMHelper.registerFCMToken();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reassert Home as selected: every other nav item/card just launches a
        // separate Activity on top, so returning here always means Home content
        // is what's showing, regardless of which tab was last tapped.
        bottomNav.setSelectedItemId(R.id.nav_home);
        // Refresh contractor stats card (rating/reviews/completed/earnings) — was only
        // ever loaded once in onCreate, so it went stale after a review or job completion.
        loadContractorData();
        // Refresh jobs when returning
        Log.d(TAG, "onResume - Refreshing available jobs");
        loadAvailableJobs();

        // Guard: this screen is always Home, so the nav must always show Home.
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void initViews() {
        tvContractorName = findViewById(R.id.tvContractorName);
        tvCategory = findViewById(R.id.tvCategory);
        tvRating = findViewById(R.id.tvRating);
        tvReviews = findViewById(R.id.tvReviews);
        tvActiveProjectsCount = findViewById(R.id.tvActiveProjectsCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings);
        tvViewAllJobs = findViewById(R.id.tvViewAllJobs);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        rvAvailableJobs = findViewById(R.id.rvAvailableJobs);
        emptyState = findViewById(R.id.emptyState);
        bottomNav = findViewById(R.id.bottomNav);
        fabAIChat = findViewById(R.id.fabAIChat);
        shimmerProfile = findViewById(R.id.shimmerProfile);
        shimmerStats = findViewById(R.id.shimmerStats);
        shimmerJobs = findViewById(R.id.shimmerJobs);

        // Create ProgressBar programmatically
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        // Set home as selected
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void setupRecyclerView() {
        jobList = new ArrayList<>();
        jobAdapter = new JobAdapter(this, jobList, job -> {
            // Handle job item click - navigate to job details
            Log.d(TAG, "Job clicked: " + job.getJobId());
            Intent intent = new Intent(ContractorDashboardActivity.this, JobDetailActivity.class);
            intent.putExtra("jobId", job.getJobId());
            startActivity(intent);
        });

        rvAvailableJobs.setLayoutManager(new LinearLayoutManager(this));
        rvAvailableJobs.setAdapter(jobAdapter);
    }

    private void setupClickListeners() {
        // AI Assistant FAB Button
        fabAIChat.setOnClickListener(v -> {
            startActivity(new Intent(ContractorDashboardActivity.this, AIChatActivity.class));
        });

        // Notifications via toolbar menu
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_notifications) {
                startActivity(new Intent(ContractorDashboardActivity.this, NotificationsActivity.class));
                return true;
            }
            return false;
        });

        // View All Jobs
        tvViewAllJobs.setOnClickListener(v -> {
            Intent intent = new Intent(ContractorDashboardActivity.this, AvailableJobsActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation
        //
        // Every branch that launches another Activity returns FALSE on purpose. This screen is
        // Home, so Home must stay the highlighted tab. Returning true would tell the
        // BottomNavigationView to move the highlight onto the tapped tab, and since these tabs
        // open a new Activity on top (no finish(), no intent flags), backing out would land here
        // with e.g. "Jobs" still lit. Returning false runs the action without moving the
        // highlight, so there is no stale selection to come back to.
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_jobs) {
                startActivity(new Intent(ContractorDashboardActivity.this, AvailableJobsActivity.class));
                return false;
            } else if (id == R.id.nav_projects) {
                startActivity(new Intent(ContractorDashboardActivity.this, MyProjectsActivity.class));
                return false;
            } else if (id == R.id.nav_messages) {
                startActivity(new Intent(ContractorDashboardActivity.this, ConversationsListActivity.class));
                return false;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(ContractorDashboardActivity.this, SettingsActivity.class));
                return false;
            }

            return false;
        });
    }

    private void loadContractorData() {
        if (currentUserId.isEmpty()) {
            Log.e(TAG, "User ID is empty!");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        Log.d(TAG, "Loading contractor data for: " + currentUserId);

        UserManager.getInstance()
                .getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        Log.d(TAG, "Contractor loaded successfully: " + user.getFullName());
                        currentUser = user;
                        updateUI(user);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error loading contractor: " + error);
                        // Never leave the skeleton shimmering forever on failure
                        clearSkeleton(tvContractorName, tvCategory, tvRating, tvReviews);
                        shimmerProfile.hideShimmer();
                        finishStatsSkeleton();
                        Toast.makeText(ContractorDashboardActivity.this,
                                "Error loading contractor data: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(User user) {
        if (user != null && user.isContractor()) {
            // Update contractor name
            tvContractorName.setText(NameFormatter.capitalize(user.getFullName()));

            // Update category
            if (user.getCategory() != null && !user.getCategory().isEmpty()) {
                tvCategory.setText(user.getCategory());
            } else {
                tvCategory.setText("Contractor");
            }

            // Update rating
            if (user.getRating() > 0) {
                tvRating.setText(String.format("%.1f", user.getRating()));
            } else {
                tvRating.setText("New");
            }

            // Update reviews count
            tvReviews.setText("(" + user.getTotalReviews() + " reviews)");

            clearSkeleton(tvContractorName, tvCategory, tvRating, tvReviews);
            shimmerProfile.hideShimmer();

            // Update statistics
            // Active projects (jobs in progress assigned to this contractor)
            loadActiveProjectsCount();

            // Completed projects
            tvCompletedCount.setText(String.valueOf(user.getCompletedProjects()));

            // Total earnings (real, accumulated from completed jobs)
            tvTotalEarnings.setText("Rs. " + formatCurrency(user.getTotalEarnings()));

            Log.d(TAG, "UI updated with contractor: " + user.getFullName());
        } else if (user != null && !user.isContractor()) {
            Log.e(TAG, "User is not a contractor!");
            Toast.makeText(this, "This account is not a contractor", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
    }

    /** Drop the grey placeholder bars once a field has its real value. */
    private void clearSkeleton(TextView... views) {
        for (TextView v : views) {
            v.setBackground(null);
            v.setMinWidth(0);
            v.setMinHeight(0);
        }
    }

    private void finishStatsSkeleton() {
        clearSkeleton(tvActiveProjectsCount, tvCompletedCount, tvTotalEarnings);
        shimmerStats.hideShimmer();
    }

    private void loadActiveProjectsCount() {
        // Count jobs assigned to this contractor with status "in_progress"
        JobManager.getInstance()
                .getJobsByContractor(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int activeCount = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Job job = doc.toObject(Job.class);
                        if (job != null && "in_progress".equals(job.getStatus())) {
                            activeCount++;
                        }
                    }
                    tvActiveProjectsCount.setText(String.valueOf(activeCount));
                    finishStatsSkeleton();
                    Log.d(TAG, "Active projects count: " + activeCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading active projects: " + e.getMessage());
                    tvActiveProjectsCount.setText("0");
                    finishStatsSkeleton();
                });
    }

    private void loadAvailableJobs() {
        Log.d(TAG, "Loading available open jobs");

        // Show loading
        showLoading(true);

        // Load all open jobs (that the contractor can bid on)
        JobManager.getInstance()
                .getOpenJobs()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Open jobs query successful. Documents found: " + queryDocumentSnapshots.size());

                    showLoading(false);

                    jobList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Job job = doc.toObject(Job.class);
                        if (job != null) {
                            Log.d(TAG, "Open job found: " + job.getTitle() + " (ID: " + job.getJobId() + ")");
                            jobList.add(job);
                        }
                    }

                    // Sort jobs by posted date (newest first)
                    Collections.sort(jobList, (j1, j2) ->
                            Long.compare(j2.getPostedDate(), j1.getPostedDate()));

                    Log.d(TAG, "Total open jobs loaded: " + jobList.size());

                    // Update adapter
                    jobAdapter.notifyDataSetChanged();

                    // Show/hide empty state
                    if (jobList.isEmpty()) {
                        Log.d(TAG, "No open jobs found");
                        rvAvailableJobs.setVisibility(View.GONE);
                        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "Open jobs found - showing RecyclerView");
                        rvAvailableJobs.setVisibility(View.VISIBLE);
                        if (emptyState != null) emptyState.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    // Resolve the screen on the error path too, otherwise the skeleton goes away
                    // and leaves nothing behind it.
                    rvAvailableJobs.setVisibility(View.GONE);
                    if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error loading open jobs: " + e.getMessage(), e);
                    Toast.makeText(ContractorDashboardActivity.this,
                            "Error loading jobs: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String formatCurrency(double amount) {
        if (amount >= 10000000) {
            return String.format("%.1f Cr", amount / 10000000);
        } else if (amount >= 100000) {
            return String.format("%.1f L", amount / 100000);
        } else if (amount >= 1000) {
            return String.format("%.1f K", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    /** Drives the jobs-list skeleton. The profile and stats groups have their own shimmers. */
    private void showLoading(boolean show) {
        if (show) {
            shimmerJobs.setVisibility(View.VISIBLE);
            shimmerJobs.showShimmer();
            rvAvailableJobs.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        } else {
            shimmerJobs.hideShimmer();
            shimmerJobs.setVisibility(View.GONE);
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ContractorDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
