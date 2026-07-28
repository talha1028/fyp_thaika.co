package com.example.madproject.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.madproject.JobDetailActivity;
import com.example.madproject.NotificationsActivity;
import com.example.madproject.R;
import com.example.madproject.firebase.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "madproject_notifications";
    private static final String CHANNEL_NAME = "MadProject Notifications";

    // Shared across notification-id and PendingIntent request-code: (int) System.currentTimeMillis()
    // truncates a long, so two notifications delivered in the same millisecond collided (one
    // silently replaced the other, and their PendingIntents - both requestCode 0, targeting the
    // same Activity class - were treated as identical, so tapping an older notification could
    // navigate using a newer one's extras).
    private static final java.util.concurrent.atomic.AtomicInteger notificationIdCounter =
            new java.util.concurrent.atomic.AtomicInteger(0);

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification Title: " + title);
            Log.d(TAG, "Notification Body: " + body);

            showNotification(title, body, remoteMessage.getData());
        }

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");

            if (title != null && message != null) {
                showNotification(title, message, remoteMessage.getData());
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Save token to Firestore for current user
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();

            UserManager.getInstance()
                    .updateField(userId, "fcmToken", token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "FCM token saved to Firestore");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save FCM token: " + e.getMessage());
                    });
        }
    }

    private void showNotification(String title, String message, java.util.Map<String, String> data) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for job updates, bids, and messages");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Determine intent based on notification type
        Intent intent;
        String type = data.get("type");
        String relatedId = data.get("relatedId");

        if ("bid".equals(type) || "job".equals(type)) {
            intent = new Intent(this, JobDetailActivity.class);
            if (relatedId != null) intent.putExtra("jobId", relatedId);
        } else if ("message".equals(type)) {
            intent = new Intent(this, com.example.madproject.ChatActivity.class);
            if (relatedId != null) intent.putExtra("receiverId", relatedId);
        } else if ("task".equals(type)) {
            intent = new Intent(this, com.example.madproject.TaskDetailActivity.class);
            if (relatedId != null) intent.putExtra("taskId", relatedId);
        } else if ("payment".equals(type)) {
            intent = new Intent(this, com.example.madproject.PaymentActivity.class);
            if (relatedId != null) intent.putExtra("jobId", relatedId);
        } else {
            intent = new Intent(this, NotificationsActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int notificationId = notificationIdCounter.incrementAndGet();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        // Show notification
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
