package com.example.madproject.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String city;
    private String userType; // "client" or "contractor"
    private String profilePictureUrl;
    private String fcmToken;
    private boolean phoneVerified;
    private boolean pushNotificationsEnabled = true;
    private boolean messageNotificationsEnabled = true;
    private long createdAt;
    private long lastLogin;

    // Contractor-specific fields (null for clients)
    private String category; // e.g., "Plumber", "Electrician", "Mason"
    private String bio;
    private int experienceYears;
    private double hourlyRate;
    private double rating;
    private int totalReviews;
    private int completedProjects;
    private List<String> portfolioImages; // URLs of portfolio images

    // Client-specific fields (null for contractors)
    private int activeJobs;
    private int completedJobs;
    private double totalSpent;

    // Required empty constructor for Firestore
    public User() {
        this.portfolioImages = new ArrayList<>();
    }

    // Constructor for Client
    public User(String userId, String email, String fullName, String phoneNumber, String userType) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.userType = userType;
        this.createdAt = System.currentTimeMillis();
        this.lastLogin = System.currentTimeMillis();
        this.rating = 0.0;
        this.totalReviews = 0;
        this.completedProjects = 0;
        this.activeJobs = 0;
        this.completedJobs = 0;
        this.totalSpent = 0.0;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public boolean isPushNotificationsEnabled() {
        return pushNotificationsEnabled;
    }

    public void setPushNotificationsEnabled(boolean enabled) {
        this.pushNotificationsEnabled = enabled;
    }

    public boolean isMessageNotificationsEnabled() {
        return messageNotificationsEnabled;
    }

    public void setMessageNotificationsEnabled(boolean enabled) {
        this.messageNotificationsEnabled = enabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public int getCompletedProjects() {
        return completedProjects;
    }

    public void setCompletedProjects(int completedProjects) {
        this.completedProjects = completedProjects;
    }

    public List<String> getPortfolioImages() {
        return portfolioImages != null ? portfolioImages : new ArrayList<>();
    }

    public void setPortfolioImages(List<String> portfolioImages) {
        this.portfolioImages = portfolioImages;
    }

    public int getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(int activeJobs) {
        this.activeJobs = activeJobs;
    }

    public int getCompletedJobs() {
        return completedJobs;
    }

    public void setCompletedJobs(int completedJobs) {
        this.completedJobs = completedJobs;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    // Helper methods
    @Exclude
    public boolean isClient() {
        return "client".equalsIgnoreCase(userType);
    }

    @Exclude
    public boolean isContractor() {
        return "contractor".equalsIgnoreCase(userType);
    }
}