# MadProject - Construction Marketplace App
## Complete Code Documentation

**Project Name:** MadProject
**Type:** Android Application (Construction/Contractor Marketplace)
**Package:** com.example.madproject
**Firebase Enabled:** Yes (Authentication, Firestore, Cloud Messaging)
**AI Integration:** Google Gemini AI

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Build Configuration](#build-configuration)
3. [Activities](#activities)
4. [Firebase Managers](#firebase-managers)
5. [Adapters](#adapters)
6. [Models](#models)
7. [Helpers](#helpers)
8. [XML Layouts](#xml-layouts)
9. [Android Manifest](#android-manifest)

---

## Project Structure

```
app/src/main/java/com/example/madproject/
├── Activities (Main UI components)
├── firebase/ (Firebase integration managers)
├── adapters/ (RecyclerView adapters)
├── models/ (Data models)
└── helpers/ (Utility classes)
```

---

## Build Configuration

### build.gradle.kts (App Module)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.madproject"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.madproject"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
    implementation("com.google.guava:guava:31.1-android")
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    // OkHttp for Gemini API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
}
```

---

*This is the start of the documentation. The complete file is being generated...*


## Activities

### AIChatActivity.java

```java
package com.example.madproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.ChatMessageAdapter;
import com.example.madproject.helpers.GeminiAIHelper;
import com.example.madproject.models.ChatMessage;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity {

    private static final String TAG = "AIChat";

    private Toolbar toolbar;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private HorizontalScrollView suggestionsContainer;
    private ChipGroup chipGroupSuggestions;
    private LinearLayout emptyState;

    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList;
    private GeminiAIHelper aiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSuggestions();
        setupClickListeners();

        // Initialize AI Helper
        aiHelper = new GeminiAIHelper(this);

        // Show welcome message
        showWelcomeMessage();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
        suggestionsContainer = findViewById(R.id.suggestionsContainer);
        chipGroupSuggestions = findViewById(R.id.chipGroupSuggestions);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Assistant");
        }
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(this, messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupSuggestions() {
        String[] suggestions = {
                "💰 Cost estimate",
                "⏱️ Timeline help",
                "🏗️ Materials needed",
                "⚠️ Safety tips",
                "✍️ Write job description"
        };

        for (String suggestion : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(suggestion);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> handleSuggestionClick(suggestion));
            chipGroupSuggestions.addView(chip);
        }
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void showWelcomeMessage() {
        String welcomeText = "👋 Hello! I'm your AI construction assistant.\n\n" +
                "I can help you with:\n" +
                "• Cost estimates (in PKR)\n" +
                "• Project timelines\n" +
                "• Material recommendations\n" +
                "• Contractor suggestions\n" +
                "• Safety guidelines\n" +
                "• Job description writing\n\n" +
                "How can I help you today?";

        ChatMessage welcomeMessage = new ChatMessage(welcomeText, false, System.currentTimeMillis());
        addMessage(welcomeMessage);
    }

    private void handleSuggestionClick(String suggestion) {
        String prompt = "";

        if (suggestion.contains("Cost estimate")) {
            prompt = "I need a cost estimate for my construction project";
        } else if (suggestion.contains("Timeline")) {
            prompt = "How long will my project take?";
        } else if (suggestion.contains("Materials")) {
            prompt = "What materials do I need?";
        } else if (suggestion.contains("Safety")) {
            prompt = "What safety precautions should I take?";
        } else if (suggestion.contains("job description")) {
            prompt = "Help me write a job description";
        }

        etMessage.setText(prompt);
        sendMessage();
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            return;
        }

        // Hide suggestions and empty state after first message
        if (suggestionsContainer.getVisibility() == View.VISIBLE) {
            suggestionsContainer.setVisibility(View.GONE);
        }
        if (emptyState.getVisibility() == View.VISIBLE) {
            emptyState.setVisibility(View.GONE);
        }

        // Add user message
        ChatMessage userMessage = new ChatMessage(message, true, System.currentTimeMillis());
        addMessage(userMessage);

        // Clear input
        etMessage.setText("");

        // Show loading
        showLoading(true);

        // Get AI response
        aiHelper.sendMessage(message, new GeminiAIHelper.AIResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    showLoading(false);
                    ChatMessage aiMessage = new ChatMessage(response, false, System.currentTimeMillis());
                    addMessage(aiMessage);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    String errorMessage = "Sorry, I encountered an error. Please try again.\n\n" +
                            "Error: " + error + "\n\n" +
                            "Make sure you have:\n" +
                            "1. Added your Gemini API key\n" +
                            "2. Added Gemini dependencies to build.gradle\n" +
                            "3. Internet connection";
                    ChatMessage aiMessage = new ChatMessage(errorMessage, false, System.currentTimeMillis());
                    addMessage(aiMessage);
                });
            }
        });
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.smoothScrollToPosition(messageList.size() - 1);
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnSend.setEnabled(false);
            etMessage.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSend.setEnabled(true);
            etMessage.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}```

---

### AddMaterialActivity.java

```java
package com.example.madproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.madproject.firebase.MaterialManager;
import com.example.madproject.models.Material;
import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public class AddMaterialActivity extends AppCompatActivity {

    private EditText etMaterialName, etQuantity, etUnitPrice, etSupplier, etDescription;
    private Spinner spinnerCategory, spinnerUnit;
    private Button btnSaveMaterial, btnCancel;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String jobId;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_material);

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

        etMaterialName = findViewById(R.id.etMaterialName);
        etQuantity = findViewById(R.id.etQuantity);
        etUnitPrice = findViewById(R.id.etUnitPrice);
        etSupplier = findViewById(R.id.etSupplier);
        etDescription = findViewById(R.id.etDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        btnSaveMaterial = findViewById(R.id.btnSaveMaterial);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupClickListeners() {
        btnSaveMaterial.setOnClickListener(v -> saveMaterial());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveMaterial() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get values from UI
        String materialName = etMaterialName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        double quantity = Double.parseDouble(etQuantity.getText().toString().trim());
        String unit = spinnerUnit.getSelectedItem().toString();
        double unitPrice = Double.parseDouble(etUnitPrice.getText().toString().trim());
        String supplier = etSupplier.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Create Material object
        String materialId = "material_" + UUID.randomUUID().toString();
        Material material = new Material(materialId, jobId, projectName, materialName,
                                       category, quantity, unit, unitPrice, supplier);
        material.setDescription(description);
        material.setAddedBy(currentUserId);

        // Show loading
        showLoading(true);

        // Save to Firebase
        MaterialManager.getInstance()
                .createMaterial(material)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Material saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error saving material: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInputs() {
        // Validate material name
        if (TextUtils.isEmpty(etMaterialName.getText().toString())) {
            etMaterialName.setError("Material name is required");
            etMaterialName.requestFocus();
            return false;
        }

        // Validate quantity
        if (TextUtils.isEmpty(etQuantity.getText().toString())) {
            etQuantity.setError("Quantity is required");
            etQuantity.requestFocus();
            return false;
        }

        try {
            double quantity = Double.parseDouble(etQuantity.getText().toString().trim());
            if (quantity <= 0) {
                etQuantity.setError("Quantity must be greater than 0");
                etQuantity.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etQuantity.setError("Invalid number");
            etQuantity.requestFocus();
            return false;
        }

        // Validate unit price
        if (TextUtils.isEmpty(etUnitPrice.getText().toString())) {
            etUnitPrice.setError("Unit price is required");
            etUnitPrice.requestFocus();
            return false;
        }

        try {
            double unitPrice = Double.parseDouble(etUnitPrice.getText().toString().trim());
            if (unitPrice < 0) {
                etUnitPrice.setError("Unit price cannot be negative");
                etUnitPrice.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etUnitPrice.setError("Invalid number");
            etUnitPrice.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        if (show) {
            btnSaveMaterial.setEnabled(false);
            btnSaveMaterial.setText("Saving...");
            btnCancel.setEnabled(false);
            etMaterialName.setEnabled(false);
            etQuantity.setEnabled(false);
            etUnitPrice.setEnabled(false);
            etSupplier.setEnabled(false);
            etDescription.setEnabled(false);
            spinnerCategory.setEnabled(false);
            spinnerUnit.setEnabled(false);
        } else {
            btnSaveMaterial.setEnabled(true);
            btnSaveMaterial.setText("Save Material");
            btnCancel.setEnabled(true);
            etMaterialName.setEnabled(true);
            etQuantity.setEnabled(true);
            etUnitPrice.setEnabled(true);
            etSupplier.setEnabled(true);
            etDescription.setEnabled(true);
            spinnerCategory.setEnabled(true);
            spinnerUnit.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}```

---

### AddTaskActivity.java

```java
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
import com.example.madproject.models.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public class AddTaskActivity extends AppCompatActivity {

    private EditText etTaskTitle, etWorkersCount, etTaskDescription, etEstimatedQuantity, etDailyWages;
    private Spinner spinnerProgressUnit;
    private Button btnCreateTask, btnCancel;

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

        etTaskTitle = findViewById(R.id.etTaskTitle);
        etWorkersCount = findViewById(R.id.etWorkersCount);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        etEstimatedQuantity = findViewById(R.id.etEstimatedQuantity);
        etDailyWages = findViewById(R.id.etDailyWages);
        spinnerProgressUnit = findViewById(R.id.spinnerProgressUnit);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupClickListeners() {
        btnCreateTask.setOnClickListener(v -> createTask());
        btnCancel.setOnClickListener(v -> finish());
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


```

---

### AllReviewsActivity.java

```java
package com.example.madproject;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.ReviewAdapter;
import com.example.madproject.firebase.ReviewManager;
import com.example.madproject.models.Review;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllReviewsActivity extends AppCompatActivity {

    private static final String TAG = "AllReviewsActivity";

    private RecyclerView rvReviews;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView tvAverageRating, tvTotalReviews;

    private String contractorId;
    private String contractorName;

    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);

        // Get contractor ID from intent
        contractorId = getIntent().getStringExtra("contractorId");
        contractorName = getIntent().getStringExtra("contractorName");

        if (contractorId == null || contractorId.isEmpty()) {
            Toast.makeText(this, "Error: Contractor not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        loadReviews();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (contractorName != null && !contractorName.isEmpty()) {
                getSupportActionBar().setTitle(contractorName + "'s Reviews");
            } else {
                getSupportActionBar().setTitle("Reviews");
            }
        }

        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRecyclerView() {
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        rvReviews.setAdapter(reviewAdapter);
    }

    private void loadReviews() {
        showLoading(true);

        ReviewManager.getInstance()
                .getReviewsByContractor(contractorId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);

                    reviewList.clear();

                    float totalRating = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            reviewList.add(review);
                            totalRating += review.getRating();
                            count++;
                        }
                    }

                    // Sort by date (newest first)
                    Collections.sort(reviewList, (r1, r2) ->
                            Long.compare(r2.getReviewDate(), r1.getReviewDate()));

                    reviewAdapter.notifyDataSetChanged();
                    updateEmptyState();

                    // Update summary
                    if (tvTotalReviews != null) {
                        tvTotalReviews.setText(count + " Reviews");
                    }

                    if (tvAverageRating != null) {
                        if (count > 0) {
                            float average = totalRating / count;
                            tvAverageRating.setText(String.format("%.1f", average));
                        } else {
                            tvAverageRating.setText("N/A");
                        }
                    }

                    Log.d(TAG, "Loaded " + reviewList.size() + " reviews");
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading reviews: " + e.getMessage());
                    Toast.makeText(this, "Error loading reviews", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            rvReviews.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (reviewList.isEmpty()) {
            rvReviews.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        } else {
            rvReviews.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---

### AvailableJobsActivity.java

```java
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
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.JobAdapter;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.models.Job;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AvailableJobsActivity extends AppCompatActivity {

    private static final String TAG = "AvailableJobs";

    private Toolbar toolbar;
    private EditText etSearch;
    private Spinner spinnerCategory;
    private RecyclerView rvJobs;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String currentUserId;

    private JobAdapter jobAdapter;
    private List<Job> allJobsList;
    private List<Job> filteredJobsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_jobs);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        initViews();
        setupToolbar();
        setupCategorySpinner();
        setupRecyclerView();
        setupSearchFilter();
        loadOpenJobs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh jobs when returning to this activity (e.g., after submitting a bid)
        loadOpenJobs();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        rvJobs = findViewById(R.id.rvJob);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Available Jobs");
        }
    }

    private void setupCategorySpinner() {
        String[] categories = {
                "All Categories",
                "Construction",
                "Plumbing",
                "Electrical",
                "Painting",
                "Carpentry",
                "Masonry",
                "Roofing",
                "Flooring",
                "Interior Design",
                "Landscaping",
                "HVAC",
                "Welding",
                "Tiling",
                "Renovation",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterJobs();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupRecyclerView() {
        allJobsList = new ArrayList<>();
        filteredJobsList = new ArrayList<>();

        jobAdapter = new JobAdapter(this, filteredJobsList, job -> {
            // Navigate to job details
            Intent intent = new Intent(AvailableJobsActivity.this, JobDetailActivity.class);
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

    private void loadOpenJobs() {
        Log.d(TAG, "Loading all open jobs");
        showLoading(true);

        JobManager.getInstance()
                .getOpenJobs()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Open jobs loaded: " + queryDocumentSnapshots.size());
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
        String selectedCategory = spinnerCategory.getSelectedItem().toString();

        filteredJobsList.clear();

        for (Job job : allJobsList) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    job.getTitle().toLowerCase().contains(searchQuery) ||
                    job.getDescription().toLowerCase().contains(searchQuery) ||
                    job.getLocation().toLowerCase().contains(searchQuery);

            boolean matchesCategory = selectedCategory.equals("All Categories") ||
                    job.getCategory().equals(selectedCategory);

            if (matchesSearch && matchesCategory) {
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

        Log.d(TAG, "Filtered jobs: " + filteredJobsList.size());
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
}```

---

### ChatActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.MessageAdapter;
import com.example.madproject.firebase.MessageManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Message;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView btnSend, btnAttach;
    private ProgressBar progressBar;
    private TextView tvReceiverName;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String receiverId;
    private String chatId;
    private User currentUser;
    private User receiverUser;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get receiver ID from intent
        receiverId = getIntent().getStringExtra("receiverId");
        String receiverName = getIntent().getStringExtra("receiverName");

        if (receiverId == null || receiverId.isEmpty()) {
            Toast.makeText(this, "Error: No receiver specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Generate chat ID (sorted to ensure consistency)
        chatId = generateChatId(currentUserId, receiverId);
        Log.d(TAG, "Chat ID: " + chatId);

        initViews();

        // Set receiver name in toolbar if provided
        if (receiverName != null && !receiverName.isEmpty()) {
            tvReceiverName.setText(receiverName);
        }

        setupRecyclerView();
        setupClickListeners();
        loadCurrentUser();
        loadReceiverUser();
        loadMessages();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);
        tvReceiverName = findViewById(R.id.tvReceiverName);

        // Create ProgressBar programmatically if not in layout
        progressBar = findViewById(R.id.progressBar);
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        // Disable send button initially
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.5f);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        btnAttach.setOnClickListener(v -> {
            Toast.makeText(this, "File attachment coming soon", Toast.LENGTH_SHORT).show();
        });

        // Enable/disable send button based on text
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                btnSend.setEnabled(hasText);
                btnSend.setAlpha(hasText ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private String generateChatId(String user1, String user2) {
        // Sort IDs to ensure consistent chat ID regardless of who initiates
        if (user1.compareTo(user2) < 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }

    private void loadCurrentUser() {
        UserManager.getInstance().getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                currentUser = user;
                Log.d(TAG, "Current user loaded: " + user.getFullName());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading current user: " + error);
            }
        });
    }

    private void loadReceiverUser() {
        UserManager.getInstance().getUserObject(receiverId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                receiverUser = user;
                tvReceiverName.setText(user.getFullName());
                Log.d(TAG, "Receiver user loaded: " + user.getFullName());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading receiver user: " + error);
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;

        if (currentUser == null) {
            Toast.makeText(this, "Please wait, loading user info...", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = "msg_" + UUID.randomUUID().toString();

        Message message = new Message(
                messageId,
                chatId,
                currentUserId,
                currentUser.getFullName(),
                receiverId,
                messageText
        );

        // Set receiver name if available
        if (receiverUser != null) {
            message.setReceiverName(receiverUser.getFullName());
        }

        // Clear input immediately for better UX
        etMessage.setText("");

        MessageManager.getInstance()
                .createMessage(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message: " + e.getMessage());
                    Toast.makeText(ChatActivity.this,
                            "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMessages() {
        Log.d(TAG, "Loading messages for chat: " + chatId);

        // Use real-time listener for messages
        MessageManager.getInstance().listenToMessages(chatId, new MessageManager.OnMessagesChangedListener() {
            @Override
            public void onMessagesChanged(com.google.firebase.firestore.QuerySnapshot messages) {
                Log.d(TAG, "Messages updated: " + messages.size());

                messageList.clear();

                for (DocumentSnapshot doc : messages) {
                    Message message = doc.toObject(Message.class);
                    if (message != null) {
                        messageList.add(message);

                        // Mark message as read if it's for current user and unread
                        if (message.getReceiverId().equals(currentUserId) && !message.isRead()) {
                            MessageManager.getInstance().markAsRead(message.getMessageId());
                        }
                    }
                }

                // Sort by timestamp (oldest first for chat)
                Collections.sort(messageList, (m1, m2) ->
                        Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                messageAdapter.notifyDataSetChanged();

                // Scroll to bottom
                if (!messageList.isEmpty()) {
                    rvMessages.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading messages: " + error);
                Toast.makeText(ChatActivity.this,
                        "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener if we stored it
        if (messageListener != null) {
            messageListener.remove();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---

### ClientDashboardActivity.java

```java
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
                startActivity(new Intent(ClientDashboardActivity.this, ChatActivity.class));
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
}```

---

### ContractorDashboardActivity.java

```java
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
                startActivity(new Intent(ContractorDashboardActivity.this, ChatActivity.class));
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
}```

---

### ContractorDirectoryActivity.java

```java
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

import com.example.madproject.adapters.ContractorAdapter;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContractorDirectoryActivity extends AppCompatActivity {

    private static final String TAG = "ContractorDirectory";

    private RecyclerView rvContractors;
    private EditText etSearch;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    private ContractorAdapter contractorAdapter;
    private List<User> contractorList;
    private List<User> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contractor_directory);

        initViews();
        setupRecyclerView();
        setupSearch();
        loadContractors();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Find Contractors");
        }

        rvContractors = findViewById(R.id.rvContractors);
        etSearch = findViewById(R.id.etSearch);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        rvContractors.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRecyclerView() {
        contractorList = new ArrayList<>();
        filteredList = new ArrayList<>();

        contractorAdapter = new ContractorAdapter(this, filteredList, contractor -> {
            // Navigate to contractor profile
            Intent intent = new Intent(this, ContractorProfileActivity.class);
            intent.putExtra("contractorId", contractor.getUserId());
            startActivity(intent);
        });

        rvContractors.setAdapter(contractorAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContractors(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterContractors(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(contractorList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (User contractor : contractorList) {
                // Search by name, category, or city
                if ((contractor.getFullName() != null &&
                        contractor.getFullName().toLowerCase().contains(lowerQuery)) ||
                    (contractor.getCategory() != null &&
                        contractor.getCategory().toLowerCase().contains(lowerQuery)) ||
                    (contractor.getCity() != null &&
                        contractor.getCity().toLowerCase().contains(lowerQuery))) {
                    filteredList.add(contractor);
                }
            }
        }

        contractorAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadContractors() {
        showLoading(true);

        UserManager.getInstance()
                .getAllContractors()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);

                    contractorList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User contractor = doc.toObject(User.class);
                        if (contractor != null) {
                            contractorList.add(contractor);
                        }
                    }

                    // Sort by rating (highest first)
                    Collections.sort(contractorList, (c1, c2) ->
                            Double.compare(c2.getRating(), c1.getRating()));

                    // Initialize filtered list with all contractors
                    filteredList.clear();
                    filteredList.addAll(contractorList);
                    contractorAdapter.notifyDataSetChanged();

                    updateEmptyState();
                    Log.d(TAG, "Loaded " + contractorList.size() + " contractors");
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading contractors: " + e.getMessage());
                    Toast.makeText(this, "Error loading contractors", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            rvContractors.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            rvContractors.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        } else {
            rvContractors.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---

### ContractorProfileActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.PortfolioAdapter;
import com.example.madproject.adapters.ReviewAdapter;
import com.example.madproject.firebase.ReviewManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Review;
import com.example.madproject.models.User;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContractorProfileActivity extends AppCompatActivity {

    private static final String TAG = "ContractorProfile";

    private CircleImageView ivProfileImage;
    private TextView tvName, tvCategory, tvRating, tvExperience, tvHourlyRate, tvBio;
    private TextView tvCompletedProjects, tvTotalReviews, tvLocation;
    private TextView tvViewAllPortfolio, tvViewAllReviews;
    private RecyclerView rvPortfolio, rvReviews;
    private Button btnCall, btnMessage;
    private ProgressBar progressBar;
    private LinearLayout portfolioSection, reviewsSection;

    private String contractorId;
    private User contractor;

    private PortfolioAdapter portfolioAdapter;
    private ReviewAdapter reviewAdapter;
    private List<String> portfolioList;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contractor_profile);

        // Get contractor ID from intent
        contractorId = getIntent().getStringExtra("contractorId");

        if (contractorId == null || contractorId.isEmpty()) {
            Toast.makeText(this, "Error: Contractor not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerViews();
        setupClickListeners();
        loadProfile();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Contractor Profile");
        }

        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvName = findViewById(R.id.tvName);
        tvCategory = findViewById(R.id.tvCategory);
        tvRating = findViewById(R.id.tvRating);
        tvExperience = findViewById(R.id.tvExperience);
        tvHourlyRate = findViewById(R.id.tvHourlyRate);
        tvBio = findViewById(R.id.tvBio);
        tvCompletedProjects = findViewById(R.id.tvCompletedProjects);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        tvLocation = findViewById(R.id.tvLocation);
        tvViewAllPortfolio = findViewById(R.id.tvViewAllPortfolio);
        tvViewAllReviews = findViewById(R.id.tvViewAllReviews);
        btnCall = findViewById(R.id.btnCall);
        btnMessage = findViewById(R.id.btnMessage);
        rvPortfolio = findViewById(R.id.rvPortfolio);
        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        portfolioSection = findViewById(R.id.portfolioSection);
        reviewsSection = findViewById(R.id.reviewsSection);

        rvPortfolio.setLayoutManager(new GridLayoutManager(this, 3));
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRecyclerViews() {
        // Portfolio adapter
        portfolioList = new ArrayList<>();
        portfolioAdapter = new PortfolioAdapter(this, portfolioList, new PortfolioAdapter.OnPortfolioItemClickListener() {
            @Override
            public void onItemClick(String imageUrl, int position) {
                // View full image
                Intent intent = new Intent(ContractorProfileActivity.this, PortfolioGalleryActivity.class);
                intent.putExtra("contractorId", contractorId);
                intent.putExtra("selectedPosition", position);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(String imageUrl, int position) {
                // Only show delete for own profile - not applicable here
            }
        });
        rvPortfolio.setAdapter(portfolioAdapter);

        // Reviews adapter
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupClickListeners() {
        btnMessage.setOnClickListener(v -> {
            if (contractor != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("receiverId", contractorId);
                intent.putExtra("receiverName", contractor.getFullName());
                startActivity(intent);
            }
        });

        btnCall.setOnClickListener(v -> {
            if (contractor != null && contractor.getPhoneNumber() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + contractor.getPhoneNumber()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        if (tvViewAllPortfolio != null) {
            tvViewAllPortfolio.setOnClickListener(v -> {
                Intent intent = new Intent(this, PortfolioGalleryActivity.class);
                intent.putExtra("contractorId", contractorId);
                startActivity(intent);
            });
        }

        if (tvViewAllReviews != null) {
            tvViewAllReviews.setOnClickListener(v -> {
                Intent intent = new Intent(this, AllReviewsActivity.class);
                intent.putExtra("contractorId", contractorId);
                if (contractor != null) {
                    intent.putExtra("contractorName", contractor.getFullName());
                }
                startActivity(intent);
            });
        }
    }

    private void loadProfile() {
        showLoading(true);

        UserManager.getInstance().getUserObject(contractorId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                showLoading(false);
                contractor = user;
                displayProfile(user);
                loadReviews();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error loading profile: " + error);
                Toast.makeText(ContractorProfileActivity.this,
                        "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayProfile(User user) {
        // Set name
        tvName.setText(user.getFullName());

        // Set category
        if (user.getCategory() != null && !user.getCategory().isEmpty()) {
            tvCategory.setText(user.getCategory());
            tvCategory.setVisibility(View.VISIBLE);
        } else {
            tvCategory.setVisibility(View.GONE);
        }

        // Set rating
        if (user.getRating() > 0) {
            tvRating.setText(String.format("%.1f (%d reviews)", user.getRating(), user.getTotalReviews()));
        } else {
            tvRating.setText("No reviews yet");
        }

        // Set experience
        if (user.getExperienceYears() > 0) {
            tvExperience.setText(user.getExperienceYears() + " years experience");
            tvExperience.setVisibility(View.VISIBLE);
        } else {
            tvExperience.setVisibility(View.GONE);
        }

        // Set hourly rate
        if (user.getHourlyRate() > 0) {
            tvHourlyRate.setText("Rs. " + formatCurrency(user.getHourlyRate()) + "/hr");
            tvHourlyRate.setVisibility(View.VISIBLE);
        } else {
            tvHourlyRate.setVisibility(View.GONE);
        }

        // Set bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            tvBio.setText(user.getBio());
            tvBio.setVisibility(View.VISIBLE);
        } else {
            tvBio.setVisibility(View.GONE);
        }

        // Set completed projects
        if (tvCompletedProjects != null) {
            tvCompletedProjects.setText(user.getCompletedProjects() + " projects");
        }

        // Set total reviews
        if (tvTotalReviews != null) {
            tvTotalReviews.setText(user.getTotalReviews() + " reviews");
        }

        // Set location
        if (tvLocation != null) {
            if (user.getCity() != null && !user.getCity().isEmpty()) {
                tvLocation.setText(user.getCity());
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }
        }

        // Load portfolio images
        List<String> portfolio = user.getPortfolioImages();
        if (portfolio != null && !portfolio.isEmpty()) {
            portfolioList.clear();
            // Show only first 6 images
            int limit = Math.min(portfolio.size(), 6);
            for (int i = 0; i < limit; i++) {
                portfolioList.add(portfolio.get(i));
            }
            portfolioAdapter.notifyDataSetChanged();

            if (portfolioSection != null) portfolioSection.setVisibility(View.VISIBLE);

            // Show "View All" if more than 6 images
            if (tvViewAllPortfolio != null) {
                tvViewAllPortfolio.setVisibility(portfolio.size() > 6 ? View.VISIBLE : View.GONE);
            }
        } else {
            if (portfolioSection != null) portfolioSection.setVisibility(View.GONE);
        }

        // TODO: Load profile image using Glide/Picasso
        // if (user.getProfilePictureUrl() != null) {
        //     Glide.with(this).load(user.getProfilePictureUrl()).into(ivProfileImage);
        // }
    }

    private void loadReviews() {
        ReviewManager.getInstance()
                .getReviewsByContractor(contractorId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            reviewList.add(review);
                        }
                    }

                    // Sort by date (newest first)
                    Collections.sort(reviewList, (r1, r2) ->
                            Long.compare(r2.getReviewDate(), r1.getReviewDate()));

                    // Show only first 3 reviews
                    List<Review> limitedReviews = new ArrayList<>();
                    int limit = Math.min(reviewList.size(), 3);
                    for (int i = 0; i < limit; i++) {
                        limitedReviews.add(reviewList.get(i));
                    }

                    reviewAdapter.updateData(limitedReviews);

                    if (reviewsSection != null) {
                        reviewsSection.setVisibility(reviewList.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    // Show "View All" if more than 3 reviews
                    if (tvViewAllReviews != null) {
                        tvViewAllReviews.setVisibility(reviewList.size() > 3 ? View.VISIBLE : View.GONE);
                    }

                    Log.d(TAG, "Loaded " + reviewList.size() + " reviews");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews: " + e.getMessage());
                });
    }

    private String formatCurrency(double amount) {
        if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---

### EditProfileActivity.java

```java
package com.example.madproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfilePicture;
    private EditText etFullName, etEmail, etPhone, etAddress;
    private Spinner spinnerCity;
    private Button btnSaveProfile, btnCancel;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupCitySpinner();
        setupClickListeners();
        loadProfile();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        spinnerCity = findViewById(R.id.spinnerCity);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancel = findViewById(R.id.btnCancel);

        // Try to find ProgressBar
        progressBar = findViewById(R.id.progressBar);
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }

        // Email field should be read-only
        etEmail.setEnabled(false);
    }

    private void setupCitySpinner() {
        // Same cities list as JobPostActivity
        String[] cities = {
                "Select City", "Karachi", "Lahore", "Islamabad", "Rawalpindi",
                "Faisalabad", "Multan", "Peshawar", "Quetta", "Sialkot",
                "Gujranwala", "Hyderabad", "Bahawalpur", "Sargodha"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                cities
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadProfile() {
        showLoading(true);

        UserManager.getInstance()
                .getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        showLoading(false);
                        currentUser = user;
                        populateFields(user);
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        Toast.makeText(EditProfileActivity.this,
                                "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateFields(User user) {
        etFullName.setText(user.getFullName());
        etEmail.setText(user.getEmail());
        etPhone.setText(user.getPhoneNumber());
        etAddress.setText(user.getAddress());

        // Set spinner value
        setSpinnerValue(spinnerCity, user.getCity());

        // Load profile picture (placeholder for now)
        // TODO: Use Glide/Picasso to load image
        // if (user.getProfilePictureUrl() != null) {
        //     Glide.with(this).load(user.getProfilePictureUrl()).into(ivProfilePicture);
        // }
    }

    private void saveProfile() {
        if (!validateInputs()) {
            return;
        }

        // Update user object
        currentUser.setFullName(etFullName.getText().toString().trim());
        currentUser.setPhoneNumber(etPhone.getText().toString().trim());
        currentUser.setAddress(etAddress.getText().toString().trim());
        currentUser.setCity(spinnerCity.getSelectedItem().toString());

        showLoading(true);

        UserManager.getInstance()
                .updateUser(currentUser)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error updating profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInputs() {
        // Validate full name
        if (TextUtils.isEmpty(etFullName.getText().toString())) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return false;
        }

        // Validate phone number
        if (TextUtils.isEmpty(etPhone.getText().toString())) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return false;
        }

        // Validate city selection
        if (spinnerCity.getSelectedItem().toString().equals("Select City")) {
            Toast.makeText(this, "Please select a city", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;

        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnSaveProfile.setEnabled(false);
            btnSaveProfile.setText("Saving...");
            btnCancel.setEnabled(false);
            etFullName.setEnabled(false);
            etPhone.setEnabled(false);
            etAddress.setEnabled(false);
            spinnerCity.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSaveProfile.setEnabled(true);
            btnSaveProfile.setText("Save Profile");
            btnCancel.setEnabled(true);
            etFullName.setEnabled(true);
            etPhone.setEnabled(true);
            etAddress.setEnabled(true);
            spinnerCity.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---

### JobDetailActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.BidAdapter;
import com.example.madproject.firebase.BidManager;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Bid;
import com.example.madproject.models.Job;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class JobDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvJobTitle, tvCategory, tvPostedDate, tvDescription, tvBudget,
            tvTimeline, tvTotalBids, tvLocation, tvStatus, btnSortBids;
    private RecyclerView rvBids;
    private LinearLayout emptyState;
    private ImageView btnEdit, btnShare;
    private Button btnSubmitBid;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String jobId;
    private Job currentJob;

    private BidAdapter bidAdapter;
    private List<Bid> bidList;
    private String currentSortOrder = "lowest"; // "lowest", "highest", "recent"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get job ID from intent
        jobId = getIntent().getStringExtra("jobId");

        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(this, "Error: Job not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadJobDetails();
        loadBids();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvJobTitle = findViewById(R.id.tvJobTitle);
        tvCategory = findViewById(R.id.tvCategory);
        tvPostedDate = findViewById(R.id.tvPostedDate);
        tvDescription = findViewById(R.id.tvDescription);
        tvBudget = findViewById(R.id.tvBudget);
        tvTimeline = findViewById(R.id.tvTimeline);
        tvTotalBids = findViewById(R.id.tvTotalBids);
        tvLocation = findViewById(R.id.tvLocation);
        tvStatus = findViewById(R.id.tvStatus);
        rvBids = findViewById(R.id.rvBids);
        emptyState = findViewById(R.id.emptyState);
        btnEdit = findViewById(R.id.btnEdit);
        btnShare = findViewById(R.id.btnShare);
        btnSortBids = findViewById(R.id.btnSortBids);
        btnSubmitBid = findViewById(R.id.btnSubmitBid);

        // Create ProgressBar programmatically
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        bidList = new ArrayList<>();

        // Create adapter with empty jobClientId initially (will be updated when job loads)
        bidAdapter = new BidAdapter(this, bidList, currentUserId, "", new BidAdapter.OnBidActionListener() {
            @Override
            public void onAcceptBid(Bid bid) {
                showAcceptBidDialog(bid);
            }

            @Override
            public void onRejectBid(Bid bid) {
                showRejectBidDialog(bid);
            }

            @Override
            public void onViewProfile(Bid bid) {
                viewContractorProfile(bid.getContractorId());
            }

            @Override
            public void onContactContractor(Bid bid) {
                contactContractor(bid.getContractorId());
            }
        });

        rvBids.setLayoutManager(new LinearLayoutManager(this));
        rvBids.setAdapter(bidAdapter);
    }

    private void updateAdapterWithJobOwner(String jobClientId) {
        // Recreate adapter with correct jobClientId
        bidAdapter = new BidAdapter(this, bidList, currentUserId, jobClientId, new BidAdapter.OnBidActionListener() {
            @Override
            public void onAcceptBid(Bid bid) {
                showAcceptBidDialog(bid);
            }

            @Override
            public void onRejectBid(Bid bid) {
                showRejectBidDialog(bid);
            }

            @Override
            public void onViewProfile(Bid bid) {
                viewContractorProfile(bid.getContractorId());
            }

            @Override
            public void onContactContractor(Bid bid) {
                contactContractor(bid.getContractorId());
            }
        });
        rvBids.setAdapter(bidAdapter);
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> editJob());
        btnShare.setOnClickListener(v -> shareJob());
        btnSortBids.setOnClickListener(v -> showSortDialog());

        if (btnSubmitBid != null) {
            btnSubmitBid.setOnClickListener(v -> submitBid());
        }
    }

    private void loadJobDetails() {
        showLoading(true);

        JobManager.getInstance()
                .getJob(jobId)
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);

                    if (documentSnapshot.exists()) {
                        currentJob = documentSnapshot.toObject(Job.class);
                        if (currentJob != null) {
                            displayJobDetails(currentJob);
                            // Update adapter with job owner ID so only owner can accept/reject bids
                            updateAdapterWithJobOwner(currentJob.getClientId());
                        }
                    } else {
                        Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading job: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void displayJobDetails(Job job) {
        // Set job title
        tvJobTitle.setText(job.getTitle());

        // Set category
        tvCategory.setText(job.getCategory());

        // Set status
        tvStatus.setText(job.getStatus().replace("_", " ").toUpperCase());
        setStatusStyle(job.getStatus());

        // Set posted date
        String dateText = getRelativeTime(job.getPostedDate());
        tvPostedDate.setText("Posted " + dateText);

        // Set description
        tvDescription.setText(job.getDescription());

        // Set budget
        tvBudget.setText("Rs. " + formatCurrency(job.getBudget()));

        // Set timeline
        tvTimeline.setText(job.getTimeline());

        // Set total bids
        tvTotalBids.setText(String.valueOf(job.getTotalBids()));

        // Set location
        tvLocation.setText(job.getLocation());

        // Show/hide buttons based on user role
        boolean isJobOwner = currentUserId.equals(job.getClientId());

        if (isJobOwner) {
            // Client view: show edit, hide submit bid
            btnEdit.setVisibility(View.VISIBLE);
            if (btnSubmitBid != null) {
                btnSubmitBid.setVisibility(View.GONE);
            }
        } else {
            // Contractor view: hide edit, show submit bid
            btnEdit.setVisibility(View.GONE);
            if (btnSubmitBid != null) {
                // Only show submit bid button if job is still open
                if ("open".equals(job.getStatus())) {
                    btnSubmitBid.setVisibility(View.VISIBLE);
                } else {
                    btnSubmitBid.setVisibility(View.GONE);
                }
            }
        }
    }

    private void loadBids() {
        showLoading(true);

        BidManager.getInstance()
                .getBidsByJob(jobId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);

                    bidList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Bid bid = doc.toObject(Bid.class);
                        if (bid != null) {
                            bidList.add(bid);
                        }
                    }

                    // Sort bids
                    sortBids();

                    // Update adapter
                    bidAdapter.notifyDataSetChanged();

                    // Show/hide empty state
                    if (bidList.isEmpty()) {
                        rvBids.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvBids.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading bids: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sortBids() {
        switch (currentSortOrder) {
            case "lowest":
                bidList.sort((b1, b2) -> Double.compare(b1.getBidAmount(), b2.getBidAmount()));
                btnSortBids.setText("Sort by: Lowest");
                break;
            case "highest":
                bidList.sort((b1, b2) -> Double.compare(b2.getBidAmount(), b1.getBidAmount()));
                btnSortBids.setText("Sort by: Highest");
                break;
            case "recent":
                bidList.sort((b1, b2) -> Long.compare(b2.getSubmittedDate(), b1.getSubmittedDate()));
                btnSortBids.setText("Sort by: Recent");
                break;
        }
    }

    private void showSortDialog() {
        String[] options = {"Lowest Price", "Highest Price", "Most Recent"};

        new AlertDialog.Builder(this)
                .setTitle("Sort Bids")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentSortOrder = "lowest";
                            break;
                        case 1:
                            currentSortOrder = "highest";
                            break;
                        case 2:
                            currentSortOrder = "recent";
                            break;
                    }
                    sortBids();
                    bidAdapter.notifyDataSetChanged();
                })
                .show();
    }

    private void showAcceptBidDialog(Bid bid) {
        new AlertDialog.Builder(this)
                .setTitle("Accept Bid")
                .setMessage("Accept bid from " + bid.getContractorName() + " for Rs. " +
                        formatCurrency(bid.getBidAmount()) + "?\n\nThis will reject all other bids and assign the contractor to the job.")
                .setPositiveButton("Accept", (dialog, which) -> acceptBid(bid))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void acceptBid(Bid bid) {
        showLoading(true);

        // Update bid status to accepted
        BidManager.getInstance()
                .acceptBid(bid.getBidId())
                .addOnSuccessListener(aVoid -> {
                    // Reject all other bids
                    BidManager.getInstance().rejectOtherBids(jobId, bid.getBidId());

                    // Assign contractor to job
                    JobManager.getInstance()
                            .assignContractor(jobId, bid.getContractorId(),
                                    bid.getContractorName(), bid.getBidId())
                            .addOnSuccessListener(aVoid2 -> {
                                showLoading(false);

                                Toast.makeText(this, "Bid accepted successfully!",
                                        Toast.LENGTH_SHORT).show();

                                // Reload job and bids
                                loadJobDetails();
                                loadBids();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error assigning contractor: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error accepting bid: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showRejectBidDialog(Bid bid) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Bid")
                .setMessage("Reject bid from " + bid.getContractorName() + "?")
                .setPositiveButton("Reject", (dialog, which) -> rejectBid(bid))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void rejectBid(Bid bid) {
        showLoading(true);

        BidManager.getInstance()
                .rejectBid(bid.getBidId())
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Bid rejected", Toast.LENGTH_SHORT).show();
                    loadBids();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error rejecting bid: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void viewContractorProfile(String contractorId) {
        Intent intent = new Intent(this, ContractorProfileActivity.class);
        intent.putExtra("contractorId", contractorId);
        startActivity(intent);
    }

    private void contactContractor(String contractorId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("receiverId", contractorId);
        startActivity(intent);
    }

    private void editJob() {
        if (currentJob == null) return;

        // Only allow editing if job is still open
        if (!"open".equals(currentJob.getStatus())) {
            Toast.makeText(this, "Can only edit open jobs", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Edit job feature coming soon!", Toast.LENGTH_SHORT).show();
        // TODO: Implement JobEditActivity
        // Intent intent = new Intent(this, JobEditActivity.class);
        // intent.putExtra("jobId", jobId);
        // startActivity(intent);
    }

    private void submitBid() {
        if (currentJob == null) return;

        // Check if job is still open
        if (!"open".equals(currentJob.getStatus())) {
            Toast.makeText(this, "This job is no longer accepting bids", Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to SubmitBidActivity
        Intent intent = new Intent(this, SubmitBidActivity.class);
        intent.putExtra("jobId", jobId);
        startActivity(intent);
    }

    private void shareJob() {
        if (currentJob == null) return;

        String shareText = "Check out this job on RebuildPak:\n\n" +
                currentJob.getTitle() + "\n" +
                "Budget: Rs. " + formatCurrency(currentJob.getBudget()) + "\n" +
                "Location: " + currentJob.getLocation() + "\n" +
                "Category: " + currentJob.getCategory();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Job"));
    }

    private void setStatusStyle(String status) {
        int color;

        switch (status.toLowerCase()) {
            case "open":
                color = 0xFF4CAF50; // Green
                break;
            case "in_progress":
                color = 0xFFFFA726; // Orange
                break;
            case "completed":
                color = 0xFF2196F3; // Blue
                break;
            case "cancelled":
                color = 0xFFF44336; // Red
                break;
            default:
                color = 0xFF757575; // Grey
                break;
        }

        tvStatus.setTextColor(color);
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

    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    private void showLoading(boolean show) {
        // Implement loading indicator
        // You can add a ProgressBar to your layout or use a loading dialog
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}```

---

### JobPostActivity.java

```java
package com.example.madproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Job;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public class JobPostActivity extends AppCompatActivity {

    private EditText etJobTitle, etJobDescription, etBudget, etTimeline, etAddress;
    private Spinner spinnerCategory, spinnerCity;
    private Button btnPostJob, btnCancel, btnTakePhoto, btnAddPhoto;
    private Toolbar toolbar;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String currentUserName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_post);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        initViews();
        setupSpinners();
        setupClickListeners();
        loadUserData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etJobTitle = findViewById(R.id.etJobTitle);
        etJobDescription = findViewById(R.id.etJobDescription);
        etBudget = findViewById(R.id.etBudget);
        etTimeline = findViewById(R.id.etTimeline);
        etAddress = findViewById(R.id.etAddress);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCity = findViewById(R.id.spinnerCity);
        btnPostJob = findViewById(R.id.btnPostJob);
        btnCancel = findViewById(R.id.btnCancel);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);

        // Create ProgressBar programmatically if not in XML
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
    }

    private void setupSpinners() {
        // Setup Category Spinner
        String[] categories = {
                "Select Category",
                "Construction",
                "Plumbing",
                "Electrical",
                "Painting",
                "Carpentry",
                "Masonry",
                "Roofing",
                "Flooring",
                "Interior Design",
                "Landscaping",
                "HVAC",
                "Welding",
                "Tiling",
                "Renovation",
                "Other"
        };

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Setup City Spinner
        String[] cities = {
                "Select City",
                "Karachi",
                "Lahore",
                "Islamabad",
                "Rawalpindi",
                "Faisalabad",
                "Multan",
                "Peshawar",
                "Quetta",
                "Sialkot",
                "Gujranwala",
                "Hyderabad",
                "Bahawalpur",
                "Sargodha",
                "Sukkur",
                "Larkana",
                "Sheikhupura",
                "Rahim Yar Khan",
                "Jhang",
                "Dera Ghazi Khan",
                "Gujrat",
                "Sahiwal",
                "Wah Cantonment",
                "Mardan",
                "Kasur",
                "Okara",
                "Mingora",
                "Nawabshah",
                "Chiniot",
                "Kotri",
                "Khanpur",
                "Hafizabad",
                "Sadiqabad",
                "Mirpur Khas",
                "Burewala",
                "Kohat",
                "Khanewal",
                "Dera Ismail Khan",
                "Turbat",
                "Muzaffargarh",
                "Abbottabad",
                "Mandi Bahauddin",
                "Shikarpur",
                "Jacobabad",
                "Jhelum",
                "Khanpur",
                "Khairpur",
                "Khuzdar",
                "Pakpattan",
                "Attock"
        };

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                cities
        );
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);
    }

    private void setupClickListeners() {
        btnPostJob.setOnClickListener(v -> postJob());
        btnCancel.setOnClickListener(v -> finish());

        // TODO: Implement photo functionality
        btnTakePhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Camera feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnAddPhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Gallery feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserData() {
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        UserManager.getInstance()
                .getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        if (user != null) {
                            currentUserName = user.getFullName();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Continue anyway, userName will be empty
                    }
                });
    }

    private void postJob() {
        // Get input values
        String title = etJobTitle.getText().toString().trim();
        String description = etJobDescription.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();
        String timelineStr = etTimeline.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String city = spinnerCity.getSelectedItem().toString();

        // Validate inputs
        if (!validateInputs(title, description, budgetStr, timelineStr, address, category, city)) {
            return;
        }

        // Parse numeric values
        double budget = Double.parseDouble(budgetStr);
        String timeline = timelineStr + " days";

        // Create location string (city + address)
        String location = city + ", " + address;

        // Generate unique job ID
        String jobId = "job_" + UUID.randomUUID().toString();

        // Create Job object
        Job job = new Job(
                jobId,
                currentUserId,
                currentUserName,
                title,
                description,
                category,
                budget,
                timeline,
                location
        );

        // Show loading
        showLoading(true);

        // Save job to Firestore
        JobManager.getInstance()
                .createJob(job)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);

                    // Update client's active jobs count
                    updateClientJobCount();

                    Toast.makeText(JobPostActivity.this,
                            "Job posted successfully!",
                            Toast.LENGTH_SHORT).show();

                    // Return to previous screen
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(JobPostActivity.this,
                            "Error posting job: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void updateClientJobCount() {
        // Increment active jobs count for client
        UserManager.getInstance()
                .getUser(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setActiveJobs(user.getActiveJobs() + 1);
                            UserManager.getInstance().updateUser(user);
                        }
                    }
                });
    }

    private boolean validateInputs(String title, String description, String budgetStr,
                                   String timelineStr, String address, String category, String city) {
        // Validate job title
        if (TextUtils.isEmpty(title)) {
            etJobTitle.setError("Job title is required");
            etJobTitle.requestFocus();
            return false;
        }

        if (title.length() < 10) {
            etJobTitle.setError("Title must be at least 10 characters");
            etJobTitle.requestFocus();
            return false;
        }

        // Validate category
        if (category.equals("Select Category")) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate description
        if (TextUtils.isEmpty(description)) {
            etJobDescription.setError("Description is required");
            etJobDescription.requestFocus();
            return false;
        }

        if (description.length() < 20) {
            etJobDescription.setError("Description must be at least 20 characters");
            etJobDescription.requestFocus();
            return false;
        }

        // Validate budget
        if (TextUtils.isEmpty(budgetStr)) {
            etBudget.setError("Budget is required");
            etBudget.requestFocus();
            return false;
        }

        try {
            double budget = Double.parseDouble(budgetStr);
            if (budget <= 0) {
                etBudget.setError("Budget must be greater than 0");
                etBudget.requestFocus();
                return false;
            }
            if (budget < 1000) {
                etBudget.setError("Minimum budget is PKR 1,000");
                etBudget.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etBudget.setError("Invalid budget amount");
            etBudget.requestFocus();
            return false;
        }

        // Validate city
        if (city.equals("Select City")) {
            Toast.makeText(this, "Please select a city", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate address
        if (TextUtils.isEmpty(address)) {
            etAddress.setError("Address is required");
            etAddress.requestFocus();
            return false;
        }

        // Validate timeline
        if (TextUtils.isEmpty(timelineStr)) {
            etTimeline.setError("Timeline is required");
            etTimeline.requestFocus();
            return false;
        }

        try {
            int timeline = Integer.parseInt(timelineStr);
            if (timeline <= 0) {
                etTimeline.setError("Timeline must be greater than 0");
                etTimeline.requestFocus();
                return false;
            }
            if (timeline > 365) {
                etTimeline.setError("Maximum timeline is 365 days");
                etTimeline.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etTimeline.setError("Invalid timeline");
            etTimeline.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        if (show) {
            btnPostJob.setEnabled(false);
            btnPostJob.setText("Posting...");
            btnCancel.setEnabled(false);
            etJobTitle.setEnabled(false);
            etJobDescription.setEnabled(false);
            etBudget.setEnabled(false);
            etTimeline.setEnabled(false);
            etAddress.setEnabled(false);
            spinnerCategory.setEnabled(false);
            spinnerCity.setEnabled(false);
            btnTakePhoto.setEnabled(false);
            btnAddPhoto.setEnabled(false);
        } else {
            btnPostJob.setEnabled(true);
            btnPostJob.setText("Post Job");
            btnCancel.setEnabled(true);
            etJobTitle.setEnabled(true);
            etJobDescription.setEnabled(true);
            etBudget.setEnabled(true);
            etTimeline.setEnabled(true);
            etAddress.setEnabled(true);
            spinnerCategory.setEnabled(true);
            spinnerCity.setEnabled(true);
            btnTakePhoto.setEnabled(true);
            btnAddPhoto.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}```

---

### MainActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText email, password;
    private Button btnLogin;
    private TextView btnCreate, forgotPassword;
    private ImageView btnClose;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize views
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnCreate = findViewById(R.id.createbtn);
        btnLogin = findViewById(R.id.loginbtn);
        btnClose = findViewById(R.id.btnClose);
        forgotPassword = findViewById(R.id.forgotPassword);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        if (auth.getCurrentUser() != null) {
            navigateBasedOnUserType(auth.getCurrentUser().getUid());
        }

        // Setup click listeners
        btnCreate.setOnClickListener(v -> createAccount());
        btnLogin.setOnClickListener(v -> loginUser());
        btnClose.setOnClickListener(v -> finish());
        forgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private void createAccount() {
        Intent i = new Intent(this, SignupActivity.class);
        startActivity(i);
    }

    private void loginUser() {
        String e = email.getText().toString().trim();
        String p = password.getText().toString().trim();

        if (!validateInputs(e, p)) return;

        // Show loading
        showLoading(true);

        auth.signInWithEmailAndPassword(e, p)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();

                        // Update last login timestamp
                        UserManager.getInstance()
                                .updateField(userId, "lastLogin", System.currentTimeMillis());

                        // Load user data and navigate to appropriate dashboard
                        loadUserAndNavigate(userId);

                    } else {
                        showLoading(false);
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserAndNavigate(String userId) {
        UserManager.getInstance()
                .getUserObject(userId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        showLoading(false);

                        if (user != null) {
                            // Navigate based on user type
                            Intent intent;
                            if (user.isContractor()) {
                                intent = new Intent(MainActivity.this, ContractorDashboardActivity.class);
                            } else {
                                intent = new Intent(MainActivity.this, ClientDashboardActivity.class);
                            }

                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(MainActivity.this,
                                    "User data not found. Please contact support.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        Toast.makeText(MainActivity.this,
                                "Error loading user data: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateBasedOnUserType(String userId) {
        // Show loading
        showLoading(true);

        UserManager.getInstance()
                .getUserObject(userId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        showLoading(false);

                        if (user != null) {
                            Intent intent;
                            if (user.isContractor()) {
                                intent = new Intent(MainActivity.this, ContractorDashboardActivity.class);
                            } else {
                                intent = new Intent(MainActivity.this, ClientDashboardActivity.class);
                            }

                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        // User not found in Firestore, stay on login
                    }
                });
    }

    private void handleForgotPassword() {
        String e = email.getText().toString().trim();

        if (e.isEmpty()) {
            email.setError("Enter your email first");
            email.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            email.setError("Enter a valid email");
            email.requestFocus();
            return;
        }

        showLoading(true);

        auth.sendPasswordResetEmail(e)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this,
                                "Password reset email sent to " + e,
                                Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Failed to send reset email";
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs(String e, String p) {
        if (e.isEmpty()) {
            email.setError("Email required");
            email.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            email.setError("Valid email required");
            email.requestFocus();
            return false;
        }

        if (p.isEmpty()) {
            password.setError("Password required");
            password.requestFocus();
            return false;
        }

        if (p.length() < 6) {
            password.setError("Minimum 6 characters");
            password.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnCreate.setEnabled(!show);
        email.setEnabled(!show);
        password.setEnabled(!show);
    }
}```

---

### MaterialManagementActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.MaterialAdapter;
import com.example.madproject.firebase.MaterialManager;
import com.example.madproject.models.Material;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MaterialManagementActivity extends AppCompatActivity {

    private RecyclerView rvMaterials;
    private Button btnRequestMaterial, btnAddMaterial;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String jobId;
    private String projectName;

    private MaterialAdapter materialAdapter;
    private List<Material> allMaterialsList;
    private List<Material> filteredMaterialsList;
    private String currentFilter = "all"; // all, in_stock, low_stock, out_of_stock

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_management);

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
        loadMaterials();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMaterials();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rvMaterials = findViewById(R.id.rvMaterials);
        btnRequestMaterial = findViewById(R.id.btnRequestMaterial);
        btnAddMaterial = findViewById(R.id.btnAddMaterial);
        tabLayout = findViewById(R.id.tabLayout);

        // Try to find ProgressBar and empty state
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

        rvMaterials.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("In Stock"));
        tabLayout.addTab(tabLayout.newTab().setText("Low Stock"));
        tabLayout.addTab(tabLayout.newTab().setText("Out of Stock"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "in_stock";
                        break;
                    case 2:
                        currentFilter = "low_stock";
                        break;
                    case 3:
                        currentFilter = "out_of_stock";
                        break;
                }
                filterMaterials();
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
        allMaterialsList = new ArrayList<>();
        filteredMaterialsList = new ArrayList<>();

        materialAdapter = new MaterialAdapter(this, filteredMaterialsList, material -> {
            // Handle material click (could open detail view)
            Toast.makeText(this, "Material: " + material.getMaterialName(), Toast.LENGTH_SHORT).show();
        });

        rvMaterials.setAdapter(materialAdapter);
    }

    private void setupClickListeners() {
        btnAddMaterial.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMaterialActivity.class);
            intent.putExtra("jobId", jobId);
            intent.putExtra("projectName", projectName);
            startActivity(intent);
        });

        // Request material button (placeholder for future feature)
        btnRequestMaterial.setOnClickListener(v -> {
            Toast.makeText(this, "Request material feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadMaterials() {
        showLoading(true);

        MaterialManager.getInstance()
                .getMaterialsByJob(jobId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    allMaterialsList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Material material = doc.toObject(Material.class);
                        if (material != null) {
                            allMaterialsList.add(material);
                        }
                    }

                    // Sort by last updated (newest first)
                    Collections.sort(allMaterialsList, (m1, m2) ->
                            Long.compare(m2.getLastUpdated(), m1.getLastUpdated()));

                    filterMaterials();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading materials: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void filterMaterials() {
        filteredMaterialsList.clear();

        for (Material material : allMaterialsList) {
            boolean matchesFilter = currentFilter.equals("all") ||
                                  material.getStatus().equals(currentFilter);

            if (matchesFilter) {
                filteredMaterialsList.add(material);
            }
        }

        materialAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvMaterials.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (filteredMaterialsList.isEmpty()) {
            rvMaterials.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvMaterials.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---

### MyJobsActivity.java

```java
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
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
}```

---

### MyProjectsActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class MyProjectsActivity extends AppCompatActivity {

    private static final String TAG = "MyProjects";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView rvProjects;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

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

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        rvProjects = findViewById(R.id.rvProjects);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);
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
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvProjects.setVisibility(View.GONE);
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
}```

---

### NotificationsActivity.java

```java
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
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.NotificationAdapter;
import com.example.madproject.firebase.NotificationManager;
import com.example.madproject.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    private RecyclerView rvNotifications;
    private TextView btnMarkAllRead;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    private FirebaseAuth mAuth;
    private String currentUserId;

    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        initViews();
        setupRecyclerView();
        loadNotifications();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }

        rvNotifications = findViewById(R.id.rvTodayNotifications);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();

        notificationAdapter = new NotificationAdapter(this, notificationList, notification -> {
            // Mark notification as read when clicked
            if (!notification.isRead()) {
                NotificationManager.getInstance()
                        .markAsRead(notification.getNotificationId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Notification marked as read");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error marking notification as read: " + e.getMessage());
                        });
            }

            // Navigate based on notification type and relatedId
            navigateToRelatedActivity(notification);
        });

        rvNotifications.setAdapter(notificationAdapter);
    }

    private void loadNotifications() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to view notifications", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading notifications for user: " + currentUserId);
        showLoading(true);

        // Set up real-time listener for notifications
        notificationListener = NotificationManager.getInstance().listenToNotifications(currentUserId, new NotificationManager.OnNotificationsChangedListener() {
            @Override
            public void onNotificationsChanged(com.google.firebase.firestore.QuerySnapshot notifications) {
                Log.d(TAG, "Notifications updated: " + notifications.size());
                showLoading(false);

                notificationList.clear();

                for (DocumentSnapshot doc : notifications) {
                    Notification notification = doc.toObject(Notification.class);
                    if (notification != null) {
                        notificationList.add(notification);
                    }
                }

                // Sort by timestamp (newest first)
                Collections.sort(notificationList, (n1, n2) ->
                        Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                notificationAdapter.notifyDataSetChanged();
                updateEmptyState();

                Log.d(TAG, "Notifications loaded: " + notificationList.size());
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error loading notifications: " + error);
                Toast.makeText(NotificationsActivity.this,
                        "Error loading notifications: " + error,
                        Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void markAllAsRead() {
        if (currentUserId.isEmpty()) return;

        Log.d(TAG, "Marking all notifications as read");
        NotificationManager.getInstance().markAllAsRead(currentUserId);
        Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
    }

    private void navigateToRelatedActivity(Notification notification) {
        String type = notification.getType();
        String relatedId = notification.getRelatedId();

        if (relatedId == null || relatedId.isEmpty()) {
            return;
        }

        Intent intent = null;

        switch (type.toLowerCase()) {
            case "job":
                intent = new Intent(this, JobDetailActivity.class);
                intent.putExtra("jobId", relatedId);
                break;

            case "bid":
                intent = new Intent(this, JobDetailActivity.class);
                intent.putExtra("jobId", relatedId);
                break;

            case "message":
                // TODO: Navigate to ChatActivity when implemented
                Toast.makeText(this, "Chat activity not yet available", Toast.LENGTH_SHORT).show();
                return;

            case "task":
                // TODO: Navigate to TaskDetailActivity when implemented
                Toast.makeText(this, "Task detail not yet available", Toast.LENGTH_SHORT).show();
                return;

            case "payment":
                // TODO: Navigate to payment screen when implemented
                Toast.makeText(this, "Payment details not yet available", Toast.LENGTH_SHORT).show();
                return;

            case "system":
            default:
                return;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener to prevent memory leaks
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---

### PortfolioGalleryActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.PortfolioAdapter;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class PortfolioGalleryActivity extends AppCompatActivity {

    private static final String TAG = "PortfolioGallery";

    private RecyclerView rvPortfolio;
    private FloatingActionButton fabAddPortfolio;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    private String contractorId;
    private String currentUserId;
    private boolean isOwnProfile;

    private PortfolioAdapter portfolioAdapter;
    private List<String> portfolioList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio_gallery);

        // Get contractor ID from intent
        contractorId = getIntent().getStringExtra("contractorId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Check if viewing own profile
        isOwnProfile = currentUserId.equals(contractorId);

        if (contractorId == null || contractorId.isEmpty()) {
            // If no contractor ID, use current user's profile
            contractorId = currentUserId;
            isOwnProfile = true;
        }

        initViews();
        setupRecyclerView();
        loadPortfolio();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Portfolio");
        }

        rvPortfolio = findViewById(R.id.rvPortfolio);
        fabAddPortfolio = findViewById(R.id.fabAddPortfolio);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        rvPortfolio.setLayoutManager(new GridLayoutManager(this, 2));

        // Only show add button for own profile
        if (fabAddPortfolio != null) {
            fabAddPortfolio.setVisibility(isOwnProfile ? View.VISIBLE : View.GONE);
            fabAddPortfolio.setOnClickListener(v -> addPortfolioItem());
        }
    }

    private void setupRecyclerView() {
        portfolioList = new ArrayList<>();

        portfolioAdapter = new PortfolioAdapter(this, portfolioList, new PortfolioAdapter.OnPortfolioItemClickListener() {
            @Override
            public void onItemClick(String imageUrl, int position) {
                // View full image - could open image viewer
                Toast.makeText(PortfolioGalleryActivity.this,
                        "Image " + (position + 1), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(String imageUrl, int position) {
                if (isOwnProfile) {
                    confirmDeleteImage(imageUrl, position);
                }
            }
        });

        rvPortfolio.setAdapter(portfolioAdapter);
    }

    private void loadPortfolio() {
        showLoading(true);

        UserManager.getInstance().getUserObject(contractorId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                showLoading(false);

                List<String> portfolio = user.getPortfolioImages();
                portfolioList.clear();

                if (portfolio != null && !portfolio.isEmpty()) {
                    portfolioList.addAll(portfolio);
                }

                portfolioAdapter.notifyDataSetChanged();
                updateEmptyState();

                Log.d(TAG, "Loaded " + portfolioList.size() + " portfolio images");
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error loading portfolio: " + error);
                Toast.makeText(PortfolioGalleryActivity.this,
                        "Error loading portfolio", Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void addPortfolioItem() {
        // TODO: Implement image picker and upload
        Toast.makeText(this, "Image upload coming soon", Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteImage(String imageUrl, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to remove this image from your portfolio?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteImage(imageUrl, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage(String imageUrl, int position) {
        // Remove from local list
        portfolioList.remove(position);

        // Update in Firestore
        UserManager.getInstance()
                .updateField(currentUserId, "portfolioImages", new ArrayList<>(portfolioList))
                .addOnSuccessListener(aVoid -> {
                    portfolioAdapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    // Restore the item
                    portfolioList.add(position, imageUrl);
                    portfolioAdapter.notifyItemInserted(position);
                    Toast.makeText(this, "Failed to remove image", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            rvPortfolio.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (portfolioList.isEmpty()) {
            rvPortfolio.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        } else {
            rvPortfolio.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---

### SettingsActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private LinearLayout btnEditProfile, btnChangePassword, btnPrivacyPolicy, btnTermsConditions, btnHelp;
    private SwitchCompat switchPushNotif, switchMessageNotif;
    private Button btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnPrivacyPolicy = findViewById(R.id.btnPrivacyPolicy);
        btnTermsConditions = findViewById(R.id.btnTermsConditions);
        btnHelp = findViewById(R.id.btnHelp);
        switchPushNotif = findViewById(R.id.switchPushNotif);
        switchMessageNotif = findViewById(R.id.switchMessageNotif);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}```

---

### SignupActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {

    private Spinner roleSpinner;
    private EditText fullName, email, mobileNumber, password;
    private Button createAccountBtn;
    private TextView loginLink;
    private ProgressBar progressBar;
    private ImageView btnClose;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_activity);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews();

        // Setup Role Spinner
        setupRoleSpinner();

        // Setup Click Listeners
        setupClickListeners();
    }

    private void initViews() {
        roleSpinner = findViewById(R.id.roleSpinner);
        fullName = findViewById(R.id.fullName);
        email = findViewById(R.id.email);
        mobileNumber = findViewById(R.id.mobileNumber);
        password = findViewById(R.id.password);
        createAccountBtn = findViewById(R.id.createAccountBtn);
        loginLink = findViewById(R.id.loginLink);
        btnClose = findViewById(R.id.btnClose);

        // Add ProgressBar to your layout or create programmatically
        // For now, we'll create it programmatically if not in XML
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        // Create Account Button Click
        createAccountBtn.setOnClickListener(v -> registerUser());

        // Login Link Click
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Close Button
        btnClose.setOnClickListener(v -> finish());
    }

    private void setupRoleSpinner() {
        // Create role options for construction app
        String[] roles = {"Select Role", "Client", "Contractor"};

        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                roles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set adapter to spinner
        roleSpinner.setAdapter(adapter);
    }

    private void registerUser() {
        // Get input values
        String name = fullName.getText().toString().trim();
        String emailText = email.getText().toString().trim();
        String mobile = mobileNumber.getText().toString().trim();
        String selectedRole = roleSpinner.getSelectedItem().toString();
        String pass = password.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(name, emailText, mobile, selectedRole, pass)) {
            return;
        }

        // Convert role to lowercase for database
        String userType = selectedRole.toLowerCase();

        // Show loading
        showLoading(true);

        // Create user with Firebase Auth
        mAuth.createUserWithEmailAndPassword(emailText, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Get the newly created user
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Create User object
                            User user = new User(userId, emailText, name, mobile, userType);
                            user.setCreatedAt(System.currentTimeMillis());
                            user.setLastLogin(System.currentTimeMillis());

                            // Save user to Firestore
                            saveUserToFirestore(user);
                        }
                    } else {
                        showLoading(false);
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(User user) {
        UserManager.getInstance()
                .createUser(user)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(SignupActivity.this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT).show();

                    // Navigate to appropriate dashboard
                    navigateToDashboard(user.getUserType());
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(SignupActivity.this,
                            "Error saving user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    // Delete the auth user if Firestore save fails
                    mAuth.getCurrentUser().delete();
                });
    }

    private void navigateToDashboard(String userType) {
        Intent intent;

        if ("contractor".equals(userType)) {
            intent = new Intent(SignupActivity.this, ContractorDashboardActivity.class);
        } else {
            intent = new Intent(SignupActivity.this, ClientDashboardActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean validateInputs(String name, String emailText, String mobile, String role, String pass) {
        // Validate full name
        if (TextUtils.isEmpty(name)) {
            fullName.setError("Full name is required");
            fullName.requestFocus();
            return false;
        }

        if (name.length() < 3) {
            fullName.setError("Name must be at least 3 characters");
            fullName.requestFocus();
            return false;
        }

        // Validate email
        if (TextUtils.isEmpty(emailText)) {
            email.setError("Email is required");
            email.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            email.setError("Enter a valid email address");
            email.requestFocus();
            return false;
        }

        // Validate mobile number
        if (TextUtils.isEmpty(mobile)) {
            mobileNumber.setError("Mobile number is required");
            mobileNumber.requestFocus();
            return false;
        }

        if (mobile.length() < 10) {
            mobileNumber.setError("Enter a valid mobile number");
            mobileNumber.requestFocus();
            return false;
        }

        // Validate role selection
        if (role.equals("Select Role")) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate password
        if (TextUtils.isEmpty(pass)) {
            password.setError("Password is required");
            password.requestFocus();
            return false;
        }

        if (pass.length() < 6) {
            password.setError("Password must be at least 6 characters");
            password.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        if (show) {
            createAccountBtn.setEnabled(false);
            createAccountBtn.setText("Creating account...");
            loginLink.setEnabled(false);
            fullName.setEnabled(false);
            email.setEnabled(false);
            mobileNumber.setEnabled(false);
            password.setEnabled(false);
            roleSpinner.setEnabled(false);
        } else {
            createAccountBtn.setEnabled(true);
            createAccountBtn.setText("Create account");
            loginLink.setEnabled(true);
            fullName.setEnabled(true);
            email.setEnabled(true);
            mobileNumber.setEnabled(true);
            password.setEnabled(true);
            roleSpinner.setEnabled(true);
        }
    }
}```

---

### SplashActivity.java

```java
package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 2000; // 2 seconds
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Delay and check authentication
        new Handler(Looper.getMainLooper()).postDelayed(() -> checkUserAuthentication(), SPLASH_DELAY);
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is signed in, check user type and navigate to appropriate dashboard
            Log.d(TAG, "User authenticated: " + currentUser.getUid());
            loadUserAndNavigate(currentUser.getUid());
        } else {
            // No user signed in, go to login
            Log.d(TAG, "No user authenticated, navigating to login");
            navigateToLogin();
        }
    }

    private void loadUserAndNavigate(String userId) {
        Log.d(TAG, "Loading user data for: " + userId);

        UserManager.getInstance()
                .getUserObject(userId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        Log.d(TAG, "User loaded: " + user.getFullName() + " - Type: " + user.getUserType());

                        Intent intent;

                        // Navigate based on user type
                        if ("contractor".equalsIgnoreCase(user.getUserType())) {
                            Log.d(TAG, "Navigating to ContractorDashboardActivity");
                            intent = new Intent(SplashActivity.this, ContractorDashboardActivity.class);
                        } else if ("client".equalsIgnoreCase(user.getUserType())) {
                            Log.d(TAG, "Navigating to ClientDashboardActivity");
                            intent = new Intent(SplashActivity.this, ClientDashboardActivity.class);
                        } else {
                            // Unknown user type, go to login
                            Log.e(TAG, "Unknown user type: " + user.getUserType());
                            Toast.makeText(SplashActivity.this,
                                    "Unknown user type. Please login again.",
                                    Toast.LENGTH_SHORT).show();
                            navigateToLogin();
                            return;
                        }

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error loading user: " + error);

                        // If error loading user data, go to login
                        Toast.makeText(SplashActivity.this,
                                "Error loading user data. Please login again.",
                                Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    }
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}```

---

### SubmitBidActivity.java

```java
package com.example.madproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.madproject.firebase.BidManager;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.NotificationManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Bid;
import com.example.madproject.models.Job;
import com.example.madproject.models.Notification;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public class SubmitBidActivity extends AppCompatActivity {

    private EditText etBidAmount, etCompletionDays, etProposal;
    private CheckBox cbTerms;
    private Button btnSubmitBid, btnCancel;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String jobId;
    private Job job;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_bid);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get job ID from Intent
        jobId = getIntent().getStringExtra("jobId");

        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(this, "Error: Job ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        loadJobDetails();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etBidAmount = findViewById(R.id.etBidAmount);
        etCompletionDays = findViewById(R.id.etCompletionDays);
        etProposal = findViewById(R.id.etProposal);
        cbTerms = findViewById(R.id.cbTerms);
        btnSubmitBid = findViewById(R.id.btnSubmitBid);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupClickListeners() {
        btnSubmitBid.setOnClickListener(v -> submitBid());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadJobDetails() {
        JobManager.getInstance()
                .getJob(jobId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        job = documentSnapshot.toObject(Job.class);

                        // Check if job is still open for bids
                        if (job != null && !"open".equals(job.getStatus())) {
                            Toast.makeText(this, "This job is no longer accepting bids",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    } else {
                        Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading job details", Toast.LENGTH_SHORT).show();
                });
    }

    private void submitBid() {
        String amount = etBidAmount.getText().toString().trim();
        String days = etCompletionDays.getText().toString().trim();
        String proposal = etProposal.getText().toString().trim();

        // Validate
        if (TextUtils.isEmpty(amount)) {
            etBidAmount.setError("Amount required");
            etBidAmount.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept terms", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Re-check job status before submitting (in case it changed while user was filling form)
        JobManager.getInstance()
                .getJob(jobId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        showLoading(false);
                        Toast.makeText(this, "Job no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Job currentJob = documentSnapshot.toObject(Job.class);
                    if (currentJob == null || !"open".equals(currentJob.getStatus())) {
                        showLoading(false);
                        Toast.makeText(this, "This job is no longer accepting bids",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Update job reference
                    job = currentJob;

                    // Check if already bid on this job
                    BidManager.getInstance()
                            .checkExistingBid(jobId, currentUserId)
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    showLoading(false);
                                    Toast.makeText(this, "You already submitted a bid for this job",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Create bid
                                createBid(amount, days, proposal);
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error checking existing bid: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error verifying job status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void createBid(String amount, String days, String proposal) {
        // Get contractor info
        UserManager.getInstance()
                .getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User contractor) {
                        if (contractor == null) {
                            showLoading(false);
                            Toast.makeText(SubmitBidActivity.this,
                                    "Error loading profile", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String bidId = "bid_" + UUID.randomUUID().toString();

                        Bid bid = new Bid(
                                bidId,
                                jobId,
                                job != null ? job.getTitle() : "",
                                currentUserId,
                                contractor.getFullName(),
                                Double.parseDouble(amount),
                                days.isEmpty() ? 30 : Integer.parseInt(days),
                                proposal
                        );

                        // Set contractor details
                        bid.setContractorCategory(contractor.getCategory());
                        bid.setContractorRating(contractor.getRating());
                        bid.setContractorCompletedProjects(contractor.getCompletedProjects());

                        // Submit bid
                        BidManager.getInstance()
                                .createBid(bid)
                                .addOnSuccessListener(aVoid -> {
                                    // Increment job's totalBids
                                    JobManager.getInstance()
                                            .incrementTotalBids(jobId)
                                            .addOnSuccessListener(aVoid2 -> {
                                                // Send notification to job owner
                                                sendBidNotification(contractor, bid);

                                                showLoading(false);
                                                Toast.makeText(SubmitBidActivity.this,
                                                        "Bid submitted successfully!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                // Bid was created but counter update failed - still finish
                                                // Send notification anyway
                                                sendBidNotification(contractor, bid);

                                                showLoading(false);
                                                Toast.makeText(SubmitBidActivity.this,
                                                        "Bid submitted successfully!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    Toast.makeText(SubmitBidActivity.this,
                                            "Error submitting bid: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        Toast.makeText(SubmitBidActivity.this,
                                "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendBidNotification(User contractor, Bid bid) {
        if (job == null || job.getClientId() == null) {
            return; // Can't send notification without job owner info
        }

        // Create notification for job owner
        String notificationId = "notif_" + UUID.randomUUID().toString();
        String title = "New Bid Received";
        String message = contractor.getFullName() + " submitted a bid of Rs. " +
                formatCurrency(bid.getBidAmount()) + " on your job \"" + job.getTitle() + "\"";

        Notification notification = new Notification(
                notificationId,
                job.getClientId(), // Send to job owner
                title,
                message,
                "bid",
                jobId // Store jobId so they can navigate to job details
        );

        // Send notification
        NotificationManager.getInstance()
                .createNotification(notification)
                .addOnSuccessListener(aVoid -> {
                    // Notification sent successfully (silent - no user feedback needed)
                })
                .addOnFailureListener(e -> {
                    // Failed to send notification (silent - don't interrupt user flow)
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
            btnSubmitBid.setEnabled(false);
            btnSubmitBid.setText("Submitting...");
            btnCancel.setEnabled(false);
            etBidAmount.setEnabled(false);
            etCompletionDays.setEnabled(false);
            etProposal.setEnabled(false);
            cbTerms.setEnabled(false);
        } else {
            btnSubmitBid.setEnabled(true);
            btnSubmitBid.setText("Submit Bid");
            btnCancel.setEnabled(true);
            etBidAmount.setEnabled(true);
            etCompletionDays.setEnabled(true);
            etProposal.setEnabled(true);
            cbTerms.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}```

---

### TaskDetailActivity.java

```java
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
import com.example.madproject.models.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {

    private TextView tvTaskTitle, tvTaskDescription, tvStartDate, tvEndDate, tvWorkers, tvDailyWages;
    private Button btnUpdateProgress, btnMarkComplete;
    private ProgressBar progressBar;

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

        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        tvTaskDescription = findViewById(R.id.tvTaskDescription);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvWorkers = findViewById(R.id.tvWorkers);
        tvDailyWages = findViewById(R.id.tvDailyWages);
        btnUpdateProgress = findViewById(R.id.btnUpdateProgress);
        btnMarkComplete = findViewById(R.id.btnMarkComplete);

        // Try to find ProgressBar, create if not in layout
        progressBar = findViewById(R.id.progressBar);
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnUpdateProgress.setOnClickListener(v -> showUpdateProgressDialog());
        btnMarkComplete.setOnClickListener(v -> markTaskComplete());
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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---

### TaskListActivity.java

```java

package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.TaskAdapter;
import com.example.madproject.firebase.TaskManager;
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

        for (Task task : allTasksList) {
            boolean matchesFilter = currentFilter.equals("all") ||
                                  task.getStatus().equals(currentFilter);

            if (matchesFilter) {
                filteredTasksList.add(task);
            }
        }

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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
```

---


## Firebase Managers

### BidManager.java

```java
package com.example.madproject.firebase;

import com.example.madproject.models.Bid;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class BidManager {
    private static BidManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "bids";

    private BidManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized BidManager getInstance() {
        if (instance == null) {
            instance = new BidManager();
        }
        return instance;
    }

    // CREATE - Submit new bid
    public Task<Void> createBid(Bid bid) {
        return db.collection(COLLECTION_NAME)
                .document(bid.getBidId())
                .set(bid);
    }

    // READ - Get single bid by ID
    public Task<DocumentSnapshot> getBid(String bidId) {
        return db.collection(COLLECTION_NAME)
                .document(bidId)
                .get();
    }

    // READ - Get all bids for a job
    public Task<QuerySnapshot> getBidsByJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .get();
    }

    // READ - Get bids by contractor
    public Task<QuerySnapshot> getBidsByContractor(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .get();
    }

    // READ - Get pending bids for a job
    public Task<QuerySnapshot> getPendingBidsByJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", "pending")
                .get();
    }

    // READ - Get accepted bids by contractor
    public Task<QuerySnapshot> getAcceptedBidsByContractor(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .whereEqualTo("status", "accepted")
                .get();
    }

    // READ - Get bids by status
    public Task<QuerySnapshot> getBidsByStatus(String status) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
    }

    // UPDATE - Update entire bid
    public Task<Void> updateBid(Bid bid) {
        return db.collection(COLLECTION_NAME)
                .document(bid.getBidId())
                .set(bid);
    }

    // UPDATE - Update specific field
    public Task<Void> updateField(String bidId, String field, Object value) {
        return db.collection(COLLECTION_NAME)
                .document(bidId)
                .update(field, value);
    }

    // UPDATE - Update bid status
    public Task<Void> updateBidStatus(String bidId, String status) {
        return updateField(bidId, "status", status);
    }

    // UPDATE - Accept bid
    public Task<Void> acceptBid(String bidId) {
        return updateBidStatus(bidId, "accepted");
    }

    // UPDATE - Reject bid
    public Task<Void> rejectBid(String bidId) {
        return updateBidStatus(bidId, "rejected");
    }

    // UPDATE - Reject all other bids for a job (when one is accepted)
    public void rejectOtherBids(String jobId, String acceptedBidId) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String bidId = doc.getId();
                        if (!bidId.equals(acceptedBidId)) {
                            rejectBid(bidId);
                        }
                    }
                });
    }

    // DELETE - Delete bid
    public Task<Void> deleteBid(String bidId) {
        return db.collection(COLLECTION_NAME)
                .document(bidId)
                .delete();
    }

    // QUERY - Get lowest bids for a job (sort in memory after fetching)
    public Task<QuerySnapshot> getLowestBids(String jobId, int limit) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", "pending")
                .get();
    }

    // QUERY - Check if contractor has already bid on job
    public Task<QuerySnapshot> checkExistingBid(String jobId, String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("contractorId", contractorId)
                .get();
    }
}```

---

### JobManager.java

```java
package com.example.madproject.firebase;

import com.example.madproject.models.Job;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class JobManager {
    private static JobManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "jobs";

    private JobManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized JobManager getInstance() {
        if (instance == null) {
            instance = new JobManager();
        }
        return instance;
    }

    // CREATE - Add new job
    public Task<Void> createJob(Job job) {
        return db.collection(COLLECTION_NAME)
                .document(job.getJobId())
                .set(job);
    }

    // READ - Get single job by ID
    public Task<DocumentSnapshot> getJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .get();
    }

    // READ - Get all jobs
    public Task<QuerySnapshot> getAllJobs() {
        return db.collection(COLLECTION_NAME)
                .orderBy("postedDate", Query.Direction.DESCENDING)
                .get();
    }

    // READ - Get open jobs
    public Task<QuerySnapshot> getOpenJobs() {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "open")
                .get();
    }

    // READ - Get jobs by client (FIXED - removed orderBy to avoid index requirement)
    public Task<QuerySnapshot> getJobsByClient(String clientId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("clientId", clientId)
                .get();
    }

    // READ - Get jobs by contractor
    public Task<QuerySnapshot> getJobsByContractor(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("assignedContractorId", contractorId)
                .get();
    }

    // READ - Get jobs by category
    public Task<QuerySnapshot> getJobsByCategory(String category) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("category", category)
                .whereEqualTo("status", "open")
                .get();
    }

    // READ - Get jobs by status
    public Task<QuerySnapshot> getJobsByStatus(String status) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
    }

    // READ - Get jobs by client and status
    public Task<QuerySnapshot> getJobsByClientAndStatus(String clientId, String status) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("clientId", clientId)
                .whereEqualTo("status", status)
                .get();
    }

    // UPDATE - Update entire job
    public Task<Void> updateJob(Job job) {
        return db.collection(COLLECTION_NAME)
                .document(job.getJobId())
                .set(job);
    }

    // UPDATE - Update specific field
    public Task<Void> updateField(String jobId, String field, Object value) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .update(field, value);
    }

    // UPDATE - Update job status
    public Task<Void> updateJobStatus(String jobId, String status) {
        return updateField(jobId, "status", status);
    }

    // UPDATE - Increment total bids (using atomic increment)
    public Task<Void> incrementTotalBids(String jobId) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .update("totalBids", FieldValue.increment(1));
    }

    // UPDATE - Assign contractor to job
    public Task<Void> assignContractor(String jobId, String contractorId, String contractorName, String bidId) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .update(
                        "assignedContractorId", contractorId,
                        "assignedContractorName", contractorName,
                        "acceptedBidId", bidId,
                        "status", "in_progress",
                        "startDate", System.currentTimeMillis()
                );
    }

    // UPDATE - Complete job
    public Task<Void> completeJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .update(
                        "status", "completed",
                        "completedDate", System.currentTimeMillis()
                );
    }

    // DELETE - Delete job
    public Task<Void> deleteJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .delete();
    }

    // SEARCH - Search jobs by title
    public Task<QuerySnapshot> searchJobsByTitle(String title) {
        return db.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("title", title)
                .whereLessThanOrEqualTo("title", title + "\uf8ff")
                .get();
    }

    // QUERY - Get jobs by budget range
    public Task<QuerySnapshot> getJobsByBudgetRange(double minBudget, double maxBudget) {
        return db.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("budget", minBudget)
                .whereLessThanOrEqualTo("budget", maxBudget)
                .whereEqualTo("status", "open")
                .get();
    }
}```

---

### MaterialManager.java

```java
package com.example.madproject.firebase;

import com.example.madproject.models.Material;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class MaterialManager {
    private static MaterialManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "materials";

    private MaterialManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized MaterialManager getInstance() {
        if (instance == null) {
            instance = new MaterialManager();
        }
        return instance;
    }

    // CREATE - Add new material
    public Task<Void> createMaterial(Material material) {
        return db.collection(COLLECTION_NAME)
                .document(material.getMaterialId())
                .set(material);
    }

    // READ - Get single material by ID
    public Task<DocumentSnapshot> getMaterial(String materialId) {
        return db.collection(COLLECTION_NAME)
                .document(materialId)
                .get();
    }

    // READ - Get all materials for a job
    public Task<QuerySnapshot> getMaterialsByJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .orderBy("addedDate", Query.Direction.DESCENDING)
                .get();
    }

    // READ - Get materials by category
    public Task<QuerySnapshot> getMaterialsByCategory(String jobId, String category) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("category", category)
                .get();
    }

    // READ - Get materials by status
    public Task<QuerySnapshot> getMaterialsByStatus(String jobId, String status) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", status)
                .get();
    }

    // READ - Get low stock materials
    public Task<QuerySnapshot> getLowStockMaterials(String jobId) {
        return getMaterialsByStatus(jobId, "low_stock");
    }

    // READ - Get out of stock materials
    public Task<QuerySnapshot> getOutOfStockMaterials(String jobId) {
        return getMaterialsByStatus(jobId, "out_of_stock");
    }

    // UPDATE - Update entire material
    public Task<Void> updateMaterial(Material material) {
        material.setLastUpdated(System.currentTimeMillis());
        return db.collection(COLLECTION_NAME)
                .document(material.getMaterialId())
                .set(material);
    }

    // UPDATE - Update specific field
    public Task<Void> updateField(String materialId, String field, Object value) {
        return db.collection(COLLECTION_NAME)
                .document(materialId)
                .update(
                        field, value,
                        "lastUpdated", System.currentTimeMillis()
                );
    }

    // UPDATE - Update quantity
    public void updateQuantity(String materialId, double newQuantity) {
        db.collection(COLLECTION_NAME)
                .document(materialId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Material material = documentSnapshot.toObject(Material.class);
                        if (material != null) {
                            material.setQuantity(newQuantity);
                            updateMaterial(material);
                        }
                    }
                });
    }

    // UPDATE - Add quantity (restock)
    public void addQuantity(String materialId, double addedQuantity) {
        db.collection(COLLECTION_NAME)
                .document(materialId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Material material = documentSnapshot.toObject(Material.class);
                        if (material != null) {
                            material.setQuantity(material.getQuantity() + addedQuantity);
                            updateMaterial(material);
                        }
                    }
                });
    }

    // UPDATE - Deduct quantity (usage)
    public void deductQuantity(String materialId, double usedQuantity) {
        db.collection(COLLECTION_NAME)
                .document(materialId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Material material = documentSnapshot.toObject(Material.class);
                        if (material != null) {
                            material.setQuantity(material.getQuantity() - usedQuantity);
                            updateMaterial(material);
                        }
                    }
                });
    }

    // DELETE - Delete material
    public Task<Void> deleteMaterial(String materialId) {
        return db.collection(COLLECTION_NAME)
                .document(materialId)
                .delete();
    }

    // QUERY - Calculate total inventory value
    public void calculateTotalInventoryValue(String jobId, OnTotalCalculatedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalValue = 0.0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Material material = doc.toObject(Material.class);
                        if (material != null) {
                            totalValue += material.getTotalCost();
                        }
                    }
                    listener.onTotalCalculated(totalValue);
                });
    }

    // Callback interface
    public interface OnTotalCalculatedListener {
        void onTotalCalculated(double totalValue);
    }
}```

---

### MessageManager.java

```java
package com.example.madproject.firebase;

import com.example.madproject.models.Message;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class MessageManager {
    private static MessageManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "messages";

    private MessageManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }
        return instance;
    }

    // CREATE - Send new message
    public Task<Void> createMessage(Message message) {
        return db.collection(COLLECTION_NAME)
                .document(message.getMessageId())
                .set(message);
    }

    // READ - Get single message by ID
    public Task<DocumentSnapshot> getMessage(String messageId) {
        return db.collection(COLLECTION_NAME)
                .document(messageId)
                .get();
    }

    // READ - Get messages in a chat (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getMessagesByChat(String chatId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .get();
    }

    // READ - Get messages in a chat with limit (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getRecentMessages(String chatId, int limit) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .limit(limit)
                .get();
    }

    // READ - Get unread messages for user (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getUnreadMessages(String userId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get();
    }

    // READ - Get unread count
    public void getUnreadCount(String userId, OnCountLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onCountLoaded(queryDocumentSnapshots.size());
                });
    }

    // UPDATE - Mark message as read
    public Task<Void> markAsRead(String messageId) {
        return db.collection(COLLECTION_NAME)
                .document(messageId)
                .update(
                        "isRead", true,
                        "readAt", System.currentTimeMillis()
                );
    }

    // UPDATE - Mark all messages in chat as read
    public void markAllAsRead(String chatId, String userId) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        markAsRead(doc.getId());
                    }
                });
    }

    // DELETE - Delete message
    public Task<Void> deleteMessage(String messageId) {
        return db.collection(COLLECTION_NAME)
                .document(messageId)
                .delete();
    }

    // DELETE - Delete all messages in a chat
    public void deleteChat(String chatId) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        deleteMessage(doc.getId());
                    }
                });
    }

    // REAL-TIME - Listen to messages in chat (sort in memory to avoid index requirement)
    public void listenToMessages(String chatId, OnMessagesChangedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        listener.onMessagesChanged(value);
                    }
                });
    }

    // Callback interfaces
    public interface OnCountLoadedListener {
        void onCountLoaded(int count);
    }

    public interface OnMessagesChangedListener {
        void onMessagesChanged(QuerySnapshot messages);
        void onError(String error);
    }
}```

---

### NotificationManager.java

```java
package com.example.madproject.firebase;

