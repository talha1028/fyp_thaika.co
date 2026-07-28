package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText email, password;
    private Button btnLogin;
    private TextView btnCreate, forgotPassword;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        // Initialize views
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnCreate = findViewById(R.id.createbtn);
        btnLogin = findViewById(R.id.loginbtn);
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
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Account data not found. Please sign up again or contact support.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        // User not found in Firestore, stay on login - but say so instead of
                        // leaving a silent dead end.
                        Toast.makeText(MainActivity.this,
                                "Could not load your account: " + error, Toast.LENGTH_LONG).show();
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
}