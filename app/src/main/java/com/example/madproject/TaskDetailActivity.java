package com.example.madproject;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.madproject.firebase.TaskManager;
import com.example.madproject.helpers.GeminiAIHelper;
import com.example.madproject.models.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {

    private TextView tvTaskTitle, tvTaskDescription, tvStartDate, tvEndDate, tvWorkers, tvDailyWages;
    private Button btnUpdateProgress, btnMarkComplete;
    private TextView btnAiSafetyTips;
    private ProgressBar progressBar;
    private GeminiAIHelper aiHelper;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String taskId;
    private Task currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get taskId from Intent
        taskId = getIntent().getStringExtra("taskId");

        if (taskId == null || taskId.isEmpty()) {
            Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        loadTaskDetails();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");

        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        tvTaskDescription = findViewById(R.id.tvTaskDescription);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvWorkers = findViewById(R.id.tvWorkers);
        tvDailyWages = findViewById(R.id.tvDailyWages);
        btnUpdateProgress = findViewById(R.id.btnUpdateProgress);
        btnMarkComplete = findViewById(R.id.btnMarkComplete);
        btnAiSafetyTips = findViewById(R.id.btnAiSafetyTips);

        progressBar = findViewById(R.id.progressBar);
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }

        aiHelper = new GeminiAIHelper(this);
    }

    private void setupClickListeners() {
        btnUpdateProgress.setOnClickListener(v -> showUpdateProgressDialog());
        btnMarkComplete.setOnClickListener(v -> markTaskComplete());

        if (btnAiSafetyTips != null) {
            btnAiSafetyTips.setOnClickListener(v -> showAiSafetyTips());
        }
    }

    private void showAiSafetyTips() {
        String taskTitle = currentTask != null ? currentTask.getTaskTitle() : "";
        String taskDesc = currentTask != null && currentTask.getDescription() != null ?
                currentTask.getDescription() : "";
        String workType = !taskTitle.isEmpty() ? taskTitle : taskDesc;

        if (workType.isEmpty()) {
            Toast.makeText(this, "Load task first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAiSafetyTips.setEnabled(false);
        btnAiSafetyTips.setText("Loading...");

        aiHelper.getSafetyTips(workType, new GeminiAIHelper.AIResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    btnAiSafetyTips.setEnabled(true);
                    btnAiSafetyTips.setText("⚠️ AI Safety Tips");
                    new AlertDialog.Builder(TaskDetailActivity.this)
                            .setTitle("Safety Tips — " + workType)
                            .setMessage(response.trim())
                            .setPositiveButton("Got it", null)
                            .show();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnAiSafetyTips.setEnabled(true);
                    btnAiSafetyTips.setText("⚠️ AI Safety Tips");
                    Toast.makeText(TaskDetailActivity.this, "AI error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadTaskDetails() {
        showLoading(true);

        TaskManager.getInstance()
                .getTask(taskId)
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);

                    if (documentSnapshot.exists()) {
                        currentTask = documentSnapshot.toObject(Task.class);
                        if (currentTask != null) {
                            displayTaskDetails(currentTask);
                        }
                    } else {
                        Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading task: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void displayTaskDetails(Task task) {
        // Set task title
        tvTaskTitle.setText(task.getTaskTitle());

        // Set task description
        tvTaskDescription.setText(task.getDescription());

        // Set start date
        if (task.getStartDate() != 0) {
            tvStartDate.setText(formatDate(task.getStartDate()));
        } else {
            tvStartDate.setText("Not started");
        }

        // Set end date
        if (task.getEndDate() != 0) {
            tvEndDate.setText(formatDate(task.getEndDate()));
        } else {
            tvEndDate.setText("Not set");
        }

        // Set workers
        tvWorkers.setText(task.getNumberOfWorkers() + " workers");

        // Set daily wages
        tvDailyWages.setText("Rs. " + formatCurrency(task.getDailyWages()) + " /day");

        // Hide complete button if already completed
        if ("completed".equals(task.getStatus())) {
            btnMarkComplete.setVisibility(View.GONE);
            btnUpdateProgress.setVisibility(View.GONE);
        }
    }

    private void showUpdateProgressDialog() {
        if (currentTask == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Progress");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Completed quantity (" + currentTask.getProgressUnit() + ")");
        input.setText(String.valueOf(currentTask.getCompletedQuantity()));
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String quantityStr = input.getText().toString().trim();
            if (!quantityStr.isEmpty()) {
                try {
                    double completedQuantity = Double.parseDouble(quantityStr);
                    updateProgress(completedQuantity);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateProgress(double completedQuantity) {
        showLoading(true);

        TaskManager.getInstance()
                .updateProgress(taskId, completedQuantity)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Progress updated successfully!", Toast.LENGTH_SHORT).show();
                    loadTaskDetails(); // Reload to show updated data
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error updating progress: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void markTaskComplete() {
        new AlertDialog.Builder(this)
                .setTitle("Mark Task Complete")
                .setMessage("Are you sure you want to mark this task as completed?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    showLoading(true);

                    TaskManager.getInstance()
                            .completeTask(taskId)
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                Toast.makeText(this, "Task marked as completed!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) {
            return "N/A";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
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
            progressBar.setVisibility(View.VISIBLE);
            btnUpdateProgress.setEnabled(false);
            btnMarkComplete.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnUpdateProgress.setEnabled(true);
            btnMarkComplete.setEnabled(true);
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