import com.example.madproject.models.Notification;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class NotificationManager {
    private static NotificationManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "notifications";

    private NotificationManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    // CREATE - Send notification
    public Task<Void> createNotification(Notification notification) {
        return db.collection(COLLECTION_NAME)
                .document(notification.getNotificationId())
                .set(notification);
    }

    // READ - Get single notification by ID
    public Task<DocumentSnapshot> getNotification(String notificationId) {
        return db.collection(COLLECTION_NAME)
                .document(notificationId)
                .get();
    }

    // READ - Get all notifications for user (sort in memory after fetching)
    public Task<QuerySnapshot> getNotificationsByUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get();
    }

    // READ - Get unread notifications (sort in memory after fetching)
    public Task<QuerySnapshot> getUnreadNotifications(String userId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get();
    }

    // READ - Get notifications by type (sort in memory after fetching)
    public Task<QuerySnapshot> getNotificationsByType(String userId, String type) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type)
                .get();
    }

    // READ - Get unread count
    public void getUnreadCount(String userId, OnCountLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onCountLoaded(queryDocumentSnapshots.size());
                });
    }

    // UPDATE - Mark as read
    public Task<Void> markAsRead(String notificationId) {
        return db.collection(COLLECTION_NAME)
                .document(notificationId)
                .update(
                        "isRead", true,
                        "readAt", System.currentTimeMillis()
                );
    }

    // UPDATE - Mark all as read
    public void markAllAsRead(String userId) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        markAsRead(doc.getId());
                    }
                });
    }

    // DELETE - Delete notification
    public Task<Void> deleteNotification(String notificationId) {
        return db.collection(COLLECTION_NAME)
                .document(notificationId)
                .delete();
    }

    // DELETE - Delete all notifications for user
    public void deleteAllNotifications(String userId) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        deleteNotification(doc.getId());
                    }
                });
    }

    // REAL-TIME - Listen to notifications (sort in memory after fetching)
    public com.google.firebase.firestore.ListenerRegistration listenToNotifications(String userId, OnNotificationsChangedListener listener) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        listener.onNotificationsChanged(value);
                    }
                });
    }

    // Callback interfaces
    public interface OnCountLoadedListener {
        void onCountLoaded(int count);
    }

    public interface OnNotificationsChangedListener {
        void onNotificationsChanged(QuerySnapshot notifications);
        void onError(String error);
    }
}```

---

### ReviewManager.java

```java
package com.example.madproject.firebase;

