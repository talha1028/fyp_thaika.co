package com.example.madproject.firebase;

import com.example.madproject.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private static UserManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "users";

    private UserManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // CREATE - Add new user
    public Task<Void> createUser(User user) {
        return db.collection(COLLECTION_NAME)
                .document(user.getUserId())
                .set(user);
    }

    // READ - Get single user by ID
    public Task<DocumentSnapshot> getUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .get();
    }

    // READ - Get user as User object
    public void getUserObject(String userId, OnUserLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        listener.onUserLoaded(user);
                    } else {
                        listener.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // READ - Get all users
    public Task<QuerySnapshot> getAllUsers() {
        return db.collection(COLLECTION_NAME).get();
    }

    // READ - Get all contractors
    public Task<QuerySnapshot> getAllContractors() {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userType", "contractor")
                .get();
    }

    // READ - Get contractors by category
    public Task<QuerySnapshot> getContractorsByCategory(String category) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userType", "contractor")
                .whereEqualTo("category", category)
                .get();
    }

    // READ - Get top rated contractors
    public Task<QuerySnapshot> getTopRatedContractors(int limit) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userType", "contractor")
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
    }

    // UPDATE - Update entire user
    public Task<Void> updateUser(User user) {
        return db.collection(COLLECTION_NAME)
                .document(user.getUserId())
                .set(user);
    }

    // UPDATE - Update specific field
    public Task<Void> updateField(String userId, String field, Object value) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .update(field, value);
    }

    // UPDATE - Update profile picture
    public Task<Void> updateProfilePicture(String userId, String imageUrl) {
        return updateField(userId, "profilePictureUrl", imageUrl);
    }

    // UPDATE - Update rating
    public Task<Void> updateRating(String userId, double rating, int totalReviews) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .update(
                        "rating", rating,
                        "totalReviews", totalReviews
                );
    }

    // UPDATE - Increment completed projects
    public void incrementCompletedProjects(String userId) {
        db.collection(COLLECTION_NAME)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setCompletedProjects(user.getCompletedProjects() + 1);
                            updateUser(user);
                        }
                    }
                });
    }

    // UPDATE - Record a completed job: atomically bumps completedProjects and
    // adds to totalEarnings, so concurrent completions can't clobber each other
    // (unlike incrementCompletedProjects, which does a read-then-write).
    public Task<Void> recordCompletedJob(String contractorId, double earningsAmount) {
        return db.collection(COLLECTION_NAME)
                .document(contractorId)
                .update(
                        "completedProjects", FieldValue.increment(1),
                        "totalEarnings", FieldValue.increment(earningsAmount)
                );
    }

    // DELETE - Delete user
    public Task<Void> deleteUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .delete();
    }

    // SEARCH - Search users by name
    public Task<QuerySnapshot> searchUsersByName(String name) {
        return db.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("fullName", name)
                .whereLessThanOrEqualTo("fullName", name + "\uf8ff")
                .get();
    }

    // Callback interface
    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onError(String error);
    }
}