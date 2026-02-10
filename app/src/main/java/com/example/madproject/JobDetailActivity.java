package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.BidAdapter;
import com.example.madproject.firebase.BidManager;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Bid;
import com.example.madproject.models.Job;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class JobDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvJobTitle, tvCategory, tvPostedDate, tvDescription, tvBudget,
            tvTimeline, tvTotalBids, tvLocation, tvStatus, btnSortBids;
    private RecyclerView rvBids;
    private LinearLayout emptyState;
    private ImageView btnEdit, btnShare;
    private Button btnSubmitBid;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String jobId;
    private Job currentJob;

    private BidAdapter bidAdapter;
    private List<Bid> bidList;
    private String currentSortOrder = "lowest"; // "lowest", "highest", "recent"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get job ID from intent
        jobId = getIntent().getStringExtra("jobId");

        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(this, "Error: Job not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadJobDetails();
        loadBids();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvJobTitle = findViewById(R.id.tvJobTitle);
        tvCategory = findViewById(R.id.tvCategory);
        tvPostedDate = findViewById(R.id.tvPostedDate);
        tvDescription = findViewById(R.id.tvDescription);
        tvBudget = findViewById(R.id.tvBudget);
        tvTimeline = findViewById(R.id.tvTimeline);
        tvTotalBids = findViewById(R.id.tvTotalBids);
        tvLocation = findViewById(R.id.tvLocation);
        tvStatus = findViewById(R.id.tvStatus);
        rvBids = findViewById(R.id.rvBids);
        emptyState = findViewById(R.id.emptyState);
        btnEdit = findViewById(R.id.btnEdit);
        btnShare = findViewById(R.id.btnShare);
        btnSortBids = findViewById(R.id.btnSortBids);
        btnSubmitBid = findViewById(R.id.btnSubmitBid);

        // Create ProgressBar programmatically
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        bidList = new ArrayList<>();

        // Create adapter with empty jobClientId initially (will be updated when job loads)
        bidAdapter = new BidAdapter(this, bidList, currentUserId, "", new BidAdapter.OnBidActionListener() {
            @Override
            public void onAcceptBid(Bid bid) {
                showAcceptBidDialog(bid);
            }

            @Override
            public void onRejectBid(Bid bid) {
                showRejectBidDialog(bid);
            }

            @Override
            public void onViewProfile(Bid bid) {
                viewContractorProfile(bid.getContractorId());
            }

            @Override
            public void onContactContractor(Bid bid) {
                contactContractor(bid.getContractorId());
            }
        });

        rvBids.setLayoutManager(new LinearLayoutManager(this));
        rvBids.setAdapter(bidAdapter);
    }

    private void updateAdapterWithJobOwner(String jobClientId) {
        // Recreate adapter with correct jobClientId
        bidAdapter = new BidAdapter(this, bidList, currentUserId, jobClientId, new BidAdapter.OnBidActionListener() {
            @Override
            public void onAcceptBid(Bid bid) {
                showAcceptBidDialog(bid);
            }

            @Override
            public void onRejectBid(Bid bid) {
                showRejectBidDialog(bid);
            }

            @Override
            public void onViewProfile(Bid bid) {
                viewContractorProfile(bid.getContractorId());
            }

            @Override
            public void onContactContractor(Bid bid) {
                contactContractor(bid.getContractorId());
            }
        });
        rvBids.setAdapter(bidAdapter);
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> editJob());
        btnShare.setOnClickListener(v -> shareJob());
        btnSortBids.setOnClickListener(v -> showSortDialog());

        if (btnSubmitBid != null) {
            btnSubmitBid.setOnClickListener(v -> submitBid());
        }
    }

    private void loadJobDetails() {
        showLoading(true);

        JobManager.getInstance()
                .getJob(jobId)
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);

                    if (documentSnapshot.exists()) {
                        currentJob = documentSnapshot.toObject(Job.class);
                        if (currentJob != null) {
                            displayJobDetails(currentJob);
                            // Update adapter with job owner ID so only owner can accept/reject bids
                            updateAdapterWithJobOwner(currentJob.getClientId());
                        }
                    } else {
                        Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading job: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void displayJobDetails(Job job) {
        // Set job title
        tvJobTitle.setText(job.getTitle());

        // Set category
        tvCategory.setText(job.getCategory());

        // Set status
        tvStatus.setText(job.getStatus().replace("_", " ").toUpperCase());
        setStatusStyle(job.getStatus());

        // Set posted date
        String dateText = getRelativeTime(job.getPostedDate());
        tvPostedDate.setText("Posted " + dateText);

        // Set description
        tvDescription.setText(job.getDescription());

        // Set budget
        tvBudget.setText("Rs. " + formatCurrency(job.getBudget()));

        // Set timeline
        tvTimeline.setText(job.getTimeline());

        // Set total bids
        tvTotalBids.setText(String.valueOf(job.getTotalBids()));

        // Set location
        tvLocation.setText(job.getLocation());

        // Show/hide buttons based on user role
        boolean isJobOwner = currentUserId.equals(job.getClientId());

        if (isJobOwner) {
            // Client view: show edit, hide submit bid
            btnEdit.setVisibility(View.VISIBLE);
            if (btnSubmitBid != null) {
                btnSubmitBid.setVisibility(View.GONE);
            }
        } else {
            // Contractor view: hide edit, show submit bid
            btnEdit.setVisibility(View.GONE);
            if (btnSubmitBid != null) {
                // Only show submit bid button if job is still open
                if ("open".equals(job.getStatus())) {
                    btnSubmitBid.setVisibility(View.VISIBLE);
                } else {
                    btnSubmitBid.setVisibility(View.GONE);
                }
            }
        }
    }

    private void loadBids() {
        showLoading(true);

        BidManager.getInstance()
                .getBidsByJob(jobId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);

                    bidList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Bid bid = doc.toObject(Bid.class);
                        if (bid != null) {
                            bidList.add(bid);
                        }
                    }

                    // Sort bids
                    sortBids();

                    // Update adapter
                    bidAdapter.notifyDataSetChanged();

                    // Show/hide empty state
                    if (bidList.isEmpty()) {
                        rvBids.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvBids.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading bids: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sortBids() {
        switch (currentSortOrder) {
            case "lowest":
                bidList.sort((b1, b2) -> Double.compare(b1.getBidAmount(), b2.getBidAmount()));
                btnSortBids.setText("Sort by: Lowest");
                break;
            case "highest":
                bidList.sort((b1, b2) -> Double.compare(b2.getBidAmount(), b1.getBidAmount()));
                btnSortBids.setText("Sort by: Highest");
                break;
            case "recent":
                bidList.sort((b1, b2) -> Long.compare(b2.getSubmittedDate(), b1.getSubmittedDate()));
                btnSortBids.setText("Sort by: Recent");
                break;
        }
    }

    private void showSortDialog() {
        String[] options = {"Lowest Price", "Highest Price", "Most Recent"};

        new AlertDialog.Builder(this)
                .setTitle("Sort Bids")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentSortOrder = "lowest";
                            break;
                        case 1:
                            currentSortOrder = "highest";
                            break;
                        case 2:
                            currentSortOrder = "recent";
                            break;
                    }
                    sortBids();
                    bidAdapter.notifyDataSetChanged();
                })
                .show();
    }

    private void showAcceptBidDialog(Bid bid) {
        new AlertDialog.Builder(this)
                .setTitle("Accept Bid")
                .setMessage("Accept bid from " + bid.getContractorName() + " for Rs. " +
                        formatCurrency(bid.getBidAmount()) + "?\n\nThis will reject all other bids and assign the contractor to the job.")
                .setPositiveButton("Accept", (dialog, which) -> acceptBid(bid))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void acceptBid(Bid bid) {
        showLoading(true);

        // Update bid status to accepted
        BidManager.getInstance()
                .acceptBid(bid.getBidId())
                .addOnSuccessListener(aVoid -> {
                    // Reject all other bids
                    BidManager.getInstance().rejectOtherBids(jobId, bid.getBidId());

                    // Assign contractor to job
                    JobManager.getInstance()
                            .assignContractor(jobId, bid.getContractorId(),
                                    bid.getContractorName(), bid.getBidId())
                            .addOnSuccessListener(aVoid2 -> {
                                showLoading(false);

                                Toast.makeText(this, "Bid accepted successfully!",
                                        Toast.LENGTH_SHORT).show();

                                // Reload job and bids
                                loadJobDetails();
                                loadBids();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error assigning contractor: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error accepting bid: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showRejectBidDialog(Bid bid) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Bid")
                .setMessage("Reject bid from " + bid.getContractorName() + "?")
                .setPositiveButton("Reject", (dialog, which) -> rejectBid(bid))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void rejectBid(Bid bid) {
        showLoading(true);

        BidManager.getInstance()
                .rejectBid(bid.getBidId())
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Bid rejected", Toast.LENGTH_SHORT).show();
                    loadBids();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error rejecting bid: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void viewContractorProfile(String contractorId) {
        Intent intent = new Intent(this, ContractorProfileActivity.class);
        intent.putExtra("contractorId", contractorId);
        startActivity(intent);
    }

    private void contactContractor(String contractorId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("receiverId", contractorId);
        startActivity(intent);
    }

    private void editJob() {
        if (currentJob == null) return;

        // Only allow editing if job is still open
        if (!"open".equals(currentJob.getStatus())) {
            Toast.makeText(this, "Can only edit open jobs", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Edit job feature coming soon!", Toast.LENGTH_SHORT).show();
        // TODO: Implement JobEditActivity
        // Intent intent = new Intent(this, JobEditActivity.class);
        // intent.putExtra("jobId", jobId);
        // startActivity(intent);
    }

    private void submitBid() {
        if (currentJob == null) return;

        // Check if job is still open
        if (!"open".equals(currentJob.getStatus())) {
            Toast.makeText(this, "This job is no longer accepting bids", Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to SubmitBidActivity
        Intent intent = new Intent(this, SubmitBidActivity.class);
        intent.putExtra("jobId", jobId);
        startActivity(intent);
    }

    private void shareJob() {
        if (currentJob == null) return;

        String shareText = "Check out this job on RebuildPak:\n\n" +
                currentJob.getTitle() + "\n" +
                "Budget: Rs. " + formatCurrency(currentJob.getBudget()) + "\n" +
                "Location: " + currentJob.getLocation() + "\n" +
                "Category: " + currentJob.getCategory();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Job"));
    }

    private void setStatusStyle(String status) {
        int color;

        switch (status.toLowerCase()) {
            case "open":
                color = 0xFF4CAF50; // Green
                break;
            case "in_progress":
                color = 0xFFFFA726; // Orange
                break;
            case "completed":
                color = 0xFF2196F3; // Blue
                break;
            case "cancelled":
                color = 0xFFF44336; // Red
                break;
            default:
                color = 0xFF757575; // Grey
                break;
        }

        tvStatus.setTextColor(color);
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

    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    private void showLoading(boolean show) {
        // Implement loading indicator
        // You can add a ProgressBar to your layout or use a loading dialog
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}