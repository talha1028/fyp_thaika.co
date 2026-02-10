package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.MessageAdapter;
import com.example.madproject.firebase.MessageManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Message;
import com.example.madproject.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView btnSend, btnAttach;
    private ProgressBar progressBar;
    private TextView tvReceiverName;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private String receiverId;
    private String chatId;
    private User currentUser;
    private User receiverUser;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get receiver ID from intent
        receiverId = getIntent().getStringExtra("receiverId");
        String receiverName = getIntent().getStringExtra("receiverName");

        if (receiverId == null || receiverId.isEmpty()) {
            Toast.makeText(this, "Error: No receiver specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Generate chat ID (sorted to ensure consistency)
        chatId = generateChatId(currentUserId, receiverId);
        Log.d(TAG, "Chat ID: " + chatId);

        initViews();

        // Set receiver name in toolbar if provided
        if (receiverName != null && !receiverName.isEmpty()) {
            tvReceiverName.setText(receiverName);
        }

        setupRecyclerView();
        setupClickListeners();
        loadCurrentUser();
        loadReceiverUser();
        loadMessages();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);
        tvReceiverName = findViewById(R.id.tvReceiverName);

        // Create ProgressBar programmatically if not in layout
        progressBar = findViewById(R.id.progressBar);
        if (progressBar == null) {
            progressBar = new ProgressBar(this);
            progressBar.setVisibility(View.GONE);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        // Disable send button initially
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.5f);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        btnAttach.setOnClickListener(v -> {
            Toast.makeText(this, "File attachment coming soon", Toast.LENGTH_SHORT).show();
        });

        // Enable/disable send button based on text
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                btnSend.setEnabled(hasText);
                btnSend.setAlpha(hasText ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private String generateChatId(String user1, String user2) {
        // Sort IDs to ensure consistent chat ID regardless of who initiates
        if (user1.compareTo(user2) < 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }

    private void loadCurrentUser() {
        UserManager.getInstance().getUserObject(currentUserId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                currentUser = user;
                Log.d(TAG, "Current user loaded: " + user.getFullName());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading current user: " + error);
            }
        });
    }

    private void loadReceiverUser() {
        UserManager.getInstance().getUserObject(receiverId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                receiverUser = user;
                tvReceiverName.setText(user.getFullName());
                Log.d(TAG, "Receiver user loaded: " + user.getFullName());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading receiver user: " + error);
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;

        if (currentUser == null) {
            Toast.makeText(this, "Please wait, loading user info...", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = "msg_" + UUID.randomUUID().toString();

        Message message = new Message(
                messageId,
                chatId,
                currentUserId,
                currentUser.getFullName(),
                receiverId,
                messageText
        );

        // Set receiver name if available
        if (receiverUser != null) {
            message.setReceiverName(receiverUser.getFullName());
        }

        // Clear input immediately for better UX
        etMessage.setText("");

        MessageManager.getInstance()
                .createMessage(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message: " + e.getMessage());
                    Toast.makeText(ChatActivity.this,
                            "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMessages() {
        Log.d(TAG, "Loading messages for chat: " + chatId);

        // Use real-time listener for messages
        MessageManager.getInstance().listenToMessages(chatId, new MessageManager.OnMessagesChangedListener() {
            @Override
            public void onMessagesChanged(com.google.firebase.firestore.QuerySnapshot messages) {
                Log.d(TAG, "Messages updated: " + messages.size());

                messageList.clear();

                for (DocumentSnapshot doc : messages) {
                    Message message = doc.toObject(Message.class);
                    if (message != null) {
                        messageList.add(message);

                        // Mark message as read if it's for current user and unread
                        if (message.getReceiverId().equals(currentUserId) && !message.isRead()) {
                            MessageManager.getInstance().markAsRead(message.getMessageId());
                        }
                    }
                }

                // Sort by timestamp (oldest first for chat)
                Collections.sort(messageList, (m1, m2) ->
                        Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                messageAdapter.notifyDataSetChanged();

                // Scroll to bottom
                if (!messageList.isEmpty()) {
                    rvMessages.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading messages: " + error);
                Toast.makeText(ChatActivity.this,
                        "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener if we stored it
        if (messageListener != null) {
            messageListener.remove();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
