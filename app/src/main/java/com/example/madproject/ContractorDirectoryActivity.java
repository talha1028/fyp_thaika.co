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