import com.example.madproject.models.Review;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class ReviewManager {
    private static ReviewManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "reviews";

    private ReviewManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized ReviewManager getInstance() {
        if (instance == null) {
            instance = new ReviewManager();
        }
        return instance;
    }

    // CREATE - Submit review
    public Task<Void> createReview(Review review) {
        return db.collection(COLLECTION_NAME)
                .document(review.getReviewId())
                .set(review);
    }

    // READ - Get single review by ID
    public Task<DocumentSnapshot> getReview(String reviewId) {
        return db.collection(COLLECTION_NAME)
                .document(reviewId)
                .get();
    }

    // READ - Get reviews for contractor (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getReviewsByContractor(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .get();
    }

    // READ - Get reviews by client (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getReviewsByClient(String clientId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("clientId", clientId)
                .get();
    }

    // READ - Get reviews for job
    public Task<QuerySnapshot> getReviewsByJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .get();
    }

    // READ - Get reviews by rating (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getReviewsByRating(String contractorId, float minRating) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .whereGreaterThanOrEqualTo("rating", minRating)
                .get();
    }

    // READ - Get verified reviews only (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getVerifiedReviews(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .whereEqualTo("isVerified", true)
                .get();
    }

    // UPDATE - Update entire review
    public Task<Void> updateReview(Review review) {
        return db.collection(COLLECTION_NAME)
                .document(review.getReviewId())
                .set(review);
    }

    // UPDATE - Add contractor response
    public Task<Void> addResponse(String reviewId, String response) {
        return db.collection(COLLECTION_NAME)
                .document(reviewId)
                .update(
                        "response", response,
                        "responseDate", System.currentTimeMillis()
                );
    }

    // DELETE - Delete review
    public Task<Void> deleteReview(String reviewId) {
        return db.collection(COLLECTION_NAME)
                .document(reviewId)
                .delete();
    }

    // CALCULATE - Calculate average rating
    public void calculateAverageRating(String contractorId, OnRatingCalculatedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onRatingCalculated(0.0, 0);
                        return;
                    }

                    float totalRating = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            totalRating += review.getRating();
                            count++;
                        }
                    }

                    double averageRating = count > 0 ? (double) totalRating / count : 0.0;
                    listener.onRatingCalculated(averageRating, count);
                });
    }

    // Callback interface
    public interface OnRatingCalculatedListener {
        void onRatingCalculated(double averageRating, int totalReviews);
    }
}```

---

### TaskManager.java

```java
package com.example.madproject.firebase;

import com.example.madproject.models.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class TaskManager {
    private static TaskManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "tasks";

    private TaskManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }

    // CREATE - Add new task
    public com.google.android.gms.tasks.Task<Void> createTask(com.example.madproject.models.Task task) {
        return db.collection(COLLECTION_NAME)
                .document(task.getTaskId())
                .set(task);
    }

    // READ - Get single task by ID
    public com.google.android.gms.tasks.Task<DocumentSnapshot> getTask(String taskId) {
        return db.collection(COLLECTION_NAME)
                .document(taskId)
                .get();
    }

    // READ - Get all tasks for a job
    public com.google.android.gms.tasks.Task<QuerySnapshot> getTasksByJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    // READ - Get tasks by status
    public com.google.android.gms.tasks.Task<QuerySnapshot> getTasksByStatus(String jobId, String status) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    // READ - Get ongoing tasks
    public com.google.android.gms.tasks.Task<QuerySnapshot> getOngoingTasks(String jobId) {
        return getTasksByStatus(jobId, "ongoing");
    }

    // READ - Get completed tasks
    public com.google.android.gms.tasks.Task<QuerySnapshot> getCompletedTasks(String jobId) {
        return getTasksByStatus(jobId, "completed");
    }

    // UPDATE - Update entire task
    public com.google.android.gms.tasks.Task<Void> updateTask(com.example.madproject.models.Task task) {
        task.setUpdatedAt(System.currentTimeMillis());
        return db.collection(COLLECTION_NAME)
                .document(task.getTaskId())
                .set(task);
    }

    // UPDATE - Update specific field
    public com.google.android.gms.tasks.Task<Void> updateField(String taskId, String field, Object value) {
        return db.collection(COLLECTION_NAME)
                .document(taskId)
                .update(
                        field, value,
                        "updatedAt", System.currentTimeMillis()
                );
    }

    // UPDATE - Update task status
    public com.google.android.gms.tasks.Task<Void> updateTaskStatus(String taskId, String status) {
        return updateField(taskId, "status", status);
    }

    // UPDATE - Update progress
    public com.google.android.gms.tasks.Task<Void> updateProgress(String taskId, double completedQuantity) {
        return db.collection(COLLECTION_NAME)
                .document(taskId)
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        com.example.madproject.models.Task taskObj = task.getResult().toObject(com.example.madproject.models.Task.class);
                        if (taskObj != null) {
                            taskObj.setCompletedQuantity(completedQuantity);
                            taskObj.calculateProgress();
                            return updateTask(taskObj);
                        }
                    }
                    return com.google.android.gms.tasks.Tasks.forException(new Exception("Task not found"));
                });
    }

    // UPDATE - Mark task as complete
    public com.google.android.gms.tasks.Task<Void> completeTask(String taskId) {
        return db.collection(COLLECTION_NAME)
                .document(taskId)
                .update(
                        "status", "completed",
                        "progressPercentage", 100.0,
                        "updatedAt", System.currentTimeMillis()
                );
    }

    // DELETE - Delete task
    public com.google.android.gms.tasks.Task<Void> deleteTask(String taskId) {
        return db.collection(COLLECTION_NAME)
                .document(taskId)
                .delete();
    }

    // QUERY - Get tasks by date range
    public com.google.android.gms.tasks.Task<QuerySnapshot> getTasksByDateRange(String jobId, long startDate, long endDate) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereGreaterThanOrEqualTo("startDate", startDate)
                .whereLessThanOrEqualTo("endDate", endDate)
                .get();
    }
}```

---

### UserManager.java

```java
package com.example.madproject.firebase;

import com.example.madproject.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private static UserManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "users";

    private UserManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // CREATE - Add new user
    public Task<Void> createUser(User user) {
        return db.collection(COLLECTION_NAME)
                .document(user.getUserId())
                .set(user);
    }

    // READ - Get single user by ID
    public Task<DocumentSnapshot> getUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .get();
    }

    // READ - Get user as User object
    public void getUserObject(String userId, OnUserLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        listener.onUserLoaded(user);
                    } else {
                        listener.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // READ - Get all users
    public Task<QuerySnapshot> getAllUsers() {
        return db.collection(COLLECTION_NAME).get();
    }

    // READ - Get all contractors
    public Task<QuerySnapshot> getAllContractors() {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userType", "contractor")
                .get();
    }

    // READ - Get contractors by category
    public Task<QuerySnapshot> getContractorsByCategory(String category) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userType", "contractor")
                .whereEqualTo("category", category)
                .get();
    }

    // READ - Get top rated contractors
    public Task<QuerySnapshot> getTopRatedContractors(int limit) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userType", "contractor")
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
    }

    // UPDATE - Update entire user
    public Task<Void> updateUser(User user) {
        return db.collection(COLLECTION_NAME)
                .document(user.getUserId())
                .set(user);
    }

    // UPDATE - Update specific field
    public Task<Void> updateField(String userId, String field, Object value) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .update(field, value);
    }

    // UPDATE - Update profile picture
    public Task<Void> updateProfilePicture(String userId, String imageUrl) {
        return updateField(userId, "profilePictureUrl", imageUrl);
    }

    // UPDATE - Update rating
    public Task<Void> updateRating(String userId, double rating, int totalReviews) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .update(
                        "rating", rating,
                        "totalReviews", totalReviews
                );
    }

    // UPDATE - Increment completed projects
    public void incrementCompletedProjects(String userId) {
        db.collection(COLLECTION_NAME)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setCompletedProjects(user.getCompletedProjects() + 1);
                            updateUser(user);
                        }
                    }
                });
    }

    // DELETE - Delete user
    public Task<Void> deleteUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .delete();
    }

    // SEARCH - Search users by name
    public Task<QuerySnapshot> searchUsersByName(String name) {
        return db.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("fullName", name)
                .whereLessThanOrEqualTo("fullName", name + "\uf8ff")
                .get();
    }

    // Callback interface
    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onError(String error);
    }
}```

---


## Adapters

### BidAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Bid;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class BidAdapter extends RecyclerView.Adapter<BidAdapter.BidViewHolder> {

    private Context context;
    private List<Bid> bidList;
    private OnBidActionListener listener;
    private String currentUserId;
    private String jobClientId;

    public interface OnBidActionListener {
        void onAcceptBid(Bid bid);
        void onRejectBid(Bid bid);
        void onViewProfile(Bid bid);
        void onContactContractor(Bid bid);
    }

    public BidAdapter(Context context, List<Bid> bidList, String currentUserId, String jobClientId, OnBidActionListener listener) {
        this.context = context;
        this.bidList = bidList;
        this.currentUserId = currentUserId;
        this.jobClientId = jobClientId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bid_card, parent, false);
        return new BidViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BidViewHolder holder, int position) {
        Bid bid = bidList.get(position);

        // Set contractor name
        holder.tvContractorName.setText(bid.getContractorName());

        // Set category
        if (bid.getContractorCategory() != null) {
            holder.tvContractorCategory.setText(bid.getContractorCategory());
        } else {
            holder.tvContractorCategory.setVisibility(View.GONE);
        }

        // Set rating
        if (bid.getContractorRating() > 0) {
            holder.tvRating.setText(String.format("⭐ %.1f", bid.getContractorRating()));
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }

        // Set completed projects
        holder.tvCompletedProjects.setText(bid.getContractorCompletedProjects() + " projects completed");

        // Format and set bid amount
        String bidAmountText = "Rs. " + formatCurrency(bid.getBidAmount());
        holder.tvBidAmount.setText(bidAmountText);

        // Set completion days
        holder.tvCompletionDays.setText(bid.getCompletionDays() + " days");

        // Set proposal
        holder.tvProposal.setText(bid.getProposal());

        // Set submitted date
        String dateText = getRelativeTime(bid.getSubmittedDate());
        holder.tvSubmittedDate.setText("Submitted " + dateText);

        // Set status badge
        holder.tvBidStatus.setText(bid.getStatus().toUpperCase());
        setBidStatusColor(holder.tvBidStatus, bid.getStatus());

        // Show/hide action buttons based on status AND if current user is job owner
        boolean isJobOwner = currentUserId != null && currentUserId.equals(jobClientId);
        boolean isPending = "pending".equals(bid.getStatus());

        if (isJobOwner && isPending) {
            // Only job owner can accept/reject pending bids
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.tvBidStatus.setVisibility(View.GONE);
        } else {
            // Hide accept/reject buttons for contractors or non-pending bids
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.tvBidStatus.setVisibility(View.VISIBLE);
        }

        // Set click listeners
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAcceptBid(bid);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRejectBid(bid);
            }
        });

        holder.btnViewProfile.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewProfile(bid);
            }
        });

        holder.btnContact.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactContractor(bid);
            }
        });

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewProfile(bid);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bidList.size();
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

    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    private void setBidStatusColor(TextView textView, String status) {
        int color;
        switch (status.toLowerCase()) {
            case "accepted":
                color = 0xFF4CAF50; // Green
                break;
            case "rejected":
                color = 0xFFF44336; // Red
                break;
            default:
                color = 0xFFFFA726; // Orange
                break;
        }
        textView.setTextColor(color);
    }

    static class BidViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        CircleImageView ivContractorPhoto;
        TextView tvContractorName, tvContractorCategory, tvRating, tvCompletedProjects;
        TextView tvBidAmount, tvCompletionDays, tvProposal, tvSubmittedDate, tvBidStatus;
        Button btnAccept, btnReject, btnViewProfile, btnContact;

        public BidViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivContractorPhoto = itemView.findViewById(R.id.ivContractorPhoto);
            tvContractorName = itemView.findViewById(R.id.tvContractorName);
            tvContractorCategory = itemView.findViewById(R.id.tvContractorCategory);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvCompletedProjects = itemView.findViewById(R.id.tvCompletedProjects);
            tvBidAmount = itemView.findViewById(R.id.tvBidAmount);
            tvCompletionDays = itemView.findViewById(R.id.tvCompletionDays);
            tvProposal = itemView.findViewById(R.id.tvProposal);
            tvSubmittedDate = itemView.findViewById(R.id.tvSubmittedDate);
            tvBidStatus = itemView.findViewById(R.id.tvBidStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
            btnContact = itemView.findViewById(R.id.btnContact);
        }
    }
}```

---

### ChatMessageAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ChatMessageAdapter
 *
 * Displays chat messages in a RecyclerView with different styles for:
 * - User messages (right side, purple background)
 * - AI messages (left side, white background)
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private Context context;
    private List<ChatMessage> messageList;

    public ChatMessageAdapter(Context context, List<ChatMessage> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        // Set message text
        holder.tvMessage.setText(message.getMessage());

        // Set timestamp
        String time = formatTime(message.getTimestamp());
        holder.tvTime.setText(time);

        // Style based on sender
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageCard.getLayoutParams();

        if (message.isUser()) {
            // User message (right side, purple background)
            params.gravity = Gravity.END;
            params.setMargins(48, 8, 8, 8);
            holder.messageCard.setLayoutParams(params);

            holder.messageCard.setCardBackgroundColor(0xFF7C4DFF); // Purple
            holder.tvMessage.setTextColor(0xFFFFFFFF); // White text
            holder.tvTime.setTextColor(0xFFE1BEE7); // Light purple
        } else {
            // AI message (left side, white background)
            params.gravity = Gravity.START;
            params.setMargins(8, 8, 48, 8);
            holder.messageCard.setLayoutParams(params);

            holder.messageCard.setCardBackgroundColor(0xFFFFFFFF); // White
            holder.tvMessage.setTextColor(0xFF212121); // Dark text
            holder.tvTime.setTextColor(0xFF9E9E9E); // Grey
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    /**
     * Format timestamp to readable time
     */
    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        CardView messageCard;
        TextView tvMessage, tvTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.messageCard);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}```

---

### ContractorAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContractorAdapter extends RecyclerView.Adapter<ContractorAdapter.ContractorViewHolder> {

    private Context context;
    private List<User> contractorList;
    private OnContractorClickListener listener;

    public interface OnContractorClickListener {
        void onContractorClick(User contractor);
    }

    public ContractorAdapter(Context context, List<User> contractorList, OnContractorClickListener listener) {
        this.context = context;
        this.contractorList = contractorList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContractorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contractor, parent, false);
        return new ContractorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContractorViewHolder holder, int position) {
        User contractor = contractorList.get(position);

        // Set contractor name
        holder.tvContractorName.setText(contractor.getFullName());

        // Set category
        if (contractor.getCategory() != null) {
            holder.tvCategory.setText(contractor.getCategory());
            holder.tvCategory.setVisibility(View.VISIBLE);
        } else {
            holder.tvCategory.setVisibility(View.GONE);
        }

        // Set rating
        if (contractor.getRating() > 0) {
            holder.tvRating.setText(String.format("%.1f (%d reviews)",
                    contractor.getRating(), contractor.getTotalReviews()));
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setText("No reviews yet");
            holder.tvRating.setVisibility(View.VISIBLE);
        }

        // Set experience
        if (contractor.getExperienceYears() > 0) {
            holder.tvExperience.setText(contractor.getExperienceYears() + " years experience");
            holder.tvExperience.setVisibility(View.VISIBLE);
        } else {
            holder.tvExperience.setVisibility(View.GONE);
        }

        // Set completed projects
        holder.tvCompletedProjects.setText(contractor.getCompletedProjects() + " projects completed");

        // Set hourly rate
        if (contractor.getHourlyRate() > 0) {
            holder.tvHourlyRate.setText("Rs. " + formatCurrency(contractor.getHourlyRate()) + "/hr");
            holder.tvHourlyRate.setVisibility(View.VISIBLE);
        } else {
            holder.tvHourlyRate.setVisibility(View.GONE);
        }

        // Set location
        if (contractor.getCity() != null && !contractor.getCity().isEmpty()) {
            holder.tvLocation.setText(contractor.getCity());
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContractorClick(contractor);
            }
        });

        // TODO: Load profile image using Glide/Picasso
        // if (contractor.getProfilePictureUrl() != null) {
        //     Glide.with(context).load(contractor.getProfilePictureUrl()).into(holder.ivContractorImage);
        // }
    }

    @Override
    public int getItemCount() {
        return contractorList.size();
    }

    private String formatCurrency(double amount) {
        if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    public void updateList(List<User> newList) {
        this.contractorList = newList;
        notifyDataSetChanged();
    }

    static class ContractorViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        CircleImageView ivContractorImage;
        TextView tvContractorName, tvCategory, tvRating, tvExperience;
        TextView tvCompletedProjects, tvHourlyRate, tvLocation;

        public ContractorViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivContractorImage = itemView.findViewById(R.id.ivContractorImage);
            tvContractorName = itemView.findViewById(R.id.tvContractorName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvCompletedProjects = itemView.findViewById(R.id.tvCompletedProjects);
            tvHourlyRate = itemView.findViewById(R.id.tvHourlyRate);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }
    }
}
```

