
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.TaskAdapter;
import com.example.madproject.firebase.TaskManager;
import com.example.madproject.helpers.GeminiAIHelper;
import com.example.madproject.models.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskListActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private FloatingActionButton fabAddTask;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView btnAiSiteReport;
    private TextView tvProjectName, tvSelectedDate, tvNotStartedCount, tvOngoingCount, tvCompletedCount;
    private GeminiAIHelper aiHelper;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String jobId;
    private String projectName;

    private TaskAdapter taskAdapter;
    private List<Task> allTasksList;
    private List<Task> filteredTasksList;
    private String currentFilter = "all"; // all, ongoing, completed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get jobId and projectName from Intent
        jobId = getIntent().getStringExtra("jobId");
        projectName = getIntent().getStringExtra("projectName");

        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(this, "Error: Job ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupTabs();
        setupRecyclerView();
        setupClickListeners();
        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks when returning to this activity
        loadTasks();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");

        rvTasks = findViewById(R.id.rvTasks);
        fabAddTask = findViewById(R.id.fabAddTask);
        tabLayout = findViewById(R.id.tabLayout);

        // Try to find ProgressBar and empty state, create if not in layout
        progressBar = findViewById(R.id.progressBar);
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }

        emptyState = findViewById(R.id.emptyState);
        if (emptyState == null) {
            emptyState = new LinearLayout(this);
            emptyState.setVisibility(View.GONE);
        }

        btnAiSiteReport   = findViewById(R.id.btnAiSiteReport);
        tvProjectName     = findViewById(R.id.tvProjectName);
        tvSelectedDate    = findViewById(R.id.tvSelectedDate);
        tvNotStartedCount = findViewById(R.id.tvNotStartedCount);
        tvOngoingCount    = findViewById(R.id.tvOngoingCount);
        tvCompletedCount  = findViewById(R.id.tvCompletedCount);
        aiHelper = new GeminiAIHelper(this);

        // Set project name from intent
        if (tvProjectName != null) {
            tvProjectName.setText(projectName != null && !projectName.isEmpty() ? projectName : "Project Tasks");
        }
        // Set today's date
        if (tvSelectedDate != null) {
            tvSelectedDate.setText(new SimpleDateFormat("dd MMM yyyy, EEE", Locale.getDefault()).format(new Date()));
        }

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Ongoing"));
        tabLayout.addTab(tabLayout.newTab().setText("Completed"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "ongoing";
                        break;
                    case 2:
                        currentFilter = "completed";
                        break;
                }
                filterTasks();
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
        allTasksList = new ArrayList<>();
        filteredTasksList = new ArrayList<>();

        taskAdapter = new TaskAdapter(this, filteredTasksList, task -> {
            // Navigate to task detail
            Intent intent = new Intent(TaskListActivity.this, TaskDetailActivity.class);
            intent.putExtra("taskId", task.getTaskId());
            startActivity(intent);
        });

        rvTasks.setAdapter(taskAdapter);
    }

    private void setupClickListeners() {
        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTaskActivity.class);
            intent.putExtra("jobId", jobId);
            intent.putExtra("projectName", projectName);
            startActivity(intent);
        });

        if (btnAiSiteReport != null) {
            btnAiSiteReport.setOnClickListener(v -> generateSiteReport());
        }
    }

    private void generateSiteReport() {
        if (allTasksList.isEmpty()) {
            Toast.makeText(this, "No tasks loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        int total = allTasksList.size();
        int completed = 0, ongoing = 0, notStarted = 0;
        for (Task t : allTasksList) {
            String s = t.getStatus();
            if ("completed".equals(s)) completed++;
            else if ("ongoing".equals(s)) ongoing++;
            else notStarted++;
        }

        btnAiSiteReport.setEnabled(false);
        btnAiSiteReport.setText("Generating...");

        String name = projectName != null ? projectName : "Project";
        int finalCompleted = completed, finalOngoing = ongoing, finalNotStarted = notStarted;

        aiHelper.generateProgressReport(name, total, finalCompleted, finalOngoing, finalNotStarted,
                new GeminiAIHelper.AIResponseListener() {
                    @Override
                    public void onResponse(String response) {
                        runOnUiThread(() -> {
                            btnAiSiteReport.setEnabled(true);
                            btnAiSiteReport.setText("📊 AI Report");

                            android.widget.ScrollView sv = new android.widget.ScrollView(TaskListActivity.this);
                            android.widget.TextView tv = new android.widget.TextView(TaskListActivity.this);
                            tv.setText(response.trim());
                            tv.setTextSize(13f);
                            tv.setTextColor(0xFF212121);
                            tv.setPadding(48, 32, 48, 32);
                            tv.setLineSpacing(4f, 1f);
                            sv.addView(tv);

                            new androidx.appcompat.app.AlertDialog.Builder(TaskListActivity.this)
                                    .setTitle("📊 Site Progress Report — " + name)
                                    .setView(sv)
                                    .setPositiveButton("Share", (d, w) -> {
                                        Intent share = new Intent(Intent.ACTION_SEND);
                                        share.setType("text/plain");
                                        share.putExtra(Intent.EXTRA_SUBJECT, "Site Report — " + name);
                                        share.putExtra(Intent.EXTRA_TEXT, response);
                                        startActivity(Intent.createChooser(share, "Share Report"));
                                    })
                                    .setNegativeButton("Close", null)
                                    .show();
                        });
                    }
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnAiSiteReport.setEnabled(true);
                            btnAiSiteReport.setText("📊 AI Report");
                            Toast.makeText(TaskListActivity.this, "AI error: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void loadTasks() {
        showLoading(true);

        TaskManager.getInstance()
                .getTasksByJob(jobId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    allTasksList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            allTasksList.add(task);
                        }
                    }

                    // Sort by updated date (newest first)
                    Collections.sort(allTasksList, (t1, t2) ->
                            Long.compare(t2.getUpdatedAt(), t1.getUpdatedAt()));

                    // Apply filter
                    filterTasks();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading tasks: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void filterTasks() {
        filteredTasksList.clear();

        int notStarted = 0, ongoing = 0, completed = 0;
        for (Task task : allTasksList) {
            String s = task.getStatus();
            if ("completed".equals(s)) completed++;
            else if ("ongoing".equals(s)) ongoing++;
            else notStarted++;

            if (currentFilter.equals("all") || s.equals(currentFilter)) {
                filteredTasksList.add(task);
            }
        }

        if (tvNotStartedCount != null) tvNotStartedCount.setText(String.valueOf(notStarted));
        if (tvOngoingCount    != null) tvOngoingCount.setText(String.valueOf(ongoing));
        if (tvCompletedCount  != null) tvCompletedCount.setText(String.valueOf(completed));

        taskAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (filteredTasksList.isEmpty()) {
            rvTasks.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvTasks.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiHelper != null) aiHelper.shutdown();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
