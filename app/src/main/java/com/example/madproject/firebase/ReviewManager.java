package com.example.madproject.firebase;

import com.example.madproject.models.Review;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class ReviewManager {
    private static ReviewManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "reviews";

    private ReviewManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized ReviewManager getInstance() {
        if (instance == null) {
            instance = new ReviewManager();
        }
        return instance;
    }

    // CREATE - Submit review
    public Task<Void> createReview(Review review) {
        return db.collection(COLLECTION_NAME)
                .document(review.getReviewId())
                .set(review);
    }

    // READ - Get single review by ID
    public Task<DocumentSnapshot> getReview(String reviewId) {
        return db.collection(COLLECTION_NAME)
                .document(reviewId)
                .get();
    }

    // READ - Get reviews for contractor (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getReviewsByContractor(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .get();
    }

    // READ - Get reviews by client (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getReviewsByClient(String clientId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("clientId", clientId)
                .get();
    }

    // READ - Get reviews for job
    public Task<QuerySnapshot> getReviewsByJob(String jobId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("jobId", jobId)
                .get();
    }

    // READ - Get reviews by rating (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getReviewsByRating(String contractorId, float minRating) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .whereGreaterThanOrEqualTo("rating", minRating)
                .get();
    }

    // READ - Get verified reviews only (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getVerifiedReviews(String contractorId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .whereEqualTo("isVerified", true)
                .get();
    }

    // UPDATE - Update entire review
    public Task<Void> updateReview(Review review) {
        return db.collection(COLLECTION_NAME)
                .document(review.getReviewId())
                .set(review);
    }

    // UPDATE - Add contractor response
    public Task<Void> addResponse(String reviewId, String response) {
        return db.collection(COLLECTION_NAME)
                .document(reviewId)
                .update(
                        "response", response,
                        "responseDate", System.currentTimeMillis()
                );
    }

    // DELETE - Delete review
    public Task<Void> deleteReview(String reviewId) {
        return db.collection(COLLECTION_NAME)
                .document(reviewId)
                .delete();
    }

    // CALCULATE - Calculate average rating
    public void calculateAverageRating(String contractorId, OnRatingCalculatedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("contractorId", contractorId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onRatingCalculated(0.0, 0);
                        return;
                    }

                    float totalRating = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            totalRating += review.getRating();
                            count++;
                        }
                    }

                    double averageRating = count > 0 ? (double) totalRating / count : 0.0;
                    listener.onRatingCalculated(averageRating, count);
                });
    }

    // Callback interface
    public interface OnRatingCalculatedListener {
        void onRatingCalculated(double averageRating, int totalReviews);
    }
}