package com.example.madproject.firebase;

import com.example.madproject.models.Contract;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ContractManager {
    private static ContractManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "contracts";

    private ContractManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized ContractManager getInstance() {
        if (instance == null) {
            instance = new ContractManager();
        }
        return instance;
    }

    // CREATE - Save new contract
    public Task<Void> createContract(Contract contract) {
        return db.collection(COLLECTION_NAME)
                .document(contract.getContractId())
                .set(contract);
    }

    // READ - Get single contract by ID
    public Task<DocumentSnapshot> getContract(String contractId) {
        return db.collection(COLLECTION_NAME)
                .document(contractId)
                .get();
    }

    // READ - Get contracts for a job
    public Task<QuerySnapshot> getContractsByJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .get();
    }

    // UPDATE - Update specific field
    public Task<Void> updateField(String contractId, String field, Object value) {
        return db.collection(COLLECTION_NAME)
                .document(contractId)
                .update(field, value);
    }

    // UPDATE - Set PDF download URL
    public Task<Void> setPdfUrl(String contractId, String url) {
        return updateField(contractId, "pdfUrl", url);
    }

    // UPDATE - Mark email sent for client or contractor
    public Task<Void> markEmailSent(String contractId, boolean forClient) {
        return updateField(contractId, forClient ? "clientEmailSent" : "contractorEmailSent", true);
    }
}