---

### JobAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Job;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {

    private Context context;
    private List<Job> jobList;
    private OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(Job job);
    }

    public JobAdapter(Context context, List<Job> jobList, OnJobClickListener listener) {
        this.context = context;
        this.jobList = jobList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_job_card, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobList.get(position);

        // Set job title
        holder.tvJobTitle.setText(job.getTitle());

        // Set category
        holder.tvCategory.setText(job.getCategory());

        // Format and set budget
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "PK"));
        String budgetText = "PKR " + formatCurrency(job.getBudget());
        holder.tvBudget.setText(budgetText);

        // Set location
        holder.tvLocation.setText(job.getLocation());

        // Set posted date
        String dateText = getRelativeTime(job.getPostedDate());
        holder.tvPostedDate.setText(dateText);

        // Set status with color
        holder.tvStatus.setText(job.getStatus().toUpperCase());
        setStatusColor(holder.tvStatus, job.getStatus());

        // Set bid count
        String bidsText = job.getTotalBids() + " bids";
        holder.tvBidCount.setText(bidsText);

        // Set click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onJobClick(job);
            }
        });
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    private String formatCurrency(double amount) {
        if (amount >= 10000000) { // 1 Crore
            return String.format("%.1f Cr", amount / 10000000);
        } else if (amount >= 100000) { // 1 Lakh
            return String.format("%.1f L", amount / 100000);
        } else if (amount >= 1000) { // 1 Thousand
            return String.format("%.1f K", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    private void setStatusColor(TextView textView, String status) {
        int color;
        switch (status.toLowerCase()) {
            case "open":
                color = 0xFF4CAF50; // Green
                break;
            case "in_progress":
                color = 0xFFFFA726; // Orange
                break;
            case "completed":
                color = 0xFF2196F3; // Blue
                break;
            case "cancelled":
                color = 0xFFF44336; // Red
                break;
            default:
                color = 0xFF757575; // Grey
                break;
        }
        textView.setTextColor(color);
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvJobTitle, tvCategory, tvBudget, tvLocation, tvPostedDate, tvStatus, tvBidCount;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPostedDate = itemView.findViewById(R.id.tvPostedDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvBidCount = itemView.findViewById(R.id.tvBidCount);
        }
    }
}```

---

### MaterialAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Material;

import java.util.List;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder> {

    private Context context;
    private List<Material> materialList;
    private OnMaterialClickListener listener;

    public interface OnMaterialClickListener {
        void onMaterialClick(Material material);
    }

    public MaterialAdapter(Context context, List<Material> materialList, OnMaterialClickListener listener) {
        this.context = context;
        this.materialList = materialList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_material_card, parent, false);
        return new MaterialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        Material material = materialList.get(position);

        // Set material name
        holder.tvMaterialName.setText(material.getMaterialName());

        // Set stock status with color
        holder.tvStockStatus.setText(material.getStatus().replace("_", " "));
        setStatusColor(holder.tvStockStatus, material.getStatus());

        // Set category and unit
        String categoryText = material.getCategory() + " • " + capitalizeFirst(material.getUnit());
        holder.tvCategory.setText(categoryText);

        // Set quantity
        holder.tvQuantity.setText(String.valueOf((int) material.getQuantity()));

        // Set price
        String priceText = "Rs. " + formatCurrency(material.getUnitPrice());
        holder.tvPrice.setText(priceText);

        // Set last updated
        String lastUpdatedText = "Updated: " + getRelativeTime(material.getLastUpdated());
        holder.tvLastUpdated.setText(lastUpdatedText);

        // Set supplier
        String supplierText = "Supplier: " + (material.getSupplier() != null ? material.getSupplier() : "N/A");
        holder.tvSupplier.setText(supplierText);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMaterialClick(material);
            }
        });
    }

    @Override
    public int getItemCount() {
        return materialList.size();
    }

    private String formatCurrency(double amount) {
        if (amount >= 10000000) { // 1 Crore
            return String.format("%.1f Cr", amount / 10000000);
        } else if (amount >= 100000) { // 1 Lakh
            return String.format("%.1f L", amount / 100000);
        } else if (amount >= 1000) { // 1 Thousand
            return String.format("%.1f K", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    private String getRelativeTime(long timestamp) {
        if (timestamp == 0) {
            return "N/A";
        }

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    private void setStatusColor(TextView textView, String status) {
        int color;
        int backgroundColor;

        switch (status.toLowerCase()) {
            case "in_stock":
                color = 0xFF4CAF50; // Green
                textView.setText("In Stock");
                break;
            case "low_stock":
                color = 0xFFFFA726; // Orange
                textView.setText("Low Stock");
                break;
            case "out_of_stock":
                color = 0xFFF44336; // Red
                textView.setText("Out of Stock");
                break;
            default:
                color = 0xFF757575; // Grey
                textView.setText(status);
                break;
        }
        textView.setTextColor(color);
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    static class MaterialViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMaterialIcon;
        TextView tvMaterialName, tvStockStatus, tvCategory, tvQuantity,
                 tvPrice, tvLastUpdated, tvSupplier;

        public MaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMaterialIcon = itemView.findViewById(R.id.ivMaterialIcon);
            tvMaterialName = itemView.findViewById(R.id.tvMaterialName);
            tvStockStatus = itemView.findViewById(R.id.tvStockStatus);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
            tvSupplier = itemView.findViewById(R.id.tvSupplier);
        }
    }
}
```

---

### MessageAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<Message> messageList;
    private String currentUserId;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                           FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);

        // Hide all layouts first
        holder.receivedMessageLayout.setVisibility(View.GONE);
        holder.sentMessageLayout.setVisibility(View.GONE);
        holder.imageMessageLayout.setVisibility(View.GONE);

        String timeText = formatTime(message.getTimestamp());

        if (message.getMessageType().equals("image") && message.getAttachmentUrl() != null) {
            // Show image message
            holder.imageMessageLayout.setVisibility(View.VISIBLE);
            // TODO: Load image using Glide/Picasso
            // Glide.with(context).load(message.getAttachmentUrl()).into(holder.ivMessageImage);
            if (message.getMessageText() != null && !message.getMessageText().isEmpty()) {
                holder.tvImageCaption.setVisibility(View.VISIBLE);
                holder.tvImageCaption.setText(message.getMessageText());
            } else {
                holder.tvImageCaption.setVisibility(View.GONE);
            }
        } else if (isSent) {
            // Show sent message
            holder.sentMessageLayout.setVisibility(View.VISIBLE);
            holder.tvSentMessage.setText(message.getMessageText());
            holder.tvSentTime.setText(timeText);

            // Show read status
            if (message.isRead()) {
                holder.ivMessageStatus.setVisibility(View.VISIBLE);
                holder.ivMessageStatus.setImageResource(R.drawable.ic_check);
                holder.ivMessageStatus.setColorFilter(0xFF4CAF50); // Green for read
            } else {
                holder.ivMessageStatus.setVisibility(View.VISIBLE);
                holder.ivMessageStatus.setImageResource(R.drawable.ic_check);
                holder.ivMessageStatus.setColorFilter(0xFF9E9E9E); // Grey for sent but not read
            }
        } else {
            // Show received message
            holder.receivedMessageLayout.setVisibility(View.VISIBLE);
            holder.tvReceivedMessage.setText(message.getMessageText());
            holder.tvReceivedTime.setText(timeText);

            // Load sender profile picture
            // TODO: Load sender image using Glide/Picasso
            // if (message.getSenderPhotoUrl() != null) {
            //     Glide.with(context).load(message.getSenderPhotoUrl()).into(holder.ivSenderImage);
            // }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout receivedMessageLayout, sentMessageLayout, imageMessageLayout;
        CircleImageView ivSenderImage;
        TextView tvReceivedMessage, tvReceivedTime;
        TextView tvSentMessage, tvSentTime;
        ImageView ivMessageStatus, ivMessageImage;
        TextView tvImageCaption;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            receivedMessageLayout = itemView.findViewById(R.id.receivedMessageLayout);
            sentMessageLayout = itemView.findViewById(R.id.sentMessageLayout);
            imageMessageLayout = itemView.findViewById(R.id.imageMessageLayout);

            ivSenderImage = itemView.findViewById(R.id.ivSenderImage);
            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);

            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentTime = itemView.findViewById(R.id.tvSentTime);
            ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);

            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            tvImageCaption = itemView.findViewById(R.id.tvImageCaption);
        }
    }
}
```

---

### NotificationAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Notification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notification> notificationList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notificationList, OnNotificationClickListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        // Set notification title
        holder.tvNotificationTitle.setText(notification.getTitle());

        // Set notification message
        holder.tvNotificationMessage.setText(notification.getMessage());

        // Set time ago
        holder.tvTime.setText(getTimeAgo(notification.getTimestamp()));

        // Set icon based on notification type
        setNotificationIcon(holder.ivNotificationIcon, notification.getType());

        // Show/hide unread indicator
        holder.unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        // Set background based on read status
        if (!notification.isRead()) {
            holder.itemView.setBackgroundColor(0xFFF5F5F5); // Light grey for unread
        } else {
            holder.itemView.setBackgroundColor(0xFFFFFFFF); // White for read
        }

        // Hide action button for now
        holder.btnAction.setVisibility(View.GONE);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    private void setNotificationIcon(ImageView imageView, String type) {
        int iconRes;
        switch (type.toLowerCase()) {
            case "job":
                iconRes = R.drawable.ic_job;
                break;
            case "bid":
                iconRes = R.drawable.ic_bid;
                break;
            case "message":
                iconRes = R.drawable.ic_message;
                break;
            case "task":
                iconRes = R.drawable.ic_task;
                break;
            case "payment":
                iconRes = R.drawable.ic_payment;
                break;
            case "system":
            default:
                iconRes = R.drawable.ic_notifications;
                break;
        }
        imageView.setImageResource(iconRes);
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        if (weeks > 0) {
            return weeks + "w ago";
        } else if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "Just now";
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivNotificationIcon;
        View unreadIndicator;
        TextView tvNotificationTitle, tvTime, tvNotificationMessage;
        Button btnAction;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivNotificationIcon = itemView.findViewById(R.id.ivNotificationIcon);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
```

---

### PortfolioAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;

import java.util.List;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.PortfolioViewHolder> {

    private Context context;
    private List<String> imageUrls;
    private OnPortfolioItemClickListener listener;

    public interface OnPortfolioItemClickListener {
        void onItemClick(String imageUrl, int position);
        void onDeleteClick(String imageUrl, int position);
    }

    public PortfolioAdapter(Context context, List<String> imageUrls, OnPortfolioItemClickListener listener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PortfolioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_portfolio, parent, false);
        return new PortfolioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PortfolioViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);

        // Load image using placeholder for now
        // TODO: Load image using Glide/Picasso
        // Glide.with(context).load(imageUrl).into(holder.ivPortfolioImage);

        // Set placeholder
        holder.ivPortfolioImage.setImageResource(R.drawable.ic_portfolio);

        holder.ivPortfolioImage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(imageUrl, position);
            }
        });

        holder.ivPortfolioImage.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(imageUrl, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public void updateList(List<String> newList) {
        this.imageUrls = newList;
        notifyDataSetChanged();
    }

    static class PortfolioViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPortfolioImage;

        public PortfolioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPortfolioImage = itemView.findViewById(R.id.ivPortfolioImage);
        }
    }
}
```

---

### ReviewAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Review;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;

    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // Set reviewer name
        holder.tvReviewerName.setText(review.getClientName());

        // Set review date
        holder.tvReviewDate.setText(getRelativeTime(review.getReviewDate()));

        // Set rating
        holder.tvRating.setText(String.format("%.1f", review.getRating()));

        // Set review comment
        holder.tvReviewComment.setText(review.getReviewText());

        // Set job title if available
        if (review.getJobTitle() != null && !review.getJobTitle().isEmpty()) {
            holder.tvJobTitle.setVisibility(View.VISIBLE);
            holder.tvJobTitle.setText(review.getJobTitle());
        } else {
            holder.tvJobTitle.setVisibility(View.GONE);
        }

        // Load reviewer profile image
        // TODO: Load image using Glide/Picasso
        // if (review.getClientPhotoUrl() != null) {
        //     Glide.with(context).load(review.getClientPhotoUrl()).into(holder.ivReviewerImage);
        // }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void updateData(List<Review> newList) {
        this.reviewList = newList;
        notifyDataSetChanged();
    }

    private String getRelativeTime(long timestamp) {
        if (timestamp == 0) {
            return "N/A";
        }

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;

        if (months > 0) {
            return months + " month" + (months > 1 ? "s" : "") + " ago";
        } else if (weeks > 0) {
            return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";
        } else if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivReviewerImage;
        TextView tvReviewerName, tvReviewDate, tvRating, tvReviewComment, tvJobTitle;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReviewerImage = itemView.findViewById(R.id.ivReviewerImage);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewComment = itemView.findViewById(R.id.tvReviewComment);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
        }
    }
}
```

---

### TaskAdapter.java

```java
package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private Context context;
    private List<Task> taskList;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskAdapter(Context context, List<Task> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        // Set task title
        holder.tvTaskTitle.setText(task.getTaskTitle());

        // Set worker count
        holder.tvTaskNumber.setText(String.valueOf(task.getNumberOfWorkers()));

        // Set last updated
        String lastUpdatedText = "Last update: " + formatDate(task.getUpdatedAt());
        holder.tvLastUpdated.setText(lastUpdatedText);

        // Set wages
        String wagesText = "Wages: Rs. " + formatCurrency(task.getDailyWages()) + " /day";
        holder.tvWages.setText(wagesText);

        // Set progress
        String progressText = task.getCompletedQuantity() + "/" + task.getEstimatedQuantity() + " " + task.getProgressUnit();
        holder.tvProgress.setText(progressText);

        // Set date range
        String dateRange = "";
        if (task.getStartDate() != 0 && task.getEndDate() != 0) {
            dateRange = formatDate(task.getStartDate()) + " - " + formatDate(task.getEndDate());
        } else if (task.getStartDate() != 0) {
            dateRange = "Started: " + formatDate(task.getStartDate());
        } else {
            dateRange = "Not started";
        }
        holder.tvDateRange.setText(dateRange);

        // Set status with color
        String statusText = task.getStatus().replace("_", " ");
        holder.tvStatus.setText(statusText);
        setStatusColor(holder.tvStatus, task.getStatus());

        // Set assigned by info
        holder.tvAssignedBy.setText(task.getCreatedBy() != null ? task.getCreatedBy() : "Admin");

        // Set assigned to info
        String assignedTo = task.getAssignedTo() != null && !task.getAssignedTo().equals("TBD")
                          ? task.getAssignedTo()
                          : "Unassigned";
        holder.tvAssignedTo.setText(assignedTo);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private String formatCurrency(double amount) {
        if (amount >= 10000000) { // 1 Crore
            return String.format("%.1f Cr", amount / 10000000);
        } else if (amount >= 100000) { // 1 Lakh
            return String.format("%.1f L", amount / 100000);
        } else if (amount >= 1000) { // 1 Thousand
            return String.format("%.1f K", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) {
            return "N/A";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM ''yy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void setStatusColor(TextView textView, String status) {
        int color;
        switch (status.toLowerCase()) {
            case "not_started":
                color = 0xFF757575; // Grey
                break;
            case "ongoing":
                color = 0xFFFFA726; // Orange
                break;
            case "completed":
                color = 0xFF4CAF50; // Green
                break;
            default:
                color = 0xFF757575; // Grey
                break;
        }
        textView.setTextColor(color);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvTaskNumber, tvLastUpdated, tvWages, tvProgress,
                 tvDateRange, tvStatus, tvAssignedBy, tvAssignedTo;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskNumber = itemView.findViewById(R.id.tvTaskNumber);
            tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
            tvWages = itemView.findViewById(R.id.tvWages);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            tvDateRange = itemView.findViewById(R.id.tvDateRange);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAssignedBy = itemView.findViewById(R.id.tvAssignedBy);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
        }
    }
}
```

---


## Models

### Bid.java

```java
package com.example.madproject.models;

import java.util.ArrayList;
import java.util.List;

public class Bid {
    private String bidId;
    private String jobId;
    private String jobTitle;
    private String contractorId;
    private String contractorName;
    private String contractorPhotoUrl;
    private String contractorCategory;
    private double contractorRating;
    private int contractorCompletedProjects;
    private double bidAmount;
    private int completionDays;
    private String proposal;
    private long submittedDate;
    private String status; // "pending", "accepted", "rejected"
    private List<String> portfolioImages;

    // Required empty constructor for Firestore
    public Bid() {
        this.portfolioImages = new ArrayList<>();
    }

    // Constructor
    public Bid(String bidId, String jobId, String jobTitle, String contractorId,
               String contractorName, double bidAmount, int completionDays, String proposal) {
        this.bidId = bidId;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.contractorId = contractorId;
        this.contractorName = contractorName;
        this.bidAmount = bidAmount;
        this.completionDays = completionDays;
        this.proposal = proposal;
        this.status = "pending";
        this.submittedDate = System.currentTimeMillis();
        this.portfolioImages = new ArrayList<>();
    }

    // Getters and Setters
    public String getBidId() {
        return bidId;
    }

    public void setBidId(String bidId) {
        this.bidId = bidId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getContractorId() {
        return contractorId;
    }

    public void setContractorId(String contractorId) {
        this.contractorId = contractorId;
    }

    public String getContractorName() {
        return contractorName;
    }

    public void setContractorName(String contractorName) {
        this.contractorName = contractorName;
    }

    public String getContractorPhotoUrl() {
        return contractorPhotoUrl;
    }

    public void setContractorPhotoUrl(String contractorPhotoUrl) {
        this.contractorPhotoUrl = contractorPhotoUrl;
    }

    public String getContractorCategory() {
        return contractorCategory;
    }

    public void setContractorCategory(String contractorCategory) {
        this.contractorCategory = contractorCategory;
    }

    public double getContractorRating() {
        return contractorRating;
    }

    public void setContractorRating(double contractorRating) {
        this.contractorRating = contractorRating;
    }

    public int getContractorCompletedProjects() {
        return contractorCompletedProjects;
    }

    public void setContractorCompletedProjects(int contractorCompletedProjects) {
        this.contractorCompletedProjects = contractorCompletedProjects;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public int getCompletionDays() {
        return completionDays;
    }

    public void setCompletionDays(int completionDays) {
        this.completionDays = completionDays;
    }

    public String getProposal() {
        return proposal;
    }

    public void setProposal(String proposal) {
        this.proposal = proposal;
    }

    public long getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(long submittedDate) {
        this.submittedDate = submittedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getPortfolioImages() {
        return portfolioImages;
    }

    public void setPortfolioImages(List<String> portfolioImages) {
        this.portfolioImages = portfolioImages;
    }
}```

---

### ChatMessage.java

```java
package com.example.madproject.models;

/**
 * ChatMessage Model
 *
 * Represents a single message in the AI chat
 */
public class ChatMessage {
    private String message;
    private boolean isUser;  // true = user message, false = AI message
    private long timestamp;

    // Empty constructor for Firestore (if you want to save chat history)
    public ChatMessage() {
    }

    public ChatMessage(String message, boolean isUser, long timestamp) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean user) {
        isUser = user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}```

---

### GeminiRequest.java

```java
package com.example.madproject.models;

public class GeminiRequest {
    private Content[] contents;

    public GeminiRequest(String message) {
        this.contents = new Content[]{new Content(message)};
    }

    public Content[] getContents() {
        return contents;
    }

    public void setContents(Content[] contents) {
        this.contents = contents;
    }

    public static class Content {
        private Part[] parts;

        public Content(String text) {
            this.parts = new Part[]{new Part(text)};
        }

        public Part[] getParts() {
            return parts;
        }

        public void setParts(Part[] parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
```

---

### GeminiResponse.java

```java
package com.example.madproject.models;

public class GeminiResponse {
    private Candidate[] candidates;
    private PromptFeedback promptFeedback;

    public Candidate[] getCandidates() {
        return candidates;
    }

    public void setCandidates(Candidate[] candidates) {
        this.candidates = candidates;
    }

    public PromptFeedback getPromptFeedback() {
        return promptFeedback;
    }

    public void setPromptFeedback(PromptFeedback promptFeedback) {
        this.promptFeedback = promptFeedback;
    }

    public static class Candidate {
        private Content content;
        private String finishReason;
        private int index;

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    public static class Content {
        private Part[] parts;
        private String role;

        public Part[] getParts() {
            return parts;
        }

        public void setParts(Part[] parts) {
            this.parts = parts;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class Part {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class PromptFeedback {
        private SafetyRating[] safetyRatings;

        public SafetyRating[] getSafetyRatings() {
            return safetyRatings;
        }

        public void setSafetyRatings(SafetyRating[] safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }

    public static class SafetyRating {
        private String category;
        private String probability;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getProbability() {
            return probability;
        }

        public void setProbability(String probability) {
            this.probability = probability;
        }
    }
}
```

---

### Job.java

```java
package com.example.madproject.models;

import java.util.ArrayList;
import java.util.List;

public class Job {
    private String jobId;
    private String clientId;
    private String clientName;
    private String clientPhotoUrl;
    private String title;
    private String description;
    private String category;
    private double budget;
    private String timeline; // e.g., "2 weeks"
    private String location;
    private String status; // "open", "in_progress", "completed", "cancelled"
    private long postedDate;
    private long startDate;
    private long completedDate;
    private int totalBids;
    private String acceptedBidId;
    private String assignedContractorId;
    private String assignedContractorName;
    private List<String> attachments; // Image URLs

    // Required empty constructor for Firestore
    public Job() {
        this.attachments = new ArrayList<>();
    }

    // Constructor
    public Job(String jobId, String clientId, String clientName, String title,
               String description, String category, double budget, String timeline, String location) {
        this.jobId = jobId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.title = title;
        this.description = description;
        this.category = category;
        this.budget = budget;
        this.timeline = timeline;
        this.location = location;
        this.status = "open";
        this.postedDate = System.currentTimeMillis();
        this.totalBids = 0;
        this.attachments = new ArrayList<>();
    }

    // Getters and Setters
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPhotoUrl() {
        return clientPhotoUrl;
    }

    public void setClientPhotoUrl(String clientPhotoUrl) {
        this.clientPhotoUrl = clientPhotoUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public String getTimeline() {
        return timeline;
    }

    public void setTimeline(String timeline) {
        this.timeline = timeline;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(long postedDate) {
        this.postedDate = postedDate;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(long completedDate) {
        this.completedDate = completedDate;
    }

    public int getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(int totalBids) {
        this.totalBids = totalBids;
    }

    public String getAcceptedBidId() {
        return acceptedBidId;
    }

    public void setAcceptedBidId(String acceptedBidId) {
        this.acceptedBidId = acceptedBidId;
    }

    public String getAssignedContractorId() {
        return assignedContractorId;
    }

    public void setAssignedContractorId(String assignedContractorId) {
        this.assignedContractorId = assignedContractorId;
    }

    public String getAssignedContractorName() {
        return assignedContractorName;
    }

    public void setAssignedContractorName(String assignedContractorName) {
        this.assignedContractorName = assignedContractorName;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }
}```

---

### Material.java

```java
package com.example.madproject.models;

import java.util.ArrayList;
import java.util.List;

public class Material {
    private String materialId;
    private String jobId;
    private String projectName;
    private String materialName;
    private String category; // "Cement", "Steel", "Bricks", "Sand", "Gravel", etc.
    private double quantity;
    private String unit; // "bags", "kg", "tons", "pieces", "cubic_meter"
    private double unitPrice;
    private double totalCost;
    private String supplier;
    private String supplierContact;
    private String description;
    private String status; // "in_stock", "low_stock", "out_of_stock"
    private double lowStockThreshold;
    private long addedDate;
    private long lastUpdated;
    private String addedBy;
    private List<String> photos;

    // Required empty constructor for Firestore
    public Material() {
        this.photos = new ArrayList<>();
    }

    // Constructor
    public Material(String materialId, String jobId, String projectName, String materialName,
                    String category, double quantity, String unit, double unitPrice, String supplier) {
        this.materialId = materialId;
        this.jobId = jobId;
        this.projectName = projectName;
        this.materialName = materialName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.supplier = supplier;
        this.totalCost = quantity * unitPrice;
        this.status = "in_stock";
        this.addedDate = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
        this.photos = new ArrayList<>();
    }

    // Getters and Setters
    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
        calculateTotalCost();
        checkStockStatus();
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        calculateTotalCost();
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getSupplierContact() {
        return supplierContact;
    }

    public void setSupplierContact(String supplierContact) {
        this.supplierContact = supplierContact;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(double lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public long getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(long addedDate) {
        this.addedDate = addedDate;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    // Helper methods
    private void calculateTotalCost() {
        this.totalCost = this.quantity * this.unitPrice;
    }

    private void checkStockStatus() {
        if (this.quantity <= 0) {
            this.status = "out_of_stock";
        } else if (this.lowStockThreshold > 0 && this.quantity <= this.lowStockThreshold) {
            this.status = "low_stock";
        } else {
            this.status = "in_stock";
        }
    }
}```

---

### Message.java

```java
package com.example.madproject.models;

public class Message {
    private String messageId;
    private String chatId; // Unique chat room ID (e.g., "clientId_contractorId")
    private String senderId;
    private String senderName;
    private String senderPhotoUrl;
    private String receiverId;
    private String receiverName;
    private String messageText;
    private String messageType; // "text", "image", "file"
    private String attachmentUrl;
    private String attachmentName;
    private long timestamp;
    private boolean isRead;
    private long readAt;

    // Required empty constructor for Firestore
    public Message() {
    }

    // Constructor for text message
    public Message(String messageId, String chatId, String senderId, String senderName,
                   String receiverId, String messageText) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.messageType = "text";
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    // Constructor for media message
    public Message(String messageId, String chatId, String senderId, String senderName,
                   String receiverId, String messageText, String messageType, String attachmentUrl) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.messageType = messageType;
        this.attachmentUrl = attachmentUrl;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhotoUrl() {
        return senderPhotoUrl;
    }

    public void setSenderPhotoUrl(String senderPhotoUrl) {
        this.senderPhotoUrl = senderPhotoUrl;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
        if (read && readAt == 0) {
            readAt = System.currentTimeMillis();
        }
    }

    public long getReadAt() {
        return readAt;
    }

    public void setReadAt(long readAt) {
        this.readAt = readAt;
    }
}```

---

### Notification.java

```java
package com.example.madproject.models;

public class Notification {
    private String notificationId;
    private String userId;
    private String title;
    private String message;
    private String type; // "job", "bid", "message", "task", "payment", "system"
    private String relatedId; // jobId, bidId, messageId, taskId, etc.
    private String relatedData; // Additional JSON data if needed
    private long timestamp;
    private boolean isRead;
    private long readAt;
    private String iconUrl;
    private String actionUrl; // Deep link to relevant screen

    // Required empty constructor for Firestore
    public Notification() {
    }

    // Constructor
    public Notification(String notificationId, String userId, String title,
                        String message, String type, String relatedId) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.relatedId = relatedId;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    // Getters and Setters
    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }

    public String getRelatedData() {
        return relatedData;
    }

    public void setRelatedData(String relatedData) {
        this.relatedData = relatedData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
        if (read && readAt == 0) {
            readAt = System.currentTimeMillis();
        }
    }

    public long getReadAt() {
        return readAt;
    }

    public void setReadAt(long readAt) {
        this.readAt = readAt;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    // Helper method to get time ago
    public String getTimeAgo() {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }
}```

---

### Review.java

```java
package com.example.madproject.models;

import java.util.ArrayList;
import java.util.List;

public class Review {
    private String reviewId;
    private String contractorId;
    private String contractorName;
    private String clientId;
    private String clientName;
    private String clientPhotoUrl;
    private String jobId;
    private String jobTitle;
    private float rating; // 1-5 stars
    private String reviewText;
    private long reviewDate;
    private List<String> photos;
    private boolean isVerified; // Was this a real completed job?
    private String response; // Contractor's response to review
    private long responseDate;

    // Required empty constructor for Firestore
    public Review() {
        this.photos = new ArrayList<>();
    }

    // Constructor
    public Review(String reviewId, String contractorId, String contractorName,
                  String clientId, String clientName, String jobId, String jobTitle,
                  float rating, String reviewText) {
        this.reviewId = reviewId;
        this.contractorId = contractorId;
        this.contractorName = contractorName;
        this.clientId = clientId;
        this.clientName = clientName;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.rating = rating;
        this.reviewText = reviewText;
        this.reviewDate = System.currentTimeMillis();
        this.photos = new ArrayList<>();
        this.isVerified = true; // Set based on job completion
    }

    // Getters and Setters
    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getContractorId() {
        return contractorId;
    }

    public void setContractorId(String contractorId) {
        this.contractorId = contractorId;
    }

    public String getContractorName() {
        return contractorName;
    }

    public void setContractorName(String contractorName) {
        this.contractorName = contractorName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPhotoUrl() {
        return clientPhotoUrl;
    }

    public void setClientPhotoUrl(String clientPhotoUrl) {
        this.clientPhotoUrl = clientPhotoUrl;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public long getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(long reviewDate) {
        this.reviewDate = reviewDate;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
        if (response != null && !response.isEmpty()) {
            this.responseDate = System.currentTimeMillis();
        }
    }

    public long getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(long responseDate) {
        this.responseDate = responseDate;
    }

    // Helper method to get star display
    public String getStarDisplay() {
        StringBuilder stars = new StringBuilder();
        int fullStars = (int) rating;
        boolean hasHalfStar = (rating - fullStars) >= 0.5;

        for (int i = 0; i < fullStars; i++) {
            stars.append("★");
        }
        if (hasHalfStar) {
            stars.append("½");
        }
        int remainingStars = 5 - fullStars - (hasHalfStar ? 1 : 0);
        for (int i = 0; i < remainingStars; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }
}```

---

### Task.java

```java
package com.example.madproject.models;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private String taskId;
    private String jobId;
    private String projectName;
    private String taskTitle;
    private String description;
    private String assignedTo; // Worker/contractor name
    private int numberOfWorkers;
    private long startDate;
    private long endDate;
    private String status; // "not_started", "ongoing", "completed"
    private double progressPercentage;
    private String progressUnit; // "sqft", "cubic_meter", "pieces", "bags", etc.
    private double estimatedQuantity;
    private double completedQuantity;
    private double dailyWages;
    private double totalCost;
    private List<String> photos;
    private long createdAt;
    private long updatedAt;
    private String createdBy;

    // Required empty constructor for Firestore
    public Task() {
        this.photos = new ArrayList<>();
    }

    // Constructor
    public Task(String taskId, String jobId, String projectName, String taskTitle,
                String description, String assignedTo, int numberOfWorkers) {
        this.taskId = taskId;
        this.jobId = jobId;
        this.projectName = projectName;
        this.taskTitle = taskTitle;
        this.description = description;
        this.assignedTo = assignedTo;
        this.numberOfWorkers = numberOfWorkers;
        this.status = "not_started";
        this.progressPercentage = 0.0;
        this.completedQuantity = 0.0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.photos = new ArrayList<>();
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public int getNumberOfWorkers() {
        return numberOfWorkers;
    }

    public void setNumberOfWorkers(int numberOfWorkers) {
        this.numberOfWorkers = numberOfWorkers;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getProgressUnit() {
        return progressUnit;
    }

    public void setProgressUnit(String progressUnit) {
        this.progressUnit = progressUnit;
    }

    public double getEstimatedQuantity() {
        return estimatedQuantity;
    }

    public void setEstimatedQuantity(double estimatedQuantity) {
        this.estimatedQuantity = estimatedQuantity;
    }

    public double getCompletedQuantity() {
        return completedQuantity;
    }

    public void setCompletedQuantity(double completedQuantity) {
        this.completedQuantity = completedQuantity;
    }

    public double getDailyWages() {
        return dailyWages;
    }

    public void setDailyWages(double dailyWages) {
        this.dailyWages = dailyWages;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // Helper method to calculate progress
    public void calculateProgress() {
        if (estimatedQuantity > 0) {
            this.progressPercentage = (completedQuantity / estimatedQuantity) * 100;
            if (this.progressPercentage >= 100) {
                this.progressPercentage = 100;
                this.status = "completed";
            }
        }
    }
}```

---

### User.java

```java
package com.example.madproject.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String city;
    private String userType; // "client" or "contractor"
    private String profilePictureUrl;
    private String fcmToken; // Firebase Cloud Messaging token for push notifications
    private long createdAt;
    private long lastLogin;

    // Contractor-specific fields (null for clients)
    private String category; // e.g., "Plumber", "Electrician", "Mason"
    private String bio;
    private int experienceYears;
    private double hourlyRate;
    private double rating;
    private int totalReviews;
    private int completedProjects;
    private List<String> portfolioImages; // URLs of portfolio images

    // Client-specific fields (null for contractors)
    private int activeJobs;
    private int completedJobs;
    private double totalSpent;

    // Required empty constructor for Firestore
    public User() {
        this.portfolioImages = new ArrayList<>();
    }

    // Constructor for Client
    public User(String userId, String email, String fullName, String phoneNumber, String userType) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.userType = userType;
        this.createdAt = System.currentTimeMillis();
        this.lastLogin = System.currentTimeMillis();
        this.rating = 0.0;
        this.totalReviews = 0;
        this.completedProjects = 0;
        this.activeJobs = 0;
        this.completedJobs = 0;
        this.totalSpent = 0.0;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public int getCompletedProjects() {
        return completedProjects;
    }

    public void setCompletedProjects(int completedProjects) {
        this.completedProjects = completedProjects;
    }

    public List<String> getPortfolioImages() {
        return portfolioImages != null ? portfolioImages : new ArrayList<>();
    }

    public void setPortfolioImages(List<String> portfolioImages) {
        this.portfolioImages = portfolioImages;
    }

    public int getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(int activeJobs) {
        this.activeJobs = activeJobs;
    }

    public int getCompletedJobs() {
        return completedJobs;
    }

    public void setCompletedJobs(int completedJobs) {
        this.completedJobs = completedJobs;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    // Helper methods
    @Exclude
    public boolean isClient() {
        return "client".equalsIgnoreCase(userType);
    }

    @Exclude
    public boolean isContractor() {
        return "contractor".equalsIgnoreCase(userType);
    }
}```

---


## Helpers

### FCMHelper.java

```java
package com.example.madproject.helpers;

import android.util.Log;

import com.example.madproject.firebase.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMHelper {

    private static final String TAG = "FCMHelper";

    /**
     * Register FCM token for current user
     * Call this method after successful login
     */
    public static void registerFCMToken() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "No user logged in, skipping FCM registration");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();

                        // Log token in a clearly visible format for testing
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "FCM TOKEN FOR TESTING:");
                        Log.d(TAG, token);
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "Copy this token to test notifications in Firebase Console");
                        // Save token to Firestore
                        UserManager.getInstance()
                                .updateField(userId, "fcmToken", token)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "FCM token saved successfully to Firestore");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save FCM token: " + e.getMessage());
                                });
                    } else {
                        Log.e(TAG, "Failed to get FCM token", task.getException());
                    }
                });
    }
}
```

