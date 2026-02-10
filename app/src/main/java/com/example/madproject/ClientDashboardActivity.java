package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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

public class ClientDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ClientDashboard";

    private TextView tvWelcome, tvUserName, tvViewAllJobs;
    private ImageView btnNotifications;
    private RecyclerView rvMyJobs;
    private LinearLayout emptyState;
    private CardView btnPostJob, cardFindContractors;
    private BottomNavigationView bottomNavigation;
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
        setContentView(R.layout.activity_client_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        Log.d(TAG, "Current User ID: " + currentUserId);

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup click listeners
        setupClickListeners();

        // Load user data and jobs
        loadUserData();
        loadUserJobs();

        // Register FCM token for push notifications
        FCMHelper.registerFCMToken();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh jobs when returning to dashboard
        Log.d(TAG, "onResume - Refreshing jobs");
        loadUserJobs();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserName = findViewById(R.id.tvUserName);
        tvViewAllJobs = findViewById(R.id.tvViewAllJobs);
        btnNotifications = findViewById(R.id.btnNotifications);
        rvMyJobs = findViewById(R.id.rvMyJobs);
        emptyState = findViewById(R.id.emptyState);
        btnPostJob = findViewById(R.id.btnPostJob);
        cardFindContractors = findViewById(R.id.cardFindContractors);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAIChat = findViewById(R.id.fabAIChat);

        // Create ProgressBar programmatically if not in XML
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        // Set home as selected
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        Log.d(TAG, "fabAIChat = " + fabAIChat);
        Log.d(TAG, "btnNotifications = " + btnNotifications);
        Log.d(TAG, "bottomNavigation = " + bottomNavigation);

    }

    private void setupRecyclerView() {
        jobList = new ArrayList<>();
        jobAdapter = new JobAdapter(this, jobList, job -> {
            // Handle job item click - navigate to job details
            Log.d(TAG, "Job clicked: " + job.getJobId());
            Intent intent = new Intent(ClientDashboardActivity.this, JobDetailActivity.class);
            intent.putExtra("jobId", job.getJobId());
            startActivity(intent);
        });

        rvMyJobs.setLayoutManager(new LinearLayoutManager(this));
        rvMyJobs.setAdapter(jobAdapter);
    }

    private void setupClickListeners() {
        // AI Assistant FAB Button
        fabAIChat.setOnClickListener(v -> {
            startActivity(new Intent(ClientDashboardActivity.this, AIChatActivity.class));
        });

        // Notifications Button
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(ClientDashboardActivity.this, NotificationsActivity.class));
        });

        // Post Job Button (CardView)
        btnPostJob.setOnClickListener(v -> {
            startActivity(new Intent(ClientDashboardActivity.this, JobPostActivity.class));
        });

        // Find Contractors Card
        cardFindContractors.setOnClickListener(v -> {
            startActivity(new Intent(ClientDashboardActivity.this, ContractorDirectoryActivity.class));
        });

        // View All Jobs
        tvViewAllJobs.setOnClickListener(v -> {
            Intent intent = new Intent(ClientDashboardActivity.this, MyJobsActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            }
            else if (id == R.id.nav_my_jobs) {
                startActivity(new Intent(ClientDashboardActivity.this, MyJobsActivity.class));
                return true;
            }
            else if (id == R.id.nav_find) {
                startActivity(new Intent(ClientDashboardActivity.this, ContractorDirectoryActivity.class));
                return true;
            } else if (id == R.id.nav_messages) {
                // TODO: Create ConversationsListActivity to show all chats
                Toast.makeText(this, "Access messages from contractor profiles or job details", Toast.LENGTH_LONG).show();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(ClientDashboardActivity.this, SettingsActivity.class));
                return true;
            }

            return false;
        });
    }

    private void loadUserData() {
        if (currentUserId.isEmpty()) {
            Log.e(TAG, "User ID is empty!");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        Log.d(TAG, "Loading user data for: " + currentUserId);

        UserManager.getInstance()
                .getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        Log.d(TAG, "User loaded successfully: " + user.getFullName());
                        currentUser = user;
                        updateUI(user);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error loading user: " + error);
                        Toast.makeText(ClientDashboardActivity.this,
                                "Error loading user data: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(User user) {
        if (user != null) {
            // Update user name
            tvUserName.setText(user.getFullName());

            // Update welcome message based on time
            tvWelcome.setText(getGreeting());

            Log.d(TAG, "UI updated with user: " + user.getFullName());
        }
    }

    private String getGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            return "Good Morning!";
        } else if (hour >= 12 && hour < 17) {
            return "Good Afternoon!";
        } else {
            return "Good Evening!";
        }
    }

    private void loadUserJobs() {
        if (currentUserId.isEmpty()) {
            Log.e(TAG, "Cannot load jobs - User ID is empty");
            return;
        }

        Log.d(TAG, "Loading jobs for client: " + currentUserId);

        // Show loading
        showLoading(true);

        JobManager.getInstance()
                .getJobsByClient(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Jobs query successful. Documents found: " + queryDocumentSnapshots.size());

                    showLoading(false);

                    jobList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Job job = doc.toObject(Job.class);
                        if (job != null) {
                            Log.d(TAG, "Job found: " + job.getTitle() + " (ID: " + job.getJobId() + ")");
                            jobList.add(job);
                        }
                    }

                    // Sort jobs by posted date (newest first) - CLIENT SIDE SORTING
                    Collections.sort(jobList, (j1, j2) ->
                            Long.compare(j2.getPostedDate(), j1.getPostedDate()));

                    Log.d(TAG, "Total jobs loaded: " + jobList.size());

                    // Update adapter
                    jobAdapter.notifyDataSetChanged();

                    // Show/hide empty state
                    if (jobList.isEmpty()) {
                        Log.d(TAG, "No jobs found - showing empty state");
                        rvMyJobs.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "Jobs found - showing RecyclerView");
                        rvMyJobs.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading jobs: " + e.getMessage(), e);
                    Toast.makeText(ClientDashboardActivity.this,
                            "Error loading jobs: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            Log.d(TAG, "Showing loading state");
            rvMyJobs.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            // Show progress indicator
        } else {
            Log.d(TAG, "Hiding loading state");
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ClientDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}