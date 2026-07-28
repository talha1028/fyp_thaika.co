package com.example.madproject.helpers;

import android.content.Context;
import android.util.Log;

import com.example.madproject.firebase.ContractManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Bid;
import com.example.madproject.models.Contract;
import com.example.madproject.models.Job;
import com.example.madproject.models.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Owns the full chain triggered when a bid is accepted: generate contract text (Gemini),
 * render it to PDF, upload to Storage, persist the Contract doc, then email both parties
 * a download link (via the "mail" collection / Trigger Email extension).
 */
public class ContractOrchestrator {
    private static final String TAG = "ContractOrchestrator";

    private final GeminiAIHelper aiHelper;
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();

    public ContractOrchestrator(Context context) {
        this.aiHelper = new GeminiAIHelper(context);
    }

    /** Call from the owning Activity's onDestroy() to release the underlying AI helper's thread. */
    public void shutdown() {
        aiHelper.shutdown();
    }

    /** Reports the outcome of an on-demand contract generation (see generateAndSendContract(Job, listener)). */
    public interface ContractResultListener {
        void onContractReady(String pdfUrl);
        void onError(String error);
    }

    /** Entry point — call right after assignContractor() succeeds. Fire-and-forget from caller's perspective. */
    public void generateAndSendContract(Job job, Bid bid) {
        generateAndSendContract(job, bid.getContractorId(), bid.getContractorName(), bid.getBidAmount(), null);
    }

    /**
     * On-demand entry point — call from a UI action (e.g. a "Generate Contract" button) once a
     * contractor is already assigned. Uses the job's own assigned-contractor/accepted-bid fields
     * instead of requiring a separate Bid lookup, and reports back via listener so the caller can
     * show the resulting PDF immediately instead of only relying on the emailed link.
     */
    public void generateAndSendContract(Job job, ContractResultListener listener) {
        double amount = job.getAcceptedBidAmount() > 0 ? job.getAcceptedBidAmount() : job.getBudget();
        generateAndSendContract(job, job.getAssignedContractorId(), job.getAssignedContractorName(), amount, listener);
    }

    private void generateAndSendContract(Job job, String contractorId, String contractorName,
                                          double amount, ContractResultListener listener) {
        String contractId = "contract_" + UUID.randomUUID();

        UserManager.getInstance().getUserObject(job.getClientId(), new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User client) {
                String clientName = client != null ? client.getFullName() : job.getClientName();
                aiHelper.generateContract(job.getTitle(), clientName, contractorName,
                        job.getDescription(), amount, job.getTimeline(), job.getLocation(),
                        new GeminiAIHelper.AIResponseListener() {
                            @Override
                            public void onResponse(String contractText) {
                                onContractTextReady(contractId, job, contractorId, client, contractText, listener);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Gemini contract generation failed: " + error);
                                if (listener != null) listener.onError(error);
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Client lookup failed: " + error);
                if (listener != null) listener.onError(error);
            }
        });
    }

    private void onContractTextReady(String contractId, Job job, String contractorId, User client,
                                      String contractText, ContractResultListener listener) {
        bgExecutor.execute(() -> {
            byte[] pdfBytes;
            try {
                pdfBytes = PdfHelper.generateContractPdf(job.getTitle(), contractText);
            } catch (Exception e) {
                Log.e(TAG, "PDF generation failed", e);
                if (listener != null) listener.onError("PDF generation failed: " + e.getMessage());
                return;
            }
            uploadPdf(contractId, job, contractorId, client, contractText, pdfBytes, listener);
        });
    }

    private void uploadPdf(String contractId, Job job, String contractorId, User client,
                            String contractText, byte[] pdfBytes, ContractResultListener listener) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("contracts/" + contractId + ".pdf");

        ref.putBytes(pdfBytes)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    Contract contract = new Contract(contractId, job.getJobId(), job.getTitle(),
                            job.getClientId(), contractorId, contractText);
                    contract.setPdfUrl(downloadUri.toString());

                    ContractManager.getInstance().createContract(contract)
                            .addOnSuccessListener(v -> {
                                if (listener != null) listener.onContractReady(downloadUri.toString());
                                sendEmails(contractId, job, contractorId, client, downloadUri.toString());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Contract save failed", e);
                                if (listener != null) listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "PDF upload failed", e);
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    private void sendEmails(String contractId, Job job, String contractorId, User client, String pdfUrl) {
        if (client != null && client.getEmail() != null) {
            EmailHelper.sendContractEmail(client.getEmail(), job.getTitle(), pdfUrl)
                    .addOnSuccessListener(v -> ContractManager.getInstance().markEmailSent(contractId, true))
                    .addOnFailureListener(e -> Log.e(TAG, "Client email failed", e));
        }

        UserManager.getInstance().getUserObject(contractorId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User contractor) {
                if (contractor != null && contractor.getEmail() != null) {
                    EmailHelper.sendContractEmail(contractor.getEmail(), job.getTitle(), pdfUrl)
                            .addOnSuccessListener(v -> ContractManager.getInstance().markEmailSent(contractId, false))
                            .addOnFailureListener(e -> Log.e(TAG, "Contractor email failed", e));
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Contractor lookup failed: " + error);
            }
        });
    }
}
