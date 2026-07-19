package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.JobAdapter;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.models.Job;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyJobsActivity extends AppCompatActivity {

    private static final String TAG = "MyJobs";

    private Toolbar toolbar;
    private EditText etSearch;
    private TabLayout tabLayout;
    private RecyclerView rvJobs;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String currentUserId;

    private JobAdapter jobAdapter;
    private List<Job> allJobsList;
    private List<Job> filteredJobsList;

    private String currentFilter = "all"; // all, open, in_progress, completed, cancelled

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_jobs);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        initViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupSearchFilter();
        loadMyJobs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh jobs when returning
        loadMyJobs();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        tabLayout = findViewById(R.id.tabLayout);
        rvJobs = findViewById(R.id.rvJob);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("My Jobs");
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Open"));
        tabLayout.addTab(tabLayout.newTab().setText("In Progress"));
        tabLayout.addTab(tabLayout.newTab().setText("Completed"));
        tabLayout.addTab(tabLayout.newTab().setText("Cancelled"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "open";
                        break;
                    case 2:
                        currentFilter = "in_progress";
                        break;
                    case 3:
                        currentFilter = "completed";
                        break;
                    case 4:
                        currentFilter = "cancelled";
                        break;
                }
                filterJobs();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        allJobsList = new ArrayList<>();
        filteredJobsList = new ArrayList<>();

        jobAdapter = new JobAdapter(this, filteredJobsList, job -> {
            // Navigate to job details
            Intent intent = new Intent(MyJobsActivity.this, JobDetailActivity.class);
            intent.putExtra("jobId", job.getJobId());
            startActivity(intent);
        });

        rvJobs.setLayoutManager(new LinearLayoutManager(this));
        rvJobs.setAdapter(jobAdapter);
    }

    private void setupSearchFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterJobs();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadMyJobs() {
        Log.d(TAG, "Loading jobs for client: " + currentUserId);
        showLoading(true);

        // Load all jobs posted by this client
        JobManager.getInstance()
                .getJobsByClient(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Jobs loaded: " + queryDocumentSnapshots.size());
                    showLoading(false);

                    allJobsList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Job job = doc.toObject(Job.class);
                        if (job != null) {
                            allJobsList.add(job);
                        }
                    }

                    // Sort by posted date (newest first)
                    Collections.sort(allJobsList, (j1, j2) ->
                            Long.compare(j2.getPostedDate(), j1.getPostedDate()));

                    // Apply filters
                    filterJobs();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading jobs: " + e.getMessage());
                    Toast.makeText(this, "Error loading jobs: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void filterJobs() {
        String searchQuery = etSearch.getText().toString().toLowerCase().trim();

        filteredJobsList.clear();

        for (Job job : allJobsList) {
            // Apply search filter
            boolean matchesSearch = searchQuery.isEmpty() ||
                    job.getTitle().toLowerCase().contains(searchQuery) ||
                    job.getDescription().toLowerCase().contains(searchQuery) ||
                    job.getCategory().toLowerCase().contains(searchQuery);

            // Apply status filter
            boolean matchesStatus = currentFilter.equals("all") ||
                    job.getStatus().equals(currentFilter);

            if (matchesSearch && matchesStatus) {
                filteredJobsList.add(job);
            }
        }

        jobAdapter.notifyDataSetChanged();

        // Show/hide empty state
        if (filteredJobsList.isEmpty()) {
            rvJobs.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvJobs.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        Log.d(TAG, "Filtered jobs (" + currentFilter + "): " + filteredJobsList.size());
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvJobs.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}