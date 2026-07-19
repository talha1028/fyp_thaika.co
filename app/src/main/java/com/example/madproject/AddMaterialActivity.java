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
import com.example.madproject.helpers.GeminiAIHelper;
import com.example.madproject.models.Material;
import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public class AddMaterialActivity extends AppCompatActivity {

    private EditText etMaterialName, etQuantity, etUnitPrice, etSupplier, etDescription;
    private Spinner spinnerCategory, spinnerUnit;
    private Button btnSaveMaterial, btnCancel;
    private android.widget.TextView btnAiSuggestMaterials;
    private GeminiAIHelper aiHelper;

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
            getSupportActionBar().setTitle("");

        etMaterialName = findViewById(R.id.etMaterialName);
        etQuantity = findViewById(R.id.etQuantity);
        etUnitPrice = findViewById(R.id.etUnitPrice);
        etSupplier = findViewById(R.id.etSupplier);
        etDescription = findViewById(R.id.etDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        btnSaveMaterial = findViewById(R.id.btnSaveMaterial);
        btnCancel = findViewById(R.id.btnCancel);
        btnAiSuggestMaterials = findViewById(R.id.btnAiSuggestMaterials);
        aiHelper = new GeminiAIHelper(this);
    }

    private void setupClickListeners() {
        btnSaveMaterial.setOnClickListener(v -> saveMaterial());
        btnCancel.setOnClickListener(v -> finish());

        if (btnAiSuggestMaterials != null) {
            btnAiSuggestMaterials.setOnClickListener(v -> getAiMaterialSuggestions());
        }
    }

    private void getAiMaterialSuggestions() {
        String category = spinnerCategory.getSelectedItem() != null ?
                spinnerCategory.getSelectedItem().toString() : "";
        String projectInfo = (projectName != null && !projectName.isEmpty() ? projectName : category);

        if (projectInfo.isEmpty()) {
            Toast.makeText(this, "Select a category first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAiSuggestMaterials.setEnabled(false);
        btnAiSuggestMaterials.setText("Suggesting...");

        aiHelper.getMaterialRecommendations(projectInfo, new GeminiAIHelper.AIResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    btnAiSuggestMaterials.setEnabled(true);
                    btnAiSuggestMaterials.setText("✨ AI Suggest");
                    new androidx.appcompat.app.AlertDialog.Builder(AddMaterialActivity.this)
                            .setTitle("Suggested Materials")
                            .setMessage(response.trim())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnAiSuggestMaterials.setEnabled(true);
                    btnAiSuggestMaterials.setText("✨ AI Suggest");
                    Toast.makeText(AddMaterialActivity.this, "AI error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
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
}