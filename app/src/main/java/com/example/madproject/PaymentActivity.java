package com.example.madproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.madproject.firebase.JobManager;
import com.example.madproject.firebase.PaymentManager;
import com.example.madproject.models.Job;
import com.example.madproject.models.Payment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private static final double DEPOSIT_PERCENT = 0.30;
    private static final double FINAL_PERCENT   = 0.70;

    // Instructions per method
    private static final String JAZZCASH_NUMBER    = "0312-1234567";
    private static final String EASYPAISA_NUMBER   = "0300-9876543";
    private static final String BANK_ACCOUNT       = "MCB Bank\nAccount: 1234-5678901-2\nIBAN: PK36MUCB0001234567890123";

    private TextView tvPaymentTypeBadge, tvJobTitle, tvContractorName,
            tvAmount, tvAmountBreakdown, tvInstructions;
    private CardView cardJazzCash, cardEasyPaisa, cardBankTransfer, cardCash, instructionsCard;
    private RadioButton rbJazzCash, rbEasyPaisa, rbBankTransfer, rbCash;
    private EditText etTransactionRef;
    private Button btnConfirmPayment;

    private String currentUserId;
    private String jobId;
    private Job currentJob;
    private String selectedMethod = null; // "jazzcash","easypaisa","bank_transfer","cash"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        jobId = getIntent().getStringExtra("jobId");
        if (jobId == null || jobId.isEmpty()) {
            Toast.makeText(this, "Error: Job not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadJob();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle("");
            }

        tvPaymentTypeBadge = findViewById(R.id.tvPaymentTypeBadge);
        tvJobTitle         = findViewById(R.id.tvJobTitle);
        tvContractorName   = findViewById(R.id.tvContractorName);
        tvAmount           = findViewById(R.id.tvAmount);
        tvAmountBreakdown  = findViewById(R.id.tvAmountBreakdown);
        tvInstructions     = findViewById(R.id.tvInstructions);
        instructionsCard   = findViewById(R.id.instructionsCard);
        etTransactionRef   = findViewById(R.id.etTransactionRef);
        btnConfirmPayment  = findViewById(R.id.btnConfirmPayment);

        cardJazzCash    = findViewById(R.id.cardJazzCash);
        cardEasyPaisa   = findViewById(R.id.cardEasyPaisa);
        cardBankTransfer = findViewById(R.id.cardBankTransfer);
        cardCash        = findViewById(R.id.cardCash);

        rbJazzCash    = findViewById(R.id.rbJazzCash);
        rbEasyPaisa   = findViewById(R.id.rbEasyPaisa);
        rbBankTransfer = findViewById(R.id.rbBankTransfer);
        rbCash        = findViewById(R.id.rbCash);

        cardJazzCash.setOnClickListener(v -> selectMethod("jazzcash"));
        cardEasyPaisa.setOnClickListener(v -> selectMethod("easypaisa"));
        cardBankTransfer.setOnClickListener(v -> selectMethod("bank_transfer"));
        cardCash.setOnClickListener(v -> selectMethod("cash"));

        btnConfirmPayment.setOnClickListener(v -> confirmPayment());
    }

    private void loadJob() {
        btnConfirmPayment.setEnabled(false);

        JobManager.getInstance()
                .getJob(jobId)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentJob = doc.toObject(Job.class);
                    if (currentJob == null) { finish(); return; }

                    if (!currentUserId.equals(currentJob.getClientId())) {
                        Toast.makeText(this, "Only the job owner can make payments",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    populateSummary();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading job: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateSummary() {
        String paymentType = resolvePaymentType();
        if (paymentType == null) {
            Toast.makeText(this, "No payment currently due for this job",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        double total  = currentJob.getAcceptedBidAmount();
        double amount = paymentType.equals("deposit")
                ? total * DEPOSIT_PERCENT
                : total * FINAL_PERCENT;

        tvJobTitle.setText(currentJob.getTitle());
        tvContractorName.setText("Contractor: " + currentJob.getAssignedContractorName());
        tvAmount.setText(formatRs(amount));

        if (paymentType.equals("deposit")) {
            tvPaymentTypeBadge.setText("DEPOSIT (30%)");
            tvAmountBreakdown.setText("30% of total " + formatRs(total));
        } else {
            tvPaymentTypeBadge.setText("FINAL PAYMENT (70%)");
            tvAmountBreakdown.setText("70% of total " + formatRs(total));
        }
    }

    /**
     * Returns "deposit" if deposit is due, "final" if final is due, null if nothing due.
     */
    private String resolvePaymentType() {
        if (currentJob == null) return null;
        String status = currentJob.getStatus();
        if (("in_progress".equals(status) || "completed".equals(status))
                && !currentJob.isDepositPaid()) {
            return "deposit";
        }
        if ("completed".equals(status) && currentJob.isDepositPaid()
                && !currentJob.isFinalPaid()) {
            return "final";
        }
        return null;
    }

    private void selectMethod(String method) {
        selectedMethod = method;

        rbJazzCash.setChecked(method.equals("jazzcash"));
        rbEasyPaisa.setChecked(method.equals("easypaisa"));
        rbBankTransfer.setChecked(method.equals("bank_transfer"));
        rbCash.setChecked(method.equals("cash"));

        // Highlight selected card
        int selectedBg = 0xFFF3E5FF;
        int defaultBg  = 0xFFFFFFFF;
        cardJazzCash.setCardBackgroundColor(method.equals("jazzcash")    ? selectedBg : defaultBg);
        cardEasyPaisa.setCardBackgroundColor(method.equals("easypaisa")   ? selectedBg : defaultBg);
        cardBankTransfer.setCardBackgroundColor(method.equals("bank_transfer") ? selectedBg : defaultBg);
        cardCash.setCardBackgroundColor(method.equals("cash")             ? selectedBg : defaultBg);

        // Show instructions for digital methods
        if (method.equals("cash")) {
            instructionsCard.setVisibility(View.GONE);
        } else {
            instructionsCard.setVisibility(View.VISIBLE);
            double total  = currentJob.getAcceptedBidAmount();
            String paymentType = resolvePaymentType();
            double amount = (paymentType != null && paymentType.equals("deposit"))
                    ? total * DEPOSIT_PERCENT : total * FINAL_PERCENT;

            switch (method) {
                case "jazzcash":
                    tvInstructions.setText(
                            "Send " + formatRs(amount) + " to JazzCash number:\n" +
                            JAZZCASH_NUMBER + "\n\nSend money → Enter amount → Confirm. " +
                            "Then enter the transaction ID below.");
                    etTransactionRef.setHint("JazzCash transaction ID");
                    break;
                case "easypaisa":
                    tvInstructions.setText(
                            "Send " + formatRs(amount) + " to EasyPaisa number:\n" +
                            EASYPAISA_NUMBER + "\n\nSend money → Enter amount → Confirm. " +
                            "Then enter the transaction ID below.");
                    etTransactionRef.setHint("EasyPaisa transaction ID");
                    break;
                case "bank_transfer":
                    tvInstructions.setText(
                            "Transfer " + formatRs(amount) + " to:\n\n" +
                            BANK_ACCOUNT + "\n\nThen enter your bank reference number below.");
                    etTransactionRef.setHint("Bank reference / UTR number");
                    break;
            }
        }

        btnConfirmPayment.setEnabled(true);
        btnConfirmPayment.setAlpha(1.0f);
    }

    private void confirmPayment() {
        if (selectedMethod == null) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        String ref = "";
        if (!selectedMethod.equals("cash")) {
            ref = etTransactionRef.getText().toString().trim();
            if (TextUtils.isEmpty(ref)) {
                etTransactionRef.setError("Transaction reference is required");
                etTransactionRef.requestFocus();
                return;
            }
        }

        String paymentType = resolvePaymentType();
        if (paymentType == null) return;

        double total  = currentJob.getAcceptedBidAmount();
        double amount = paymentType.equals("deposit")
                ? total * DEPOSIT_PERCENT
                : total * FINAL_PERCENT;

        btnConfirmPayment.setEnabled(false);
        btnConfirmPayment.setText("Processing...");

        String paymentId = "pay_" + UUID.randomUUID().toString();
        Payment payment = new Payment(
                paymentId, jobId, currentJob.getTitle(),
                currentUserId, currentJob.getAssignedContractorId(),
                currentJob.getAssignedContractorName(),
                amount, paymentType, selectedMethod, ref);

        final String finalRef = ref;
        final String finalPaymentType = paymentType;

        PaymentManager.getInstance()
                .createPayment(payment)
                .addOnSuccessListener(aVoid -> {
                    // Mark flag on job
                    String field = finalPaymentType.equals("deposit") ? "depositPaid" : "finalPaid";
                    JobManager.getInstance()
                            .updateField(jobId, field, true)
                            .addOnSuccessListener(aVoid2 -> {
                                showSuccess(finalPaymentType, amount, selectedMethod);
                            })
                            .addOnFailureListener(e -> {
                                // Payment recorded but flag update failed — not catastrophic
                                showSuccess(finalPaymentType, amount, selectedMethod);
                            });
                })
                .addOnFailureListener(e -> {
                    btnConfirmPayment.setEnabled(true);
                    btnConfirmPayment.setText("Confirm Payment");
                    Toast.makeText(this, "Payment failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showSuccess(String type, double amount, String method) {
        String label = type.equals("deposit") ? "Deposit" : "Final payment";
        String methodLabel = method.equals("jazzcash") ? "JazzCash"
                : method.equals("easypaisa") ? "EasyPaisa"
                : method.equals("bank_transfer") ? "Bank Transfer" : "Cash";

        Toast.makeText(this,
                label + " of " + formatRs(amount) + " via " + methodLabel + " recorded!",
                Toast.LENGTH_LONG).show();

        setResult(RESULT_OK);
        finish();
    }

    private String formatRs(double amount) {
        if (amount >= 100000) {
            return String.format(Locale.getDefault(), "Rs. %.1f L", amount / 100000);
        } else if (amount >= 1000) {
            return String.format(Locale.getDefault(), "Rs. %,.0f", amount);
        }
        return String.format(Locale.getDefault(), "Rs. %.0f", amount);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
