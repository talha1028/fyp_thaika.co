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

    /** Entry point — call right after assignContractor() succeeds. Fire-and-forget from caller's perspective. */
    public void generateAndSendContract(Job job, Bid bid) {
        String contractId = "contract_" + UUID.randomUUID();

        UserManager.getInstance().getUserObject(job.getClientId(), new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User client) {
                String clientName = client != null ? client.getFullName() : job.getClientName();
                aiHelper.generateContract(job.getTitle(), clientName, bid.getContractorName(),
                        job.getDescription(), bid.getBidAmount(), job.getTimeline(), job.getLocation(),
                        new GeminiAIHelper.AIResponseListener() {
                            @Override
                            public void onResponse(String contractText) {
                                onContractTextReady(contractId, job, bid, client, contractText);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Gemini contract generation failed: " + error);
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Client lookup failed: " + error);
            }
        });
    }

    private void onContractTextReady(String contractId, Job job, Bid bid, User client, String contractText) {
        bgExecutor.execute(() -> {
            byte[] pdfBytes;
            try {
                pdfBytes = PdfHelper.generateContractPdf(job.getTitle(), contractText);
            } catch (Exception e) {
                Log.e(TAG, "PDF generation failed", e);
                return;
            }
            uploadPdf(contractId, job, bid, client, contractText, pdfBytes);
        });
    }

    private void uploadPdf(String contractId, Job job, Bid bid, User client, String contractText, byte[] pdfBytes) {
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
                            job.getClientId(), bid.getContractorId(), contractText);
                    contract.setPdfUrl(downloadUri.toString());

                    ContractManager.getInstance().createContract(contract)
                            .addOnSuccessListener(v -> sendEmails(contractId, job, bid, client, downloadUri.toString()))
                            .addOnFailureListener(e -> Log.e(TAG, "Contract save failed", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "PDF upload failed", e));
    }

    private void sendEmails(String contractId, Job job, Bid bid, User client, String pdfUrl) {
        if (client != null && client.getEmail() != null) {
            EmailHelper.sendContractEmail(client.getEmail(), job.getTitle(), pdfUrl)
                    .addOnSuccessListener(v -> ContractManager.getInstance().markEmailSent(contractId, true))
                    .addOnFailureListener(e -> Log.e(TAG, "Client email failed", e));
        }

        UserManager.getInstance().getUserObject(bid.getContractorId(), new UserManager.OnUserLoadedListener() {
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
