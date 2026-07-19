package com.example.madproject.models;

/**
 * Conversation Model
 * Represents a chat conversation between two users
 */
public class Conversation {
    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserProfilePic;
    private String lastMessage;
    private long lastMessageTime;
    private int unreadCount;
    private boolean isOnline;

    public Conversation() {
        // Empty constructor for Firestore
    }

    public Conversation(String chatId, String otherUserId, String otherUserName) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.lastMessageTime = System.currentTimeMillis();
        this.unreadCount = 0;
        this.isOnline = false;
    }

    // Getters and Setters
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getOtherUserProfilePic() {
        return otherUserProfilePic;
    }

    public void setOtherUserProfilePic(String otherUserProfilePic) {
        this.otherUserProfilePic = otherUserProfilePic;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
