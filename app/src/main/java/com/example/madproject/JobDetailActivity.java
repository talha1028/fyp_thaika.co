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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.BidAdapter;
import com.example.madproject.firebase.BidManager;
import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.NotificationManager;
import com.example.madproject.firebase.ReviewManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.helpers.ContractOrchestrator;
import com.example.madproject.helpers.GeminiAIHelper;
import com.example.madproject.models.Bid;
import com.example.madproject.models.Job;
import com.example.madproject.models.Notification;
import com.example.madproject.models.Review;
import com.example.madproject.views.ShimmerLayout;
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
    private ShimmerLayout shimmerJobInfo, shimmerBids;
    private ImageView btnEdit;
    private Button btnSubmitBid;
    private ProgressBar progressBar;

    // Payment card
    private androidx.cardview.widget.CardView paymentCard;
    private TextView tvPaymentLabel, tvPaymentAmount;
    private Button btnPayNow;

    // Mark complete / review
    private Button btnMarkComplete;
    private Button btnWriteReview;
    private Button btnGenerateContract;
    private Button btnBudgetTips;
    private GeminiAIHelper aiHelper;
    private ContractOrchestrator contractOrchestrator;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String jobId;
    private Job currentJob;

    private BidAdapter bidAdapter;
    private List<Bid> bidList;
    private String currentSortOrder = "lowest"; // "lowest", "highest", "recent"

    private final ActivityResultLauncher<Intent> editJobLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadJobDetails();
                }
            });

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
            getSupportActionBar().setTitle("");
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
        shimmerJobInfo = findViewById(R.id.shimmerJobInfo);
        shimmerBids = findViewById(R.id.shimmerBids);
        btnEdit = findViewById(R.id.btnEdit);
        btnSortBids = findViewById(R.id.btnSortBids);
        btnSubmitBid = findViewById(R.id.btnSubmitBid);

        // Create ProgressBar programmatically
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        paymentCard     = findViewById(R.id.paymentCard);
        tvPaymentLabel  = findViewById(R.id.tvPaymentLabel);
        tvPaymentAmount = findViewById(R.id.tvPaymentAmount);
        btnPayNow       = findViewById(R.id.btnPayNow);
        btnMarkComplete     = findViewById(R.id.btnMarkComplete);
        btnWriteReview      = findViewById(R.id.btnWriteReview);
        btnGenerateContract = findViewById(R.id.btnGenerateContract);
        btnBudgetTips       = findViewById(R.id.btnBudgetTips);
        aiHelper = new GeminiAIHelper(this);
        contractOrchestrator = new ContractOrchestrator(this);
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
                contactContractor(bid.getContractorId(), bid.getContractorName());
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
                contactContractor(bid.getContractorId(), bid.getContractorName());
            }
        });
        rvBids.setAdapter(bidAdapter);
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> editJob());
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
                    // Never leave the skeleton shimmering forever on failure.
                    finishJobInfoSkeleton();
                    Toast.makeText(this, "Error loading job: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /** Drop the grey placeholder bars once a field has its real value. */
    private void clearSkeleton(TextView... views) {
        for (TextView v : views) {
            v.setBackground(null);
            v.setMinWidth(0);
            v.setMinHeight(0);
        }
    }

    /**
     * Stop the job-info skeleton. tvStatus is handled separately: its skeleton replaced the
     * bg_status_green pill in XML, so it has to be put back rather than nulled out.
     */
    private void finishJobInfoSkeleton() {
        clearSkeleton(tvJobTitle, tvCategory, tvPostedDate, tvDescription,
                tvBudget, tvTimeline, tvTotalBids, tvLocation);
        tvStatus.setMinWidth(0);
        tvStatus.setMinHeight(0);
        tvStatus.setBackgroundResource(R.drawable.bg_status_green);
        shimmerJobInfo.hideShimmer();
    }

    private void showBidsLoading(boolean show) {
        if (show) {
            shimmerBids.setVisibility(View.VISIBLE);
            shimmerBids.showShimmer();
            rvBids.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            shimmerBids.hideShimmer();
            shimmerBids.setVisibility(View.GONE);
        }
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

        finishJobInfoSkeleton();

        // Show/hide buttons based on user role
        boolean isJobOwner = currentUserId.equals(job.getClientId());
        String status = job.getStatus();

        if (isJobOwner) {
            btnEdit.setVisibility("open".equals(status) ? View.VISIBLE : View.GONE);
            if (btnSubmitBid != null) btnSubmitBid.setVisibility(View.GONE);
            updatePaymentCard(job);

            // Mark complete: visible when in_progress and both payments handled or no bid amount
            boolean canComplete = "in_progress".equals(status) &&
                    (job.getAcceptedBidAmount() == 0 || job.isDepositPaid());
            if (btnMarkComplete != null) {
                btnMarkComplete.setVisibility(canComplete ? View.VISIBLE : View.GONE);
                btnMarkComplete.setOnClickListener(v -> showMarkCompleteDialog());
            }

            // Write review: visible when completed, contractor assigned, and not yet reviewed
            if (btnWriteReview != null) {
                boolean jobDone = "completed".equals(status) && job.getAssignedContractorId() != null
                        && !job.getAssignedContractorId().isEmpty();
                btnWriteReview.setVisibility(jobDone ? View.VISIBLE : View.GONE);
                btnWriteReview.setOnClickListener(v -> showReviewDialog());
            }

            // Contract: visible when job is in_progress (bid accepted)
            if (btnGenerateContract != null) {
                boolean contractReady = "in_progress".equals(status) || "completed".equals(status);
                btnGenerateContract.setVisibility(contractReady ? View.VISIBLE : View.GONE);
                btnGenerateContract.setOnClickListener(v -> generateContract());
            }

            // Budget tips: visible for open and in_progress jobs
            if (btnBudgetTips != null) {
                boolean showTips = "open".equals(status) || "in_progress".equals(status);
                btnBudgetTips.setVisibility(showTips ? View.VISIBLE : View.GONE);
                btnBudgetTips.setOnClickListener(v -> showBudgetTips());
            }
        } else {
            btnEdit.setVisibility(View.GONE);
            if (btnSubmitBid != null) {
                btnSubmitBid.setVisibility("open".equals(status) ? View.VISIBLE : View.GONE);
            }
            if (paymentCard != null) paymentCard.setVisibility(View.GONE);
            if (btnMarkComplete != null) btnMarkComplete.setVisibility(View.GONE);
            if (btnWriteReview != null) btnWriteReview.setVisibility(View.GONE);
            if (btnGenerateContract != null) btnGenerateContract.setVisibility(View.GONE);
            if (btnBudgetTips != null) btnBudgetTips.setVisibility(View.GONE);
        }
    }

    private void updatePaymentCard(Job job) {
        if (paymentCard == null) return;
        double total = job.getAcceptedBidAmount();
        String status = job.getStatus();

        boolean depositDue = ("in_progress".equals(status) || "completed".equals(status))
                && !job.isDepositPaid() && total > 0;
        boolean finalDue   = "completed".equals(status)
                && job.isDepositPaid() && !job.isFinalPaid() && total > 0;

        if (depositDue) {
            paymentCard.setVisibility(View.VISIBLE);
            tvPaymentLabel.setText("Deposit Due (30%)");
            tvPaymentAmount.setText(formatRs(total * 0.30));
            btnPayNow.setOnClickListener(v -> launchPayment());
        } else if (finalDue) {
            paymentCard.setVisibility(View.VISIBLE);
            tvPaymentLabel.setText("Final Payment Due (70%)");
            tvPaymentAmount.setText(formatRs(total * 0.70));
            btnPayNow.setOnClickListener(v -> launchPayment());
        } else {
            paymentCard.setVisibility(View.GONE);
        }
    }

    private void launchPayment() {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("jobId", jobId);
        startActivity(intent);
    }

    private String formatRs(double amount) {
        if (amount >= 100000) return String.format("Rs. %.1f L", amount / 100000);
        if (amount >= 1000)   return String.format("Rs. %,.0f", amount);
        return String.format("Rs. %.0f", amount);
    }

    private void loadBids() {
        showBidsLoading(true);

        BidManager.getInstance()
                .getBidsByJob(jobId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showBidsLoading(false);

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
                    showBidsLoading(false);
                    // Resolve the section on the error path too, otherwise the skeleton goes away
                    // and leaves nothing behind it.
                    rvBids.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
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
                                    bid.getContractorName(), bid.getBidId(), bid.getBidAmount())
                            .addOnSuccessListener(aVoid2 -> {
                                showLoading(false);
                                Toast.makeText(this, "Bid accepted successfully!", Toast.LENGTH_SHORT).show();

                                // Notify contractor
                                String notifId = "notif_" + System.currentTimeMillis();
                                String jobTitle = currentJob != null ? currentJob.getTitle() : "a job";
                                Notification notif = new Notification(notifId, bid.getContractorId(),
                                        "Bid Accepted!",
                                        "Your bid for \"" + jobTitle + "\" was accepted. Get started!",
                                        "bid", jobId);
                                NotificationManager.getInstance().createNotification(notif);

                                contractOrchestrator.generateAndSendContract(currentJob, bid);

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

    private void showMarkCompleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mark Job Complete")
                .setMessage("Mark this job as completed? This will notify the contractor and trigger the final payment.")
                .setPositiveButton("Mark Complete", (d, w) -> markJobComplete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markJobComplete() {
        showLoading(true);
        JobManager.getInstance()
                .completeJob(jobId)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Job marked as completed!", Toast.LENGTH_SHORT).show();

                    // Notify contractor
                    if (currentJob != null && currentJob.getAssignedContractorId() != null) {
                        String notifId = "notif_" + System.currentTimeMillis();
                        Notification notif = new Notification(notifId,
                                currentJob.getAssignedContractorId(),
                                "Job Completed",
                                "\"" + currentJob.getTitle() + "\" has been marked complete. Final payment incoming!",
                                "job", jobId);
                        NotificationManager.getInstance().createNotification(notif);
                    }

                    loadJobDetails();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showReviewDialog() {
        if (currentJob == null) return;

        // Check if already reviewed
        ReviewManager.getInstance()
                .getReviewsByJob(jobId)
                .addOnSuccessListener(snap -> {
                    boolean alreadyReviewed = false;
                    for (DocumentSnapshot doc : snap) {
                        Review r = doc.toObject(Review.class);
                        if (r != null && currentUserId.equals(r.getClientId())) {
                            alreadyReviewed = true;
                            break;
                        }
                    }
                    if (alreadyReviewed) {
                        Toast.makeText(this, "You already reviewed this contractor", Toast.LENGTH_SHORT).show();
                        if (btnWriteReview != null) btnWriteReview.setVisibility(View.GONE);
                        return;
                    }
                    launchReviewDialog();
                });
    }

    private void launchReviewDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);

        final float[] selectedRating = {5f};

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        // Rating label
        android.widget.TextView tvRatingLabel = new android.widget.TextView(this);
        tvRatingLabel.setText("Rating (1–5 stars)");
        tvRatingLabel.setTextSize(14);
        tvRatingLabel.setTextColor(0xFF212121);
        layout.addView(tvRatingLabel);

        // Rating bar
        android.widget.RatingBar ratingBar = new android.widget.RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);
        ratingBar.setRating(5f);
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> selectedRating[0] = Math.max(1, rating));
        layout.addView(ratingBar);

        // Review text
        android.widget.TextView tvReviewLabel = new android.widget.TextView(this);
        tvReviewLabel.setText("Review");
        tvReviewLabel.setTextSize(14);
        tvReviewLabel.setTextColor(0xFF212121);
        tvReviewLabel.setPadding(0, 24, 0, 8);
        layout.addView(tvReviewLabel);

        android.widget.EditText etReview = new android.widget.EditText(this);
        etReview.setHint("Share your experience with this contractor...");
        etReview.setMinLines(3);
        etReview.setMaxLines(5);
        etReview.setGravity(android.view.Gravity.TOP);
        etReview.setBackground(null);
        etReview.setPadding(0, 8, 0, 8);
        layout.addView(etReview);

        new AlertDialog.Builder(this)
                .setTitle("Rate Contractor")
                .setView(layout)
                .setPositiveButton("Submit Review", (d, w) -> {
                    String reviewText = etReview.getText().toString().trim();
                    if (reviewText.isEmpty()) {
                        Toast.makeText(this, "Please write a review", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitReview(selectedRating[0], reviewText);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReview(float rating, String reviewText) {
        if (currentJob == null) return;
        showLoading(true);

        // Get current user name first
        UserManager.getInstance().getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(com.example.madproject.models.User user) {
                String reviewId = "review_" + System.currentTimeMillis();
                Review review = new Review(reviewId,
                        currentJob.getAssignedContractorId(),
                        currentJob.getAssignedContractorName(),
                        currentUserId,
                        user.getFullName(),
                        jobId,
                        currentJob.getTitle(),
                        rating,
                        reviewText);

                ReviewManager.getInstance().createReview(review)
                        .addOnSuccessListener(aVoid -> {
                            // Update contractor's average rating
                            ReviewManager.getInstance().calculateAverageRating(
                                    currentJob.getAssignedContractorId(),
                                    (avg, count) -> UserManager.getInstance()
                                            .updateRating(currentJob.getAssignedContractorId(), avg, count));

                            showLoading(false);
                            Toast.makeText(JobDetailActivity.this, "Review submitted!", Toast.LENGTH_SHORT).show();
                            if (btnWriteReview != null) btnWriteReview.setVisibility(View.GONE);
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            Toast.makeText(JobDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(JobDetailActivity.this, "Could not load user: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateContract() {
        if (currentJob == null) return;
        btnGenerateContract.setEnabled(false);
        btnGenerateContract.setText("Generating...");

        UserManager.getInstance().getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(com.example.madproject.models.User user) {
                String clientName = user != null ? user.getFullName() : "Client";
                String contractorName = currentJob.getAssignedContractorName() != null ?
                        currentJob.getAssignedContractorName() : "Contractor";

                aiHelper.generateContract(
                        currentJob.getTitle(),
                        clientName,
                        contractorName,
                        currentJob.getDescription(),
                        currentJob.getAcceptedBidAmount() > 0 ? currentJob.getAcceptedBidAmount() : currentJob.getBudget(),
                        currentJob.getTimeline(),
                        currentJob.getLocation(),
                        new GeminiAIHelper.AIResponseListener() {
                            @Override
                            public void onResponse(String response) {
                                runOnUiThread(() -> {
                                    btnGenerateContract.setEnabled(true);
                                    btnGenerateContract.setText("📄 Generate Contract");
                                    showContractDialog(response);
                                });
                            }
                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    btnGenerateContract.setEnabled(true);
                                    btnGenerateContract.setText("📄 Generate Contract");
                                    Toast.makeText(JobDetailActivity.this, "AI error: " + error, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnGenerateContract.setEnabled(true);
                    btnGenerateContract.setText("📄 Generate Contract");
                });
            }
        });
    }

    private void showContractDialog(String contractText) {
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(contractText);
        tv.setTextSize(13f);
        tv.setTextColor(0xFF212121);
        tv.setPadding(48, 32, 48, 32);
        tv.setLineSpacing(4f, 1f);
        sv.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("📄 Construction Contract")
                .setView(sv)
                .setPositiveButton("Share", (d, w) -> {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_SUBJECT, "Contract — " + (currentJob != null ? currentJob.getTitle() : ""));
                    share.putExtra(Intent.EXTRA_TEXT, contractText);
                    startActivity(Intent.createChooser(share, "Share Contract"));
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showBudgetTips() {
        if (currentJob == null) return;
        btnBudgetTips.setEnabled(false);
        btnBudgetTips.setText("Analyzing...");

        aiHelper.getBudgetOptimizationTips(
                currentJob.getCategory() + " — " + currentJob.getTitle(),
                currentJob.getDescription(),
                currentJob.getBudget(),
                currentJob.getLocation(),
                currentJob.getTimeline(),
                currentJob.getTotalBids(),
                new GeminiAIHelper.AIResponseListener() {
                    @Override
                    public void onResponse(String response) {
                        runOnUiThread(() -> {
                            btnBudgetTips.setEnabled(true);
                            btnBudgetTips.setText("💡 Budget Optimization");
                            new AlertDialog.Builder(JobDetailActivity.this)
                                    .setTitle("💡 Budget Optimization Tips")
                                    .setMessage(response.trim())
                                    .setPositiveButton("Got it", null)
                                    .show();
                        });
                    }
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnBudgetTips.setEnabled(true);
                            btnBudgetTips.setText("💡 Budget Optimization");
                            Toast.makeText(JobDetailActivity.this, "AI error: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
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
        contactContractor(contractorId, null);
    }

    private void contactContractor(String contractorId, String contractorName) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("receiverId", contractorId);
        if (contractorName != null && !contractorName.isEmpty()) {
            intent.putExtra("receiverName", contractorName);
        }
        startActivity(intent);
    }

    private void editJob() {
        if (currentJob == null) return;

        // Only allow editing if job is still open
        if (!"open".equals(currentJob.getStatus())) {
            Toast.makeText(this, "Can only edit open jobs", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, JobEditActivity.class);
        intent.putExtra("jobId", jobId);
        editJobLauncher.launch(intent);
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