---

### GeminiAIHelper.java

```java
package com.example.madproject.helpers;

import android.content.Context;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * GeminiAIHelper - AI Assistant for RebuildPak Construction Marketplace
 *
 * FIXED VERSION - Using "gemini-pro" model (most compatible)
 */
public class GeminiAIHelper {

    private static final String TAG = "GeminiAI";

    // ⚠️ IMPORTANT: Replace with your actual Gemini API key
    private static final String API_KEY = "AIzaSyCP14QE15TFDIbOgKPX23sPN8qlOnvIORY";

    private GenerativeModelFutures model;
    private Executor executor;
    private Context context;

    public GeminiAIHelper(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();

        // ✅ FIXED: Using "gemini-pro" - most stable model
        GenerativeModel gm = new GenerativeModel(
                "gemini-pro",  // ← Changed from "gemini-1.5-flash"
                API_KEY
        );

        this.model = GenerativeModelFutures.from(gm);

        Log.d(TAG, "GeminiAI initialized with gemini-pro model");
    }

    public void sendMessage(String userMessage, AIResponseListener listener) {
        String systemContext = "You are an AI assistant for RebuildPak, a construction marketplace app in Pakistan. " +
                "You help users with construction-related queries.\n\n" +
                "Your capabilities:\n" +
                "- Provide cost estimates in Pakistani Rupees (PKR)\n" +
                "- Calculate project timelines and milestones\n" +
                "- Recommend construction materials available in Pakistan\n" +
                "- Suggest appropriate contractor types for jobs\n" +
                "- Explain construction terminology simply\n" +
                "- Provide safety guidelines\n" +
                "- Help write clear job descriptions\n\n" +
                "Guidelines:\n" +
                "- Keep responses concise and practical (2-3 paragraphs max)\n" +
                "- Use simple language, avoid jargon\n" +
                "- Always use PKR for costs\n" +
                "- Consider Pakistani construction standards and materials\n" +
                "- Be helpful and friendly\n\n" +
                "User question: " + userMessage;

        Content content = new Content.Builder()
                .addText(systemContext)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String aiResponse = result.getText();
                    Log.d(TAG, "✅ AI Response received successfully");
                    listener.onResponse(aiResponse);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parsing response: " + e.getMessage());
                    listener.onError("Error parsing response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "❌ AI Request failed: " + t.getMessage());
                String errorMsg = t.getMessage();

                if (errorMsg != null && errorMsg.contains("API key")) {
                    listener.onError("Invalid API key. Generate new key from Google AI Studio");
                } else if (errorMsg != null && errorMsg.contains("models/gemini")) {
                    listener.onError("Model not found. Try generating a new API key");
                } else if (errorMsg != null && errorMsg.contains("network")) {
                    listener.onError("Network error. Check internet connection");
                } else {
                    listener.onError(errorMsg != null ? errorMsg : "Unknown error occurred");
                }
            }
        }, executor);
    }

    public void getConstructionEstimate(String projectDescription, AIResponseListener listener) {
        String prompt = "Provide a rough cost estimate in PKR for this construction project in Pakistan: " +
                projectDescription +
                "\n\nBreak down costs by: Materials, Labor, Equipment, Total estimated cost. Keep it concise.";
        sendMessage(prompt, listener);
    }

    public void getTimelineEstimate(String projectDescription, AIResponseListener listener) {
        String prompt = "Estimate the timeline for this construction project: " +
                projectDescription +
                "\n\nProvide: Estimated total days, Key phases with durations, Factors that could affect timeline. Be brief.";
        sendMessage(prompt, listener);
    }

    public void getMaterialRecommendations(String projectType, AIResponseListener listener) {
        String prompt = "What materials are needed for " + projectType +
                " in Pakistan?\n\nList: Essential materials, Approximate quantities, Estimated costs in PKR. Keep it concise.";
        sendMessage(prompt, listener);
    }

    public void getContractorRecommendation(String jobDescription, AIResponseListener listener) {
        String prompt = "What type of contractor is best for this job: " +
                jobDescription +
                "\n\nSuggest: Primary contractor type, Required skills, Any additional specialists. Be brief.";
        sendMessage(prompt, listener);
    }

    public void helpWriteJobDescription(String basicInfo, AIResponseListener listener) {
        String prompt = "Help me write a clear job description for: " + basicInfo +
                "\n\nProvide: Clear title, Detailed description, Required skills, Expected deliverables. Keep it professional and concise.";
        sendMessage(prompt, listener);
    }

    public void getSafetyTips(String workType, AIResponseListener listener) {
        String prompt = "What are key safety considerations for " + workType +
                " work?\n\nProvide: Top 5 safety tips, Required safety equipment, Common hazards. Be brief and actionable.";
        sendMessage(prompt, listener);
    }

    public interface AIResponseListener {
        void onResponse(String response);
        void onError(String error);
    }
}```

---

### GeminiHelper.java

```java
package com.example.madproject.helpers;

import android.util.Log;

import com.example.madproject.models.GeminiRequest;
import com.example.madproject.models.GeminiResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiHelper {
    private static final String TAG = "GeminiHelper";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Gson gson = new Gson();

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public static void sendMessage(String apiKey, String message, GeminiCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API key is required");
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            callback.onError("Message cannot be empty");
            return;
        }

        try {
            GeminiRequest request = new GeminiRequest(message.trim());
            String json = gson.toJson(request);

            RequestBody body = RequestBody.create(json, JSON);
            Request httpRequest = new Request.Builder()
                    .url(BASE_URL + apiKey)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network error: " + e.getMessage(), e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        try {
                            GeminiResponse geminiResponse = gson.fromJson(responseBody, GeminiResponse.class);

                            if (geminiResponse != null &&
                                    geminiResponse.getCandidates() != null &&
                                    geminiResponse.getCandidates().length > 0 &&
                                    geminiResponse.getCandidates()[0].getContent() != null &&
                                    geminiResponse.getCandidates()[0].getContent().getParts() != null &&
                                    geminiResponse.getCandidates()[0].getContent().getParts().length > 0) {

                                String text = geminiResponse.getCandidates()[0].getContent().getParts()[0].getText();
                                if (text != null && !text.isEmpty()) {
                                    callback.onSuccess(text);
                                } else {
                                    callback.onError("Empty response from Gemini");
                                }
                            } else {
                                Log.e(TAG, "Invalid response structure: " + responseBody);
                                callback.onError("Invalid response structure");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Parse error: " + e.getMessage(), e);
                            Log.e(TAG, "Response body: " + responseBody);
                            callback.onError("Parse error: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "API error: " + response.code() + " - " + responseBody);
                        callback.onError("API error: " + response.code() + " - " + responseBody);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }

    public static void sendMessageWithContext(String apiKey, String systemInstruction, String message, GeminiCallback callback) {
        String fullMessage = systemInstruction + "\n\nUser: " + message;
        sendMessage(apiKey, fullMessage, callback);
    }
}
```

---


## XML Layouts

### Key Layout Files

#### activity_add_material.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:background="#F8F9FA">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Top App Bar -->
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="@android:color/white"
            android:elevation="4dp"
            app:navigationIcon="@drawable/ic_back"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Add Material"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#212121" />
        </com.google.android.material.appbar.MaterialToolbar>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp"
            app:layout_constraintTop_toBottomOf="@id/toolbar">

            <!-- Material Name -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Material Name *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etMaterialName"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="e.g., Cement Bags"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="text"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Category -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Category *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <Spinner
                android:id="@+id/spinnerCategory"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Quantity & Unit Row -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Quantity  Unit *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp">

                <EditText
                    android:id="@+id/etQuantity"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:hint="Quantity"
                    android:background="@drawable/bg_input_field_light"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:inputType="number"
                    android:textSize="15sp"
                    android:layout_marginEnd="8dp" />

                <Spinner
                    android:id="@+id/spinnerUnit"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:background="@drawable/bg_input_field_light"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:layout_marginStart="8dp" />
            </LinearLayout>

            <!-- Unit Price -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Unit Price (PKR) *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etUnitPrice"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="Price per unit"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="numberDecimal"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Total Cost (Auto-calculated) -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Total Cost"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <TextView
                android:id="@+id/tvTotalCost"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Rs. 0"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#4CAF50"
                android:gravity="center_vertical"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Supplier Name -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Supplier Name"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etSupplier"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="e.g., ABC Hardware"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="text"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Purchase Date -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Purchase Date"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp"
                android:clickable="true"
                android:focusable="true">

                <TextView
                    android:id="@+id/tvSelectedDate"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Select date"
                    android:textSize="15sp"
                    android:textColor="#757575" />

                <ImageView
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:src="@drawable/ic_time"
                    app:tint="#7C4DFF" />
            </LinearLayout>

            <!-- Description/Notes -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Description / Notes"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etDescription"
                android:layout_width="match_parent"
                android:layout_height="120dp"
                android:hint="Add any additional notes..."
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:paddingTop="12dp"
                android:gravity="top"
                android:inputType="textMultiLine"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Attach Photos -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Attach Photos (Optional)"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginBottom="8dp" />

            <Button
                android:id="@+id/btnAddPhoto"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Add Photo"
                android:textAllCaps="false"
                android:drawableLeft="@drawable/ic_gallery"
                android:drawablePadding="8dp"
                android:backgroundTint="#EEEEEE"
                android:textColor="#212121"
                android:layout_marginBottom="24dp" />

            <!-- Save Button -->
            <Button
                android:id="@+id/btnSaveMaterial"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Save Material"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_button_purple" />

            <!-- Cancel Button -->
            <Button
                android:id="@+id/btnCancel"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Cancel"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:backgroundTint="#EEEEEE"
                android:textColor="#212121"
                android:layout_marginTop="12dp"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>```

#### activity_add_task.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:background="#F8F9FA">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Top App Bar -->
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="@android:color/white"
            android:elevation="4dp"
            app:navigationIcon="@drawable/ic_back"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Add New Task"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#212121" />
        </com.google.android.material.appbar.MaterialToolbar>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp"
            app:layout_constraintTop_toBottomOf="@id/toolbar">

            <!-- Task Title -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Task Title *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etTaskTitle"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="e.g., Electrician, Plumber"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="text"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Assign to Contact -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Assign to Contact *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <LinearLayout
                android:id="@+id/selectContactLayout"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp"
                android:clickable="true"
                android:focusable="true">

                <TextView
                    android:id="@+id/tvSelectedContact"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Select contact"
                    android:textSize="15sp"
                    android:textColor="#757575" />

                <ImageView
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:src="@drawable/ic_dropdown"
                    app:tint="#7C4DFF" />
            </LinearLayout>

            <!-- Number of Workers -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Number of Workers *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etWorkersCount"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="e.g., 3"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="number"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Task Description -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Task Description *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etTaskDescription"
                android:layout_width="match_parent"
                android:layout_height="120dp"
                android:hint="Describe the task details..."
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:paddingTop="12dp"
                android:gravity="top"
                android:inputType="textMultiLine"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Start & End Date Row -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Timeline *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp">

                <!-- Start Date -->
                <LinearLayout
                    android:id="@+id/selectStartDateLayout"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:background="@drawable/bg_input_field_light"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:layout_marginEnd="8dp"
                    android:clickable="true"
                    android:focusable="true">

                    <TextView
                        android:id="@+id/tvStartDate"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Start Date"
                        android:textSize="14sp"
                        android:textColor="#757575" />

                    <ImageView
                        android:layout_width="18dp"
                        android:layout_height="18dp"
                        android:src="@drawable/ic_time"
                        app:tint="#7C4DFF" />
                </LinearLayout>

                <!-- End Date -->
                <LinearLayout
                    android:id="@+id/selectEndDateLayout"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:background="@drawable/bg_input_field_light"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:layout_marginStart="8dp"
                    android:clickable="true"
                    android:focusable="true">

                    <TextView
                        android:id="@+id/tvEndDate"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="End Date"
                        android:textSize="14sp"
                        android:textColor="#757575" />

                    <ImageView
                        android:layout_width="18dp"
                        android:layout_height="18dp"
                        android:src="@drawable/ic_time"
                        app:tint="#7C4DFF" />
                </LinearLayout>
            </LinearLayout>

            <!-- Progress Unit & Quantity -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Progress Tracking"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp">

                <Spinner
                    android:id="@+id/spinnerProgressUnit"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:background="@drawable/bg_input_field_light"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:layout_marginEnd="8dp" />

                <EditText
                    android:id="@+id/etEstimatedQuantity"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:hint="Estimated Qty"
                    android:background="@drawable/bg_input_field_light"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:inputType="number"
                    android:textSize="15sp"
                    android:layout_marginStart="8dp" />
            </LinearLayout>

            <!-- Daily Wages -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Daily Wages (PKR)"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etDailyWages"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="e.g., 500"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="numberDecimal"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Attach Photos -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Attach Photos (Optional)"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginBottom="8dp" />

            <Button
                android:id="@+id/btnAddPhotos"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Add Photos"
                android:textAllCaps="false"
                android:drawableLeft="@drawable/ic_camera"
                android:drawablePadding="8dp"
                android:backgroundTint="#EEEEEE"
                android:textColor="#212121"
                android:layout_marginBottom="24dp" />

            <!-- Create Task Button -->
            <Button
                android:id="@+id/btnCreateTask"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Create Task"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_button_purple" />

            <!-- Cancel Button -->
            <Button
                android:id="@+id/btnCancel"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Cancel"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:backgroundTint="#EEEEEE"
                android:textColor="#212121"
                android:layout_marginTop="12dp"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>```

#### activity_ai_chat.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Toolbar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="AI Assistant"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <ImageView
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:src="@drawable/ic_ai_bot"
                android:layout_marginEnd="16dp" />
        </LinearLayout>
    </com.google.android.material.appbar.MaterialToolbar>

    <!-- Messages RecyclerView -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvMessages"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:paddingTop="8dp"
        android:paddingBottom="8dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/suggestionsContainer"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Empty State -->
    <LinearLayout
        android:id="@+id/emptyState"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="24dp"
        app:layout_constraintTop_toTopOf="@id/rvMessages"
        app:layout_constraintBottom_toBottomOf="@id/rvMessages"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageView
            android:layout_width="120dp"
            android:layout_height="120dp"
            android:src="@drawable/ic_ai_bot"
            android:alpha="0.5"
            app:tint="#7C4DFF" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="AI Assistant Ready"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="#212121"
            android:layout_marginTop="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Ask me anything about construction!"
            android:textSize="14sp"
            android:textColor="#757575"
            android:layout_marginTop="4dp" />
    </LinearLayout>

    <!-- Suggestions Container (HorizontalScrollView) -->
    <HorizontalScrollView
        android:id="@+id/suggestionsContainer"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:paddingStart="8dp"
        android:paddingEnd="8dp"
        android:paddingTop="8dp"
        android:paddingBottom="8dp"
        android:scrollbars="none"
        app:layout_constraintBottom_toTopOf="@id/inputContainer"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <!-- Suggestion Chips -->
        <com.google.android.material.chip.ChipGroup
            android:id="@+id/chipGroupSuggestions"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:singleLine="true" />
    </HorizontalScrollView>

    <!-- Input Container -->
    <LinearLayout
        android:id="@+id/inputContainer"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:background="@android:color/white"
        android:padding="8dp"
        android:elevation="4dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <EditText
            android:id="@+id/etMessage"
            android:layout_width="0dp"
            android:layout_height="48dp"
            android:layout_weight="1"
            android:hint="Ask me anything..."
            android:background="@drawable/bg_input_field_light"
            android:paddingStart="16dp"
            android:paddingEnd="16dp"
            android:inputType="textMultiLine|textCapSentences"
            android:maxLines="3"
            android:imeOptions="actionSend" />

        <ImageButton
            android:id="@+id/btnSend"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:layout_marginStart="8dp"
            android:src="@drawable/ic_send"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="Send" />

        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:layout_gravity="center"
            android:layout_marginStart="12dp"
            android:visibility="gone" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_all_reviews.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Reviews"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="#212121" />

        <ImageView
            android:id="@+id/btnFilter"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="end"
            android:layout_marginEnd="16dp"
            android:padding="4dp"
            android:src="@drawable/ic_filter" />
    </com.google.android.material.appbar.MaterialToolbar>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toBottomOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Rating Summary Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:padding="20dp">

                    <!-- Overall Rating -->
                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical"
                        android:gravity="center">

                        <TextView
                            android:id="@+id/tvAverageRating"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="4.8"
                            android:textSize="48sp"
                            android:textStyle="bold"
                            android:textColor="#7C4DFF" />

                        <LinearLayout
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal"
                            android:layout_marginTop="4dp">

                            <ImageView
                                android:layout_width="18dp"
                                android:layout_height="18dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726" />

                            <ImageView
                                android:layout_width="18dp"
                                android:layout_height="18dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726"
                                android:layout_marginStart="2dp" />

                            <ImageView
                                android:layout_width="18dp"
                                android:layout_height="18dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726"
                                android:layout_marginStart="2dp" />

                            <ImageView
                                android:layout_width="18dp"
                                android:layout_height="18dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726"
                                android:layout_marginStart="2dp" />

                            <ImageView
                                android:layout_width="18dp"
                                android:layout_height="18dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726"
                                android:layout_marginStart="2dp" />
                        </LinearLayout>

                        <TextView
                            android:id="@+id/tvTotalReviews"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Based on 24 reviews"
                            android:textSize="13sp"
                            android:textColor="#757575"
                            android:layout_marginTop="8dp" />
                    </LinearLayout>

                    <View
                        android:layout_width="1dp"
                        android:layout_height="match_parent"
                        android:background="#E0E0E0"
                        android:layout_marginStart="16dp"
                        android:layout_marginEnd="16dp" />

                    <!-- Rating Breakdown -->
                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <!-- 5 Stars -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal"
                            android:gravity="center_vertical"
                            android:layout_marginBottom="4dp">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="5"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <ImageView
                                android:layout_width="12dp"
                                android:layout_height="12dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726"
                                android:layout_marginStart="4dp"
                                android:layout_marginEnd="8dp" />

                            <ProgressBar
                                style="?android:attr/progressBarStyleHorizontal"
                                android:layout_width="0dp"
                                android:layout_height="8dp"
                                android:layout_weight="1"
                                android:progress="75"
                                android:progressTint="#4CAF50" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="18"
                                android:textSize="12sp"
                                android:textColor="#757575"
                                android:layout_marginStart="8dp" />
                        </LinearLayout>

                        <!-- 4 Stars -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal"
                            android:gravity="center_vertical"
                            android:layout_marginBottom="4dp">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="4"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <ImageView
                                android:layout_width="12dp"
                                android:layout_height="12dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726"
                                android:layout_marginStart="4dp"
                                android:layout_marginEnd="8dp" />

                            <ProgressBar
                                style="?android:attr/progressBarStyleHorizontal"
                                android:layout_width="0dp"
                                android:layout_height="8dp"
                                android:layout_weight="1"
                                android:progress="20"
                                android:progressTint="#7C4DFF" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="5"
                                android:textSize="12sp"
                                android:textColor="#757575"
                                android:layout_marginStart="8dp" />
                        </LinearLayout>

                        <!-- 3 Stars -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal"
                            android:gravity="center_vertical"
                            android:layout_marginBottom="4dp">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="3"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <ImageView
                                android:layout_width="12dp"
                                android:layout_height="12dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726"
                                android:layout_marginStart="4dp"
                                android:layout_marginEnd="8dp" />

                            <ProgressBar
                                style="?android:attr/progressBarStyleHorizontal"
                                android:layout_width="0dp"
                                android:layout_height="8dp"
                                android:layout_weight="1"
                                android:progress="4"
                                android:progressTint="#FFA726" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="1"
                                android:textSize="12sp"
                                android:textColor="#757575"
                                android:layout_marginStart="8dp" />
                        </LinearLayout>

                        <!-- 2 Stars -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal"
                            android:gravity="center_vertical"
                            android:layout_marginBottom="4dp">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="2"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <ImageView
                                android:layout_width="12dp"
                                android:layout_height="12dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726"
                                android:layout_marginStart="4dp"
                                android:layout_marginEnd="8dp" />

                            <ProgressBar
                                style="?android:attr/progressBarStyleHorizontal"
                                android:layout_width="0dp"
                                android:layout_height="8dp"
                                android:layout_weight="1"
                                android:progress="0"
                                android:progressTint="#E53935" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="0"
                                android:textSize="12sp"
                                android:textColor="#757575"
                                android:layout_marginStart="8dp" />
                        </LinearLayout>

                        <!-- 1 Star -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal"
                            android:gravity="center_vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="1"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <ImageView
                                android:layout_width="12dp"
                                android:layout_height="12dp"
                                android:src="@drawable/ic_star"
                                app:tint="#FFA726"
                                android:layout_marginStart="4dp"
                                android:layout_marginEnd="8dp" />

                            <ProgressBar
                                style="?android:attr/progressBarStyleHorizontal"
                                android:layout_width="0dp"
                                android:layout_height="8dp"
                                android:layout_weight="1"
                                android:progress="0"
                                android:progressTint="#E53935" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="0"
                                android:textSize="12sp"
                                android:textColor="#757575"
                                android:layout_marginStart="8dp" />
                        </LinearLayout>
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Filter Pills -->
            <HorizontalScrollView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:scrollbars="none"
                android:layout_marginBottom="16dp">

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:id="@+id/btnAllReviews"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="All"
                        android:textSize="14sp"
                        android:textColor="@android:color/white"
                        android:background="@drawable/bg_pill_selected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btn5Star"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="5 Stars"
                        android:textSize="14sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btn4Star"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="4 Stars"
                        android:textSize="14sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btn3Star"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="3 Stars"
                        android:textSize="14sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center" />
                </LinearLayout>
            </HorizontalScrollView>

            <!-- Reviews List -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvReviews"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                tools:listitem="@layout/item_review" />

            <!-- Empty State -->
            <LinearLayout
                android:id="@+id/emptyState"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:orientation="vertical"
                android:gravity="center"
                android:visibility="gone">

                <ImageView
                    android:layout_width="80dp"
                    android:layout_height="80dp"
                    android:src="@drawable/ic_star"
                    android:alpha="0.5" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="No reviews yet"
                    android:textSize="16sp"
                    android:textColor="#757575"
                    android:layout_marginTop="16dp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Be the first to leave a review"
                    android:textSize="14sp"
                    android:textColor="#9E9E9E"
                    android:layout_marginTop="4dp" />
            </LinearLayout>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

    <!-- Loading Indicator -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_available_jobs.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Toolbar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- Search Bar -->
    <EditText
        android:id="@+id/etSearch"
        android:layout_width="0dp"
        android:layout_height="48dp"
        android:layout_margin="16dp"
        android:hint="Search jobs..."
        android:drawableStart="@drawable/ic_search"
        android:drawablePadding="8dp"
        android:paddingStart="12dp"
        android:paddingEnd="12dp"
        android:background="@drawable/bg_input_field_light"
        android:inputType="text"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Category Filter -->
    <Spinner
        android:id="@+id/spinnerCategory"
        android:layout_width="0dp"
        android:layout_height="48dp"
        android:layout_marginStart="16dp"
        android:layout_marginEnd="16dp"
        android:layout_marginTop="8dp"
        android:background="@drawable/bg_input_field_light"
        android:paddingStart="12dp"
        android:paddingEnd="12dp"
        app:layout_constraintTop_toBottomOf="@id/etSearch"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Progress Bar -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Jobs RecyclerView -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvJob"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginTop="16dp"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        android:paddingBottom="16dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/spinnerCategory"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        tools:listitem="@layout/item_job_card" />

    <!-- Empty State -->
    <LinearLayout
        android:id="@+id/emptyState"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageView
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:src="@drawable/ic_empty_jobs"
            android:alpha="0.5" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="No jobs found"
            android:textSize="16sp"
            android:textColor="#757575"
            android:layout_marginTop="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Try adjusting your filters"
            android:textSize="14sp"
            android:textColor="#9E9E9E"
            android:layout_marginTop="4dp" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_chat.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <de.hdodenhof.circleimageview.CircleImageView
                android:id="@+id/ivProfileImage"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_default_profile"
                app:civ_border_width="1dp"
                app:civ_border_color="#E0E0E0" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical"
                android:layout_marginStart="12dp">

                <TextView
                    android:id="@+id/tvReceiverName"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Ahmad Ali"
                    android:textSize="16sp"
                    android:textStyle="bold"
                    android:textColor="#212121" />

                <TextView
                    android:id="@+id/tvOnlineStatus"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Online"
                    android:textSize="12sp"
                    android:textColor="#4CAF50"
                    android:layout_marginTop="2dp" />
            </LinearLayout>

            <ImageView
                android:id="@+id/btnCall"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_phone"
                android:padding="8dp"
                android:layout_marginEnd="8dp"
                app:tint="#7C4DFF"
                android:background="?attr/selectableItemBackgroundBorderless" />

            <ImageView
                android:id="@+id/btnMore"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_more"
                android:padding="8dp"
                android:layout_marginEnd="8dp"
                app:tint="#757575"
                android:background="?attr/selectableItemBackgroundBorderless" />
        </LinearLayout>
    </com.google.android.material.appbar.MaterialToolbar>

    <!-- Messages RecyclerView -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvMessages"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:padding="16dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/messageInputContainer"
        tools:listitem="@layout/item_message" />

    <!-- Empty State -->
    <LinearLayout
        android:id="@+id/emptyState"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:orientation="vertical"
        android:gravity="center"
        android:visibility="gone"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/messageInputContainer">

        <ImageView
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:src="@drawable/ic_chat_empty"
            android:alpha="0.5" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="No messages yet"
            android:textSize="16sp"
            android:textColor="#757575"
            android:layout_marginTop="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Start a conversation"
            android:textSize="14sp"
            android:textColor="#9E9E9E"
            android:layout_marginTop="4dp" />
    </LinearLayout>

    <!-- Message Input Container -->
    <androidx.cardview.widget.CardView
        android:id="@+id/messageInputContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="12dp"
        app:cardCornerRadius="24dp"
        app:cardElevation="4dp"
        app:layout_constraintBottom_toBottomOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="8dp">

            <!-- Attach Button -->
            <ImageView
                android:id="@+id/btnAttach"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_attach"
                android:padding="8dp"
                app:tint="#757575"
                android:background="?attr/selectableItemBackgroundBorderless" />

            <!-- Message Input -->
            <EditText
                android:id="@+id/etMessage"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:hint="Type a message..."
                android:background="@android:color/transparent"
                android:paddingStart="8dp"
                android:paddingEnd="8dp"
                android:paddingTop="12dp"
                android:paddingBottom="12dp"
                android:textSize="15sp"
                android:maxLines="4"
                android:inputType="textMultiLine|textCapSentences" />

            <!-- Camera Button -->
            <ImageView
                android:id="@+id/btnCamera"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_camera"
                android:padding="8dp"
                app:tint="#757575"
                android:background="?attr/selectableItemBackgroundBorderless" />

            <!-- Send Button -->
            <ImageView
                android:id="@+id/btnSend"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:src="@drawable/ic_send"
                android:padding="8dp"
                app:tint="#7C4DFF"
                android:background="?attr/selectableItemBackgroundBorderless" />
        </LinearLayout>
    </androidx.cardview.widget.CardView>

    <!-- Loading Indicator -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_client_dashboard.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA"
    tools:context=".ClientDashboardActivity">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Thaika.co"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="#212121" />

        <ImageView
            android:id="@+id/btnNotifications"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="end"
            android:layout_marginEnd="16dp"
            android:padding="4dp"
            android:src="@drawable/ic_notifications"
            android:contentDescription="Notifications" />
    </com.google.android.material.appbar.MaterialToolbar>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:fillViewport="true"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/bottomNavigation">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp"
            android:paddingBottom="80dp">

            <!-- Welcome Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="16dp"
                app:cardElevation="2dp"
                app:cardBackgroundColor="#7C4DFF">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="20dp">

                    <TextView
                        android:id="@+id/tvWelcome"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Welcome back!"
                        android:textColor="@android:color/white"
                        android:textSize="16sp"
                        />

                    <TextView
                        android:id="@+id/tvUserName"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="User Name"
                        android:textColor="@android:color/white"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        android:layout_marginTop="4dp" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Ready to start your next project?"
                        android:textColor="#E1BEE7"
                        android:textSize="14sp"
                        android:layout_marginTop="8dp" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Quick Actions -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Quick Actions"
                android:textSize="18sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginBottom="12dp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:weightSum="2"
                android:layout_marginBottom="16dp">

                <!-- Post Job Button -->
                <androidx.cardview.widget.CardView
                    android:id="@+id/btnPostJob"
                    android:layout_width="0dp"
                    android:layout_height="120dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="8dp"
                    app:cardCornerRadius="16dp"
                    app:cardElevation="2dp"
                    android:clickable="true"
                    android:foreground="?attr/selectableItemBackground">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:orientation="vertical"
                        android:gravity="center"
                        android:padding="16dp">

                        <ImageView
                            android:layout_width="48dp"
                            android:layout_height="48dp"
                            android:src="@drawable/ic_add_job"
                            app:tint="#7C4DFF" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Post New Job"
                            android:textSize="14sp"
                            android:textStyle="bold"
                            android:textColor="#212121"
                            android:textAlignment="center"
                            android:layout_marginTop="8dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>

                <!-- Find Contractors Button -->
                <androidx.cardview.widget.CardView
                    android:id="@+id/cardFindContractors"
                    android:layout_width="0dp"
                    android:layout_height="120dp"
                    android:layout_weight="1"
                    android:layout_marginStart="8dp"
                    app:cardCornerRadius="16dp"
                    app:cardElevation="2dp"
                    android:clickable="true"
                    android:foreground="?attr/selectableItemBackground">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:orientation="vertical"
                        android:gravity="center"
                        android:padding="16dp">

                        <ImageView
                            android:layout_width="48dp"
                            android:layout_height="48dp"
                            android:src="@drawable/ic_search"
                            app:tint="#7C4DFF" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Find Contractors"
                            android:textSize="14sp"
                            android:textStyle="bold"
                            android:textColor="#212121"
                            android:textAlignment="center"
                            android:layout_marginTop="8dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>
            </LinearLayout>

            <!-- My Jobs Section -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="My Jobs"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="#212121" />

                <TextView
                    android:id="@+id/tvViewAllJobs"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="View All"
                    android:textColor="#7C4DFF"
                    android:textSize="14sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <!-- Jobs RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvMyJobs"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                tools:listitem="@layout/item_job_card" />

            <!-- Empty State -->
            <LinearLayout
                android:id="@+id/emptyState"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:orientation="vertical"
                android:gravity="center"
                android:visibility="gone">

                <ImageView
                    android:layout_width="80dp"
                    android:layout_height="80dp"
                    android:src="@drawable/ic_empty_jobs"
                    android:alpha="0.5" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="No jobs posted yet"
                    android:textSize="16sp"
                    android:textColor="#757575"
                    android:layout_marginTop="16dp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Post your first job to get started"
                    android:textSize="14sp"
                    android:textColor="#9E9E9E"
                    android:layout_marginTop="4dp" />
            </LinearLayout>

        </LinearLayout>
    </ScrollView>

    <!-- AI Assistant FAB Button -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAIChat"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_margin="16dp"
        android:src="@drawable/ic_ai_bot"
        android:contentDescription="AI Assistant"
        app:backgroundTint="#7C4DFF"
        app:tint="@android:color/white"
        app:elevation="6dp"
        app:layout_constraintBottom_toTopOf="@id/bottomNavigation"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Bottom Navigation -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNavigation"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        android:elevation="8dp"
        app:menu="@menu/bottom_nav_client"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_contractor_dashboard.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA"
    tools:context=".ContractorDashboardActivity">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Thaika.co"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="#212121" />

        <ImageView
            android:id="@+id/btnNotifications"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="end"
            android:layout_marginEnd="16dp"
            android:padding="4dp"
            android:src="@drawable/ic_notifications"
            android:contentDescription="Notifications" />
    </com.google.android.material.appbar.MaterialToolbar>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:fillViewport="true"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/bottomNav">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp"
            android:paddingBottom="80dp">

            <!-- Profile Summary Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="16dp"
                app:cardElevation="2dp">

                <androidx.constraintlayout.widget.ConstraintLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:padding="16dp">

                    <!-- Profile Image -->
                    <de.hdodenhof.circleimageview.CircleImageView
                        android:id="@+id/ivProfileImage"
                        android:layout_width="64dp"
                        android:layout_height="64dp"
                        android:src="@drawable/ic_default_profile"
                        app:civ_border_width="2dp"
                        app:civ_border_color="#7C4DFF"
                        app:layout_constraintTop_toTopOf="parent"
                        app:layout_constraintStart_toStartOf="parent" />

                    <!-- Contractor Name -->
                    <TextView
                        android:id="@+id/tvContractorName"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:text="Ahmad Ali"
                        android:textSize="18sp"
                        android:textStyle="bold"
                        android:textColor="#212121"
                        android:layout_marginStart="16dp"
                        app:layout_constraintTop_toTopOf="@id/ivProfileImage"
                        app:layout_constraintStart_toEndOf="@id/ivProfileImage"
                        app:layout_constraintEnd_toEndOf="parent" />

                    <!-- Category -->
                    <TextView
                        android:id="@+id/tvCategory"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:text="Electrician"
                        android:textSize="14sp"
                        android:textColor="#7C4DFF"
                        android:layout_marginStart="16dp"
                        android:layout_marginTop="4dp"
                        app:layout_constraintTop_toBottomOf="@id/tvContractorName"
                        app:layout_constraintStart_toEndOf="@id/ivProfileImage"
                        app:layout_constraintEnd_toEndOf="parent" />

                    <!-- Rating -->
                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginStart="16dp"
                        android:layout_marginTop="4dp"
                        app:layout_constraintTop_toBottomOf="@id/tvCategory"
                        app:layout_constraintStart_toEndOf="@id/ivProfileImage">

                        <ImageView
                            android:layout_width="16dp"
                            android:layout_height="16dp"
                            android:src="@drawable/ic_star"
                            app:tint="#FFA726" />

                        <TextView
                            android:id="@+id/tvRating"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="4.8"
                            android:textSize="14sp"
                            android:textStyle="bold"
                            android:textColor="#212121"
                            android:layout_marginStart="4dp" />

                        <TextView
                            android:id="@+id/tvReviews"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="(24 reviews)"
                            android:textSize="12sp"
                            android:textColor="#757575"
                            android:layout_marginStart="4dp" />
                    </LinearLayout>

                    <!-- View Profile Button -->
                    <Button
                        android:id="@+id/btnViewProfile"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="View Profile"
                        android:textAllCaps="false"
                        android:textSize="13sp"
                        android:backgroundTint="#7C4DFF"
                        android:layout_marginTop="16dp"
                        app:layout_constraintTop_toBottomOf="@id/ivProfileImage"
                        app:layout_constraintEnd_toEndOf="parent" />
                </androidx.constraintlayout.widget.ConstraintLayout>
            </androidx.cardview.widget.CardView>

            <!-- Statistics Cards -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:weightSum="3"
                android:layout_marginBottom="16dp">

                <!-- Active Projects -->
                <androidx.cardview.widget.CardView
                    android:id="@+id/tvActiveProjects"
                    android:layout_width="0dp"
                    android:layout_height="100dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="8dp"
                    app:cardCornerRadius="12dp"
                    app:cardElevation="2dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:orientation="vertical"
                        android:gravity="center"
                        android:padding="12dp">

                        <TextView
                            android:id="@+id/tvActiveProjectsCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="3"
                            android:textSize="28sp"
                            android:textStyle="bold"
                            android:textColor="#7C4DFF" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Active"
                            android:textSize="12sp"
                            android:textColor="#757575"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>

                <!-- Completed Projects -->
                <androidx.cardview.widget.CardView
                    android:layout_width="0dp"
                    android:layout_height="100dp"
                    android:layout_weight="1"
                    android:layout_marginStart="4dp"
                    android:layout_marginEnd="4dp"
                    app:cardCornerRadius="12dp"
                    app:cardElevation="2dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:orientation="vertical"
                        android:gravity="center"
                        android:padding="12dp">

                        <TextView
                            android:id="@+id/tvCompletedCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="15"
                            android:textSize="28sp"
                            android:textStyle="bold"
                            android:textColor="#4CAF50" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Completed"
                            android:textSize="12sp"
                            android:textColor="#757575"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>

                <!-- Total Earnings -->
                <androidx.cardview.widget.CardView
                    android:layout_width="0dp"
                    android:layout_height="100dp"
                    android:layout_weight="1"
                    android:layout_marginStart="8dp"
                    app:cardCornerRadius="12dp"
                    app:cardElevation="2dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:orientation="vertical"
                        android:gravity="center"
                        android:padding="12dp">

                        <TextView
                            android:id="@+id/tvTotalEarnings"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="250K"
                            android:textSize="24sp"
                            android:textStyle="bold"
                            android:textColor="#FFA726" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Earnings"
                            android:textSize="12sp"
                            android:textColor="#757575"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>
            </LinearLayout>

            <!-- Available Jobs Section -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Available Jobs"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="#212121" />

                <TextView
                    android:id="@+id/tvViewAllJobs"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="View All"
                    android:textColor="#7C4DFF"
                    android:textSize="14sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <!-- Jobs RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvAvailableJobs"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                tools:listitem="@layout/item_available_job_card" />

        </LinearLayout>
    </ScrollView>

    <!-- AI Assistant FAB Button -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAIChat"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_margin="16dp"
        android:src="@drawable/ic_ai_bot"
        android:contentDescription="AI Assistant"
        app:backgroundTint="#7C4DFF"
        app:tint="@android:color/white"
        app:elevation="6dp"
        app:layout_constraintBottom_toTopOf="@id/bottomNav"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Bottom Navigation -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNav"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        android:elevation="8dp"
        app:menu="@menu/bottom_nav_contractor"
        app:layout_constraintBottom_toBottomOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_contractor_directory.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Find Contractors"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="#212121" />
    </com.google.android.material.appbar.MaterialToolbar>

    <!-- Search and Filter Container -->
    <androidx.cardview.widget.CardView
        android:id="@+id/searchCard"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="16dp"
        app:cardCornerRadius="12dp"
        app:cardElevation="2dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="12dp">

            <!-- Search Bar -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/bg_search">

                <ImageView
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:src="@drawable/ic_search"
                    android:layout_marginStart="12dp"
                    app:tint="#757575" />

                <EditText
                    android:id="@+id/etSearch"
                    android:layout_width="0dp"
                    android:layout_height="match_parent"
                    android:layout_weight="1"
                    android:hint="Search contractors..."
                    android:background="@android:color/transparent"
                    android:paddingStart="12dp"
                    android:paddingEnd="12dp"
                    android:textSize="14sp"
                    android:maxLines="1" />

                <ImageView
                    android:id="@+id/btnFilter"
                    android:layout_width="40dp"
                    android:layout_height="40dp"
                    android:src="@drawable/ic_filter"
                    android:padding="8dp"
                    android:layout_marginEnd="4dp"
                    app:tint="#7C4DFF"
                    android:background="?attr/selectableItemBackgroundBorderless" />
            </LinearLayout>

            <!-- Category Pills -->
            <HorizontalScrollView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:scrollbars="none"
                android:layout_marginTop="12dp">

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:id="@+id/btnAllCategories"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="All"
                        android:textSize="13sp"
                        android:textStyle="bold"
                        android:textColor="@android:color/white"
                        android:background="@drawable/bg_pill_selected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btnElectrician"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="Electrician"
                        android:textSize="13sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btnPlumber"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="Plumber"
                        android:textSize="13sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btnMason"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="Mason"
                        android:textSize="13sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btnCarpenter"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="Carpenter"
                        android:textSize="13sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btnPainter"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="Painter"
                        android:textSize="13sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center" />
                </LinearLayout>
            </HorizontalScrollView>
        </LinearLayout>
    </androidx.cardview.widget.CardView>

    <!-- Results Count -->
    <TextView
        android:id="@+id/tvResultsCount"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="120 contractors found"
        android:textSize="14sp"
        android:textColor="#757575"
        android:layout_marginStart="16dp"
        android:layout_marginTop="8dp"
        app:layout_constraintTop_toBottomOf="@id/searchCard"
        app:layout_constraintStart_toStartOf="parent" />

    <!-- Sort Button -->
    <TextView
        android:id="@+id/btnSort"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Sort by: Rating"
        android:textSize="14sp"
        android:textStyle="bold"
        android:textColor="#7C4DFF"
        android:drawableEnd="@drawable/ic_dropdown"
        android:drawablePadding="4dp"
        android:layout_marginEnd="16dp"
        app:layout_constraintTop_toTopOf="@id/tvResultsCount"
        app:layout_constraintBottom_toBottomOf="@id/tvResultsCount"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Contractors List -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvContractors"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:padding="16dp"
        android:clipToPadding="false"
        android:layout_marginTop="8dp"
        app:layout_constraintTop_toBottomOf="@id/tvResultsCount"
        app:layout_constraintBottom_toBottomOf="parent"
        tools:listitem="@layout/item_contractor_card" />

    <!-- Loading Indicator -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Empty State -->
    <LinearLayout
        android:id="@+id/emptyState"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="32dp"
        android:visibility="gone"
        app:layout_constraintTop_toBottomOf="@id/tvResultsCount"
        app:layout_constraintBottom_toBottomOf="parent">

        <ImageView
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:src="@drawable/ic_contractor"
            android:alpha="0.5" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="No contractors found"
            android:textSize="16sp"
            android:textColor="#757575"
            android:layout_marginTop="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Try adjusting your search criteria"
            android:textSize="14sp"
            android:textColor="#9E9E9E"
            android:layout_marginTop="4dp" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_contractor_profile.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:background="#F8F9FA">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Top App Bar -->
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="@android:color/white"
            android:elevation="4dp"
            app:navigationIcon="@drawable/ic_back"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Contractor Profile"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <ImageView
                android:id="@+id/btnShare"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_gravity="end"
                android:layout_marginEnd="48dp"
                android:padding="4dp"
                android:src="@drawable/ic_share"
                android:contentDescription="Share" />

            <ImageView
                android:id="@+id/btnFavorite"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_gravity="end"
                android:layout_marginEnd="16dp"
                android:padding="4dp"
                android:src="@drawable/ic_favorite_border"
                android:contentDescription="Favorite" />
        </com.google.android.material.appbar.MaterialToolbar>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            app:layout_constraintTop_toBottomOf="@id/toolbar">

            <!-- Profile Header Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_margin="16dp"
                app:cardCornerRadius="16dp"
                app:cardElevation="2dp">

                <androidx.constraintlayout.widget.ConstraintLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:padding="20dp">

                    <!-- Profile Image -->
                    <de.hdodenhof.circleimageview.CircleImageView
                        android:id="@+id/ivProfileImage"
                        android:layout_width="80dp"
                        android:layout_height="80dp"
                        android:src="@drawable/ic_default_profile"
                        app:civ_border_width="3dp"
                        app:civ_border_color="#7C4DFF"
                        app:layout_constraintTop_toTopOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />

                    <!-- Verified Badge -->
                    <ImageView
                        android:layout_width="24dp"
                        android:layout_height="24dp"
                        android:src="@drawable/ic_verified"
                        app:layout_constraintBottom_toBottomOf="@id/ivProfileImage"
                        app:layout_constraintEnd_toEndOf="@id/ivProfileImage" />

                    <!-- Name -->
                    <TextView
                        android:id="@+id/tvName"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Ahmad Ali"
                        android:textSize="22sp"
                        android:textStyle="bold"
                        android:textColor="#212121"
                        android:layout_marginTop="16dp"
                        app:layout_constraintTop_toBottomOf="@id/ivProfileImage"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />

                    <!-- Category -->
                    <TextView
                        android:id="@+id/tvCategory"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Professional Electrician"
                        android:textSize="15sp"
                        android:textColor="#7C4DFF"
                        android:layout_marginTop="4dp"
                        app:layout_constraintTop_toBottomOf="@id/tvName"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />

                    <!-- Rating and Reviews -->
                    <LinearLayout
                        android:id="@+id/ratingLayout"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginTop="12dp"
                        app:layout_constraintTop_toBottomOf="@id/tvCategory"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent">

                        <ImageView
                            android:layout_width="18dp"
                            android:layout_height="18dp"
                            android:src="@drawable/ic_star"
                            app:tint="#FFA726" />

                        <TextView
                            android:id="@+id/tvRating"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="4.8"
                            android:textSize="16sp"
                            android:textStyle="bold"
                            android:textColor="#212121"
                            android:layout_marginStart="6dp" />

                        <TextView
                            android:id="@+id/tvTotalReviews"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="(24 reviews)"
                            android:textSize="14sp"
                            android:textColor="#757575"
                            android:layout_marginStart="4dp" />
                    </LinearLayout>

                    <!-- Stats Row -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:weightSum="3"
                        android:layout_marginTop="20dp"
                        android:padding="12dp"
                        android:background="#F5F5F5"
                        app:layout_constraintTop_toBottomOf="@id/ratingLayout">

                        <!-- Projects Completed -->
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical"
                            android:gravity="center">

                            <TextView
                                android:id="@+id/tvCompletedProjects"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="15"
                                android:textSize="20sp"
                                android:textStyle="bold"
                                android:textColor="#7C4DFF" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Projects"
                                android:textSize="12sp"
                                android:textColor="#757575"
                                android:layout_marginTop="2dp" />
                        </LinearLayout>

                        <!-- Experience -->
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical"
                            android:gravity="center">

                            <TextView
                                android:id="@+id/tvExperience"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="5"
                                android:textSize="20sp"
                                android:textStyle="bold"
                                android:textColor="#4CAF50" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Years Exp"
                                android:textSize="12sp"
                                android:textColor="#757575"
                                android:layout_marginTop="2dp" />
                        </LinearLayout>

                        <!-- Response Time -->
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical"
                            android:gravity="center">

                            <TextView
                                android:id="@+id/tvResponseTime"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="2h"
                                android:textSize="20sp"
                                android:textStyle="bold"
                                android:textColor="#FFA726" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Avg Response"
                                android:textSize="12sp"
                                android:textColor="#757575"
                                android:layout_marginTop="2dp" />
                        </LinearLayout>
                    </LinearLayout>

                </androidx.constraintlayout.widget.ConstraintLayout>
            </androidx.cardview.widget.CardView>

            <!-- Contact Buttons -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:weightSum="2"
                android:layout_marginStart="16dp"
                android:layout_marginEnd="16dp"
                android:layout_marginBottom="16dp">

                <Button
                    android:id="@+id/btnMessage"
                    android:layout_width="0dp"
                    android:layout_height="52dp"
                    android:layout_weight="1"
                    android:text="Message"
                    android:textAllCaps="false"
                    android:textSize="15sp"
                    android:textStyle="bold"
                    android:backgroundTint="#7C4DFF"
                    android:drawableLeft="@drawable/ic_chat"
                    android:drawablePadding="8dp"
                    android:layout_marginEnd="8dp" />

                <Button
                    android:id="@+id/btnCall"
                    android:layout_width="0dp"
                    android:layout_height="52dp"
                    android:layout_weight="1"
                    android:text="Call"
                    android:textAllCaps="false"
                    android:textSize="15sp"
                    android:textStyle="bold"
                    android:backgroundTint="#4CAF50"
                    android:drawableLeft="@drawable/ic_phone"
                    android:drawablePadding="8dp"
                    android:layout_marginStart="8dp" />
            </LinearLayout>

            <!-- About Section -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="16dp"
                android:layout_marginEnd="16dp"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="About"
                        android:textSize="16sp"
                        android:textStyle="bold"
                        android:textColor="#212121"
                        android:layout_marginBottom="8dp" />

                    <TextView
                        android:id="@+id/tvBio"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Professional electrician with 5+ years of experience in residential and commercial electrical work. Specialized in wiring, circuit breaker installation, and electrical troubleshooting."
                        android:textSize="14sp"
                        android:textColor="#757575"
                        android:lineSpacingExtra="4dp" />

                    <!-- Location -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginTop="12dp">

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_location"
                            app:tint="#7C4DFF" />

                        <TextView
                            android:id="@+id/tvLocation"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="DHA Phase 5, Lahore"
                            android:textSize="14sp"
                            android:textColor="#212121"
                            android:layout_marginStart="8dp" />
                    </LinearLayout>

                    <!-- Hourly Rate -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginTop="8dp">

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_money"
                            app:tint="#4CAF50" />

                        <TextView
                            android:id="@+id/tvHourlyRate"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Rs. 1,500/day"
                            android:textSize="14sp"
                            android:textColor="#212121"
                            android:layout_marginStart="8dp" />
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Portfolio Section -->
            <androidx.cardview.widget.CardView
                android:id="@+id/portfolioSection"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="16dp"
                android:layout_marginEnd="16dp"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Portfolio"
                            android:textSize="16sp"
                            android:textStyle="bold"
                            android:textColor="#212121" />

                        <TextView
                            android:id="@+id/tvViewAllPortfolio"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="View All"
                            android:textColor="#7C4DFF"
                            android:textSize="14sp"
                            android:textStyle="bold" />
                    </LinearLayout>

                    <!-- Portfolio Images Grid -->
                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/rvPortfolio"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="12dp"
                        android:nestedScrollingEnabled="false"
                        tools:listitem="@layout/item_portfolio_image" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Reviews Section -->
            <androidx.cardview.widget.CardView
                android:id="@+id/reviewsSection"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="16dp"
                android:layout_marginEnd="16dp"
                android:layout_marginBottom="24dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Reviews"
                            android:textSize="16sp"
                            android:textStyle="bold"
                            android:textColor="#212121" />

                        <TextView
                            android:id="@+id/tvViewAllReviews"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="View All"
                            android:textColor="#7C4DFF"
                            android:textSize="14sp"
                            android:textStyle="bold" />
                    </LinearLayout>

                    <!-- Reviews RecyclerView -->
                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/rvReviews"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="12dp"
                        android:nestedScrollingEnabled="false"
                        tools:listitem="@layout/item_review" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

        </LinearLayout>

        <!-- Loading Indicator -->
        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:visibility="gone"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>```

#### activity_edit_profile.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:background="#F8F9FA">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Top App Bar -->
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="@android:color/white"
            android:elevation="4dp"
            app:navigationIcon="@drawable/ic_back"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Edit Profile"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#212121" />
        </com.google.android.material.appbar.MaterialToolbar>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp"
            app:layout_constraintTop_toBottomOf="@id/toolbar">

            <!-- Profile Picture -->
            <RelativeLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginBottom="24dp">

                <de.hdodenhof.circleimageview.CircleImageView
                    android:id="@+id/ivProfilePicture"
                    android:layout_width="120dp"
                    android:layout_height="120dp"
                    android:src="@drawable/ic_default_profile"
                    app:civ_border_width="3dp"
                    app:civ_border_color="#7C4DFF" />

                <ImageView
                    android:id="@+id/btnChangePicture"
                    android:layout_width="40dp"
                    android:layout_height="40dp"
                    android:src="@drawable/ic_camera"
                    android:background="@drawable/bg_button_purple"
                    android:padding="8dp"
                    app:tint="@android:color/white"
                    android:layout_alignEnd="@id/ivProfilePicture"
                    android:layout_alignBottom="@id/ivProfilePicture" />
            </RelativeLayout>

            <!-- Full Name -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Full Name *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etFullName"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="Enter your full name"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="textPersonName"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Email -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Email *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etEmail"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="Enter your email"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="textEmailAddress"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Phone Number -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Phone Number *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etPhone"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="Enter your phone number"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="phone"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- City -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="City *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <Spinner
                android:id="@+id/spinnerCity"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Address -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Address *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etAddress"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="Enter your address"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="textPostalAddress"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Bio (For Contractors) -->
            <TextView
                android:id="@+id/labelBio"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Professional Bio"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:visibility="gone" />

            <EditText
                android:id="@+id/etBio"
                android:layout_width="match_parent"
                android:layout_height="120dp"
                android:hint="Tell clients about your expertise..."
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:paddingTop="12dp"
                android:gravity="top"
                android:inputType="textMultiLine"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp"
                android:visibility="gone" />

            <!-- Category (For Contractors) -->
            <TextView
                android:id="@+id/labelCategory"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Category *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:visibility="gone" />

            <Spinner
                android:id="@+id/spinnerCategory"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp"
                android:visibility="gone" />

            <!-- Years of Experience (For Contractors) -->
            <TextView
                android:id="@+id/labelExperience"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Years of Experience *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:visibility="gone" />

            <EditText
                android:id="@+id/etExperience"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="e.g., 5"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="number"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp"
                android:visibility="gone" />

            <!-- Hourly Rate (For Contractors) -->
            <TextView
                android:id="@+id/labelHourlyRate"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Hourly Rate (PKR) *"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:visibility="gone" />

            <EditText
                android:id="@+id/etHourlyRate"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="e.g., 1500"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="numberDecimal"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="24dp"
                android:visibility="gone" />

            <!-- Save Button -->
            <Button
                android:id="@+id/btnSaveProfile"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Save Changes"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_button_purple" />

            <!-- Cancel Button -->
            <Button
                android:id="@+id/btnCancel"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Cancel"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:backgroundTint="#EEEEEE"
                android:textColor="#212121"
                android:layout_marginTop="12dp"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>```

