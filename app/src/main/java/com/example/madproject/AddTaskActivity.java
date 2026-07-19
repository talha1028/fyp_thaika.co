package com.example.madproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.madproject.firebase.TaskManager;
import com.example.madproject.helpers.GeminiAIHelper;
import com.example.madproject.models.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public class AddTaskActivity extends AppCompatActivity {

    private EditText etTaskTitle, etWorkersCount, etTaskDescription, etEstimatedQuantity, etDailyWages;
    private Spinner spinnerProgressUnit;
    private Button btnCreateTask, btnCancel;
    private android.widget.TextView btnAiTimeline;
    private GeminiAIHelper aiHelper;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String jobId;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

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
        setupClickListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");

        etTaskTitle = findViewById(R.id.etTaskTitle);
        etWorkersCount = findViewById(R.id.etWorkersCount);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        etEstimatedQuantity = findViewById(R.id.etEstimatedQuantity);
        etDailyWages = findViewById(R.id.etDailyWages);
        spinnerProgressUnit = findViewById(R.id.spinnerProgressUnit);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        btnCancel = findViewById(R.id.btnCancel);
        btnAiTimeline = findViewById(R.id.btnAiTimeline);
        aiHelper = new GeminiAIHelper(this);
    }

    private void setupClickListeners() {
        btnCreateTask.setOnClickListener(v -> createTask());
        btnCancel.setOnClickListener(v -> finish());

        if (btnAiTimeline != null) {
            btnAiTimeline.setOnClickListener(v -> getAiTimelineEstimate());
        }
    }

    private void getAiTimelineEstimate() {
        String title = etTaskTitle.getText().toString().trim();
        String desc = etTaskDescription.getText().toString().trim();
        String workers = etWorkersCount.getText().toString().trim();
        String qty = etEstimatedQuantity.getText().toString().trim();
        String unit = spinnerProgressUnit.getSelectedItem() != null ?
                spinnerProgressUnit.getSelectedItem().toString() : "";

        if (title.isEmpty()) {
            Toast.makeText(this, "Enter task title first", Toast.LENGTH_SHORT).show();
            return;
        }

        String projectInfo = title;
        if (!desc.isEmpty()) projectInfo += ". " + desc;
        if (!workers.isEmpty()) projectInfo += ". Workers: " + workers;
        if (!qty.isEmpty() && !unit.isEmpty()) projectInfo += ". Quantity: " + qty + " " + unit;

        btnAiTimeline.setEnabled(false);
        btnAiTimeline.setText("Estimating...");

        aiHelper.getTimelineEstimate(projectInfo, new GeminiAIHelper.AIResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    btnAiTimeline.setEnabled(true);
                    btnAiTimeline.setText("⏱ AI Estimate");
                    new androidx.appcompat.app.AlertDialog.Builder(AddTaskActivity.this)
                            .setTitle("Timeline Estimate")
                            .setMessage(response.trim())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnAiTimeline.setEnabled(true);
                    btnAiTimeline.setText("⏱ AI Estimate");
                    Toast.makeText(AddTaskActivity.this, "AI error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void createTask() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get values from UI
        String taskTitle = etTaskTitle.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();
        int workersCount = Integer.parseInt(etWorkersCount.getText().toString().trim());
        double estimatedQuantity = Double.parseDouble(etEstimatedQuantity.getText().toString().trim());
        double dailyWages = Double.parseDouble(etDailyWages.getText().toString().trim());
        String progressUnit = spinnerProgressUnit.getSelectedItem().toString();

        // Create Task object
        String taskId = "task_" + UUID.randomUUID().toString();
        Task task = new Task(taskId, jobId, projectName, taskTitle, description, "TBD", workersCount);
        task.setEstimatedQuantity(estimatedQuantity);
        task.setDailyWages(dailyWages);
        task.setProgressUnit(progressUnit);
        task.setCreatedBy(currentUserId);
        task.setStatus("not_started");

        // Show loading
        showLoading(true);

        // Save to Firebase
        TaskManager.getInstance()
                .createTask(task)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Task created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error creating task: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInputs() {
        // Validate task title
        if (TextUtils.isEmpty(etTaskTitle.getText().toString())) {
            etTaskTitle.setError("Task title is required");
            etTaskTitle.requestFocus();
            return false;
        }

        if (etTaskTitle.getText().toString().trim().length() < 5) {
            etTaskTitle.setError("Task title must be at least 5 characters");
            etTaskTitle.requestFocus();
            return false;
        }

        // Validate workers count
        if (TextUtils.isEmpty(etWorkersCount.getText().toString())) {
            etWorkersCount.setError("Workers count is required");
            etWorkersCount.requestFocus();
            return false;
        }

        try {
            int workersCount = Integer.parseInt(etWorkersCount.getText().toString().trim());
            if (workersCount <= 0) {
                etWorkersCount.setError("Workers count must be greater than 0");
                etWorkersCount.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etWorkersCount.setError("Invalid number");
            etWorkersCount.requestFocus();
            return false;
        }

        // Validate estimated quantity
        if (TextUtils.isEmpty(etEstimatedQuantity.getText().toString())) {
            etEstimatedQuantity.setError("Estimated quantity is required");
            etEstimatedQuantity.requestFocus();
            return false;
        }

        try {
            double quantity = Double.parseDouble(etEstimatedQuantity.getText().toString().trim());
            if (quantity <= 0) {
                etEstimatedQuantity.setError("Quantity must be greater than 0");
                etEstimatedQuantity.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etEstimatedQuantity.setError("Invalid number");
            etEstimatedQuantity.requestFocus();
            return false;
        }

        // Validate daily wages
        if (TextUtils.isEmpty(etDailyWages.getText().toString())) {
            etDailyWages.setError("Daily wages is required");
            etDailyWages.requestFocus();
            return false;
        }

        try {
            double wages = Double.parseDouble(etDailyWages.getText().toString().trim());
            if (wages <= 0) {
                etDailyWages.setError("Wages must be greater than 0");
                etDailyWages.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etDailyWages.setError("Invalid number");
            etDailyWages.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        if (show) {
            btnCreateTask.setEnabled(false);
            btnCreateTask.setText("Creating...");
            btnCancel.setEnabled(false);
            etTaskTitle.setEnabled(false);
            etWorkersCount.setEnabled(false);
            etTaskDescription.setEnabled(false);
            etEstimatedQuantity.setEnabled(false);
            etDailyWages.setEnabled(false);
            spinnerProgressUnit.setEnabled(false);
        } else {
            btnCreateTask.setEnabled(true);
            btnCreateTask.setText("Create Task");
            btnCancel.setEnabled(true);
            etTaskTitle.setEnabled(true);
            etWorkersCount.setEnabled(true);
            etTaskDescription.setEnabled(true);
            etEstimatedQuantity.setEnabled(true);
            etDailyWages.setEnabled(true);
            spinnerProgressUnit.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}


