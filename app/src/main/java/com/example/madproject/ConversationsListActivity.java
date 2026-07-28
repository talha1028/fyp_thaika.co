package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.ConversationAdapter;
import com.example.madproject.firebase.MessageManager;
import com.example.madproject.firebase.UserManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.madproject.models.Conversation;
import com.example.madproject.models.Message;
import com.example.madproject.models.User;
import com.example.madproject.views.ShimmerLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversationsListActivity extends AppCompatActivity {

    private static final String TAG = "ConversationsListActivity";

    private RecyclerView rvConversations;
    private EditText etSearch;
    private LinearLayout emptyState;
    private ShimmerLayout shimmerConversations;

    /** True while a fetch is in flight - the skeleton owns the screen until it resolves. */
    private boolean isLoading = true;

    private FirebaseAuth mAuth;
    private String currentUserId;

    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;
    private List<Conversation> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations_list);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupSearch();
        loadConversations();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        rvConversations = findViewById(R.id.rvConversations);
        etSearch = findViewById(R.id.etSearch);
        emptyState = findViewById(R.id.emptyState);
        shimmerConversations = findViewById(R.id.shimmerConversations);

        rvConversations.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRecyclerView() {
        conversationList = new ArrayList<>();
        filteredList = new ArrayList<>();

        conversationAdapter = new ConversationAdapter(this, filteredList, conversation -> {
            // Open ChatActivity with this conversation
            Intent intent = new Intent(ConversationsListActivity.this, ChatActivity.class);
            intent.putExtra("receiverId", conversation.getOtherUserId());
            intent.putExtra("receiverName", conversation.getOtherUserName());
            startActivity(intent);
        });

        rvConversations.setAdapter(conversationAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterConversations(String query) {
        filteredList.clear();

        if (query.trim().isEmpty()) {
            filteredList.addAll(conversationList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Conversation conv : conversationList) {
                if (conv.getOtherUserName().toLowerCase().contains(lowerQuery) ||
                        (conv.getLastMessage() != null && conv.getLastMessage().toLowerCase().contains(lowerQuery))) {
                    filteredList.add(conv);
                }
            }
        }

        conversationAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadConversations() {
        showLoading(true);
        loadAllUserMessages();
    }

    private void loadAllUserMessages() {
        Map<String, Conversation> conversationMap = new HashMap<>();
        Map<String, Integer> unreadCountMap = new HashMap<>();

        // Query 1: Get messages sent by current user
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("messages")
                .whereEqualTo("senderId", currentUserId)
                .get()
                .addOnSuccessListener(sentMessages -> {
                    Log.d(TAG, "Sent messages: " + sentMessages.size());

                    // Process sent messages
                    for (DocumentSnapshot doc : sentMessages) {
                        Message message = doc.toObject(Message.class);
                        if (message != null) {
                            processMessage(message, conversationMap, unreadCountMap);
                        }
                    }

                    // Query 2: Get messages received by current user
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("messages")
                            .whereEqualTo("receiverId", currentUserId)
                            .get()
                            .addOnSuccessListener(receivedMessages -> {
                                Log.d(TAG, "Received messages: " + receivedMessages.size());

                                // Process received messages
                                for (DocumentSnapshot doc : receivedMessages) {
                                    Message message = doc.toObject(Message.class);
                                    if (message != null) {
                                        processMessage(message, conversationMap, unreadCountMap);
                                    }
                                }

                                // Convert map to list
                                conversationList.clear();
                                conversationList.addAll(conversationMap.values());

                                // Sort by last message time (newest first)
                                Collections.sort(conversationList, (c1, c2) ->
                                        Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

                                filteredList.clear();
                                filteredList.addAll(conversationList);

                                showLoading(false);
                                conversationAdapter.notifyDataSetChanged();
                                updateEmptyState();

                                Log.d(TAG, "Loaded " + conversationList.size() + " conversations");
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Log.e(TAG, "Error loading received messages: " + e.getMessage());
                                Toast.makeText(this, "Error loading conversations", Toast.LENGTH_SHORT).show();
                                updateEmptyState();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading sent messages: " + e.getMessage());
                    Toast.makeText(this, "Error loading conversations", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void processMessage(Message message, Map<String, Conversation> conversationMap, Map<String, Integer> unreadCountMap) {
        String chatId = message.getChatId();

        // Determine the other user
        String otherUserId = message.getSenderId().equals(currentUserId) ?
                message.getReceiverId() : message.getSenderId();

        // Update or create conversation
        if (!conversationMap.containsKey(chatId)) {
            Conversation conv = new Conversation();
            conv.setChatId(chatId);
            conv.setOtherUserId(otherUserId);

            // Set other user name from message
            if (message.getSenderId().equals(currentUserId)) {
                conv.setOtherUserName(message.getReceiverName() != null ?
                        message.getReceiverName() : "User");
            } else {
                conv.setOtherUserName(message.getSenderName() != null ?
                        message.getSenderName() : "User");
            }

            conv.setLastMessage(message.getMessageText());
            conv.setLastMessageTime(message.getTimestamp());
            conv.setUnreadCount(0);

            conversationMap.put(chatId, conv);
            unreadCountMap.put(chatId, 0);
        }

        // Update if this message is newer
        Conversation existing = conversationMap.get(chatId);
        if (message.getTimestamp() > existing.getLastMessageTime()) {
            existing.setLastMessage(message.getMessageText());
            existing.setLastMessageTime(message.getTimestamp());

            // Update other user name if current message has it
            if (message.getSenderId().equals(currentUserId)) {
                if (message.getReceiverName() != null && !message.getReceiverName().isEmpty()) {
                    existing.setOtherUserName(message.getReceiverName());
                }
            } else {
                if (message.getSenderName() != null && !message.getSenderName().isEmpty()) {
                    existing.setOtherUserName(message.getSenderName());
                }
            }
        }

        // Count unread messages
        if (message.getReceiverId().equals(currentUserId) && !message.isRead()) {
            int currentCount = unreadCountMap.getOrDefault(chatId, 0);
            unreadCountMap.put(chatId, currentCount + 1);
            existing.setUnreadCount(currentCount + 1);
        }
    }

    private void showLoading(boolean show) {
        isLoading = show;
        if (show) {
            shimmerConversations.setVisibility(View.VISIBLE);
            shimmerConversations.showShimmer();
            rvConversations.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            shimmerConversations.hideShimmer();
            shimmerConversations.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (isLoading) {
            // The skeleton owns the screen until the fetch resolves; an empty list here just
            // means the data has not arrived yet, not that there is nothing to show.
            return;
        }

        if (filteredList.isEmpty()) {
            rvConversations.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvConversations.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