#### activity_job_detail.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- App Bar -->
    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/appBarLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        android:elevation="4dp">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:navigationIcon="@drawable/ic_back">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Job Details"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <ImageView
                android:id="@+id/btnShare"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_gravity="end"
                android:layout_marginEnd="48dp"
                android:padding="4dp"
                android:src="@drawable/ic_share"
                android:contentDescription="Share" />

            <ImageView
                android:id="@+id/btnEdit"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_gravity="end"
                android:layout_marginEnd="16dp"
                android:padding="4dp"
                android:src="@drawable/ic_edit"
                android:contentDescription="Edit" />
        </com.google.android.material.appbar.MaterialToolbar>
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Job Info Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <androidx.constraintlayout.widget.ConstraintLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:padding="16dp">

                    <!-- Job Title -->
                    <TextView
                        android:id="@+id/tvJobTitle"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:text="House Renovation Required"
                        android:textSize="20sp"
                        android:textStyle="bold"
                        android:textColor="#212121"
                        app:layout_constraintTop_toTopOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toStartOf="@id/tvStatus" />

                    <!-- Status Badge -->
                    <TextView
                        android:id="@+id/tvStatus"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Open"
                        android:textSize="12sp"
                        android:textStyle="bold"
                        android:textColor="#4CAF50"
                        android:background="@drawable/bg_status_green"
                        android:paddingStart="12dp"
                        android:paddingEnd="12dp"
                        android:paddingTop="4dp"
                        android:paddingBottom="4dp"
                        app:layout_constraintTop_toTopOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />

                    <!-- Category -->
                    <TextView
                        android:id="@+id/tvCategory"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Electrician"
                        android:textSize="14sp"
                        android:textColor="#7C4DFF"
                        android:layout_marginTop="8dp"
                        app:layout_constraintTop_toBottomOf="@id/tvJobTitle"
                        app:layout_constraintStart_toStartOf="parent" />

                    <!-- Posted Date -->
                    <TextView
                        android:id="@+id/tvPostedDate"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Posted 2 days ago"
                        android:textSize="13sp"
                        android:textColor="#9E9E9E"
                        app:layout_constraintTop_toTopOf="@id/tvCategory"
                        app:layout_constraintBottom_toBottomOf="@id/tvCategory"
                        app:layout_constraintEnd_toEndOf="parent" />

                    <!-- Description -->
                    <TextView
                        android:id="@+id/tvDescription"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Need experienced electrician for complete house wiring. Must have 5+ years experience in residential electrical work."
                        android:textSize="15sp"
                        android:textColor="#424242"
                        android:lineSpacingExtra="2dp"
                        android:layout_marginTop="16dp"
                        app:layout_constraintTop_toBottomOf="@id/tvCategory" />

                    <!-- Divider -->
                    <View
                        android:id="@+id/divider1"
                        android:layout_width="match_parent"
                        android:layout_height="1dp"
                        android:background="#E0E0E0"
                        android:layout_marginTop="16dp"
                        app:layout_constraintTop_toBottomOf="@id/tvDescription" />

                    <!-- Budget & Timeline Row -->
                    <LinearLayout
                        android:id="@+id/statsLayout"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:weightSum="3"
                        android:layout_marginTop="16dp"
                        app:layout_constraintTop_toBottomOf="@id/divider1">

                        <!-- Budget -->
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical"
                            android:gravity="center">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Budget"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <TextView
                                android:id="@+id/tvBudget"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Rs. 50,000"
                                android:textSize="18sp"
                                android:textStyle="bold"
                                android:textColor="#7C4DFF"
                                android:layout_marginTop="4dp" />
                        </LinearLayout>

                        <!-- Timeline -->
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical"
                            android:gravity="center">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Timeline"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <TextView
                                android:id="@+id/tvTimeline"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="7 days"
                                android:textSize="18sp"
                                android:textStyle="bold"
                                android:textColor="#FFA726"
                                android:layout_marginTop="4dp" />
                        </LinearLayout>

                        <!-- Total Bids -->
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical"
                            android:gravity="center">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Bids"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <TextView
                                android:id="@+id/tvTotalBids"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="5"
                                android:textSize="18sp"
                                android:textStyle="bold"
                                android:textColor="#4CAF50"
                                android:layout_marginTop="4dp" />
                        </LinearLayout>
                    </LinearLayout>

                    <!-- Location -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginTop="16dp"
                        app:layout_constraintTop_toBottomOf="@id/statsLayout">

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_location"
                            app:tint="#7C4DFF" />

                        <TextView
                            android:id="@+id/tvLocation"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="DHA Phase 5, Lahore, Pakistan"
                            android:textSize="14sp"
                            android:textColor="#212121"
                            android:layout_marginStart="8dp" />
                    </LinearLayout>
                </androidx.constraintlayout.widget.ConstraintLayout>
            </androidx.cardview.widget.CardView>

            <!-- Bids Section Header -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Received Bids"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="#212121" />

                <TextView
                    android:id="@+id/btnSortBids"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Sort by: Lowest"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    android:textColor="#7C4DFF"
                    android:drawableEnd="@drawable/ic_dropdown"
                    android:drawablePadding="4dp" />
            </LinearLayout>

            <!-- Bids RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvBids"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                tools:listitem="@layout/item_bid_card" />

            <!-- Empty State -->
            <LinearLayout
                android:id="@+id/emptyState"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:orientation="vertical"
                android:gravity="center"
                android:visibility="gone">

                <ImageView
                    android:layout_width="80dp"
                    android:layout_height="80dp"
                    android:src="@drawable/ic_bid"
                    android:alpha="0.5" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="No bids yet"
                    android:textSize="16sp"
                    android:textColor="#757575"
                    android:layout_marginTop="16dp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Contractors will submit their bids soon"
                    android:textSize="14sp"
                    android:textColor="#9E9E9E"
                    android:layout_marginTop="4dp" />
            </LinearLayout>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

    <!-- Submit Bid Button (Visible for Contractors) -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnSubmitBid"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:layout_margin="16dp"
        android:text="Submit Bid"
        android:textSize="16sp"
        android:textStyle="bold"
        android:textColor="@android:color/white"
        android:backgroundTint="#7C4DFF"
        android:paddingTop="14dp"
        android:paddingBottom="14dp"
        app:cornerRadius="12dp"
        app:elevation="4dp"
        android:visibility="gone" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>```

#### activity_job_post.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:background="#F8F9FA">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Top App Bar -->
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="@android:color/white"
            android:elevation="4dp"
            app:navigationIcon="@drawable/ic_back"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Post a Job"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#212121" />
        </com.google.android.material.appbar.MaterialToolbar>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp"
            app:layout_constraintTop_toBottomOf="@id/toolbar">

            <!-- Job Title -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Job Title"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etJobTitle"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:hint="e.g., House Renovation Required"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:textSize="14sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Category -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Category"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <Spinner
                android:id="@+id/spinnerCategory"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Description -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Description"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etJobDescription"
                android:layout_width="match_parent"
                android:layout_height="120dp"
                android:hint="Describe the work you need done..."
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:paddingTop="12dp"
                android:gravity="top"
                android:inputType="textMultiLine"
                android:textSize="14sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Budget -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Budget (PKR)"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etBudget"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:hint="Enter your budget"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="number"
                android:textSize="14sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Location -->
            <EditText
                android:id="@+id/etLocation"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Location"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <!-- City Spinner -->
            <Spinner
                android:id="@+id/spinnerCity"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="8dp" />

            <!-- Address -->
            <EditText
                android:id="@+id/etAddress"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:hint="Enter complete address"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:textSize="14sp"
                android:layout_marginBottom="16dp" />

            <!-- Timeline -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Expected Timeline"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp">

                <EditText
                    android:id="@+id/etTimeline"
                    android:layout_width="0dp"
                    android:layout_height="52dp"
                    android:layout_weight="1"
                    android:hint="Number of days"
                    android:background="@drawable/bg_input_field_light"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:inputType="number"
                    android:textSize="14sp"
                    android:layout_marginEnd="8dp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="52dp"
                    android:text="days"
                    android:gravity="center_vertical"
                    android:textSize="14sp"
                    android:textColor="#757575" />
            </LinearLayout>

            <!-- Upload Photos -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Upload Photos (Optional)"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginBottom="8dp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="24dp">

                <Button
                    android:id="@+id/btnTakePhoto"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:text="Take Photo"
                    android:textAllCaps="false"
                    android:drawableLeft="@drawable/ic_camera"
                    android:drawablePadding="8dp"
                    android:backgroundTint="#EEEEEE"
                    android:textColor="#212121"
                    android:layout_marginEnd="8dp" />

                <Button
                    android:id="@+id/btnAddPhoto"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:text="Add Photo"
                    android:textAllCaps="false"
                    android:drawableLeft="@drawable/ic_gallery"
                    android:drawablePadding="8dp"
                    android:backgroundTint="#EEEEEE"
                    android:textColor="#212121"
                    android:layout_marginStart="8dp" />
            </LinearLayout>

            <!-- Selected Photos RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvSelectedPhotos"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="24dp"
                android:visibility="gone" />

            <!-- Post Job Button -->
            <Button
                android:id="@+id/btnPostJob"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Post Job"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_button_purple" />
            <Button
                android:id="@+id/btnCancel"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Cancel"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:textStyle="bold"
                android:layout_marginTop="15dp"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_button_red" />

        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>```

#### activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <!-- Background Image -->
    <ImageView
        android:id="@+id/backgroundImage"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="centerCrop"
        android:src="@drawable/intro_background"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Login Card Container -->
    <androidx.cardview.widget.CardView
        android:id="@+id/loginCard"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="24dp"
        android:layout_marginEnd="24dp"
        app:cardBackgroundColor="#CC000000"
        app:cardCornerRadius="24dp"
        app:cardElevation="8dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <androidx.constraintlayout.widget.ConstraintLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="28dp">

            <!-- Close Button -->
            <ImageView
                android:id="@+id/btnClose"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:padding="6dp"
                android:src="@drawable/ic_close"
                android:contentDescription="Close"
                app:layout_constraintTop_toTopOf="parent"
                app:layout_constraintEnd_toEndOf="parent"
                app:tint="@android:color/white" />

            <!-- Title - Welcome Back to -->
            <TextView
                android:id="@+id/textView"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Welcome Back to"
                android:textSize="18sp"
                android:textColor="@android:color/white"
                android:textAlignment="center"
                android:layout_marginTop="32dp"
                app:layout_constraintTop_toBottomOf="@id/btnClose"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent" />

            <!-- App Name -->
            <TextView
                android:id="@+id/appNameText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Thaika.co"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="@android:color/white"
                android:textAlignment="center"
                app:layout_constraintTop_toBottomOf="@id/textView"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent" />

            <!-- Subtitle -->
            <TextView
                android:id="@+id/subtitleText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="filled below fields to login your account"
                android:textSize="13sp"
                android:textColor="#99FFFFFF"
                android:textAlignment="center"
                android:layout_marginTop="8dp"
                app:layout_constraintTop_toBottomOf="@id/appNameText"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent" />

            <!-- Email Label -->
            <TextView
                android:id="@+id/emailLabel"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Email Address"
                android:textSize="14sp"
                android:textColor="@android:color/white"
                android:layout_marginTop="24dp"
                app:layout_constraintTop_toBottomOf="@id/subtitleText"
                app:layout_constraintStart_toStartOf="parent" />

            <!-- Email Input -->
            <EditText
                android:id="@+id/email"
                android:layout_width="0dp"
                android:layout_height="52dp"
                android:hint="Enter email address"
                android:inputType="textEmailAddress"
                android:background="@drawable/bg_input_field"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:textColor="@android:color/white"
                android:textColorHint="#66FFFFFF"
                android:textSize="14sp"
                android:layout_marginTop="8dp"
                app:layout_constraintTop_toBottomOf="@id/emailLabel"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent" />

            <!-- Password Label -->
            <TextView
                android:id="@+id/passwordLabel"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Password"
                android:textSize="14sp"
                android:textColor="@android:color/white"
                android:layout_marginTop="16dp"
                app:layout_constraintTop_toBottomOf="@id/email"
                app:layout_constraintStart_toStartOf="parent" />

            <!-- Password Input -->
            <EditText
                android:id="@+id/password"
                android:layout_width="0dp"
                android:layout_height="52dp"
                android:hint="Enter password"
                android:inputType="textPassword"
                android:background="@drawable/bg_input_field"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:textColor="@android:color/white"
                android:textColorHint="#66FFFFFF"
                android:textSize="14sp"
                android:layout_marginTop="8dp"
                app:layout_constraintTop_toBottomOf="@id/passwordLabel"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent" />

            <!-- Forgot Password Link -->
            <TextView
                android:id="@+id/forgotPassword"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Forgot Password?"
                android:textColor="#FFA726"
                android:textSize="13sp"
                android:layout_marginTop="12dp"
                app:layout_constraintTop_toBottomOf="@id/password"
                app:layout_constraintEnd_toEndOf="parent" />

            <!-- Login Button -->
            <Button
                android:id="@+id/loginbtn"
                android:layout_width="0dp"
                android:layout_height="56dp"
                android:text="Log In"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_button_purple"
                android:layout_marginTop="24dp"
                app:layout_constraintTop_toBottomOf="@id/forgotPassword"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent" />

            <!-- Create Account Link Container -->
            <LinearLayout
                android:id="@+id/signupContainer"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="16dp"
                app:layout_constraintTop_toBottomOf="@id/loginbtn"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Don't have an account? "
                    android:textColor="#99FFFFFF"
                    android:textSize="13sp" />

                <TextView
                    android:id="@+id/createbtn"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Create an account"
                    android:textColor="#7C4DFF"
                    android:textSize="13sp"
                    android:textStyle="bold" />

                <ProgressBar
                    android:id="@+id/progressBar"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:visibility="gone" />

            </LinearLayout>

        </androidx.constraintlayout.widget.ConstraintLayout>
    </androidx.cardview.widget.CardView>

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_material_management.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Material Management"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="#212121" />

        <ImageView
            android:id="@+id/btnNotifications"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="end"
            android:layout_marginEnd="16dp"
            android:padding="4dp"
            android:src="@drawable/ic_notifications"
            android:contentDescription="Notifications" />
    </com.google.android.material.appbar.MaterialToolbar>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:fillViewport="true"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/bottomButtonLayout">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Tab Layout -->
            <com.google.android.material.tabs.TabLayout
                android:id="@+id/tabLayout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="@android:color/white"
                app:tabMode="fixed"
                app:tabSelectedTextColor="#7C4DFF"
                app:tabTextColor="#757575"
                app:tabIndicatorColor="#7C4DFF"
                android:layout_marginBottom="16dp">

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Inventory" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Request" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Received" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Used" />
            </com.google.android.material.tabs.TabLayout>

            <!-- Search Bar -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="24dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="48dp"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp">

                    <ImageView
                        android:layout_width="20dp"
                        android:layout_height="20dp"
                        android:src="@drawable/ic_search"
                        app:tint="#757575" />

                    <EditText
                        android:id="@+id/etSearch"
                        android:layout_width="0dp"
                        android:layout_height="match_parent"
                        android:layout_weight="1"
                        android:hint="Search materials..."
                        android:textSize="14sp"
                        android:background="@android:color/transparent"
                        android:paddingStart="12dp"
                        android:paddingEnd="12dp" />

                    <ImageView
                        android:id="@+id/btnFilter"
                        android:layout_width="20dp"
                        android:layout_height="20dp"
                        android:src="@drawable/ic_filter"
                        app:tint="#7C4DFF" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Stats Cards -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:weightSum="3"
                android:layout_marginBottom="16dp">

                <!-- Total Items -->
                <androidx.cardview.widget.CardView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginEnd="8dp"
                    app:cardCornerRadius="12dp"
                    app:cardElevation="2dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="16dp"
                        android:gravity="center">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Total Items"
                            android:textSize="11sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvTotalItems"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="45"
                            android:textSize="28sp"
                            android:textStyle="bold"
                            android:textColor="#7C4DFF"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>

                <!-- Low Stock -->
                <androidx.cardview.widget.CardView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="4dp"
                    android:layout_marginEnd="4dp"
                    app:cardCornerRadius="12dp"
                    app:cardElevation="2dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="16dp"
                        android:gravity="center">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Low Stock"
                            android:textSize="11sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvLowStock"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="8"
                            android:textSize="28sp"
                            android:textStyle="bold"
                            android:textColor="#FFA726"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>

                <!-- Total Value -->
                <androidx.cardview.widget.CardView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="8dp"
                    app:cardCornerRadius="12dp"
                    app:cardElevation="2dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="16dp"
                        android:gravity="center">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Total Value"
                            android:textSize="11sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvTotalValue"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="₹2.5L"
                            android:textSize="24sp"
                            android:textStyle="bold"
                            android:textColor="#4CAF50"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>
            </LinearLayout>

            <!-- Materials Section Header -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Materials"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="#212121" />

                <TextView
                    android:id="@+id/tvSortBy"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Sort by: Name"
                    android:textSize="13sp"
                    android:textColor="#7C4DFF"
                    android:drawableEnd="@drawable/ic_dropdown"
                    android:drawablePadding="4dp" />
            </LinearLayout>

            <!-- Materials RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvMaterials"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                tools:listitem="@layout/item_material_card" />

            <!-- Empty State -->
            <LinearLayout
                android:id="@+id/emptyState"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:orientation="vertical"
                android:gravity="center"
                android:visibility="gone">

                <ImageView
                    android:layout_width="80dp"
                    android:layout_height="80dp"
                    android:src="@drawable/ic_empty_jobs"
                    android:alpha="0.5" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="No materials yet"
                    android:textSize="16sp"
                    android:textColor="#757575"
                    android:layout_marginTop="16dp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Add your first material to get started"
                    android:textSize="14sp"
                    android:textColor="#9E9E9E"
                    android:layout_marginTop="4dp" />
            </LinearLayout>

        </LinearLayout>
    </ScrollView>

    <!-- Bottom Action Buttons -->
    <LinearLayout
        android:id="@+id/bottomButtonLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:weightSum="2"
        android:padding="16dp"
        android:background="@android:color/white"
        android:elevation="8dp"
        app:layout_constraintBottom_toBottomOf="parent">

        <Button
            android:id="@+id/btnRequestMaterial"
            android:layout_width="0dp"
            android:layout_height="56dp"
            android:layout_weight="1"
            android:text="Request Material"
            android:textAllCaps="false"
            android:textSize="15sp"
            android:backgroundTint="#EEEEEE"
            android:textColor="#212121"
            android:layout_marginEnd="8dp"
            style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

        <Button
            android:id="@+id/btnAddMaterial"
            android:layout_width="0dp"
            android:layout_height="56dp"
            android:layout_weight="1"
            android:text="Add Material"
            android:textAllCaps="false"
            android:textSize="15sp"
            android:backgroundTint="#7C4DFF"
            android:layout_marginStart="8dp"
            android:drawableLeft="@drawable/ic_add_job"
            android:drawablePadding="8dp" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_my_jobs.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Toolbar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- Search Bar -->
    <EditText
        android:id="@+id/etSearch"
        android:layout_width="0dp"
        android:layout_height="48dp"
        android:layout_margin="16dp"
        android:hint="Search jobs..."
        android:drawableStart="@drawable/ic_search"
        android:drawablePadding="8dp"
        android:paddingStart="12dp"
        android:paddingEnd="12dp"
        android:background="@drawable/bg_input_field_light"
        android:inputType="text"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Tab Layout -->
    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:background="@android:color/white"
        app:tabTextColor="#757575"
        app:tabSelectedTextColor="#7C4DFF"
        app:tabIndicatorColor="#7C4DFF"
        app:tabMode="scrollable"
        app:layout_constraintTop_toBottomOf="@id/etSearch" />

    <!-- Progress Bar -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Jobs RecyclerView -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvJob"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        android:paddingTop="16dp"
        android:paddingBottom="16dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/tabLayout"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        tools:listitem="@layout/item_job_card" />

    <!-- Empty State -->
    <LinearLayout
        android:id="@+id/emptyState"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageView
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:src="@drawable/ic_empty_jobs"
            android:alpha="0.5" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="No jobs found"
            android:textSize="16sp"
            android:textColor="#757575"
            android:layout_marginTop="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Try posting a new job or adjust filters"
            android:textSize="14sp"
            android:textColor="#9E9E9E"
            android:layout_marginTop="4dp" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_my_projects.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Toolbar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- Tab Layout -->
    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        app:tabTextColor="#757575"
        app:tabSelectedTextColor="#7C4DFF"
        app:tabIndicatorColor="#7C4DFF"
        app:tabMode="fixed"
        app:layout_constraintTop_toBottomOf="@id/toolbar" />

    <!-- Progress Bar -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Projects RecyclerView -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvProjects"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        android:paddingTop="16dp"
        android:paddingBottom="16dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/tabLayout"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        tools:listitem="@layout/item_job_card" />

    <!-- Empty State -->
    <LinearLayout
        android:id="@+id/emptyState"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageView
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:src="@drawable/ic_empty_jobs"
            android:alpha="0.5" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="No projects yet"
            android:textSize="16sp"
            android:textColor="#757575"
            android:layout_marginTop="16dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Your assigned projects will appear here"
            android:textSize="14sp"
            android:textColor="#9E9E9E"
            android:layout_marginTop="4dp" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_notifications.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Notifications"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="#212121" />

        <TextView
            android:id="@+id/btnMarkAllRead"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end"
            android:layout_marginEnd="16dp"
            android:text="Mark all read"
            android:textSize="14sp"
            android:textStyle="bold"
            android:textColor="#7C4DFF"
            android:padding="8dp" />
    </com.google.android.material.appbar.MaterialToolbar>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toBottomOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Today Section -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Today"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginBottom="12dp" />

            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvTodayNotifications"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                android:layout_marginBottom="24dp"
                tools:listitem="@layout/item_notification" />

            <!-- Earlier Section -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Earlier"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginBottom="12dp" />

            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvEarlierNotifications"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                tools:listitem="@layout/item_notification" />

            <!-- Empty State -->
            <LinearLayout
                android:id="@+id/emptyState"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:orientation="vertical"
                android:gravity="center"
                android:visibility="gone">

                <ImageView
                    android:layout_width="80dp"
                    android:layout_height="80dp"
                    android:src="@drawable/ic_notifications"
                    android:alpha="0.5" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="No notifications"
                    android:textSize="16sp"
                    android:textColor="#757575"
                    android:layout_marginTop="16dp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="You're all caught up!"
                    android:textSize="14sp"
                    android:textColor="#9E9E9E"
                    android:layout_marginTop="4dp" />
            </LinearLayout>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_portfolio_gallery.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Portfolio"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="#212121" />

        <ImageView
            android:id="@+id/btnFilter"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="end"
            android:layout_marginEnd="16dp"
            android:padding="4dp"
            android:src="@drawable/ic_filter" />
    </com.google.android.material.appbar.MaterialToolbar>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toBottomOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Portfolio Stats -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:weightSum="3"
                android:layout_marginBottom="16dp">

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:gravity="center">

                    <TextView
                        android:id="@+id/tvTotalProjects"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="24"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        android:textColor="#7C4DFF" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Projects"
                        android:textSize="12sp"
                        android:textColor="#757575"
                        android:layout_marginTop="4dp" />
                </LinearLayout>

                <View
                    android:layout_width="1dp"
                    android:layout_height="match_parent"
                    android:background="#E0E0E0" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:gravity="center">

                    <TextView
                        android:id="@+id/tvTotalPhotos"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="156"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        android:textColor="#4CAF50" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Photos"
                        android:textSize="12sp"
                        android:textColor="#757575"
                        android:layout_marginTop="4dp" />
                </LinearLayout>

                <View
                    android:layout_width="1dp"
                    android:layout_height="match_parent"
                    android:background="#E0E0E0" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:gravity="center">

                    <TextView
                        android:id="@+id/tvTotalLikes"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="892"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        android:textColor="#FFA726" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Likes"
                        android:textSize="12sp"
                        android:textColor="#757575"
                        android:layout_marginTop="4dp" />
                </LinearLayout>
            </LinearLayout>

            <!-- Category Filter Pills -->
            <HorizontalScrollView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:scrollbars="none"
                android:layout_marginBottom="16dp">

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:id="@+id/btnAll"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="All"
                        android:textSize="14sp"
                        android:textColor="@android:color/white"
                        android:background="@drawable/bg_pill_selected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btnElectrical"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="Electrical"
                        android:textSize="14sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btnPlumbing"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="Plumbing"
                        android:textSize="14sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/btnConstruction"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="Construction"
                        android:textSize="14sp"
                        android:textColor="#757575"
                        android:background="@drawable/bg_pill_unselected"
                        android:paddingStart="20dp"
                        android:paddingEnd="20dp"
                        android:gravity="center" />
                </LinearLayout>
            </HorizontalScrollView>

            <!-- Portfolio Grid -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvPortfolio"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                tools:listitem="@layout/item_portfolio_image" />

            <!-- Empty State -->
            <LinearLayout
                android:id="@+id/emptyState"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:orientation="vertical"
                android:gravity="center"
                android:visibility="gone">

                <ImageView
                    android:layout_width="80dp"
                    android:layout_height="80dp"
                    android:src="@drawable/ic_gallery"
                    android:alpha="0.5" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="No portfolio yet"
                    android:textSize="16sp"
                    android:textColor="#757575"
                    android:layout_marginTop="16dp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Start showcasing your work"
                    android:textSize="14sp"
                    android:textColor="#9E9E9E"
                    android:layout_marginTop="4dp" />
            </LinearLayout>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

    <!-- Add Portfolio FAB -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAddPortfolio"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginEnd="16dp"
        android:layout_marginBottom="16dp"
        android:src="@drawable/ic_add_job"
        android:contentDescription="Add Portfolio"
        app:backgroundTint="#7C4DFF"
        app:tint="@android:color/white"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

    <!-- Loading Indicator -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_settings.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Settings"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="#212121" />
    </com.google.android.material.appbar.MaterialToolbar>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toBottomOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Account Section -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="ACCOUNT"
                android:textSize="12sp"
                android:textStyle="bold"
                android:textColor="#757575"
                android:layout_marginBottom="12dp" />

            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="24dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical">

                    <LinearLayout
                        android:id="@+id/btnEditProfile"
                        android:layout_width="match_parent"
                        android:layout_height="56dp"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:paddingStart="16dp"
                        android:paddingEnd="16dp"
                        android:background="?attr/selectableItemBackground">

                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:src="@drawable/ic_profile"
                            app:tint="#7C4DFF" />

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Edit Profile"
                            android:textSize="15sp"
                            android:textColor="#212121"
                            android:layout_marginStart="16dp" />

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_forward"
                            app:tint="#757575" />
                    </LinearLayout>

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="1dp"
                        android:background="#F0F0F0"
                        android:layout_marginStart="56dp" />

                    <LinearLayout
                        android:id="@+id/btnChangePassword"
                        android:layout_width="match_parent"
                        android:layout_height="56dp"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:paddingStart="16dp"
                        android:paddingEnd="16dp"
                        android:background="?attr/selectableItemBackground">

                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:src="@drawable/ic_settings"
                            app:tint="#7C4DFF" />

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Change Password"
                            android:textSize="15sp"
                            android:textColor="#212121"
                            android:layout_marginStart="16dp" />

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_forward"
                            app:tint="#757575" />
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Notifications Section -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="NOTIFICATIONS"
                android:textSize="12sp"
                android:textStyle="bold"
                android:textColor="#757575"
                android:layout_marginBottom="12dp" />

            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="24dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="56dp"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:paddingStart="16dp"
                        android:paddingEnd="16dp">

                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:src="@drawable/ic_notifications"
                            app:tint="#7C4DFF" />

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Push Notifications"
                            android:textSize="15sp"
                            android:textColor="#212121"
                            android:layout_marginStart="16dp" />

                        <androidx.appcompat.widget.SwitchCompat
                            android:id="@+id/switchPushNotif"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:checked="true"
                            app:thumbTint="#FFFFFF"
                            app:trackTint="#7C4DFF" />
                    </LinearLayout>

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="1dp"
                        android:background="#F0F0F0"
                        android:layout_marginStart="56dp" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="56dp"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:paddingStart="16dp"
                        android:paddingEnd="16dp">

                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:src="@drawable/ic_chat"
                            app:tint="#7C4DFF" />

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Message Notifications"
                            android:textSize="15sp"
                            android:textColor="#212121"
                            android:layout_marginStart="16dp" />

                        <androidx.appcompat.widget.SwitchCompat
                            android:id="@+id/switchMessageNotif"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:checked="true"
                            app:thumbTint="#FFFFFF"
                            app:trackTint="#7C4DFF" />
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- About Section -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="ABOUT"
                android:textSize="12sp"
                android:textStyle="bold"
                android:textColor="#757575"
                android:layout_marginBottom="12dp" />

            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="24dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical">

                    <LinearLayout
                        android:id="@+id/btnPrivacyPolicy"
                        android:layout_width="match_parent"
                        android:layout_height="56dp"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:paddingStart="16dp"
                        android:paddingEnd="16dp"
                        android:background="?attr/selectableItemBackground">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Privacy Policy"
                            android:textSize="15sp"
                            android:textColor="#212121" />

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_forward"
                            app:tint="#757575" />
                    </LinearLayout>

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="1dp"
                        android:background="#F0F0F0" />

                    <LinearLayout
                        android:id="@+id/btnTermsConditions"
                        android:layout_width="match_parent"
                        android:layout_height="56dp"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:paddingStart="16dp"
                        android:paddingEnd="16dp"
                        android:background="?attr/selectableItemBackground">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Terms and Conditions"
                            android:textSize="15sp"
                            android:textColor="#212121" />

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_forward"
                            app:tint="#757575" />
                    </LinearLayout>

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="1dp"
                        android:background="#F0F0F0" />

                    <LinearLayout
                        android:id="@+id/btnHelp"
                        android:layout_width="match_parent"
                        android:layout_height="56dp"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:paddingStart="16dp"
                        android:paddingEnd="16dp"
                        android:background="?attr/selectableItemBackground">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Help and Support"
                            android:textSize="15sp"
                            android:textColor="#212121" />

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:src="@drawable/ic_forward"
                            app:tint="#757575" />
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- App Version -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:text="Version 1.0.0"
                android:textSize="12sp"
                android:textColor="#9E9E9E"
                android:layout_marginTop="16dp"
                android:layout_marginBottom="16dp" />

            <!-- Logout Button -->
            <Button
                android:id="@+id/btnLogout"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Logout"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:textStyle="bold"
                android:backgroundTint="#E53935"
                android:layout_marginBottom="16dp" />

        </LinearLayout>
    </ScrollView>

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_splash.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/intro_background">

    <!-- Overlay -->
    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#807C4DFF" />

    <!-- Logo -->
    <ImageView
        android:id="@+id/ivLogo"
        android:layout_width="120dp"
        android:layout_height="120dp"
        android:src="@drawable/ic_work"
        app:tint="@android:color/white"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintVertical_bias="0.4" />

    <!-- App Name -->
    <TextView
        android:id="@+id/tvAppName"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Thaika.co"
        android:textSize="32sp"
        android:textStyle="bold"
        android:textColor="@android:color/white"
        android:layout_marginTop="24dp"
        app:layout_constraintTop_toBottomOf="@id/ivLogo"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Tagline -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Your Trusted Contractor Marketplace"
        android:textSize="14sp"
        android:textColor="@android:color/white"
        android:alpha="0.9"
        android:layout_marginTop="8dp"
        app:layout_constraintTop_toBottomOf="@id/tvAppName"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Loading Indicator -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:indeterminateTint="@android:color/white"
        android:layout_marginBottom="48dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>```

#### activity_submit_bid.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:background="#F8F9FA">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Top App Bar -->
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="@android:color/white"
            android:elevation="4dp"
            app:navigationIcon="@drawable/ic_back"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Submit Your Bid"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#212121" />
        </com.google.android.material.appbar.MaterialToolbar>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp"
            app:layout_constraintTop_toBottomOf="@id/toolbar">

            <!-- Job Summary Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="24dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp"
                app:cardBackgroundColor="#F5F5F5">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Job Summary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:textColor="#212121"
                        android:layout_marginBottom="8dp" />

                    <TextView
                        android:id="@+id/tvJobTitle"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="House Renovation Required"
                        android:textSize="16sp"
                        android:textStyle="bold"
                        android:textColor="#212121"
                        android:layout_marginBottom="8dp" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Budget: "
                            android:textSize="13sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvJobBudget"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Rs. 50,000"
                            android:textSize="13sp"
                            android:textStyle="bold"
                            android:textColor="#7C4DFF" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="  •  Timeline: "
                            android:textSize="13sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvJobTimeline"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="7 days"
                            android:textSize="13sp"
                            android:textStyle="bold"
                            android:textColor="#FFA726" />
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Your Bid Amount -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Your Bid Amount (PKR)"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <EditText
                android:id="@+id/etBidAmount"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:hint="Enter your bid amount"
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:inputType="number"
                android:textSize="16sp"
                android:textStyle="bold"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Estimated Completion Days -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Estimated Completion Time"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp">

                <EditText
                    android:id="@+id/etCompletionDays"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:hint="Number"
                    android:background="@drawable/bg_input_field_light"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:inputType="number"
                    android:textSize="16sp"
                    android:layout_marginEnd="8dp" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="56dp"
                    android:text="days"
                    android:gravity="center_vertical"
                    android:textSize="16sp"
                    android:textColor="#757575"
                    android:paddingStart="8dp" />
            </LinearLayout>

            <!-- Proposal/Message -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Your Proposal"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Describe your experience and why you're the best fit"
                android:textSize="12sp"
                android:textColor="#9E9E9E"
                android:layout_marginTop="4dp" />

            <EditText
                android:id="@+id/etProposal"
                android:layout_width="match_parent"
                android:layout_height="140dp"
                android:hint="Write your proposal here..."
                android:background="@drawable/bg_input_field_light"
                android:paddingStart="16dp"
                android:paddingEnd="16dp"
                android:paddingTop="12dp"
                android:gravity="top"
                android:inputType="textMultiLine"
                android:textSize="15sp"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="16dp" />

            <!-- Attach Portfolio (Optional) -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Attach Work Photos (Optional)"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginBottom="8dp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="24dp">

                <Button
                    android:id="@+id/btnTakePhoto"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:text="Take Photo"
                    android:textAllCaps="false"
                    android:drawableLeft="@drawable/ic_camera"
                    android:drawablePadding="8dp"
                    android:backgroundTint="#EEEEEE"
                    android:textColor="#212121"
                    android:layout_marginEnd="8dp" />

                <Button
                    android:id="@+id/btnAddPhoto"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:text="Add Photo"
                    android:textAllCaps="false"
                    android:drawableLeft="@drawable/ic_gallery"
                    android:drawablePadding="8dp"
                    android:backgroundTint="#EEEEEE"
                    android:textColor="#212121"
                    android:layout_marginStart="8dp" />
            </LinearLayout>

            <!-- Selected Photos RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvSelectedPhotos"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="24dp"
                android:visibility="gone" />

            <!-- Terms Checkbox -->
            <CheckBox
                android:id="@+id/cbTerms"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="I agree to the terms and conditions"
                android:textSize="13sp"
                android:textColor="#757575"
                android:layout_marginBottom="16dp" />

            <!-- Submit Bid Button -->
            <Button
                android:id="@+id/btnSubmitBid"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Submit Bid"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_button_purple" />

            <!-- Cancel Button -->
            <Button
                android:id="@+id/btnCancel"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Cancel"
                android:textAllCaps="false"
                android:textSize="16sp"
                android:backgroundTint="#EEEEEE"
                android:textColor="#212121"
                android:layout_marginTop="12dp"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>```

#### activity_task_detail.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- App Bar -->
    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/appBarLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/white"
        android:elevation="4dp">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:navigationIcon="@drawable/ic_back">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Task Details"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <ImageView
                android:id="@+id/btnEdit"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_gravity="end"
                android:layout_marginEnd="48dp"
                android:padding="4dp"
                android:src="@drawable/ic_edit" />

            <ImageView
                android:id="@+id/btnMore"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_gravity="end"
                android:layout_marginEnd="16dp"
                android:padding="4dp"
                android:src="@drawable/ic_more" />
        </com.google.android.material.appbar.MaterialToolbar>
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Task Info Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical">

                        <TextView
                            android:id="@+id/tvTaskTitle"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Electrician Work"
                            android:textSize="20sp"
                            android:textStyle="bold"
                            android:textColor="#212121" />

                        <TextView
                            android:id="@+id/tvTaskStatus"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Ongoing"
                            android:textSize="12sp"
                            android:textStyle="bold"
                            android:textColor="#FFA726"
                            android:background="@drawable/bg_pill_unselected"
                            android:paddingStart="12dp"
                            android:paddingEnd="12dp"
                            android:paddingTop="4dp"
                            android:paddingBottom="4dp" />
                    </LinearLayout>

                    <TextView
                        android:id="@+id/tvTaskDescription"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Complete electrical wiring for the main building including installation of switches and sockets."
                        android:textSize="15sp"
                        android:textColor="#757575"
                        android:lineSpacingExtra="2dp"
                        android:layout_marginTop="12dp" />

                    <View
                        android:layout_width="match_parent"
                        android:layout_height="1dp"
                        android:background="#E0E0E0"
                        android:layout_marginTop="16dp"
                        android:layout_marginBottom="16dp" />

                    <!-- Progress Bar -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginBottom="8dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Progress:"
                            android:textSize="13sp"
                            android:textColor="#757575" />

                        <ProgressBar
                            android:id="@+id/progressBar"
                            style="?android:attr/progressBarStyleHorizontal"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:layout_marginStart="12dp"
                            android:layout_marginEnd="12dp"
                            android:progress="60"
                            android:progressTint="#7C4DFF" />

                        <TextView
                            android:id="@+id/tvProgressPercent"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="60%"
                            android:textSize="14sp"
                            android:textStyle="bold"
                            android:textColor="#7C4DFF" />
                    </LinearLayout>

                    <!-- Stats Grid -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:weightSum="2">

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Start Date"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <TextView
                                android:id="@+id/tvStartDate"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="17 Oct 2024"
                                android:textSize="15sp"
                                android:textStyle="bold"
                                android:textColor="#212121"
                                android:layout_marginTop="4dp" />
                        </LinearLayout>

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="End Date"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <TextView
                                android:id="@+id/tvEndDate"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="31 Oct 2024"
                                android:textSize="15sp"
                                android:textStyle="bold"
                                android:textColor="#212121"
                                android:layout_marginTop="4dp" />
                        </LinearLayout>
                    </LinearLayout>

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:weightSum="2"
                        android:layout_marginTop="16dp">

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Workers"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <TextView
                                android:id="@+id/tvWorkers"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="3 Workers"
                                android:textSize="15sp"
                                android:textStyle="bold"
                                android:textColor="#212121"
                                android:layout_marginTop="4dp" />
                        </LinearLayout>

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Daily Wages"
                                android:textSize="12sp"
                                android:textColor="#757575" />

                            <TextView
                                android:id="@+id/tvDailyWages"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Rs. 500 /day"
                                android:textSize="15sp"
                                android:textStyle="bold"
                                android:textColor="#4CAF50"
                                android:layout_marginTop="4dp" />
                        </LinearLayout>
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Assigned Info Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Task Assignment"
                        android:textSize="16sp"
                        android:textStyle="bold"
                        android:textColor="#212121"
                        android:layout_marginBottom="12dp" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical">

                        <de.hdodenhof.circleimageview.CircleImageView
                            android:id="@+id/ivAssignedBy"
                            android:layout_width="48dp"
                            android:layout_height="48dp"
                            android:src="@drawable/ic_default_profile" />

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical"
                            android:layout_marginStart="12dp">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Assigned By"
                                android:textSize="11sp"
                                android:textColor="#757575" />

                            <TextView
                                android:id="@+id/tvAssignedBy"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Alesha (Super Admin)"
                                android:textSize="14sp"
                                android:textStyle="bold"
                                android:textColor="#212121"
                                android:layout_marginTop="2dp" />
                        </LinearLayout>

                        <ImageView
                            android:layout_width="32dp"
                            android:layout_height="32dp"
                            android:src="@drawable/ic_forward"
                            android:padding="4dp"
                            app:tint="#757575" />

                        <de.hdodenhof.circleimageview.CircleImageView
                            android:id="@+id/ivAssignedTo"
                            android:layout_width="48dp"
                            android:layout_height="48dp"
                            android:src="@drawable/ic_default_profile"
                            android:layout_marginStart="8dp" />

                        <LinearLayout
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:orientation="vertical"
                            android:layout_marginStart="12dp">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Assigned To"
                                android:textSize="11sp"
                                android:textColor="#757575" />

                            <TextView
                                android:id="@+id/tvAssignedTo"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Shannon"
                                android:textSize="14sp"
                                android:textStyle="bold"
                                android:textColor="#212121"
                                android:layout_marginTop="2dp" />
                        </LinearLayout>
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Action Buttons -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:weightSum="2">

                <Button
                    android:id="@+id/btnUpdateProgress"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:text="Update Progress"
                    android:textAllCaps="false"
                    android:backgroundTint="#EEEEEE"
                    android:textColor="#212121"
                    android:layout_marginEnd="8dp"
                    style="@style/Widget.MaterialComponents.Button.OutlinedButton" />

                <Button
                    android:id="@+id/btnMarkComplete"
                    android:layout_width="0dp"
                    android:layout_height="56dp"
                    android:layout_weight="1"
                    android:text="Mark Complete"
                    android:textAllCaps="false"
                    android:backgroundTint="#4CAF50"
                    android:layout_marginStart="8dp" />
            </LinearLayout>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

</androidx.coordinatorlayout.widget.CoordinatorLayout>```

#### activity_task_list.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8F9FA">

    <!-- Top App Bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@android:color/white"
        android:elevation="4dp"
        app:navigationIcon="@drawable/ic_back"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:id="@+id/tvProjectName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Monte Carlo Casino"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="#212121" />

        <ImageView
            android:id="@+id/btnNotifications"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="end"
            android:layout_marginEnd="48dp"
            android:padding="4dp"
            android:src="@drawable/ic_notifications"
            android:contentDescription="Notifications" />

        <ImageView
            android:id="@+id/btnMore"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="end"
            android:layout_marginEnd="16dp"
            android:padding="4dp"
            android:src="@drawable/ic_more"
            android:contentDescription="More" />
    </com.google.android.material.appbar.MaterialToolbar>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:fillViewport="true"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toBottomOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Tab Layout -->
            <com.google.android.material.tabs.TabLayout
                android:id="@+id/tabLayout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="@android:color/white"
                app:tabMode="scrollable"
                app:tabSelectedTextColor="#7C4DFF"
                app:tabTextColor="#757575"
                app:tabIndicatorColor="#7C4DFF"
                android:layout_marginBottom="16dp">

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Site" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Task" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Attendance" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Material" />
            </com.google.android.material.tabs.TabLayout>

            <!-- Task Status Cards -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:weightSum="3"
                android:layout_marginBottom="16dp">

                <!-- Not Started -->
                <androidx.cardview.widget.CardView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginEnd="8dp"
                    app:cardCornerRadius="12dp"
                    app:cardElevation="2dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="12dp"
                        android:gravity="center">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Not Started"
                            android:textSize="12sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvNotStartedCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="12"
                            android:textSize="28sp"
                            android:textStyle="bold"
                            android:textColor="#E53935"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>

                <!-- Ongoing -->
                <androidx.cardview.widget.CardView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="4dp"
                    android:layout_marginEnd="4dp"
                    app:cardCornerRadius="12dp"
                    app:cardElevation="2dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="12dp"
                        android:gravity="center">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Ongoing"
                            android:textSize="12sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvOngoingCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="09"
                            android:textSize="28sp"
                            android:textStyle="bold"
                            android:textColor="#FFA726"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>

                <!-- Completed -->
                <androidx.cardview.widget.CardView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="8dp"
                    app:cardCornerRadius="12dp"
                    app:cardElevation="2dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="12dp"
                        android:gravity="center">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Completed"
                            android:textSize="12sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvCompletedCount"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="15"
                            android:textSize="28sp"
                            android:textStyle="bold"
                            android:textColor="#4CAF50"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </androidx.cardview.widget.CardView>
            </LinearLayout>

            <!-- Date Navigation -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:padding="16dp">

                    <ImageView
                        android:id="@+id/btnPreviousDate"
                        android:layout_width="32dp"
                        android:layout_height="32dp"
                        android:src="@drawable/ic_back"
                        android:padding="4dp"
                        android:background="?attr/selectableItemBackgroundBorderless" />

                    <TextView
                        android:id="@+id/tvSelectedDate"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="01 Oct 2024, Mon"
                        android:textSize="16sp"
                        android:textStyle="bold"
                        android:textColor="#7C4DFF"
                        android:gravity="center" />

                    <ImageView
                        android:id="@+id/btnNextDate"
                        android:layout_width="32dp"
                        android:layout_height="32dp"
                        android:src="@drawable/ic_forward"
                        android:padding="4dp"
                        android:background="?attr/selectableItemBackgroundBorderless" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Worker Info Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:weightSum="3"
                    android:padding="16dp">

                    <!-- Present Worker -->
                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical"
                        android:gravity="center">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Present Worker"
                            android:textSize="11sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvPresentWorkers"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="0"
                            android:textSize="24sp"
                            android:textStyle="bold"
                            android:textColor="#212121"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>

                    <!-- Absent Worker -->
                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical"
                        android:gravity="center">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Absent Worker"
                            android:textSize="11sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvAbsentWorkers"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="0"
                            android:textSize="24sp"
                            android:textStyle="bold"
                            android:textColor="#212121"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>

                    <!-- Total Salary -->
                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical"
                        android:gravity="center">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Total Salary"
                            android:textSize="11sp"
                            android:textColor="#757575" />

                        <TextView
                            android:id="@+id/tvTotalSalary"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="₹0"
                            android:textSize="24sp"
                            android:textStyle="bold"
                            android:textColor="#212121"
                            android:layout_marginTop="4dp" />
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- Tasks Section Header -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Tasks"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="#212121" />

                <TextView
                    android:id="@+id/btnFilters"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Filters"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    android:textColor="#7C4DFF"
                    android:drawableEnd="@drawable/ic_filter"
                    android:drawablePadding="4dp" />
            </LinearLayout>

            <!-- Tasks RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvTasks"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                tools:listitem="@layout/item_task_card" />

        </LinearLayout>
    </ScrollView>

    <!-- Add New Task FAB -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAddTask"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginEnd="16dp"
        android:layout_marginBottom="16dp"
        android:src="@drawable/ic_add_job"
        android:contentDescription="Add New Task"
        app:backgroundTint="#7C4DFF"
        app:tint="@android:color/white"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>```


### Item Layouts

#### item_ai_chat_message.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="8dp">

    <!-- AI Message (Left) -->
    <LinearLayout
        android:id="@+id/aiMessageLayout"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="start"
        android:orientation="horizontal"
        android:maxWidth="300dp"
        android:visibility="gone">

        <!-- AI Avatar -->
        <androidx.cardview.widget.CardView
            android:layout_width="36dp"
            android:layout_height="36dp"
            app:cardCornerRadius="18dp"
            app:cardElevation="0dp"
            app:cardBackgroundColor="#7C4DFF"
            android:layout_marginEnd="8dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:text="AI"
                android:textSize="12sp"
                android:textStyle="bold"
                android:textColor="@android:color/white"
                android:gravity="center" />
        </androidx.cardview.widget.CardView>

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <androidx.cardview.widget.CardView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                app:cardCornerRadius="16dp"
                app:cardElevation="1dp"
                app:cardBackgroundColor="#F5F5F5">

                <TextView
                    android:id="@+id/tvAiMessage"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="AI message here"
                    android:textSize="15sp"
                    android:textColor="#212121"
                    android:padding="12dp"
                    android:lineSpacingExtra="4dp" />
            </androidx.cardview.widget.CardView>

            <TextView
                android:id="@+id/tvAiTime"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="10:30 AM"
                android:textSize="11sp"
                android:textColor="#9E9E9E"
                android:layout_marginStart="12dp"
                android:layout_marginTop="4dp" />
        </LinearLayout>
    </LinearLayout>

    <!-- User Message (Right) -->
    <LinearLayout
        android:id="@+id/userMessageLayout"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="end"
        android:orientation="vertical"
        android:maxWidth="300dp"
        android:visibility="gone">

        <androidx.cardview.widget.CardView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end"
            app:cardCornerRadius="16dp"
            app:cardElevation="1dp"
            app:cardBackgroundColor="#7C4DFF">

            <TextView
                android:id="@+id/tvUserMessage"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="User message here"
                android:textSize="15sp"
                android:textColor="@android:color/white"
                android:padding="12dp"
                android:lineSpacingExtra="4dp" />
        </androidx.cardview.widget.CardView>

        <TextView
            android:id="@+id/tvUserTime"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="10:32 AM"
            android:textSize="11sp"
            android:textColor="#9E9E9E"
            android:layout_gravity="end"
            android:layout_marginEnd="12dp"
            android:layout_marginTop="4dp" />
    </LinearLayout>

</FrameLayout>
```

#### item_available_job_card.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp">

        <!-- Job Title -->
        <TextView
            android:id="@+id/tvJobTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="House Renovation Required"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="#212121"
            android:maxLines="2"
            android:ellipsize="end"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/tvBudget" />

        <!-- Budget -->
        <TextView
            android:id="@+id/tvBudget"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Rs. 50K"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="#7C4DFF"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Client Info -->
        <LinearLayout
            android:id="@+id/clientLayout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/tvJobTitle"
            app:layout_constraintStart_toStartOf="parent">

            <de.hdodenhof.circleimageview.CircleImageView
                android:id="@+id/ivClientImage"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:src="@drawable/ic_default_profile"
                app:civ_border_width="1dp"
                app:civ_border_color="#E0E0E0" />

            <TextView
                android:id="@+id/tvClientName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Ahmed Khan"
                android:textSize="13sp"
                android:textColor="#212121"
                android:layout_marginStart="8dp" />

            <ImageView
                android:id="@+id/ivVerified"
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:src="@drawable/ic_verified"
                android:layout_marginStart="4dp"
                android:visibility="visible" />
        </LinearLayout>

        <!-- Category Tag -->
        <TextView
            android:id="@+id/tvCategory"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Electrician"
            android:textSize="12sp"
            android:textColor="#7C4DFF"
            android:background="@drawable/bg_pill_unselected"
            android:paddingStart="12dp"
            android:paddingEnd="12dp"
            android:paddingTop="4dp"
            android:paddingBottom="4dp"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/clientLayout"
            app:layout_constraintStart_toStartOf="parent" />

        <!-- Description -->
        <TextView
            android:id="@+id/tvDescription"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Need experienced electrician for complete house wiring. Must have 5+ years experience."
            android:textSize="14sp"
            android:textColor="#757575"
            android:maxLines="2"
            android:ellipsize="end"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/tvCategory" />

        <!-- Divider -->
        <View
            android:id="@+id/divider"
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="#E0E0E0"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/tvDescription" />

        <!-- Bottom Info Row -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/divider">

            <!-- Location -->
            <ImageView
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:src="@drawable/ic_location"
                app:tint="#757575" />

            <TextView
                android:id="@+id/tvLocation"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="DHA Phase 5, Lahore"
                android:textSize="13sp"
                android:textColor="#757575"
                android:layout_marginStart="4dp"
                android:maxLines="1"
                android:ellipsize="end" />

            <!-- Timeline -->
            <ImageView
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:src="@drawable/ic_time"
                app:tint="#FFA726"
                android:layout_marginStart="16dp" />

            <TextView
                android:id="@+id/tvTimeline"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="7 days"
                android:textSize="13sp"
                android:textColor="#757575"
                android:layout_marginStart="4dp" />

            <!-- Posted Time -->
            <TextView
                android:id="@+id/tvPostedTime"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="2h ago"
                android:textSize="12sp"
                android:textColor="#9E9E9E"
                android:layout_marginStart="16dp" />
        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>```

#### item_bid_card.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/cardView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp">

        <!-- Contractor Photo -->
        <de.hdodenhof.circleimageview.CircleImageView
            android:id="@+id/ivContractorPhoto"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:src="@drawable/ic_person_placeholder"
            app:civ_border_width="2dp"
            app:civ_border_color="#E0E0E0"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent" />

        <!-- Contractor Name -->
        <TextView
            android:id="@+id/tvContractorName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Ali Hassan"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="#212121"
            android:maxLines="1"
            android:ellipsize="end"
            android:layout_marginStart="12dp"
            app:layout_constraintTop_toTopOf="@id/ivContractorPhoto"
            app:layout_constraintStart_toEndOf="@id/ivContractorPhoto"
            app:layout_constraintEnd_toStartOf="@id/tvBidStatus" />

        <!-- Contractor Category -->
        <TextView
            android:id="@+id/tvContractorCategory"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Electrician"
            android:textSize="13sp"
            android:textColor="#7C4DFF"
            app:layout_constraintTop_toBottomOf="@id/tvContractorName"
            app:layout_constraintStart_toStartOf="@id/tvContractorName" />

        <!-- Rating & Completed Projects -->
        <LinearLayout
            android:id="@+id/layoutInfo"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="2dp"
            app:layout_constraintTop_toBottomOf="@id/tvContractorCategory"
            app:layout_constraintStart_toStartOf="@id/tvContractorName"
            app:layout_constraintEnd_toEndOf="parent">

            <TextView
                android:id="@+id/tvRating"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="⭐ 4.5"
                android:textSize="12sp"
                android:textColor="#FFA726"
                android:layout_marginEnd="12dp" />

            <TextView
                android:id="@+id/tvCompletedProjects"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="15 projects completed"
                android:textSize="12sp"
                android:textColor="#757575" />
        </LinearLayout>

        <!-- Bid Status Badge -->
        <TextView
            android:id="@+id/tvBidStatus"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="ACCEPTED"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textColor="#4CAF50"
            android:visibility="gone"
            app:layout_constraintTop_toTopOf="@id/tvContractorName"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Divider -->
        <View
            android:id="@+id/divider"
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="#E0E0E0"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/layoutInfo" />

        <!-- Bid Amount & Completion Days -->
        <LinearLayout
            android:id="@+id/layoutBidInfo"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:weightSum="2"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/divider">

            <!-- Bid Amount -->
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Bid Amount"
                    android:textSize="12sp"
                    android:textColor="#757575" />

                <TextView
                    android:id="@+id/tvBidAmount"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Rs. 45,000"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="#7C4DFF"
                    android:layout_marginTop="4dp" />
            </LinearLayout>

            <!-- Completion Days -->
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical"
                android:gravity="end">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Completion Time"
                    android:textSize="12sp"
                    android:textColor="#757575" />

                <TextView
                    android:id="@+id/tvCompletionDays"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="7 days"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="#FFA726"
                    android:layout_marginTop="4dp" />
            </LinearLayout>
        </LinearLayout>

        <!-- Proposal -->
        <TextView
            android:id="@+id/tvProposal"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="I have 10 years of experience in electrical work. I can complete this project with high quality..."
            android:textSize="14sp"
            android:textColor="#424242"
            android:maxLines="3"
            android:ellipsize="end"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/layoutBidInfo" />

        <!-- Submitted Date -->
        <TextView
            android:id="@+id/tvSubmittedDate"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Submitted 2 hours ago"
            android:textSize="12sp"
            android:textColor="#9E9E9E"
            android:layout_marginTop="8dp"
            app:layout_constraintTop_toBottomOf="@id/tvProposal"
            app:layout_constraintStart_toStartOf="parent" />

        <!-- Action Buttons -->
        <LinearLayout
            android:id="@+id/layoutButtons"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:weightSum="4"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/tvSubmittedDate">

            <!-- Accept Button -->
            <Button
                android:id="@+id/btnAccept"
                android:layout_width="0dp"
                android:layout_height="42dp"
                android:layout_weight="1"
                android:text="Accept"
                android:textAllCaps="false"
                android:textSize="13sp"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_button_green"
                android:layout_marginEnd="4dp" />

            <!-- Reject Button -->
            <Button
                android:id="@+id/btnReject"
                android:layout_width="0dp"
                android:layout_height="42dp"
                android:layout_weight="1"
                android:text="Reject"
                android:textAllCaps="false"
                android:textSize="13sp"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_button_red"
                android:layout_marginStart="4dp"
                android:layout_marginEnd="4dp" />

            <!-- View Profile Button -->
            <Button
                android:id="@+id/btnViewProfile"
                android:layout_width="0dp"
                android:layout_height="42dp"
                android:layout_weight="1"
                android:text="Profile"
                android:textAllCaps="false"
                android:textSize="13sp"
                android:textColor="#7C4DFF"
                android:background="@drawable/bg_button_outline"
                android:layout_marginStart="4dp"
                android:layout_marginEnd="4dp" />

            <!-- Contact Button -->
            <Button
                android:id="@+id/btnContact"
                android:layout_width="0dp"
                android:layout_height="42dp"
                android:layout_weight="1"
                android:text="Chat"
                android:textAllCaps="false"
                android:textSize="13sp"
                android:textColor="#7C4DFF"
                android:background="@drawable/bg_button_outline"
                android:layout_marginStart="4dp" />
        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>```

