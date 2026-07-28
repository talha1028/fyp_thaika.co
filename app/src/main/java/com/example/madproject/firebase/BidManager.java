package com.example.madproject.firebase;

import com.example.madproject.models.Bid;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class BidManager {
    private static BidManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "bids";

    private BidManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized BidManager getInstance() {
        if (instance == null) {
            instance = new BidManager();
        }
        return instance;
    }

    // CREATE - Submit new bid
    public Task<Void> createBid(Bid bid) {
        return db.collection(COLLECTION_NAME)
                .document(bid.getBidId())
                .set(bid);
    }

    // CREATE - Submit new bid, atomically rejecting a duplicate. Relies on bid.getBidId() being
    // a deterministic id (e.g. "bid_" + jobId + "_" + contractorId) so two near-simultaneous
    // submissions for the same job/contractor pair can't both succeed - the transaction's read
    // of the doc happens before either write commits, so the second call always sees the first
    // one's doc and aborts instead of silently creating a duplicate bid.
    public Task<Void> createBidIfNotExists(Bid bid) {
        DocumentReference ref = db.collection(COLLECTION_NAME).document(bid.getBidId());
        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref);
            if (snapshot.exists()) {
                throw new FirebaseFirestoreException("Bid already exists for this job/contractor",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }
            transaction.set(ref, bid);
            return null;
        });
    }

    // READ - Get single bid by ID
    public Task<DocumentSnapshot> getBid(String bidId) {
        return db.collection(COLLECTION_NAME)
                .document(bidId)
                .get();
    }

    // READ - Get all bids for a job
    public Task<QuerySnapshot> getBidsByJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .get();
    }

    // READ - Get bids by contractor
    public Task<QuerySnapshot> getBidsByContractor(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .get();
    }

    // READ - Get pending bids for a job
    public Task<QuerySnapshot> getPendingBidsByJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", "pending")
                .get();
    }

    // READ - Get accepted bids by contractor
    public Task<QuerySnapshot> getAcceptedBidsByContractor(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .whereEqualTo("status", "accepted")
                .get();
    }

    // READ - Get bids by status
    public Task<QuerySnapshot> getBidsByStatus(String status) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
    }

    // UPDATE - Update entire bid
    public Task<Void> updateBid(Bid bid) {
        return db.collection(COLLECTION_NAME)
                .document(bid.getBidId())
                .set(bid);
    }

    // UPDATE - Update specific field
    public Task<Void> updateField(String bidId, String field, Object value) {
        return db.collection(COLLECTION_NAME)
                .document(bidId)
                .update(field, value);
    }

    // UPDATE - Update bid status
    public Task<Void> updateBidStatus(String bidId, String status) {
        return updateField(bidId, "status", status);
    }

    // UPDATE - Accept bid
    public Task<Void> acceptBid(String bidId) {
        return updateBidStatus(bidId, "accepted");
    }

    // UPDATE - Reject bid
    public Task<Void> rejectBid(String bidId) {
        return updateBidStatus(bidId, "rejected");
    }

    // UPDATE - Reject all other bids for a job (when one is accepted)
    public void rejectOtherBids(String jobId, String acceptedBidId) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String bidId = doc.getId();
                        if (!bidId.equals(acceptedBidId)) {
                            rejectBid(bidId);
                        }
                    }
                });
    }

    // DELETE - Delete bid
    public Task<Void> deleteBid(String bidId) {
        return db.collection(COLLECTION_NAME)
                .document(bidId)
                .delete();
    }

    // QUERY - Get lowest bids for a job (sort in memory after fetching)
    public Task<QuerySnapshot> getLowestBids(String jobId, int limit) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", "pending")
                .get();
    }

    // QUERY - Check if contractor has already bid on job
    public Task<QuerySnapshot> checkExistingBid(String jobId, String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("contractorId", contractorId)
                .get();
    }
}