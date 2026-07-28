package com.example.madproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.SelectedPhotoAdapter;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.helpers.GeminiAIHelper;
import com.example.madproject.models.Job;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobPostActivity extends AppCompatActivity {

    private EditText etJobTitle, etJobDescription, etBudget, etTimeline, etAddress;
    private Spinner spinnerCategory, spinnerCity;
    private Button btnPostJob, btnCancel, btnTakePhoto, btnAddPhoto;
    private android.widget.TextView btnAiGenerateDesc, btnAiContractorTip, btnAiPermitCheck;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private RecyclerView rvSelectedPhotos;
    private GeminiAIHelper aiHelper;

    private FirebaseAuth mAuth;
    private StorageReference storageRef;
    private String currentUserId;
    private String currentUserName = "";

    private final List<Uri> selectedPhotoUris = new ArrayList<>();
    private SelectedPhotoAdapter photoAdapter;
    private Uri cameraImageUri;

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    selectedPhotoUris.add(cameraImageUri);
                    photoAdapter.notifyItemInserted(selectedPhotoUris.size() - 1);
                    rvSelectedPhotos.setVisibility(View.VISIBLE);
                }
            });

    /**
     * The manifest declares android.permission.CAMERA, and once an app declares it the system
     * throws a SecurityException from ACTION_IMAGE_CAPTURE unless it has actually been granted -
     * so it has to be requested at runtime before the camera can be opened.
     */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission is needed to take a photo",
                            Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    selectedPhotoUris.addAll(uris);
                    photoAdapter.notifyDataSetChanged();
                    rvSelectedPhotos.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_post);

        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
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
            getSupportActionBar().setTitle("");
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
        rvSelectedPhotos = findViewById(R.id.rvSelectedPhotos);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        btnAiGenerateDesc   = findViewById(R.id.btnAiGenerateDesc);
        btnAiContractorTip  = findViewById(R.id.btnAiContractorTip);
        btnAiPermitCheck    = findViewById(R.id.btnAiPermitCheck);
        aiHelper = new GeminiAIHelper(this);

        photoAdapter = new SelectedPhotoAdapter(this, selectedPhotoUris, position -> {
            selectedPhotoUris.remove(position);
            photoAdapter.notifyItemRemoved(position);
            if (selectedPhotoUris.isEmpty()) rvSelectedPhotos.setVisibility(View.GONE);
        });
        rvSelectedPhotos.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelectedPhotos.setAdapter(photoAdapter);
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
        btnTakePhoto.setOnClickListener(v -> launchCamera());
        btnAddPhoto.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        if (btnAiGenerateDesc != null) {
            btnAiGenerateDesc.setOnClickListener(v -> generateAiDescription());
        }
        if (btnAiContractorTip != null) {
            btnAiContractorTip.setOnClickListener(v -> getAiContractorAdvice());
        }
        if (btnAiPermitCheck != null) {
            btnAiPermitCheck.setOnClickListener(v -> checkPermitRequirements());
        }
    }

    private void checkPermitRequirements() {
        String category = spinnerCategory.getSelectedItem() != null ?
                spinnerCategory.getSelectedItem().toString() : "";
        String city = spinnerCity.getSelectedItem() != null ?
                spinnerCity.getSelectedItem().toString() : "";

        if (category.isEmpty() || category.equals("Select Category")) {
            Toast.makeText(this, "Select a category first", Toast.LENGTH_SHORT).show();
            return;
        }

        String location = (!city.isEmpty() && !city.equals("Select City")) ? city : "Pakistan";

        btnAiPermitCheck.setEnabled(false);
        btnAiPermitCheck.setText("Checking...");

        aiHelper.checkPermitRequirements(category, location, new GeminiAIHelper.AIResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    btnAiPermitCheck.setEnabled(true);
                    btnAiPermitCheck.setText("📋 Permit Check");
                    new androidx.appcompat.app.AlertDialog.Builder(JobPostActivity.this)
                            .setTitle("📋 Permits & Compliance — " + category)
                            .setMessage(response.trim())
                            .setPositiveButton("Got it", null)
                            .show();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnAiPermitCheck.setEnabled(true);
                    btnAiPermitCheck.setText("📋 Permit Check");
                    Toast.makeText(JobPostActivity.this, "AI error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void generateAiDescription() {
        String title = etJobTitle.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem() != null ?
                spinnerCategory.getSelectedItem().toString() : "";
        String existing = etJobDescription.getText().toString().trim();

        if (title.isEmpty() && category.isEmpty() && existing.isEmpty()) {
            Toast.makeText(this, "Enter a job title or description first", Toast.LENGTH_SHORT).show();
            return;
        }

        String info = (!title.isEmpty() ? "Job: " + title : "") +
                (!category.isEmpty() && !category.equals("Select Category") ? ", Category: " + category : "") +
                (!existing.isEmpty() ? ", Notes: " + existing : "");

        btnAiGenerateDesc.setEnabled(false);
        btnAiGenerateDesc.setText("Generating...");
        Toast.makeText(this, "Generating description...", Toast.LENGTH_SHORT).show();

        aiHelper.helpWriteJobDescription(info, new GeminiAIHelper.AIResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    etJobDescription.setText(response.trim());
                    btnAiGenerateDesc.setEnabled(true);
                    btnAiGenerateDesc.setText("✨ Generate description with AI");
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(JobPostActivity.this, "AI error: " + error, Toast.LENGTH_SHORT).show();
                    btnAiGenerateDesc.setEnabled(true);
                    btnAiGenerateDesc.setText("✨ Generate description with AI");
                });
            }
        });
    }

    private void getAiContractorAdvice() {
        String title = etJobTitle.getText().toString().trim();
        String desc = etJobDescription.getText().toString().trim();
        if (title.isEmpty() && desc.isEmpty()) {
            Toast.makeText(this, "Enter job details first", Toast.LENGTH_SHORT).show();
            return;
        }

        String jobInfo = (!title.isEmpty() ? title : "") + (!desc.isEmpty() ? ": " + desc : "");
        btnAiContractorTip.setEnabled(false);

        aiHelper.getContractorRecommendation(jobInfo, new GeminiAIHelper.AIResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    btnAiContractorTip.setEnabled(true);
                    new androidx.appcompat.app.AlertDialog.Builder(JobPostActivity.this)
                            .setTitle("AI Contractor Advice")
                            .setMessage(response.trim())
                            .setPositiveButton("Got it", null)
                            .show();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnAiContractorTip.setEnabled(true);
                    Toast.makeText(JobPostActivity.this, "AI error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
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

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        try {
            File photoDir = new File(getCacheDir(), "photos");
            photoDir.mkdirs();
            File photoFile = new File(photoDir, "job_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            // Log the cause - swallowing it silently is what made this look like a dead button.
            Log.e("JobPost", "Could not open camera", e);
            Toast.makeText(this, "Camera unavailable: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void postJob() {
        String title = etJobTitle.getText().toString().trim();
        String description = etJobDescription.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();
        String timelineStr = etTimeline.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String city = spinnerCity.getSelectedItem().toString();

        if (!validateInputs(title, description, budgetStr, timelineStr, address, category, city)) {
            return;
        }

        double budget = Double.parseDouble(budgetStr);
        String timeline = timelineStr + " days";
        String location = city + ", " + address;
        String jobId = "job_" + UUID.randomUUID().toString();

        Job job = new Job(jobId, currentUserId, currentUserName,
                title, description, category, budget, timeline, location);

        showLoading(true);

        if (selectedPhotoUris.isEmpty()) {
            saveJobToFirestore(job);
        } else {
            uploadPhotosAndPost(job);
        }
    }

    private void uploadPhotosAndPost(Job job) {
        List<String> uploadedUrls = new ArrayList<>();
        int[] remaining = {selectedPhotoUris.size()};
        int[] failedCount = {0};

        for (int i = 0; i < selectedPhotoUris.size(); i++) {
            Uri uri = selectedPhotoUris.get(i);
            String path = "job_attachments/" + job.getJobId() + "/" + i + ".jpg";
            StorageReference ref = storageRef.child(path);
            ref.putFile(uri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> {
                        uploadedUrls.add(downloadUri.toString());
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            job.setAttachments(uploadedUrls);
                            saveJobToFirestore(job, failedCount[0]);
                        }
                    })
                    .addOnFailureListener(e -> {
                        failedCount[0]++;
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            job.setAttachments(uploadedUrls);
                            saveJobToFirestore(job, failedCount[0]);
                        }
                    });
        }
    }

    private void saveJobToFirestore(Job job) {
        saveJobToFirestore(job, 0);
    }

    private void saveJobToFirestore(Job job, int failedPhotoCount) {
        JobManager.getInstance()
                .createJob(job)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    updateClientJobCount();
                    String message = failedPhotoCount > 0
                            ? "Job posted, but " + failedPhotoCount + " photo(s) failed to upload."
                            : "Job posted successfully!";
                    Toast.makeText(JobPostActivity.this, message, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(JobPostActivity.this,
                            "Error posting job: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateClientJobCount() {
        // Atomic increment - avoids the lost-update race of a read-modify-write when jobs are
        // posted back-to-back or from two devices.
        UserManager.getInstance()
                .updateField(currentUserId, "activeJobs", com.google.firebase.firestore.FieldValue.increment(1));
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