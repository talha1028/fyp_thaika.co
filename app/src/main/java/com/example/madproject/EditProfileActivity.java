package com.example.madproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.example.madproject.views.ShimmerLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfilePicture;
    private EditText etFullName, etEmail, etPhone, etAddress;
    private Spinner spinnerCity;
    private Button btnSaveProfile, btnCancel, btnManagePortfolio;
    private ProgressBar progressBar;
    private TextView tvPhoneVerifyStatus;
    private ShimmerLayout shimmerForm;
    private View formContent;

    // Contractor-only fields
    private TextView labelBio, labelCategory, labelExperience, labelHourlyRate;
    private EditText etBio, etExperience, etHourlyRate;
    private Spinner spinnerCategory;

    private static final String[] CATEGORIES = {
            "Select Category", "Construction", "Plumbing", "Electrical", "Painting",
            "Carpentry", "Masonry", "Roofing", "Flooring", "Interior Design",
            "Landscaping", "HVAC", "Welding", "Tiling", "Renovation", "Other"
    };

    private FirebaseAuth mAuth;
    private StorageReference storageRef;
    private String currentUserId;
    private User currentUser;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadProfilePicture(uri);
            });

    private final ActivityResultLauncher<Intent> phoneVerifyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (currentUser != null) currentUser.setPhoneVerified(true);
                    updateVerifyBadge(true);
                    Toast.makeText(this, "Phone verified!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
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
            getSupportActionBar().setTitle("");

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        spinnerCity = findViewById(R.id.spinnerCity);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancel = findViewById(R.id.btnCancel);
        btnManagePortfolio = findViewById(R.id.btnManagePortfolio);
        tvPhoneVerifyStatus = findViewById(R.id.tvPhoneVerifyStatus);
        shimmerForm = findViewById(R.id.shimmerForm);
        formContent = findViewById(R.id.formContent);

        labelBio = findViewById(R.id.labelBio);
        etBio = findViewById(R.id.etBio);
        labelCategory = findViewById(R.id.labelCategory);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        labelExperience = findViewById(R.id.labelExperience);
        etExperience = findViewById(R.id.etExperience);
        labelHourlyRate = findViewById(R.id.labelHourlyRate);
        etHourlyRate = findViewById(R.id.etHourlyRate);

        if (spinnerCategory != null) {
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, CATEGORIES);
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(catAdapter);
        }

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
        if (btnManagePortfolio != null) {
            btnManagePortfolio.setOnClickListener(v -> {
                Intent intent = new Intent(this, PortfolioGalleryActivity.class);
                intent.putExtra("contractorId", currentUserId);
                startActivity(intent);
            });
        }
        ivProfilePicture.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        if (tvPhoneVerifyStatus != null) {
            tvPhoneVerifyStatus.setOnClickListener(v -> {
                if (currentUser != null && currentUser.isPhoneVerified()) return;
                String phone = etPhone.getText().toString().trim();
                if (phone.isEmpty()) {
                    etPhone.setError("Enter phone number first");
                    etPhone.requestFocus();
                    return;
                }
                Intent intent = new Intent(this, PhoneVerificationActivity.class);
                intent.putExtra("phoneNumber", phone);
                phoneVerifyLauncher.launch(intent);
            });
        }
    }

    /**
     * Swap between the skeleton and the real form. Deliberately separate from showLoading(),
     * which is shared with saveProfile() and uploadProfilePicture() - reusing it would make the
     * whole form vanish into a skeleton every time the user hits Save.
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

    private void loadProfile() {
        showFormSkeleton(true);

        UserManager.getInstance()
                .getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        showFormSkeleton(false);
                        currentUser = user;
                        populateFields(user);
                    }

                    @Override
                    public void onError(String error) {
                        // This branch does not finish(), so the skeleton has to be cleared here
                        // or the form shimmers forever behind the toast.
                        showFormSkeleton(false);
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

        setSpinnerValue(spinnerCity, user.getCity());
        updateVerifyBadge(user.isPhoneVerified());

        if (btnManagePortfolio != null) {
            btnManagePortfolio.setVisibility(user.isContractor() ? View.VISIBLE : View.GONE);
        }

        int contractorVisibility = user.isContractor() ? View.VISIBLE : View.GONE;
        setViewVisibility(labelBio, contractorVisibility);
        setViewVisibility(etBio, contractorVisibility);
        setViewVisibility(labelCategory, contractorVisibility);
        setViewVisibility(spinnerCategory, contractorVisibility);
        setViewVisibility(labelExperience, contractorVisibility);
        setViewVisibility(etExperience, contractorVisibility);
        setViewVisibility(labelHourlyRate, contractorVisibility);
        setViewVisibility(etHourlyRate, contractorVisibility);

        if (user.isContractor()) {
            if (etBio != null) etBio.setText(user.getBio());
            if (spinnerCategory != null) setSpinnerValue(spinnerCategory, user.getCategory());
            if (etExperience != null && user.getExperienceYears() > 0) {
                etExperience.setText(String.valueOf(user.getExperienceYears()));
            }
            if (etHourlyRate != null && user.getHourlyRate() > 0) {
                etHourlyRate.setText(String.valueOf((int) user.getHourlyRate()));
            }
        }

        Glide.with(this)
                .load(user.getProfilePictureUrl())
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .into(ivProfilePicture);
    }

    private void setViewVisibility(View view, int visibility) {
        if (view != null) view.setVisibility(visibility);
    }

    private void setEnabledIfPresent(View view, boolean enabled) {
        if (view != null) view.setEnabled(enabled);
    }

    private void updateVerifyBadge(boolean verified) {
        if (tvPhoneVerifyStatus == null) return;
        if (verified) {
            tvPhoneVerifyStatus.setText("Verified");
            tvPhoneVerifyStatus.setTextColor(0xFF4CAF50);
            tvPhoneVerifyStatus.setBackgroundResource(R.drawable.bg_status_green);
        } else {
            tvPhoneVerifyStatus.setText("Verify");
            tvPhoneVerifyStatus.setTextColor(0xFF7C4DFF);
            tvPhoneVerifyStatus.setBackgroundResource(R.drawable.bg_pill_unselected);
        }
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

        if (currentUser.isContractor()) {
            if (etBio != null) currentUser.setBio(etBio.getText().toString().trim());
            if (spinnerCategory != null) currentUser.setCategory(spinnerCategory.getSelectedItem().toString());
            if (etExperience != null) {
                String experienceStr = etExperience.getText().toString().trim();
                if (!experienceStr.isEmpty()) currentUser.setExperienceYears(Integer.parseInt(experienceStr));
            }
            if (etHourlyRate != null) {
                String rateStr = etHourlyRate.getText().toString().trim();
                if (!rateStr.isEmpty()) currentUser.setHourlyRate(Double.parseDouble(rateStr));
            }
        }

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

        if (currentUser != null && currentUser.isContractor()) {
            if (spinnerCategory != null && spinnerCategory.getSelectedItem().toString().equals("Select Category")) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (etExperience != null) {
                String experienceStr = etExperience.getText().toString().trim();
                if (!experienceStr.isEmpty()) {
                    try {
                        int years = Integer.parseInt(experienceStr);
                        if (years < 0 || years > 80) {
                            etExperience.setError("Enter a valid number of years");
                            etExperience.requestFocus();
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        etExperience.setError("Enter a valid number");
                        etExperience.requestFocus();
                        return false;
                    }
                }
            }
            if (etHourlyRate != null) {
                String rateStr = etHourlyRate.getText().toString().trim();
                if (!rateStr.isEmpty()) {
                    try {
                        double rate = Double.parseDouble(rateStr);
                        if (rate <= 0) {
                            etHourlyRate.setError("Hourly rate must be greater than 0");
                            etHourlyRate.requestFocus();
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        etHourlyRate.setError("Enter a valid amount");
                        etHourlyRate.requestFocus();
                        return false;
                    }
                }
            }
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
            setEnabledIfPresent(etBio, false);
            setEnabledIfPresent(spinnerCategory, false);
            setEnabledIfPresent(etExperience, false);
            setEnabledIfPresent(etHourlyRate, false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSaveProfile.setEnabled(true);
            btnSaveProfile.setText("Save Profile");
            btnCancel.setEnabled(true);
            etFullName.setEnabled(true);
            etPhone.setEnabled(true);
            etAddress.setEnabled(true);
            spinnerCity.setEnabled(true);
            setEnabledIfPresent(etBio, true);
            setEnabledIfPresent(spinnerCategory, true);
            setEnabledIfPresent(etExperience, true);
            setEnabledIfPresent(etHourlyRate, true);
        }
    }

    private void uploadProfilePicture(Uri imageUri) {
        showLoading(true);
        StorageReference ref = storageRef.child("profile_pictures/" + currentUserId + ".jpg");
        ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String url = downloadUri.toString();
                    currentUser.setProfilePictureUrl(url);
                    UserManager.getInstance()
                            .updateField(currentUserId, "profilePictureUrl", url)
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                Glide.with(this).load(url).circleCrop().into(ivProfilePicture);
                                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Failed to save picture URL", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
