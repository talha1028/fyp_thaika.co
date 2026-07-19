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
    private android.widget.TextView tvTotalItems, tvLowStock, tvTotalValue;

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
            getSupportActionBar().setTitle("");

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

        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvLowStock   = findViewById(R.id.tvLowStock);
        tvTotalValue = findViewById(R.id.tvTotalValue);

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

        int lowStockCount = 0;
        double totalValue = 0;
        for (Material material : allMaterialsList) {
            totalValue += material.getTotalCost();
            if ("low_stock".equals(material.getStatus()) || "out_of_stock".equals(material.getStatus())) {
                lowStockCount++;
            }
            if (currentFilter.equals("all") || material.getStatus().equals(currentFilter)) {
                filteredMaterialsList.add(material);
            }
        }

        if (tvTotalItems != null) tvTotalItems.setText(String.valueOf(allMaterialsList.size()));
        if (tvLowStock   != null) tvLowStock.setText(String.valueOf(lowStockCount));
        if (tvTotalValue != null) tvTotalValue.setText(formatCurrency(totalValue));

        materialAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private String formatCurrency(double amount) {
        if (amount >= 100000) return String.format("%.1f L", amount / 100000);
        if (amount >= 1000)   return String.format("%.0fK", amount / 1000);
        return String.format("%.0f", amount);
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
