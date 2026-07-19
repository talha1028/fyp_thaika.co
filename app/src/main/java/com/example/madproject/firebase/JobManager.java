package com.example.madproject.firebase;

import com.example.madproject.models.Job;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class JobManager {
    private static JobManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "jobs";

    private JobManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized JobManager getInstance() {
        if (instance == null) {
            instance = new JobManager();
        }
        return instance;
    }

    // CREATE - Add new job
    public Task<Void> createJob(Job job) {
        return db.collection(COLLECTION_NAME)
                .document(job.getJobId())
                .set(job);
    }

    // READ - Get single job by ID
    public Task<DocumentSnapshot> getJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .get();
    }

    // READ - Get all jobs
    public Task<QuerySnapshot> getAllJobs() {
        return db.collection(COLLECTION_NAME)
                .orderBy("postedDate", Query.Direction.DESCENDING)
                .get();
    }

    // READ - Get open jobs
    public Task<QuerySnapshot> getOpenJobs() {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "open")
                .get();
    }

    // READ - Get jobs by client (FIXED - removed orderBy to avoid index requirement)
    public Task<QuerySnapshot> getJobsByClient(String clientId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("clientId", clientId)
                .get();
    }

    // READ - Get jobs by contractor
    public Task<QuerySnapshot> getJobsByContractor(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("assignedContractorId", contractorId)
                .get();
    }

    // READ - Get jobs by category
    public Task<QuerySnapshot> getJobsByCategory(String category) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("category", category)
                .whereEqualTo("status", "open")
                .get();
    }

    // READ - Get jobs by status
    public Task<QuerySnapshot> getJobsByStatus(String status) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
    }

    // READ - Get jobs by client and status
    public Task<QuerySnapshot> getJobsByClientAndStatus(String clientId, String status) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("clientId", clientId)
                .whereEqualTo("status", status)
                .get();
    }

    // UPDATE - Update entire job
    public Task<Void> updateJob(Job job) {
        return db.collection(COLLECTION_NAME)
                .document(job.getJobId())
                .set(job);
    }

    // UPDATE - Update specific field
    public Task<Void> updateField(String jobId, String field, Object value) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .update(field, value);
    }

    // UPDATE - Update job status
    public Task<Void> updateJobStatus(String jobId, String status) {
        return updateField(jobId, "status", status);
    }

    // UPDATE - Increment total bids (using atomic increment)
    public Task<Void> incrementTotalBids(String jobId) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .update("totalBids", FieldValue.increment(1));
    }

    // UPDATE - Assign contractor to job
    public Task<Void> assignContractor(String jobId, String contractorId, String contractorName,
                                       String bidId, double bidAmount) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .update(
                        "assignedContractorId", contractorId,
                        "assignedContractorName", contractorName,
                        "acceptedBidId", bidId,
                        "acceptedBidAmount", bidAmount,
                        "status", "in_progress",
                        "startDate", System.currentTimeMillis()
                );
    }

    // UPDATE - Complete job
    public Task<Void> completeJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .update(
                        "status", "completed",
                        "completedDate", System.currentTimeMillis()
                );
    }

    // DELETE - Delete job
    public Task<Void> deleteJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .document(jobId)
                .delete();
    }

    // SEARCH - Search jobs by title
    public Task<QuerySnapshot> searchJobsByTitle(String title) {
        return db.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("title", title)
                .whereLessThanOrEqualTo("title", title + "\uf8ff")
                .get();
    }

    // QUERY - Get jobs by budget range
    public Task<QuerySnapshot> getJobsByBudgetRange(double minBudget, double maxBudget) {
        return db.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("budget", minBudget)
                .whereLessThanOrEqualTo("budget", maxBudget)
                .whereEqualTo("status", "open")
                .get();
    }
}