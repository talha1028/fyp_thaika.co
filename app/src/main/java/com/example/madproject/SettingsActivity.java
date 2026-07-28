package com.example.madproject;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.example.madproject.firebase.UserManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
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
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

        btnPrivacyPolicy.setOnClickListener(v -> showInfoDialog(
                "Privacy Policy",
                "How we handle your data",
                R.drawable.ic_privacy,
                "Thaika.co collects personal information (name, phone, email) to connect clients " +
                "with contractors. Your data is stored securely on Firebase and is never sold to third parties. " +
                "Location data is used only to match you with nearby contractors. " +
                "You may request deletion of your account at any time by contacting support."));

        btnTermsConditions.setOnClickListener(v -> showInfoDialog(
                "Terms and Conditions",
                "The rules of using Thaika.co",
                R.drawable.ic_terms,
                "By using Thaika.co you agree to:\n\n" +
                "•  Use the platform only for legitimate construction services\n\n" +
                "•  Provide accurate information in job posts and bids\n\n" +
                "•  Complete payment obligations once a bid is accepted\n\n" +
                "•  Not engage in fraudulent activity or fake reviews\n\n" +
                "•  Resolve disputes through the in-app process\n\n" +
                "Thaika.co is not liable for work quality or contractor performance. " +
                "All transactions are between clients and contractors directly."));

        btnHelp.setOnClickListener(v -> showInfoDialog(
                "Help & Support",
                "We're here to help",
                R.drawable.ic_help,
                "NEED HELP? CONTACT US\n\n" +
                "📧   support@thaika.co\n\n" +
                "📞   +92-300-THAIKA\n\n" +
                "⏰   Mon–Fri, 9am–6pm PKT\n\n\n" +
                "COMMON ISSUES\n\n" +
                "•  Bid not showing — refresh the job page\n\n" +
                "•  Payment failed — try a different method\n\n" +
                "•  Can't find contractor — try adjusting filters\n\n" +
                "•  Chat not loading — check internet connection"));

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
        View content = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        TextInputLayout tilCurrent = content.findViewById(R.id.tilCurrent);
        TextInputLayout tilNew = content.findViewById(R.id.tilNew);
        TextInputLayout tilConfirm = content.findViewById(R.id.tilConfirm);
        EditText etCurrent = content.findViewById(R.id.etCurrentPassword);
        EditText etNew = content.findViewById(R.id.etNewPassword);
        EditText etConfirm = content.findViewById(R.id.etConfirmPassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Change Password")
                .setView(content)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String current = etCurrent.getText().toString().trim();
                String newPass = etNew.getText().toString().trim();
                String confirm = etConfirm.getText().toString().trim();

                // Clear any error from a previous attempt, otherwise a fixed field keeps its
                // red outline and the layout stays expanded.
                tilCurrent.setError(null);
                tilNew.setError(null);
                tilConfirm.setError(null);

                if (TextUtils.isEmpty(current)) {
                    tilCurrent.setError("Enter current password");
                    return;
                }
                if (newPass.length() < 6) {
                    tilNew.setError("Min 6 characters");
                    return;
                }
                if (!newPass.equals(confirm)) {
                    tilConfirm.setError("Passwords do not match");
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

    /**
     * Read-only info dialog used by Privacy Policy, Terms and Conditions and Help & Support.
     * Uses the custom dialog_info layout - icon badge, subtitle and a scrollable body - instead
     * of a bare AlertDialog, whose plain title/message rendering looked unstyled next to the
     * rest of the app.
     */
    private void showInfoDialog(String title, String subtitle, int iconRes, String message) {
        View content = getLayoutInflater().inflate(R.layout.dialog_info, null);

        ((ImageView) content.findViewById(R.id.ivInfoIcon)).setImageResource(iconRes);
        ((TextView) content.findViewById(R.id.tvInfoTitle)).setText(title);
        ((TextView) content.findViewById(R.id.tvInfoSubtitle)).setText(subtitle);
        ((TextView) content.findViewById(R.id.tvInfoMessage)).setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();

        // Without this the platform draws its own square white background behind our rounded card.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        content.findViewById(R.id.btnInfoDismiss).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
