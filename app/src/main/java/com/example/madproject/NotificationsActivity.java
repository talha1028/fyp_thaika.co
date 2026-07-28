package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.NotificationAdapter;
import com.example.madproject.firebase.NotificationManager;
import com.example.madproject.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    private RecyclerView rvNotifications;
    private TextView btnMarkAllRead;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    private FirebaseAuth mAuth;
    private String currentUserId;

    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        initViews();
        setupRecyclerView();
        loadNotifications();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("");
        }

        rvNotifications = findViewById(R.id.rvTodayNotifications);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();

        notificationAdapter = new NotificationAdapter(this, notificationList, notification -> {
            // Mark notification as read when clicked
            if (!notification.isRead()) {
                NotificationManager.getInstance()
                        .markAsRead(notification.getNotificationId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Notification marked as read");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error marking notification as read: " + e.getMessage());
                        });
            }

            // Navigate based on notification type and relatedId
            navigateToRelatedActivity(notification);
        });

        rvNotifications.setAdapter(notificationAdapter);
    }

    private void loadNotifications() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to view notifications", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading notifications for user: " + currentUserId);
        showLoading(true);

        // Set up real-time listener for notifications
        notificationListener = NotificationManager.getInstance().listenToNotifications(currentUserId, new NotificationManager.OnNotificationsChangedListener() {
            @Override
            public void onNotificationsChanged(com.google.firebase.firestore.QuerySnapshot notifications) {
                Log.d(TAG, "Notifications updated: " + notifications.size());
                showLoading(false);

                notificationList.clear();

                for (DocumentSnapshot doc : notifications) {
                    Notification notification = doc.toObject(Notification.class);
                    if (notification != null) {
                        notificationList.add(notification);
                    }
                }

                // Sort by timestamp (newest first)
                Collections.sort(notificationList, (n1, n2) ->
                        Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                notificationAdapter.notifyDataSetChanged();
                updateEmptyState();

                Log.d(TAG, "Notifications loaded: " + notificationList.size());
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error loading notifications: " + error);
                Toast.makeText(NotificationsActivity.this,
                        "Error loading notifications: " + error,
                        Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void markAllAsRead() {
        if (currentUserId.isEmpty()) return;

        Log.d(TAG, "Marking all notifications as read");
        NotificationManager.getInstance().markAllAsRead(currentUserId);
        Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
    }

    private void navigateToRelatedActivity(Notification notification) {
        String type = notification.getType();
        String relatedId = notification.getRelatedId();

        if (relatedId == null || relatedId.isEmpty() || type == null) {
            return;
        }

        Intent intent = null;

        switch (type.toLowerCase()) {
            case "job":
                intent = new Intent(this, JobDetailActivity.class);
                intent.putExtra("jobId", relatedId);
                break;

            case "bid":
                intent = new Intent(this, JobDetailActivity.class);
                intent.putExtra("jobId", relatedId);
                break;

            case "message":
                intent = new Intent(this, ChatActivity.class);
                intent.putExtra("receiverId", relatedId);
                break;

            case "task":
                intent = new Intent(this, TaskDetailActivity.class);
                intent.putExtra("taskId", relatedId);
                break;

            case "payment":
                intent = new Intent(this, PaymentActivity.class);
                intent.putExtra("jobId", relatedId);
                break;

            case "system":
            default:
                return;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            rvNotifications.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener to prevent memory leaks
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
