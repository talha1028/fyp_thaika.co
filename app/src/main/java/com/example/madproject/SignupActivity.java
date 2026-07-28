package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.signup_activity);

        // Keep the form clear of the status bar, navigation bar and soft keyboard
        View formScroll = findViewById(R.id.formScroll);
        ViewCompat.setOnApplyWindowInsetsListener(formScroll, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(0, bars.top, 0, Math.max(bars.bottom, ime.bottom));
            if (ime.bottom > 0) {
                // The viewport just shrank. Wait for layout, then scroll the focused
                // field back above the keyboard if the keyboard now covers it.
                OneShotPreDrawListener.add(v, () -> {
                    View focused = getCurrentFocus();
                    if (focused == null || !(v instanceof ScrollView)) {
                        return;
                    }
                    ScrollView sv = (ScrollView) v;
                    int[] svXY = new int[2];
                    int[] focusedXY = new int[2];
                    sv.getLocationOnScreen(svXY);
                    focused.getLocationOnScreen(focusedXY);
                    int gap = (int) (16 * getResources().getDisplayMetrics().density);
                    int visibleBottom = svXY[1] + sv.getHeight() - sv.getPaddingBottom();
                    int delta = (focusedXY[1] + focused.getHeight() + gap) - visibleBottom;
                    if (delta > 0) {
                        sv.smoothScrollBy(0, delta);
                    }
                });
            }
            return insets;
        });

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
}