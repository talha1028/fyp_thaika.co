package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.example.madproject.firebase.UserManager;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private LinearLayout btnEditProfile, btnChangePassword, btnPrivacyPolicy, btnTermsConditions, btnHelp;
    private SwitchCompat switchPushNotif, switchMessageNotif;
    private Button btnLogout;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private boolean loadingPrefs = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        initViews();
        setupClickListeners();
        loadNotificationPrefs();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle("");
            }

        btnEditProfile      = findViewById(R.id.btnEditProfile);
        btnChangePassword   = findViewById(R.id.btnChangePassword);
        btnPrivacyPolicy    = findViewById(R.id.btnPrivacyPolicy);
        btnTermsConditions  = findViewById(R.id.btnTermsConditions);
        btnHelp             = findViewById(R.id.btnHelp);
        switchPushNotif     = findViewById(R.id.switchPushNotif);
        switchMessageNotif  = findViewById(R.id.switchMessageNotif);
        btnLogout           = findViewById(R.id.btnLogout);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnPrivacyPolicy.setOnClickListener(v -> showInfoDialog("Privacy Policy",
                "RebuildPak collects personal information (name, phone, email) to connect clients " +
                "with contractors. Your data is stored securely on Firebase and is never sold to third parties. " +
                "Location data is used only to match you with nearby contractors. " +
                "You may request deletion of your account at any time by contacting support."));

        btnTermsConditions.setOnClickListener(v -> showInfoDialog("Terms and Conditions",
                "By using RebuildPak you agree to:\n\n" +
                "• Use the platform only for legitimate construction services\n" +
                "• Provide accurate information in job posts and bids\n" +
                "• Complete payment obligations once a bid is accepted\n" +
                "• Not engage in fraudulent activity or fake reviews\n" +
                "• Resolve disputes through the in-app process\n\n" +
                "RebuildPak is not liable for work quality or contractor performance. " +
                "All transactions are between clients and contractors directly."));

        btnHelp.setOnClickListener(v -> showInfoDialog("Help & Support",
                "Need help? Contact us:\n\n" +
                "📧 Email: support@rebuildpak.com\n" +
                "📞 Phone: +92-300-REBUILD\n" +
                "⏰ Hours: Mon–Fri, 9am–6pm PKT\n\n" +
                "Common issues:\n" +
                "• Bid not showing — refresh the job page\n" +
                "• Payment failed — try a different method\n" +
                "• Can't find contractor — try adjusting filters\n" +
                "• Chat not loading — check internet connection"));

        switchPushNotif.setOnCheckedChangeListener((btn, checked) -> {
            if (!loadingPrefs) saveNotifPref("pushNotifications", checked);
        });

        switchMessageNotif.setOnCheckedChangeListener((btn, checked) -> {
            if (!loadingPrefs) saveNotifPref("messageNotifications", checked);
        });

        btnLogout.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Logout", (d, w) -> {
                            mAuth.signOut();
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        EditText etCurrent = new EditText(this);
        etCurrent.setHint("Current password");
        etCurrent.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etCurrent);

        EditText etNew = new EditText(this);
        etNew.setHint("New password (min 6 characters)");
        etNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etNew.setPadding(0, 24, 0, 0);
        layout.addView(etNew);

        EditText etConfirm = new EditText(this);
        etConfirm.setHint("Confirm new password");
        etConfirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etConfirm.setPadding(0, 16, 0, 0);
        layout.addView(etConfirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String current = etCurrent.getText().toString().trim();
                String newPass = etNew.getText().toString().trim();
                String confirm = etConfirm.getText().toString().trim();

                if (TextUtils.isEmpty(current)) {
                    etCurrent.setError("Enter current password");
                    return;
                }
                if (newPass.length() < 6) {
                    etNew.setError("Min 6 characters");
                    return;
                }
                if (!newPass.equals(confirm)) {
                    etConfirm.setError("Passwords do not match");
                    return;
                }
                dialog.dismiss();
                changePassword(current, newPass);
            });
        });

        dialog.show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid ->
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show()))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Current password incorrect", Toast.LENGTH_SHORT).show());
    }

    private void loadNotificationPrefs() {
        if (currentUserId.isEmpty()) return;
        loadingPrefs = true;

        UserManager.getInstance().getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(com.example.madproject.models.User user) {
                loadingPrefs = false;
                // Default true if field not set
                switchPushNotif.setChecked(user.isPushNotificationsEnabled());
                switchMessageNotif.setChecked(user.isMessageNotificationsEnabled());
            }

            @Override
            public void onError(String error) {
                loadingPrefs = false;
            }
        });
    }

    private void saveNotifPref(String field, boolean value) {
        if (currentUserId.isEmpty()) return;
        UserManager.getInstance().updateField(currentUserId, field, value);
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
