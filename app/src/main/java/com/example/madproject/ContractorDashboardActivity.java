package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.example.madproject.models.Job;
import com.example.madproject.models.User;
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
    private ImageView btnNotifications;
    private CircleImageView ivProfileImage;
    private Button btnViewProfile;
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
        // Refresh jobs when returning
        Log.d(TAG, "onResume - Refreshing available jobs");
        loadAvailableJobs();
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
        btnNotifications = findViewById(R.id.btnNotifications);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        btnViewProfile = findViewById(R.id.btnViewProfile);
        rvAvailableJobs = findViewById(R.id.rvAvailableJobs);
        bottomNav = findViewById(R.id.bottomNav);
        fabAIChat = findViewById(R.id.fabAIChat);

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

        // Notifications Button
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(ContractorDashboardActivity.this, NotificationsActivity.class));
        });

        // View Profile Button
        btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ContractorDashboardActivity.this, ContractorProfileActivity.class);
            intent.putExtra("contractorId", currentUserId);
            startActivity(intent);
        });

        // View All Jobs
        tvViewAllJobs.setOnClickListener(v -> {
            Intent intent = new Intent(ContractorDashboardActivity.this, AvailableJobsActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_jobs) {
                startActivity(new Intent(ContractorDashboardActivity.this, AvailableJobsActivity.class));
                return true;
            } else if (id == R.id.nav_projects) {
                startActivity(new Intent(ContractorDashboardActivity.this, MyProjectsActivity.class));
                return true;
            } else if (id == R.id.nav_messages) {
                // TODO: Create ConversationsListActivity to show all chats
                Toast.makeText(this, "Access messages from contractor profiles or job details", Toast.LENGTH_LONG).show();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(ContractorDashboardActivity.this, SettingsActivity.class));
                return true;
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
                        Toast.makeText(ContractorDashboardActivity.this,
                                "Error loading contractor data: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(User user) {
        if (user != null && user.isContractor()) {
            // Update contractor name
            tvContractorName.setText(user.getFullName());

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

            // Update statistics
            // Active projects (jobs in progress assigned to this contractor)
            loadActiveProjectsCount();

            // Completed projects
            tvCompletedCount.setText(String.valueOf(user.getCompletedProjects()));

            // Total earnings (calculate from hourly rate * completed projects as estimate)
            double estimatedEarnings = user.getHourlyRate() * user.getCompletedProjects() * 40; // Estimate
            tvTotalEarnings.setText("Rs. " + formatCurrency(estimatedEarnings));

            Log.d(TAG, "UI updated with contractor: " + user.getFullName());
        } else if (user != null && !user.isContractor()) {
            Log.e(TAG, "User is not a contractor!");
            Toast.makeText(this, "This account is not a contractor", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
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
                    Log.d(TAG, "Active projects count: " + activeCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading active projects: " + e.getMessage());
                    tvActiveProjectsCount.setText("0");
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
                        // Show empty state if you have it
                    } else {
                        Log.d(TAG, "Open jobs found - showing RecyclerView");
                        rvAvailableJobs.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
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

    private void showLoading(boolean show) {
        if (show) {
            Log.d(TAG, "Showing loading state");
            // Show progress indicator
        } else {
            Log.d(TAG, "Hiding loading state");
            // Hide progress indicator
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ContractorDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}