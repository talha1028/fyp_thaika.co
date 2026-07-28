package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.JobAdapter;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.models.Job;
import com.example.madproject.views.ShimmerLayout;
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
    private Spinner spinnerCity;
    private TextView btnBudgetFilter;
    private TextView btnSort;
    private RecyclerView rvJobs;
    private LinearLayout emptyState;
    private ShimmerLayout shimmerJobs;

    /**
     * True while a fetch is in flight. The category/city Spinners fire their initial
     * onItemSelected during the first layout pass - after onCreate has already kicked off the
     * load - so filterJobs() would otherwise flip the "No jobs found" empty state on before any
     * data has arrived. The skeleton owns the screen until the load resolves.
     */
    private boolean isLoading = true;

    private FirebaseAuth mAuth;
    private String currentUserId;

    private JobAdapter jobAdapter;
    private List<Job> allJobsList;
    private List<Job> filteredJobsList;

    private double minBudget = 0;
    private double maxBudget = Double.MAX_VALUE;
    private String selectedSort = "newest";

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
        setupCitySpinner();
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
        spinnerCity = findViewById(R.id.spinnerCity);
        btnBudgetFilter = findViewById(R.id.btnBudgetFilter);
        btnSort = findViewById(R.id.btnSort);
        rvJobs = findViewById(R.id.rvJob);
        emptyState = findViewById(R.id.emptyState);
        shimmerJobs = findViewById(R.id.shimmerJobs);

        btnBudgetFilter.setOnClickListener(v -> showBudgetDialog());
        btnSort.setOnClickListener(v -> showSortDialog());
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
                    // Resolve the screen on the error path too, otherwise the skeleton goes away
                    // and leaves nothing behind it.
                    filterJobs();
                    Log.e(TAG, "Error loading jobs: " + e.getMessage());
                    Toast.makeText(this, "Error loading jobs: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupCitySpinner() {
        String[] cities = {
                "All Cities", "Karachi", "Lahore", "Islamabad", "Rawalpindi",
                "Faisalabad", "Multan", "Peshawar", "Quetta", "Sialkot",
                "Gujranwala", "Hyderabad", "Abbottabad", "Murree", "Other"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(adapter);
        spinnerCity.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                filterJobs();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
    }

    private void showBudgetDialog() {
        EditText etMin = new EditText(this);
        EditText etMax = new EditText(this);
        etMin.setHint("Min Budget (Rs.)");
        etMax.setHint("Max Budget (Rs.)");
        etMin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etMax.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (minBudget > 0) etMin.setText(String.valueOf((int) minBudget));
        if (maxBudget < Double.MAX_VALUE) etMax.setText(String.valueOf((int) maxBudget));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 16);
        layout.addView(etMin);
        layout.addView(etMax);

        new AlertDialog.Builder(this)
                .setTitle("Budget Filter (Rs.)")
                .setView(layout)
                .setPositiveButton("Apply", (d, w) -> {
                    String minStr = etMin.getText().toString().trim();
                    String maxStr = etMax.getText().toString().trim();
                    minBudget = TextUtils.isEmpty(minStr) ? 0 : Double.parseDouble(minStr);
                    maxBudget = TextUtils.isEmpty(maxStr) ? Double.MAX_VALUE : Double.parseDouble(maxStr);
                    boolean hasFilter = minBudget > 0 || maxBudget < Double.MAX_VALUE;
                    btnBudgetFilter.setBackgroundResource(hasFilter ?
                            R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
                    btnBudgetFilter.setTextColor(hasFilter ?
                            0xFFFFFFFF : 0xFF7C4DFF);
                    filterJobs();
                })
                .setNegativeButton("Clear", (d, w) -> {
                    minBudget = 0;
                    maxBudget = Double.MAX_VALUE;
                    btnBudgetFilter.setBackgroundResource(R.drawable.bg_pill_unselected);
                    btnBudgetFilter.setTextColor(0xFF7C4DFF);
                    filterJobs();
                })
                .show();
    }

    private void showSortDialog() {
        String[] options = {"Newest First", "Oldest First", "Lowest Budget", "Highest Budget", "Most Bids"};
        new AlertDialog.Builder(this)
                .setTitle("Sort Jobs")
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0: selectedSort = "newest"; btnSort.setText("Newest"); break;
                        case 1: selectedSort = "oldest"; btnSort.setText("Oldest"); break;
                        case 2: selectedSort = "budget_low"; btnSort.setText("Budget ↑"); break;
                        case 3: selectedSort = "budget_high"; btnSort.setText("Budget ↓"); break;
                        case 4: selectedSort = "most_bids"; btnSort.setText("Most Bids"); break;
                    }
                    filterJobs();
                })
                .show();
    }

    private void filterJobs() {
        String searchQuery = etSearch.getText().toString().toLowerCase().trim();
        String selectedCategory = spinnerCategory.getSelectedItem().toString();
        String selectedCity = spinnerCity.getSelectedItem() != null ?
                spinnerCity.getSelectedItem().toString() : "All Cities";

        filteredJobsList.clear();

        for (Job job : allJobsList) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (job.getTitle() != null && job.getTitle().toLowerCase().contains(searchQuery)) ||
                    (job.getDescription() != null && job.getDescription().toLowerCase().contains(searchQuery)) ||
                    (job.getLocation() != null && job.getLocation().toLowerCase().contains(searchQuery));

            boolean matchesCategory = selectedCategory.equals("All Categories") ||
                    (job.getCategory() != null && job.getCategory().equals(selectedCategory));

            boolean matchesCity = selectedCity.equals("All Cities") ||
                    (job.getLocation() != null && job.getLocation().toLowerCase().contains(selectedCity.toLowerCase()));

            boolean matchesBudget = job.getBudget() >= minBudget && job.getBudget() <= maxBudget;

            if (matchesSearch && matchesCategory && matchesCity && matchesBudget) {
                filteredJobsList.add(job);
            }
        }

        switch (selectedSort) {
            case "newest":
                Collections.sort(filteredJobsList, (j1, j2) -> Long.compare(j2.getPostedDate(), j1.getPostedDate()));
                break;
            case "oldest":
                Collections.sort(filteredJobsList, (j1, j2) -> Long.compare(j1.getPostedDate(), j2.getPostedDate()));
                break;
            case "budget_low":
                Collections.sort(filteredJobsList, (j1, j2) -> Double.compare(j1.getBudget(), j2.getBudget()));
                break;
            case "budget_high":
                Collections.sort(filteredJobsList, (j1, j2) -> Double.compare(j2.getBudget(), j1.getBudget()));
                break;
            case "most_bids":
                Collections.sort(filteredJobsList, (j1, j2) -> Integer.compare(j2.getTotalBids(), j1.getTotalBids()));
                break;
        }

        jobAdapter.notifyDataSetChanged();

        if (isLoading) {
            // The skeleton owns the screen until the fetch resolves; an empty list here just
            // means the data has not arrived yet, not that there is nothing to show.
            return;
        }

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
        isLoading = show;
        if (show) {
            shimmerJobs.setVisibility(View.VISIBLE);
            shimmerJobs.showShimmer();
            rvJobs.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            shimmerJobs.hideShimmer();
            shimmerJobs.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}