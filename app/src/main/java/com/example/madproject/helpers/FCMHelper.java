package com.example.madproject.helpers;

import android.util.Log;

import com.example.madproject.firebase.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMHelper {

    private static final String TAG = "FCMHelper";

    /**
     * Register FCM token for current user
     * Call this method after successful login
     */
    public static void registerFCMToken() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "No user logged in, skipping FCM registration");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();

                        // Log token in a clearly visible format for testing
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "FCM TOKEN FOR TESTING:");
                        Log.d(TAG, token);
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "Copy this token to test notifications in Firebase Console");
                        // Save token to Firestore
                        UserManager.getInstance()
                                .updateField(userId, "fcmToken", token)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "FCM token saved successfully to Firestore");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save FCM token: " + e.getMessage());
                                });
                    } else {
                        Log.e(TAG, "Failed to get FCM token", task.getException());
                    }
                });
    }
}
