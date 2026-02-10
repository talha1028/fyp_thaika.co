package com.example.madproject.firebase;

import com.example.madproject.models.Message;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class MessageManager {
    private static MessageManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "messages";

    private MessageManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }
        return instance;
    }

    // CREATE - Send new message
    public Task<Void> createMessage(Message message) {
        return db.collection(COLLECTION_NAME)
                .document(message.getMessageId())
                .set(message);
    }

    // READ - Get single message by ID
    public Task<DocumentSnapshot> getMessage(String messageId) {
        return db.collection(COLLECTION_NAME)
                .document(messageId)
                .get();
    }

    // READ - Get messages in a chat (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getMessagesByChat(String chatId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .get();
    }

    // READ - Get messages in a chat with limit (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getRecentMessages(String chatId, int limit) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .limit(limit)
                .get();
    }

    // READ - Get unread messages for user (sort in memory to avoid index requirement)
    public Task<QuerySnapshot> getUnreadMessages(String userId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get();
    }

    // READ - Get unread count
    public void getUnreadCount(String userId, OnCountLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onCountLoaded(queryDocumentSnapshots.size());
                });
    }

    // UPDATE - Mark message as read
    public Task<Void> markAsRead(String messageId) {
        return db.collection(COLLECTION_NAME)
                .document(messageId)
                .update(
                        "isRead", true,
                        "readAt", System.currentTimeMillis()
                );
    }

    // UPDATE - Mark all messages in chat as read
    public void markAllAsRead(String chatId, String userId) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        markAsRead(doc.getId());
                    }
                });
    }

    // DELETE - Delete message
    public Task<Void> deleteMessage(String messageId) {
        return db.collection(COLLECTION_NAME)
                .document(messageId)
                .delete();
    }

    // DELETE - Delete all messages in a chat
    public void deleteChat(String chatId) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        deleteMessage(doc.getId());
                    }
                });
    }

    // REAL-TIME - Listen to messages in chat (sort in memory to avoid index requirement)
    public void listenToMessages(String chatId, OnMessagesChangedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("chatId", chatId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        listener.onMessagesChanged(value);
                    }
                });
    }

    // Callback interfaces
    public interface OnCountLoadedListener {
        void onCountLoaded(int count);
    }

    public interface OnMessagesChangedListener {
        void onMessagesChanged(QuerySnapshot messages);
        void onError(String error);
    }
}