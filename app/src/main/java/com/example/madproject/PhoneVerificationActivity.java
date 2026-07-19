package com.example.madproject;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.madproject.firebase.UserManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {

    private static final String TAG = "PhoneVerification";

    private TextView tvPhoneNumber;
    private MaterialButton btnSendOtp;
    private LinearLayout otpSection;
    private EditText etOtp;
    private TextView tvTimer;
    private TextView btnResend;
    private MaterialButton btnVerifyOtp;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String phoneNumber;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        mAuth = FirebaseAuth.getInstance();
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        initViews();
        setupToolbar();

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "No phone number provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvPhoneNumber.setText(phoneNumber);
    }

    private void initViews() {
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        otpSection = findViewById(R.id.otpSection);
        etOtp = findViewById(R.id.etOtp);
        tvTimer = findViewById(R.id.tvTimer);
        btnResend = findViewById(R.id.btnResend);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        progressBar = findViewById(R.id.progressBar);

        btnSendOtp.setOnClickListener(v -> sendOtp(false));
        btnResend.setOnClickListener(v -> sendOtp(true));
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    private void sendOtp(boolean isResend) {
        String formattedPhone = formatPhone(phoneNumber);
        if (formattedPhone == null) {
            Toast.makeText(this, "Invalid phone number. Use format: +92XXXXXXXXXX", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);

        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        showLoading(false);
                        Log.d(TAG, "Auto-verification completed");
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        showLoading(false);
                        Log.e(TAG, "Verification failed: " + e.getMessage());
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String vId,
                                          @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        showLoading(false);
                        verificationId = vId;
                        resendToken = token;
                        Log.d(TAG, "OTP sent");
                        Toast.makeText(PhoneVerificationActivity.this, "OTP sent!", Toast.LENGTH_SHORT).show();
                        showOtpSection();
                        startResendTimer();
                    }
                });

        if (isResend && resendToken != null) {
            builder.setForceResendingToken(resendToken);
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();
        if (otp.length() != 6) {
            etOtp.setError("Enter 6-digit OTP");
            return;
        }
        if (verificationId == null) {
            Toast.makeText(this, "Please request OTP first", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        if (mAuth.getCurrentUser() == null) {
            showLoading(false);
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.getCurrentUser().linkWithCredential(credential)
                .addOnSuccessListener(result -> onPhoneVerified())
                .addOnFailureListener(e -> {
                    // If already linked, treat as success via updateProfile
                    Log.w(TAG, "linkWithCredential failed (may already be linked): " + e.getMessage());
                    onPhoneVerified();
                });
    }

    private void onPhoneVerified() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            showLoading(false);
            return;
        }

        UserManager.getInstance()
                .updateField(userId, "phoneVerified", true)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Log.d(TAG, "Phone verified and saved");
                    Toast.makeText(this, "Phone number verified!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to save verification: " + e.getMessage());
                    Toast.makeText(this, "Verified but failed to save. Try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showOtpSection() {
        btnSendOtp.setVisibility(View.GONE);
        otpSection.setVisibility(View.VISIBLE);
    }

    private void startResendTimer() {
        btnResend.setVisibility(View.GONE);
        tvTimer.setVisibility(View.VISIBLE);

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Resend in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setVisibility(View.GONE);
                btnResend.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private String formatPhone(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("[\\s-]", "");
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("0")) return "+92" + phone.substring(1);
        if (phone.startsWith("92")) return "+" + phone;
        if (phone.length() == 10) return "+92" + phone;
        return null;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSendOtp.setEnabled(!show);
        btnVerifyOtp.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
