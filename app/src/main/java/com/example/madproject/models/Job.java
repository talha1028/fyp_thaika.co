package com.example.madproject.models;

import java.util.ArrayList;
import java.util.List;

public class Job {
    private String jobId;
    private String clientId;
    private String clientName;
    private String clientPhotoUrl;
    private String title;
    private String description;
    private String category;
    private double budget;
    private String timeline; // e.g., "2 weeks"
    private String location;
    private String status; // "open", "in_progress", "completed", "cancelled"
    private long postedDate;
    private long startDate;
    private long completedDate;
    private int totalBids;
    private String acceptedBidId;
    private double acceptedBidAmount;
    private String assignedContractorId;
    private String assignedContractorName;
    private List<String> attachments; // Image URLs
    private boolean depositPaid;
    private boolean finalPaid;
    private boolean paymentVerified; // contractor confirms they received the deposit

    // Required empty constructor for Firestore
    public Job() {
        this.attachments = new ArrayList<>();
    }

    // Constructor
    public Job(String jobId, String clientId, String clientName, String title,
               String description, String category, double budget, String timeline, String location) {
        this.jobId = jobId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.title = title;
        this.description = description;
        this.category = category;
        this.budget = budget;
        this.timeline = timeline;
        this.location = location;
        this.status = "open";
        this.postedDate = System.currentTimeMillis();
        this.totalBids = 0;
        this.attachments = new ArrayList<>();
    }

    // Getters and Setters
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPhotoUrl() {
        return clientPhotoUrl;
    }

    public void setClientPhotoUrl(String clientPhotoUrl) {
        this.clientPhotoUrl = clientPhotoUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public String getTimeline() {
        return timeline;
    }

    public void setTimeline(String timeline) {
        this.timeline = timeline;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(long postedDate) {
        this.postedDate = postedDate;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(long completedDate) {
        this.completedDate = completedDate;
    }

    public int getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(int totalBids) {
        this.totalBids = totalBids;
    }

    public String getAcceptedBidId() {
        return acceptedBidId;
    }

    public void setAcceptedBidId(String acceptedBidId) {
        this.acceptedBidId = acceptedBidId;
    }

    public double getAcceptedBidAmount() {
        return acceptedBidAmount;
    }

    public void setAcceptedBidAmount(double acceptedBidAmount) {
        this.acceptedBidAmount = acceptedBidAmount;
    }

    public boolean isDepositPaid() {
        return depositPaid;
    }

    public void setDepositPaid(boolean depositPaid) {
        this.depositPaid = depositPaid;
    }

    public boolean isFinalPaid() {
        return finalPaid;
    }

    public void setFinalPaid(boolean finalPaid) {
        this.finalPaid = finalPaid;
    }

    public boolean isPaymentVerified() {
        return paymentVerified;
    }

    public void setPaymentVerified(boolean paymentVerified) {
        this.paymentVerified = paymentVerified;
    }

    public String getAssignedContractorId() {
        return assignedContractorId;
    }

    public void setAssignedContractorId(String assignedContractorId) {
        this.assignedContractorId = assignedContractorId;
    }

    public String getAssignedContractorName() {
        return assignedContractorName;
    }

    public void setAssignedContractorName(String assignedContractorName) {
        this.assignedContractorName = assignedContractorName;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }
}