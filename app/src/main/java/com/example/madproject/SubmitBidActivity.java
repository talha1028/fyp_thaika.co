package com.example.madproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.madproject.firebase.BidManager;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.NotificationManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Bid;
import com.example.madproject.models.Job;
import com.example.madproject.models.Notification;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public class SubmitBidActivity extends AppCompatActivity {

    private EditText etBidAmount, etCompletionDays, etProposal;
    private CheckBox cbTerms;
    private Button btnSubmitBid, btnCancel;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String jobId;
    private Job job;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_bid);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get job ID from Intent
        jobId = getIntent().getStringExtra("jobId");

        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(this, "Error: Job ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        loadJobDetails();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etBidAmount = findViewById(R.id.etBidAmount);
        etCompletionDays = findViewById(R.id.etCompletionDays);
        etProposal = findViewById(R.id.etProposal);
        cbTerms = findViewById(R.id.cbTerms);
        btnSubmitBid = findViewById(R.id.btnSubmitBid);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupClickListeners() {
        btnSubmitBid.setOnClickListener(v -> submitBid());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadJobDetails() {
        JobManager.getInstance()
                .getJob(jobId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        job = documentSnapshot.toObject(Job.class);

                        // Check if job is still open for bids
                        if (job != null && !"open".equals(job.getStatus())) {
                            Toast.makeText(this, "This job is no longer accepting bids",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    } else {
                        Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading job details", Toast.LENGTH_SHORT).show();
                });
    }

    private void submitBid() {
        String amount = etBidAmount.getText().toString().trim();
        String days = etCompletionDays.getText().toString().trim();
        String proposal = etProposal.getText().toString().trim();

        // Validate
        if (TextUtils.isEmpty(amount)) {
            etBidAmount.setError("Amount required");
            etBidAmount.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept terms", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Re-check job status before submitting (in case it changed while user was filling form)
        JobManager.getInstance()
                .getJob(jobId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        showLoading(false);
                        Toast.makeText(this, "Job no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Job currentJob = documentSnapshot.toObject(Job.class);
                    if (currentJob == null || !"open".equals(currentJob.getStatus())) {
                        showLoading(false);
                        Toast.makeText(this, "This job is no longer accepting bids",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Update job reference
                    job = currentJob;

                    // Check if already bid on this job
                    BidManager.getInstance()
                            .checkExistingBid(jobId, currentUserId)
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    showLoading(false);
                                    Toast.makeText(this, "You already submitted a bid for this job",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Create bid
                                createBid(amount, days, proposal);
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error checking existing bid: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error verifying job status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void createBid(String amount, String days, String proposal) {
        // Get contractor info
        UserManager.getInstance()
                .getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User contractor) {
                        if (contractor == null) {
                            showLoading(false);
                            Toast.makeText(SubmitBidActivity.this,
                                    "Error loading profile", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String bidId = "bid_" + UUID.randomUUID().toString();

                        Bid bid = new Bid(
                                bidId,
                                jobId,
                                job != null ? job.getTitle() : "",
                                currentUserId,
                                contractor.getFullName(),
                                Double.parseDouble(amount),
                                days.isEmpty() ? 30 : Integer.parseInt(days),
                                proposal
                        );

                        // Set contractor details
                        bid.setContractorCategory(contractor.getCategory());
                        bid.setContractorRating(contractor.getRating());
                        bid.setContractorCompletedProjects(contractor.getCompletedProjects());

                        // Submit bid
                        BidManager.getInstance()
                                .createBid(bid)
                                .addOnSuccessListener(aVoid -> {
                                    // Increment job's totalBids
                                    JobManager.getInstance()
                                            .incrementTotalBids(jobId)
                                            .addOnSuccessListener(aVoid2 -> {
                                                // Send notification to job owner
                                                sendBidNotification(contractor, bid);

                                                showLoading(false);
                                                Toast.makeText(SubmitBidActivity.this,
                                                        "Bid submitted successfully!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                // Bid was created but counter update failed - still finish
                                                // Send notification anyway
                                                sendBidNotification(contractor, bid);

                                                showLoading(false);
                                                Toast.makeText(SubmitBidActivity.this,
                                                        "Bid submitted successfully!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    Toast.makeText(SubmitBidActivity.this,
                                            "Error submitting bid: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        Toast.makeText(SubmitBidActivity.this,
                                "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendBidNotification(User contractor, Bid bid) {
        if (job == null || job.getClientId() == null) {
            return; // Can't send notification without job owner info
        }

        // Create notification for job owner
        String notificationId = "notif_" + UUID.randomUUID().toString();
        String title = "New Bid Received";
        String message = contractor.getFullName() + " submitted a bid of Rs. " +
                formatCurrency(bid.getBidAmount()) + " on your job \"" + job.getTitle() + "\"";

        Notification notification = new Notification(
                notificationId,
                job.getClientId(), // Send to job owner
                title,
                message,
                "bid",
                jobId // Store jobId so they can navigate to job details
        );

        // Send notification
        NotificationManager.getInstance()
                .createNotification(notification)
                .addOnSuccessListener(aVoid -> {
                    // Notification sent successfully (silent - no user feedback needed)
                })
                .addOnFailureListener(e -> {
                    // Failed to send notification (silent - don't interrupt user flow)
                });
    }

    private String formatCurrency(double amount) {
        if (amount >= 10000000) {
            return String.format("%.1f Cr", amount / 10000000);
        } else if (amount >= 100000) {
            return String.format("%.1f L", amount / 100000);
        } else if (amount >= 1000) {
            return String.format("%.1f K", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            btnSubmitBid.setEnabled(false);
            btnSubmitBid.setText("Submitting...");
            btnCancel.setEnabled(false);
            etBidAmount.setEnabled(false);
            etCompletionDays.setEnabled(false);
            etProposal.setEnabled(false);
            cbTerms.setEnabled(false);
        } else {
            btnSubmitBid.setEnabled(true);
            btnSubmitBid.setText("Submit Bid");
            btnCancel.setEnabled(true);
            etBidAmount.setEnabled(true);
            etCompletionDays.setEnabled(true);
            etProposal.setEnabled(true);
            cbTerms.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}