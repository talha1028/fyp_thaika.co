package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.JobAdapter;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.models.Job;
import com.example.madproject.views.ShimmerLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyProjectsActivity extends AppCompatActivity {

    private static final String TAG = "MyProjects";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView rvProjects;
    private LinearLayout emptyState;
    private ShimmerLayout shimmerProjects;

    /** True while a fetch is in flight - the skeleton owns the screen until it resolves. */
    private boolean isLoading = true;

    private FirebaseAuth mAuth;
    private String currentUserId;

    private JobAdapter jobAdapter;
    private List<Job> allProjectsList;
    private List<Job> filteredProjectsList;

    private String currentFilter = "all"; // all, in_progress, completed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        initViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        loadMyProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh on return - a project's status/payment state may have changed elsewhere
        // (JobDetailActivity) while this screen stayed alive on the back stack.
        loadMyProjects();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        rvProjects = findViewById(R.id.rvProjects);
        emptyState = findViewById(R.id.emptyState);
        shimmerProjects = findViewById(R.id.shimmerProjects);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Projects");
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("In Progress"));
        tabLayout.addTab(tabLayout.newTab().setText("Completed"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "in_progress";
                        break;
                    case 2:
                        currentFilter = "completed";
                        break;
                }
                filterProjects();
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
        allProjectsList = new ArrayList<>();
        filteredProjectsList = new ArrayList<>();

        jobAdapter = new JobAdapter(this, filteredProjectsList, job -> {
            // Navigate to job details
            Intent intent = new Intent(MyProjectsActivity.this, JobDetailActivity.class);
            intent.putExtra("jobId", job.getJobId());
            startActivity(intent);
        });

        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(jobAdapter);
    }

    private void loadMyProjects() {
        Log.d(TAG, "Loading projects for contractor: " + currentUserId);
        showLoading(true);

        // Load all jobs assigned to this contractor
        JobManager.getInstance()
                .getJobsByContractor(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Projects loaded: " + queryDocumentSnapshots.size());
                    showLoading(false);

                    allProjectsList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Job job = doc.toObject(Job.class);
                        if (job != null) {
                            allProjectsList.add(job);
                        }
                    }

                    // Sort by start date (most recent first)
                    Collections.sort(allProjectsList, (j1, j2) -> {
                        long date1 = j1.getStartDate() != 0 ? j1.getStartDate() : j1.getPostedDate();
                        long date2 = j2.getStartDate() != 0 ? j2.getStartDate() : j2.getPostedDate();
                        return Long.compare(date2, date1);
                    });

                    // Apply filter
                    filterProjects();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    // Resolve the screen on the error path too, otherwise the skeleton goes away
                    // and leaves nothing behind it.
                    filterProjects();
                    Log.e(TAG, "Error loading projects: " + e.getMessage());
                    Toast.makeText(this, "Error loading projects: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void filterProjects() {
        filteredProjectsList.clear();

        for (Job job : allProjectsList) {
            if (currentFilter.equals("all")) {
                filteredProjectsList.add(job);
            } else if (job.getStatus().equals(currentFilter)) {
                filteredProjectsList.add(job);
            }
        }

        jobAdapter.notifyDataSetChanged();

        if (isLoading) {
            // The skeleton owns the screen until the fetch resolves; an empty list here just
            // means the data has not arrived yet, not that there is nothing to show.
            return;
        }

        // Show/hide empty state
        if (filteredProjectsList.isEmpty()) {
            rvProjects.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvProjects.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        Log.d(TAG, "Filtered projects (" + currentFilter + "): " + filteredProjectsList.size());
    }

    private void showLoading(boolean show) {
        isLoading = show;
        if (show) {
            shimmerProjects.setVisibility(View.VISIBLE);
            shimmerProjects.showShimmer();
            rvProjects.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            shimmerProjects.hideShimmer();
            shimmerProjects.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}