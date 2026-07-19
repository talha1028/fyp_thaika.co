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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.ContractorAdapter;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.google.android.material.appbar.MaterialToolbar;
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
    private TextView tvResultsCount;
    private TextView btnSort;

    // Category pill buttons
    private TextView btnAllCategories, btnElectrician, btnPlumber, btnMason, btnCarpenter, btnPainter;

    private ContractorAdapter contractorAdapter;
    private List<User> contractorList;
    private List<User> filteredList;

    private String selectedCategory = "";
    private double minRating = 0;
    private int minExperience = 0;
    private String selectedSort = "rating";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contractor_directory);

        initViews();
        setupRecyclerView();
        setupSearch();
        setupCategoryPills();
        setupFilterButton();
        setupSortButton();
        loadContractors();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("");
        }

        rvContractors = findViewById(R.id.rvContractors);
        etSearch = findViewById(R.id.etSearch);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        btnSort = findViewById(R.id.btnSort);

        btnAllCategories = findViewById(R.id.btnAllCategories);
        btnElectrician = findViewById(R.id.btnElectrician);
        btnPlumber = findViewById(R.id.btnPlumber);
        btnMason = findViewById(R.id.btnMason);
        btnCarpenter = findViewById(R.id.btnCarpenter);
        btnPainter = findViewById(R.id.btnPainter);

        rvContractors.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRecyclerView() {
        contractorList = new ArrayList<>();
        filteredList = new ArrayList<>();

        contractorAdapter = new ContractorAdapter(this, filteredList, contractor -> {
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
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryPills() {
        View.OnClickListener pillClick = v -> {
            resetPillStyles();
            v.setBackgroundResource(R.drawable.bg_pill_selected);
            ((TextView) v).setTextColor(0xFFFFFFFF);

            if (v.getId() == R.id.btnAllCategories) {
                selectedCategory = "";
            } else if (v.getId() == R.id.btnElectrician) {
                selectedCategory = "Electrical";
            } else if (v.getId() == R.id.btnPlumber) {
                selectedCategory = "Plumbing";
            } else if (v.getId() == R.id.btnMason) {
                selectedCategory = "Masonry";
            } else if (v.getId() == R.id.btnCarpenter) {
                selectedCategory = "Carpentry";
            } else if (v.getId() == R.id.btnPainter) {
                selectedCategory = "Painting";
            }
            applyFilters();
        };

        btnAllCategories.setOnClickListener(pillClick);
        btnElectrician.setOnClickListener(pillClick);
        btnPlumber.setOnClickListener(pillClick);
        btnMason.setOnClickListener(pillClick);
        btnCarpenter.setOnClickListener(pillClick);
        btnPainter.setOnClickListener(pillClick);
    }

    private void resetPillStyles() {
        TextView[] pills = {btnAllCategories, btnElectrician, btnPlumber, btnMason, btnCarpenter, btnPainter};
        for (TextView pill : pills) {
            pill.setBackgroundResource(R.drawable.bg_pill_unselected);
            pill.setTextColor(0xFF757575);
        }
    }

    private void setupFilterButton() {
        View btnFilter = findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }
    }

    private void showFilterDialog() {
        String[] ratingOptions = {"Any Rating", "3+ Stars", "4+ Stars", "4.5+ Stars"};
        double[] ratingValues = {0, 3.0, 4.0, 4.5};
        String[] expOptions = {"Any Experience", "2+ Years", "5+ Years", "10+ Years"};
        int[] expValues = {0, 2, 5, 10};

        int currentRatingIdx = 0;
        for (int i = ratingValues.length - 1; i >= 0; i--) {
            if (minRating >= ratingValues[i]) { currentRatingIdx = i; break; }
        }
        int currentExpIdx = 0;
        for (int i = expValues.length - 1; i >= 0; i--) {
            if (minExperience >= expValues[i]) { currentExpIdx = i; break; }
        }

        final int[] selectedRatingIdx = {currentRatingIdx};
        final int[] selectedExpIdx = {currentExpIdx};

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        TextView tvRatingLabel = new TextView(this);
        tvRatingLabel.setText("Minimum Rating");
        tvRatingLabel.setTextSize(14);
        tvRatingLabel.setTextColor(0xFF212121);
        tvRatingLabel.setPadding(0, 0, 0, 8);
        layout.addView(tvRatingLabel);

        android.widget.Spinner spinnerRating = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> ratingAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ratingOptions);
        ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRating.setAdapter(ratingAdapter);
        spinnerRating.setSelection(selectedRatingIdx[0]);
        layout.addView(spinnerRating);

        TextView tvExpLabel = new TextView(this);
        tvExpLabel.setText("Minimum Experience");
        tvExpLabel.setTextSize(14);
        tvExpLabel.setTextColor(0xFF212121);
        tvExpLabel.setPadding(0, 24, 0, 8);
        layout.addView(tvExpLabel);

        android.widget.Spinner spinnerExp = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> expAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, expOptions);
        expAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExp.setAdapter(expAdapter);
        spinnerExp.setSelection(selectedExpIdx[0]);
        layout.addView(spinnerExp);

        new AlertDialog.Builder(this)
                .setTitle("Filter Contractors")
                .setView(layout)
                .setPositiveButton("Apply", (d, w) -> {
                    minRating = ratingValues[spinnerRating.getSelectedItemPosition()];
                    minExperience = expValues[spinnerExp.getSelectedItemPosition()];
                    applyFilters();
                })
                .setNegativeButton("Clear", (d, w) -> {
                    minRating = 0;
                    minExperience = 0;
                    applyFilters();
                })
                .show();
    }

    private void setupSortButton() {
        if (btnSort == null) return;
        btnSort.setOnClickListener(v -> {
            String[] options = {"Highest Rating", "Most Reviews", "Most Experience", "Most Projects"};
            new AlertDialog.Builder(this)
                    .setTitle("Sort Contractors")
                    .setItems(options, (d, which) -> {
                        switch (which) {
                            case 0: selectedSort = "rating"; btnSort.setText("Sort by: Rating"); break;
                            case 1: selectedSort = "reviews"; btnSort.setText("Sort by: Reviews"); break;
                            case 2: selectedSort = "experience"; btnSort.setText("Sort by: Experience"); break;
                            case 3: selectedSort = "projects"; btnSort.setText("Sort by: Projects"); break;
                        }
                        applyFilters();
                    })
                    .show();
        });
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase().trim();
        filteredList.clear();

        for (User contractor : contractorList) {
            boolean matchSearch = query.isEmpty() ||
                    (contractor.getFullName() != null && contractor.getFullName().toLowerCase().contains(query)) ||
                    (contractor.getCategory() != null && contractor.getCategory().toLowerCase().contains(query)) ||
                    (contractor.getCity() != null && contractor.getCity().toLowerCase().contains(query));

            boolean matchCategory = selectedCategory.isEmpty() ||
                    (contractor.getCategory() != null && contractor.getCategory().equals(selectedCategory));

            boolean matchRating = contractor.getRating() >= minRating;
            boolean matchExp = contractor.getExperienceYears() >= minExperience;

            if (matchSearch && matchCategory && matchRating && matchExp) {
                filteredList.add(contractor);
            }
        }

        switch (selectedSort) {
            case "rating":
                Collections.sort(filteredList, (c1, c2) -> Double.compare(c2.getRating(), c1.getRating()));
                break;
            case "reviews":
                Collections.sort(filteredList, (c1, c2) -> Integer.compare(c2.getTotalReviews(), c1.getTotalReviews()));
                break;
            case "experience":
                Collections.sort(filteredList, (c1, c2) -> Integer.compare(c2.getExperienceYears(), c1.getExperienceYears()));
                break;
            case "projects":
                Collections.sort(filteredList, (c1, c2) -> Integer.compare(c2.getCompletedProjects(), c1.getCompletedProjects()));
                break;
        }

        contractorAdapter.notifyDataSetChanged();
        if (tvResultsCount != null) {
            tvResultsCount.setText(filteredList.size() + " contractors found");
        }
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

                    applyFilters();
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
