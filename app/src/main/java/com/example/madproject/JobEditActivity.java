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

import com.example.madproject.firebase.BidManager;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.NotificationManager;
import com.example.madproject.models.Bid;
import com.example.madproject.models.Job;
import com.example.madproject.models.Notification;
import com.example.madproject.views.ShimmerLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.UUID;

public class JobEditActivity extends AppCompatActivity {

    private EditText etJobTitle, etJobDescription, etBudget, etTimeline, etAddress;
    private Spinner spinnerCategory, spinnerCity;
    private Button btnSaveJob, btnCancel;
    private ProgressBar progressBar;
    private ShimmerLayout shimmerForm;
    private View formContent;

    private String currentUserId;
    private String jobId;
    private Job currentJob;

    private static final String[] CATEGORIES = {
            "Select Category", "Construction", "Plumbing", "Electrical", "Painting",
            "Carpentry", "Masonry", "Roofing", "Flooring", "Interior Design",
            "Landscaping", "HVAC", "Welding", "Tiling", "Renovation", "Other"
    };

    private static final String[] CITIES = {
            "Select City", "Karachi", "Lahore", "Islamabad", "Rawalpindi",
            "Faisalabad", "Multan", "Peshawar", "Quetta", "Sialkot",
            "Gujranwala", "Hyderabad", "Bahawalpur", "Sargodha", "Sukkur",
            "Larkana", "Sheikhupura", "Rahim Yar Khan", "Jhang", "Dera Ghazi Khan",
            "Gujrat", "Sahiwal", "Wah Cantonment", "Mardan", "Kasur",
            "Okara", "Mingora", "Nawabshah", "Chiniot", "Kotri",
            "Khanpur", "Hafizabad", "Sadiqabad", "Mirpur Khas", "Burewala",
            "Kohat", "Khanewal", "Dera Ismail Khan", "Turbat", "Muzaffargarh",
            "Abbottabad", "Mandi Bahauddin", "Shikarpur", "Jacobabad", "Jhelum",
            "Khairpur", "Khuzdar", "Pakpattan", "Attock"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_edit);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        jobId = getIntent().getStringExtra("jobId");
        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(this, "Error: Job not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupSpinners();
        loadJob();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
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
        btnSaveJob = findViewById(R.id.btnSaveJob);
        btnCancel = findViewById(R.id.btnCancel);
        shimmerForm = findViewById(R.id.shimmerForm);
        formContent = findViewById(R.id.formContent);
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        btnSaveJob.setOnClickListener(v -> saveJob());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CATEGORIES);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CITIES);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);
    }

    /**
     * Swap between the skeleton and the real form. The form starts gone so an edit screen never
     * shows empty hinted fields before the job it is editing has been fetched.
     */
    private void showFormSkeleton(boolean show) {
        if (show) {
            shimmerForm.setVisibility(View.VISIBLE);
            shimmerForm.showShimmer();
            formContent.setVisibility(View.GONE);
        } else {
            shimmerForm.hideShimmer();
            shimmerForm.setVisibility(View.GONE);
            formContent.setVisibility(View.VISIBLE);
        }
    }

    private void loadJob() {
        showFormSkeleton(true);
        setFieldsEnabled(false);
        btnSaveJob.setText("Loading...");

        JobManager.getInstance()
                .getJob(jobId)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentJob = doc.toObject(Job.class);
                    if (currentJob == null) {
                        finish();
                        return;
                    }

                    // Only the job owner can edit
                    if (!currentUserId.equals(currentJob.getClientId())) {
                        Toast.makeText(this, "You can only edit your own jobs", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    populateFields(currentJob);
                    showFormSkeleton(false);
                    setFieldsEnabled(true);
                    btnSaveJob.setText("Save Changes");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading job: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateFields(Job job) {
        etJobTitle.setText(job.getTitle());
        etJobDescription.setText(job.getDescription());
        etBudget.setText(String.valueOf((int) job.getBudget()));

        // Timeline stored as "X days" — strip the " days" suffix
        String timeline = job.getTimeline();
        if (timeline != null) {
            etTimeline.setText(timeline.replace(" days", "").trim());
        }

        // Location stored as "City, Address" — split on first ", "
        String location = job.getLocation();
        if (location != null) {
            int commaIdx = location.indexOf(", ");
            if (commaIdx >= 0) {
                setSpinnerValue(spinnerCity, location.substring(0, commaIdx));
                etAddress.setText(location.substring(commaIdx + 2));
            } else {
                etAddress.setText(location);
            }
        }

        setSpinnerValue(spinnerCategory, job.getCategory());
    }

    private void saveJob() {
        if (currentJob == null) return;

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

        double newBudget = Double.parseDouble(budgetStr);
        boolean scopeChanged = currentJob.getTotalBids() > 0 && (
                Double.compare(currentJob.getBudget(), newBudget) != 0
                        || !description.equals(currentJob.getDescription())
                        || !category.equals(currentJob.getCategory()));

        currentJob.setTitle(title);
        currentJob.setDescription(description);
        currentJob.setBudget(newBudget);
        currentJob.setTimeline(timelineStr + " days");
        currentJob.setLocation(city + ", " + address);
        currentJob.setCategory(category);

        setFieldsEnabled(false);
        btnSaveJob.setText("Saving...");

        JobManager.getInstance()
                .updateJob(currentJob)
                .addOnSuccessListener(aVoid -> {
                    if (scopeChanged) notifyBiddersOfChange();
                    Toast.makeText(this, "Job updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setFieldsEnabled(true);
                    btnSaveJob.setText("Save Changes");
                    Toast.makeText(this, "Error saving job: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Budget/description/category changed after contractors already bid against the original
     * numbers - let existing bidders know their bid may now be based on stale terms, rather than
     * leaving them to find out only if/when their bid gets accepted.
     */
    private void notifyBiddersOfChange() {
        BidManager.getInstance()
                .getPendingBidsByJob(jobId)
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Bid bid = doc.toObject(Bid.class);
                        if (bid == null || bid.getContractorId() == null) continue;

                        Notification notif = new Notification(
                                "notif_" + UUID.randomUUID(),
                                bid.getContractorId(),
                                "Job Updated",
                                "The client updated \"" + currentJob.getTitle()
                                        + "\" (budget, description, or category changed). Review before it's accepted.",
                                "job", jobId);
                        NotificationManager.getInstance().createNotification(notif);
                    }
                });
    }

    private boolean validateInputs(String title, String description, String budgetStr,
                                   String timelineStr, String address, String category, String city) {
        if (TextUtils.isEmpty(title) || title.length() < 10) {
            etJobTitle.setError("Title must be at least 10 characters");
            etJobTitle.requestFocus();
            return false;
        }
        if (category.equals("Select Category")) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(description) || description.length() < 20) {
            etJobDescription.setError("Description must be at least 20 characters");
            etJobDescription.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(budgetStr)) {
            etBudget.setError("Budget is required");
            etBudget.requestFocus();
            return false;
        }
        try {
            double budget = Double.parseDouble(budgetStr);
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
        if (city.equals("Select City")) {
            Toast.makeText(this, "Please select a city", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(address)) {
            etAddress.setError("Address is required");
            etAddress.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(timelineStr)) {
            etTimeline.setError("Timeline is required");
            etTimeline.requestFocus();
            return false;
        }
        try {
            int days = Integer.parseInt(timelineStr);
            if (days <= 0 || days > 365) {
                etTimeline.setError("Timeline must be between 1 and 365 days");
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

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setFieldsEnabled(boolean enabled) {
        etJobTitle.setEnabled(enabled);
        etJobDescription.setEnabled(enabled);
        etBudget.setEnabled(enabled);
        etTimeline.setEnabled(enabled);
        etAddress.setEnabled(enabled);
        spinnerCategory.setEnabled(enabled);
        spinnerCity.setEnabled(enabled);
        btnSaveJob.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