#### item_chat_message.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="4dp">

    <androidx.cardview.widget.CardView
        android:id="@+id/messageCard"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="start"
        app:cardCornerRadius="16dp"
        app:cardElevation="2dp"
        app:cardUseCompatPadding="true">

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="12dp"
            android:maxWidth="280dp">

            <TextView
                android:id="@+id/tvMessage"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="This is a message"
                android:textSize="15sp"
                android:lineSpacingExtra="2dp"
                android:textColor="#212121" />

            <TextView
                android:id="@+id/tvTime"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="12:30 PM"
                android:textSize="11sp"
                android:textColor="#9E9E9E"
                android:layout_marginTop="4dp"
                android:layout_gravity="end" />
        </LinearLayout>
    </androidx.cardview.widget.CardView>

</LinearLayout>```

#### item_contractor.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/cardView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp">

        <!-- Profile Image -->
        <de.hdodenhof.circleimageview.CircleImageView
            android:id="@+id/ivContractorImage"
            android:layout_width="64dp"
            android:layout_height="64dp"
            android:src="@drawable/ic_default_profile"
            app:civ_border_width="2dp"
            app:civ_border_color="#E0E0E0" />

        <!-- Details -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:layout_marginStart="16dp">

            <!-- Name -->
            <TextView
                android:id="@+id/tvContractorName"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Contractor Name"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:maxLines="1"
                android:ellipsize="end" />

            <!-- Category -->
            <TextView
                android:id="@+id/tvCategory"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Electrician"
                android:textSize="13sp"
                android:textColor="#7C4DFF"
                android:layout_marginTop="2dp" />

            <!-- Rating -->
            <TextView
                android:id="@+id/tvRating"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="4.5 (10 reviews)"
                android:textSize="12sp"
                android:textColor="#757575"
                android:layout_marginTop="4dp"
                android:drawableStart="@drawable/ic_star"
                android:drawablePadding="4dp" />

            <!-- Experience -->
            <TextView
                android:id="@+id/tvExperience"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="5 years experience"
                android:textSize="12sp"
                android:textColor="#757575"
                android:layout_marginTop="2dp" />

            <!-- Completed Projects -->
            <TextView
                android:id="@+id/tvCompletedProjects"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="15 projects completed"
                android:textSize="12sp"
                android:textColor="#757575"
                android:layout_marginTop="2dp" />

            <!-- Location and Rate Row -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="8dp"
                android:gravity="center_vertical">

                <!-- Location -->
                <TextView
                    android:id="@+id/tvLocation"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Lahore"
                    android:textSize="12sp"
                    android:textColor="#757575"
                    android:drawableStart="@drawable/ic_location"
                    android:drawablePadding="4dp"
                    android:maxLines="1"
                    android:ellipsize="end" />

                <!-- Hourly Rate -->
                <TextView
                    android:id="@+id/tvHourlyRate"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Rs. 500/hr"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    android:textColor="#7C4DFF" />

            </LinearLayout>

        </LinearLayout>

    </LinearLayout>

</androidx.cardview.widget.CardView>
```

#### item_contractor_card.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp">

        <!-- Profile Image -->
        <de.hdodenhof.circleimageview.CircleImageView
            android:id="@+id/ivProfileImage"
            android:layout_width="60dp"
            android:layout_height="60dp"
            android:src="@drawable/ic_default_profile"
            app:civ_border_width="2dp"
            app:civ_border_color="#E0E0E0"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent" />

        <!-- Verified Badge -->
        <ImageView
            android:id="@+id/ivVerified"
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:src="@drawable/ic_verified"
            android:visibility="visible"
            app:layout_constraintBottom_toBottomOf="@id/ivProfileImage"
            app:layout_constraintEnd_toEndOf="@id/ivProfileImage" />

        <!-- Contractor Name -->
        <TextView
            android:id="@+id/tvContractorName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Ahmad Ali"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="#212121"
            android:layout_marginStart="16dp"
            android:maxLines="1"
            android:ellipsize="end"
            app:layout_constraintTop_toTopOf="@id/ivProfileImage"
            app:layout_constraintStart_toEndOf="@id/ivProfileImage"
            app:layout_constraintEnd_toStartOf="@id/btnFavorite" />

        <!-- Category -->
        <TextView
            android:id="@+id/tvCategory"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Electrician"
            android:textSize="13sp"
            android:textColor="#7C4DFF"
            android:layout_marginStart="16dp"
            android:layout_marginTop="4dp"
            app:layout_constraintTop_toBottomOf="@id/tvContractorName"
            app:layout_constraintStart_toEndOf="@id/ivProfileImage"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Rating and Experience -->
        <LinearLayout
            android:id="@+id/ratingLayout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginStart="16dp"
            android:layout_marginTop="4dp"
            app:layout_constraintTop_toBottomOf="@id/tvCategory"
            app:layout_constraintStart_toEndOf="@id/ivProfileImage">

            <ImageView
                android:layout_width="14dp"
                android:layout_height="14dp"
                android:src="@drawable/ic_star"
                app:tint="#FFA726" />

            <TextView
                android:id="@+id/tvRating"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="4.8"
                android:textSize="13sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginStart="4dp" />

            <TextView
                android:id="@+id/tvReviews"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="(24)"
                android:textSize="12sp"
                android:textColor="#757575"
                android:layout_marginStart="2dp" />

            <View
                android:layout_width="1dp"
                android:layout_height="12dp"
                android:background="#E0E0E0"
                android:layout_marginStart="12dp"
                android:layout_marginEnd="12dp" />

            <TextView
                android:id="@+id/tvExperience"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="5 years exp"
                android:textSize="12sp"
                android:textColor="#757575" />
        </LinearLayout>

        <!-- Favorite Button -->
        <ImageView
            android:id="@+id/btnFavorite"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:src="@drawable/ic_favorite_border"
            android:padding="6dp"
            app:tint="#757575"
            android:background="?attr/selectableItemBackgroundBorderless"
            app:layout_constraintTop_toTopOf="@id/ivProfileImage"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Divider -->
        <View
            android:id="@+id/divider"
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="#E0E0E0"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/ivProfileImage" />

        <!-- Location -->
        <LinearLayout
            android:id="@+id/locationLayout"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/divider"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/priceLayout">

            <ImageView
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:src="@drawable/ic_location"
                app:tint="#757575" />

            <TextView
                android:id="@+id/tvLocation"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="DHA Phase 5, Lahore"
                android:textSize="13sp"
                android:textColor="#757575"
                android:maxLines="1"
                android:ellipsize="end"
                android:layout_marginStart="6dp" />
        </LinearLayout>

        <!-- Hourly Rate -->
        <LinearLayout
            android:id="@+id/priceLayout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginStart="16dp"
            app:layout_constraintTop_toTopOf="@id/locationLayout"
            app:layout_constraintBottom_toBottomOf="@id/locationLayout"
            app:layout_constraintEnd_toEndOf="parent">

            <TextView
                android:id="@+id/tvHourlyRate"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Rs. 1,500"
                android:textSize="15sp"
                android:textStyle="bold"
                android:textColor="#7C4DFF" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="/day"
                android:textSize="12sp"
                android:textColor="#757575"
                android:layout_marginStart="2dp" />
        </LinearLayout>

        <!-- View Profile Button -->
        <Button
            android:id="@+id/btnViewProfile"
            android:layout_width="match_parent"
            android:layout_height="44dp"
            android:text="View Profile"
            android:textAllCaps="false"
            android:textSize="14sp"
            android:textStyle="bold"
            android:background="@drawable/bg_button_purple"
            android:layout_marginTop="16dp"
            app:layout_constraintTop_toBottomOf="@id/locationLayout"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>```

#### item_job_card.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/cardView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp">

        <!-- Job Title -->
        <TextView
            android:id="@+id/tvJobTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="House Construction Required"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="#212121"
            android:maxLines="2"
            android:ellipsize="end"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/tvStatus" />

        <!-- Status Badge -->
        <TextView
            android:id="@+id/tvStatus"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="OPEN"
            android:textSize="12sp"
            android:textStyle="bold"
            android:textColor="#4CAF50"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Category -->
        <TextView
            android:id="@+id/tvCategory"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Construction"
            android:textSize="13sp"
            android:textColor="#7C4DFF"
            android:background="@drawable/bg_category_chip"
            android:paddingStart="12dp"
            android:paddingEnd="12dp"
            android:paddingTop="4dp"
            android:paddingBottom="4dp"
            android:layout_marginTop="8dp"
            app:layout_constraintTop_toBottomOf="@id/tvJobTitle"
            app:layout_constraintStart_toStartOf="parent" />

        <!-- Budget -->
        <LinearLayout
            android:id="@+id/layoutBudget"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/tvCategory"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/layoutBids">

            <ImageView
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:src="@drawable/ic_money"
                app:tint="#757575" />

            <TextView
                android:id="@+id/tvBudget"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="PKR 500K"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginStart="6dp" />
        </LinearLayout>

        <!-- Bid Count -->
        <LinearLayout
            android:id="@+id/layoutBids"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/tvCategory"
            app:layout_constraintEnd_toEndOf="parent">

            <ImageView
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:src="@drawable/ic_bids"
                app:tint="#757575" />

            <TextView
                android:id="@+id/tvBidCount"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="5 bids"
                android:textSize="13sp"
                android:textColor="#757575"
                android:layout_marginStart="6dp" />
        </LinearLayout>

        <!-- Location -->
        <LinearLayout
            android:id="@+id/layoutLocation"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="8dp"
            app:layout_constraintTop_toBottomOf="@id/layoutBudget"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent">

            <ImageView
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:src="@drawable/ic_location"
                app:tint="#757575" />

            <TextView
                android:id="@+id/tvLocation"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Lahore, DHA Phase 5"
                android:textSize="13sp"
                android:textColor="#757575"
                android:maxLines="1"
                android:ellipsize="end"
                android:layout_marginStart="6dp" />
        </LinearLayout>

        <!-- Posted Date -->
        <TextView
            android:id="@+id/tvPostedDate"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="2 days ago"
            android:textSize="12sp"
            android:textColor="#9E9E9E"
            android:layout_marginTop="8dp"
            app:layout_constraintTop_toBottomOf="@id/layoutLocation"
            app:layout_constraintStart_toStartOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>```

#### item_material_card.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp">

        <!-- Material Icon/Image -->
        <ImageView
            android:id="@+id/ivMaterialIcon"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:src="@drawable/ic_gallery"
            android:background="#F5F5F5"
            android:padding="12dp"
            android:scaleType="centerCrop"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent" />

        <!-- Material Name -->
        <TextView
            android:id="@+id/tvMaterialName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Cement Bags"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="#212121"
            android:layout_marginStart="12dp"
            android:maxLines="1"
            android:ellipsize="end"
            app:layout_constraintTop_toTopOf="@id/ivMaterialIcon"
            app:layout_constraintStart_toEndOf="@id/ivMaterialIcon"
            app:layout_constraintEnd_toStartOf="@id/tvStockStatus" />

        <!-- Stock Status Badge -->
        <TextView
            android:id="@+id/tvStockStatus"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="In Stock"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textColor="#4CAF50"
            android:background="@drawable/bg_status_green"
            android:paddingStart="10dp"
            android:paddingEnd="10dp"
            android:paddingTop="4dp"
            android:paddingBottom="4dp"
            app:layout_constraintTop_toTopOf="@id/ivMaterialIcon"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Category/Unit -->
        <TextView
            android:id="@+id/tvCategory"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Construction Material • Bags"
            android:textSize="12sp"
            android:textColor="#757575"
            android:layout_marginStart="12dp"
            android:layout_marginTop="4dp"
            android:maxLines="1"
            android:ellipsize="end"
            app:layout_constraintTop_toBottomOf="@id/tvMaterialName"
            app:layout_constraintStart_toEndOf="@id/ivMaterialIcon"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Quantity & Price Row -->
        <LinearLayout
            android:id="@+id/quantityPriceLayout"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginStart="12dp"
            android:layout_marginTop="4dp"
            app:layout_constraintTop_toBottomOf="@id/tvCategory"
            app:layout_constraintStart_toEndOf="@id/ivMaterialIcon"
            app:layout_constraintEnd_toEndOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Qty: "
                android:textSize="13sp"
                android:textColor="#757575" />

            <TextView
                android:id="@+id/tvQuantity"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="50"
                android:textSize="13sp"
                android:textStyle="bold"
                android:textColor="#212121" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text=" bags"
                android:textSize="13sp"
                android:textColor="#757575" />

            <View
                android:layout_width="1dp"
                android:layout_height="12dp"
                android:background="#E0E0E0"
                android:layout_marginStart="12dp"
                android:layout_marginEnd="12dp"
                android:layout_gravity="center_vertical" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Price: "
                android:textSize="13sp"
                android:textColor="#757575" />

            <TextView
                android:id="@+id/tvPrice"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Rs. 450"
                android:textSize="13sp"
                android:textStyle="bold"
                android:textColor="#4CAF50" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="/bag"
                android:textSize="12sp"
                android:textColor="#757575" />
        </LinearLayout>

        <!-- Divider -->
        <View
            android:id="@+id/divider"
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="#E0E0E0"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/ivMaterialIcon" />

        <!-- Bottom Info Row -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/divider">

            <!-- Last Updated -->
            <TextView
                android:id="@+id/tvLastUpdated"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Updated: 2 days ago"
                android:textSize="11sp"
                android:textColor="#9E9E9E" />

            <!-- Supplier -->
            <TextView
                android:id="@+id/tvSupplier"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Supplier: ABC Hardware"
                android:textSize="11sp"
                android:textColor="#757575"
                android:drawableStart="@drawable/ic_profile"
                android:drawablePadding="4dp"
                android:drawableTint="#757575"
                android:maxLines="1"
                android:ellipsize="end" />
        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>```

#### item_message.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="8dp">

    <!-- Received Message (Left) -->
    <LinearLayout
        android:id="@+id/receivedMessageLayout"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="start"
        android:orientation="horizontal"
        android:visibility="gone">

        <de.hdodenhof.circleimageview.CircleImageView
            android:id="@+id/ivSenderImage"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:src="@drawable/ic_default_profile"
            android:layout_marginEnd="8dp"
            app:civ_border_width="1dp"
            app:civ_border_color="#E0E0E0" />

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:maxWidth="260dp">

            <androidx.cardview.widget.CardView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                app:cardCornerRadius="16dp"
                app:cardElevation="0dp"
                app:cardBackgroundColor="#F0F0F0">

                <TextView
                    android:id="@+id/tvReceivedMessage"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Hello! I'm interested in this job."
                    android:textSize="15sp"
                    android:textColor="#212121"
                    android:padding="12dp"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp" />
            </androidx.cardview.widget.CardView>

            <TextView
                android:id="@+id/tvReceivedTime"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="10:30 AM"
                android:textSize="11sp"
                android:textColor="#9E9E9E"
                android:layout_marginStart="12dp"
                android:layout_marginTop="4dp" />
        </LinearLayout>
    </LinearLayout>

    <!-- Sent Message (Right) -->
    <LinearLayout
        android:id="@+id/sentMessageLayout"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="end"
        android:orientation="vertical"
        android:maxWidth="260dp"
        android:visibility="gone">

        <androidx.cardview.widget.CardView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end"
            app:cardCornerRadius="16dp"
            app:cardElevation="0dp"
            app:cardBackgroundColor="#7C4DFF">

            <TextView
                android:id="@+id/tvSentMessage"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Great! When can you start?"
                android:textSize="15sp"
                android:textColor="@android:color/white"
                android:padding="12dp"
                android:paddingStart="16dp"
                android:paddingEnd="16dp" />
        </androidx.cardview.widget.CardView>

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_gravity="end"
            android:layout_marginEnd="12dp"
            android:layout_marginTop="4dp">

            <TextView
                android:id="@+id/tvSentTime"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="10:32 AM"
                android:textSize="11sp"
                android:textColor="#9E9E9E" />

            <ImageView
                android:id="@+id/ivMessageStatus"
                android:layout_width="14dp"
                android:layout_height="14dp"
                android:src="@drawable/ic_check"
                app:tint="#4CAF50"
                android:layout_marginStart="4dp"
                android:visibility="visible" />
        </LinearLayout>
    </LinearLayout>

    <!-- Image Message (Optional) -->
    <LinearLayout
        android:id="@+id/imageMessageLayout"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:visibility="gone">

        <androidx.cardview.widget.CardView
            android:layout_width="200dp"
            android:layout_height="wrap_content"
            app:cardCornerRadius="12dp"
            app:cardElevation="2dp">

            <ImageView
                android:id="@+id/ivMessageImage"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:scaleType="centerCrop" />
        </androidx.cardview.widget.CardView>

        <TextView
            android:id="@+id/tvImageCaption"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Caption text"
            android:textSize="14sp"
            android:textColor="#757575"
            android:layout_marginTop="4dp"
            android:visibility="gone" />
    </LinearLayout>

</FrameLayout>```

#### item_notification.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="8dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="0dp"
    app:cardBackgroundColor="#FFFFFF"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp">

        <!-- Notification Icon -->
        <ImageView
            android:id="@+id/ivNotificationIcon"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/ic_bid"
            android:background="#F5F5F5"
            android:padding="10dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent" />

        <!-- Unread Indicator -->
        <View
            android:id="@+id/unreadIndicator"
            android:layout_width="10dp"
            android:layout_height="10dp"
            android:background="@drawable/bg_pill_selected"
            android:visibility="visible"
            app:layout_constraintTop_toTopOf="@id/ivNotificationIcon"
            app:layout_constraintEnd_toEndOf="@id/ivNotificationIcon" />

        <!-- Notification Title -->
        <TextView
            android:id="@+id/tvNotificationTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="New Bid Received"
            android:textSize="15sp"
            android:textStyle="bold"
            android:textColor="#212121"
            android:layout_marginStart="12dp"
            android:maxLines="1"
            android:ellipsize="end"
            app:layout_constraintTop_toTopOf="@id/ivNotificationIcon"
            app:layout_constraintStart_toEndOf="@id/ivNotificationIcon"
            app:layout_constraintEnd_toStartOf="@id/tvTime" />

        <!-- Time -->
        <TextView
            android:id="@+id/tvTime"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="2m ago"
            android:textSize="12sp"
            android:textColor="#9E9E9E"
            app:layout_constraintTop_toTopOf="@id/ivNotificationIcon"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Notification Message -->
        <TextView
            android:id="@+id/tvNotificationMessage"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Ahmad Ali submitted a bid of Rs. 45,000 for your House Renovation job."
            android:textSize="14sp"
            android:textColor="#757575"
            android:maxLines="2"
            android:ellipsize="end"
            android:layout_marginStart="12dp"
            android:layout_marginTop="4dp"
            app:layout_constraintTop_toBottomOf="@id/tvNotificationTitle"
            app:layout_constraintStart_toEndOf="@id/ivNotificationIcon"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Action Button (Optional) -->
        <Button
            android:id="@+id/btnAction"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="View Bid"
            android:textAllCaps="false"
            android:textSize="13sp"
            android:backgroundTint="#7C4DFF"
            android:layout_marginStart="60dp"
            android:layout_marginTop="8dp"
            android:visibility="gone"
            app:layout_constraintTop_toBottomOf="@id/tvNotificationMessage"
            app:layout_constraintStart_toStartOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>```

#### item_portfolio.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    app:cardCornerRadius="8dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <ImageView
        android:id="@+id/ivPortfolioImage"
        android:layout_width="match_parent"
        android:layout_height="150dp"
        android:scaleType="centerCrop"
        android:src="@drawable/ic_gallery"
        android:background="#F5F5F5" />

</androidx.cardview.widget.CardView>
```

#### item_portfolio_image.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginEnd="12dp"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Portfolio Image -->
        <ImageView
            android:id="@+id/ivPortfolioImage"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:scaleType="centerCrop"
            android:src="@drawable/ic_gallery"
            android:background="#F5F5F5"
            app:layout_constraintTop_toTopOf="parent" />

        <!-- Overlay Gradient (Optional) -->
        <View
            android:layout_width="match_parent"
            android:layout_height="60dp"
            android:background="@drawable/gradient_overlay"
            app:layout_constraintBottom_toBottomOf="@id/ivPortfolioImage" />

        <!-- Project Title -->
        <TextView
            android:id="@+id/tvProjectTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="House Wiring Project"
            android:textSize="14sp"
            android:textStyle="bold"
            android:textColor="@android:color/white"
            android:padding="12dp"
            android:maxLines="2"
            android:ellipsize="end"
            android:shadowColor="#80000000"
            android:shadowDx="0"
            android:shadowDy="1"
            android:shadowRadius="4"
            app:layout_constraintBottom_toBottomOf="@id/ivPortfolioImage"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>```

#### item_review.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="0dp"
    app:cardBackgroundColor="#F5F5F5">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp">

        <!-- Reviewer Profile Image -->
        <de.hdodenhof.circleimageview.CircleImageView
            android:id="@+id/ivReviewerImage"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@drawable/ic_default_profile"
            app:civ_border_width="1dp"
            app:civ_border_color="#E0E0E0"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent" />

        <!-- Reviewer Name -->
        <TextView
            android:id="@+id/tvReviewerName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Ahmed Khan"
            android:textSize="14sp"
            android:textStyle="bold"
            android:textColor="#212121"
            android:layout_marginStart="12dp"
            app:layout_constraintTop_toTopOf="@id/ivReviewerImage"
            app:layout_constraintStart_toEndOf="@id/ivReviewerImage"
            app:layout_constraintEnd_toStartOf="@id/tvReviewDate" />

        <!-- Review Date -->
        <TextView
            android:id="@+id/tvReviewDate"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="2 days ago"
            android:textSize="12sp"
            android:textColor="#9E9E9E"
            app:layout_constraintTop_toTopOf="@id/ivReviewerImage"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Rating Stars -->
        <LinearLayout
            android:id="@+id/ratingLayout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginStart="12dp"
            android:layout_marginTop="4dp"
            app:layout_constraintTop_toBottomOf="@id/tvReviewerName"
            app:layout_constraintStart_toEndOf="@id/ivReviewerImage">

            <ImageView
                android:layout_width="14dp"
                android:layout_height="14dp"
                android:src="@drawable/ic_star"
                app:tint="#FFA726" />

            <ImageView
                android:layout_width="14dp"
                android:layout_height="14dp"
                android:src="@drawable/ic_star"
                app:tint="#FFA726"
                android:layout_marginStart="2dp" />

            <ImageView
                android:layout_width="14dp"
                android:layout_height="14dp"
                android:src="@drawable/ic_star"
                app:tint="#FFA726"
                android:layout_marginStart="2dp" />

            <ImageView
                android:layout_width="14dp"
                android:layout_height="14dp"
                android:src="@drawable/ic_star"
                app:tint="#FFA726"
                android:layout_marginStart="2dp" />

            <ImageView
                android:layout_width="14dp"
                android:layout_height="14dp"
                android:src="@drawable/ic_star"
                app:tint="#FFA726"
                android:layout_marginStart="2dp" />

            <TextView
                android:id="@+id/tvRating"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="5.0"
                android:textSize="13sp"
                android:textStyle="bold"
                android:textColor="#212121"
                android:layout_marginStart="6dp" />
        </LinearLayout>

        <!-- Review Comment -->
        <TextView
            android:id="@+id/tvReviewComment"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Excellent work! Very professional and completed the job on time. Highly recommended for electrical work."
            android:textSize="14sp"
            android:textColor="#424242"
            android:lineSpacingExtra="2dp"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/ivReviewerImage" />

        <!-- Job Title (Optional) -->
        <TextView
            android:id="@+id/tvJobTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="House Wiring Project"
            android:textSize="12sp"
            android:textColor="#7C4DFF"
            android:textStyle="italic"
            android:layout_marginTop="8dp"
            android:visibility="visible"
            app:layout_constraintTop_toBottomOf="@id/tvReviewComment"
            app:layout_constraintStart_toStartOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>```

#### item_task_card.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp">

        <!-- Task Title & Badge -->
        <TextView
            android:id="@+id/tvTaskTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Electrician"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="#212121"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/tvTaskNumber" />

        <TextView
            android:id="@+id/tvTaskNumber"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="3"
            android:textSize="14sp"
            android:textStyle="bold"
            android:textColor="#7C4DFF"
            android:background="@drawable/bg_pill_unselected"
            android:paddingStart="10dp"
            android:paddingEnd="10dp"
            android:paddingTop="2dp"
            android:paddingBottom="2dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Last Updated -->
        <TextView
            android:id="@+id/tvLastUpdated"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Last update: 17 Oct '24"
            android:textSize="12sp"
            android:textColor="#9E9E9E"
            android:layout_marginTop="4dp"
            app:layout_constraintTop_toBottomOf="@id/tvTaskTitle"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/tvUpdateStock" />

        <TextView
            android:id="@+id/tvUpdateStock"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Update Stock?"
            android:textSize="12sp"
            android:textStyle="bold"
            android:textColor="#7C4DFF"
            app:layout_constraintTop_toTopOf="@id/tvLastUpdated"
            app:layout_constraintBottom_toBottomOf="@id/tvLastUpdated"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Wages & Progress -->
        <TextView
            android:id="@+id/tvWages"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Wages: ₹500 /day"
            android:textSize="13sp"
            android:textColor="#757575"
            android:layout_marginTop="8dp"
            app:layout_constraintTop_toBottomOf="@id/tvLastUpdated"
            app:layout_constraintStart_toStartOf="parent" />

        <TextView
            android:id="@+id/tvProgress"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="20/100%"
            android:textSize="13sp"
            android:textStyle="bold"
            android:textColor="#4CAF50"
            app:layout_constraintTop_toTopOf="@id/tvWages"
            app:layout_constraintBottom_toBottomOf="@id/tvWages"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Date Range -->
        <TextView
            android:id="@+id/tvDateRange"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="17 Oct - 31 Oct"
            android:textSize="12sp"
            android:textColor="#9E9E9E"
            android:layout_marginTop="4dp"
            app:layout_constraintTop_toBottomOf="@id/tvWages"
            app:layout_constraintStart_toStartOf="parent" />

        <TextView
            android:id="@+id/tvStatus"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Ongoing"
            android:textSize="12sp"
            android:textStyle="bold"
            android:textColor="#FFA726"
            app:layout_constraintTop_toTopOf="@id/tvDateRange"
            app:layout_constraintBottom_toBottomOf="@id/tvDateRange"
            app:layout_constraintEnd_toEndOf="parent" />

        <!-- Divider -->
        <View
            android:id="@+id/divider"
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="#E0E0E0"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/tvDateRange" />

        <!-- Assigned Info -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="12dp"
            app:layout_constraintTop_toBottomOf="@id/divider">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="ASSIGNED BY:"
                android:textSize="10sp"
                android:textStyle="bold"
                android:textColor="#757575"
                android:layout_marginEnd="8dp" />

            <de.hdodenhof.circleimageview.CircleImageView
                android:id="@+id/ivAssignedBy"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:src="@drawable/ic_default_profile"
                android:layout_marginEnd="4dp" />

            <TextView
                android:id="@+id/tvAssignedBy"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Alesha\nSuper Admin"
                android:textSize="11sp"
                android:textColor="#212121"
                android:layout_gravity="center_vertical"
                android:layout_marginEnd="16dp" />

            <ImageView
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:src="@drawable/ic_forward"
                android:layout_gravity="center_vertical"
                android:layout_marginEnd="8dp"
                app:tint="#757575" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="ASSIGNED TO:"
                android:textSize="10sp"
                android:textStyle="bold"
                android:textColor="#757575"
                android:layout_marginEnd="8dp"
                android:layout_gravity="center_vertical" />

            <de.hdodenhof.circleimageview.CircleImageView
                android:id="@+id/ivAssignedTo"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:src="@drawable/ic_default_profile"
                android:layout_marginEnd="4dp" />

            <TextView
                android:id="@+id/tvAssignedTo"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Shannon\nElectrician"
                android:textSize="11sp"
                android:textColor="#212121"
                android:layout_gravity="center_vertical" />
        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>```


---

## Android Manifest

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MaterialComponents.Light.NoActionBar"
        tools:targetApi="31">

        <!-- Splash Activity (LAUNCHER) -->
        <activity
            android:name=".SplashActivity"
            android:exported="true"
            android:theme="@style/Theme.MaterialComponents.Light.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Authentication Activities -->
        <activity
            android:name=".MainActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize" />

        <activity
            android:name=".SignupActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize" />

        <!-- Dashboard Activities -->
        <activity
            android:name=".ClientDashboardActivity"
            android:exported="false" />

        <activity
            android:name=".ContractorDashboardActivity"
            android:exported="false" />

        <!-- Job Management Activities -->
        <activity
            android:name=".JobPostActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize"
            android:parentActivityName=".ClientDashboardActivity" />

        <activity
            android:name=".JobDetailActivity"
            android:exported="false"
            android:parentActivityName=".ClientDashboardActivity" />

        <!-- NEW: Client Jobs List Activity -->
        <activity
            android:name=".MyJobsActivity"
            android:exported="false"
            android:parentActivityName=".ClientDashboardActivity" />

        <activity
            android:name=".SubmitBidActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize"
            android:parentActivityName=".JobDetailActivity" />

        <!-- Contractor Activities -->
        <activity
            android:name=".ContractorDirectoryActivity"
            android:exported="false"
            android:parentActivityName=".ClientDashboardActivity" />

        <activity
            android:name=".ContractorProfileActivity"
            android:exported="false"
            android:parentActivityName=".ContractorDirectoryActivity" />

        <activity
            android:name=".PortfolioGalleryActivity"
            android:exported="false"
            android:parentActivityName=".ContractorProfileActivity" />

        <activity
            android:name=".AllReviewsActivity"
            android:exported="false"
            android:parentActivityName=".ContractorProfileActivity" />

        <!-- NEW: Contractor Available Jobs Activity -->
        <activity
            android:name=".AvailableJobsActivity"
            android:exported="false"
            android:parentActivityName=".ContractorDashboardActivity" />

        <!-- NEW: Contractor Projects Activity -->
        <activity
            android:name=".MyProjectsActivity"
            android:exported="false"
            android:parentActivityName=".ContractorDashboardActivity" />

        <!-- Task Management Activities -->
        <activity
            android:name=".TaskListActivity"
            android:exported="false"
            android:parentActivityName=".ContractorDashboardActivity" />

        <activity
            android:name=".AddTaskActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize"
            android:parentActivityName=".TaskListActivity" />

        <activity
            android:name=".TaskDetailActivity"
            android:exported="false"
            android:parentActivityName=".TaskListActivity" />

        <!-- Material Management Activities -->
        <activity
            android:name=".MaterialManagementActivity"
            android:exported="false"
            android:parentActivityName=".ContractorDashboardActivity" />

        <activity
            android:name=".AddMaterialActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize"
            android:parentActivityName=".MaterialManagementActivity" />

        <!-- Communication Activities -->
        <activity
            android:name=".ChatActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize"
            android:parentActivityName=".ClientDashboardActivity" />

        <!-- ⭐ AI Chat Activity (NEW) ⭐ -->
        <activity
            android:name=".AIChatActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize"
            android:parentActivityName=".ClientDashboardActivity" />

        <activity
            android:name=".NotificationsActivity"
            android:exported="false"
            android:parentActivityName=".ClientDashboardActivity" />

        <!-- User Management Activities -->
        <activity
            android:name=".EditProfileActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize" />

        <activity
            android:name=".SettingsActivity"
            android:exported="false" />

        <!-- Google Maps API Key (Optional - Add your key) -->
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="YOUR_API_KEY_HERE" />

        <!-- Firebase Cloud Messaging Service -->
        <service
            android:name=".services.MyFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <!-- Firebase Notification Channel (for Android O+) -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_channel_id"
            android:value="madproject_notifications" />

    </application>

</manifest>```

---

## Project Summary

### Features Implemented:
- ✅ Firebase Authentication (Login/Register)
- ✅ Firebase Firestore Database
- ✅ Firebase Cloud Messaging (Push Notifications)
- ✅ Job Posting & Bidding System
- ✅ Real-time Chat Messaging
- ✅ Contractor Directory & Profiles
- ✅ Portfolio Gallery
- ✅ Review & Rating System
- ✅ AI Chat Assistant (Gemini)
- ✅ Notification System
- ✅ Job Status Management (Open → In Progress → Completed)

### Key Components:
- **Activities:** 30+ screens
- **Firebase Managers:** 7 singleton managers
- **Adapters:** 10 RecyclerView adapters
- **Models:** 13 data models
- **Helpers:** FCM, Gemini AI integration

### Technologies Used:
- Android SDK (Java)
- Firebase (Auth, Firestore, FCM)
- Google Gemini AI
- Material Design Components
- CircleImageView
- OkHttp & Gson

---

**End of Documentation**

