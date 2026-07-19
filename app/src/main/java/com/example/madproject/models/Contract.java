package com.example.madproject.models;

public class Contract {
    private String contractId;
    private String jobId;
    private String jobTitle;
    private String clientId;
    private String contractorId;
    private String contractText;
    private String pdfUrl;
    private long generatedDate;
    private boolean clientEmailSent;
    private boolean contractorEmailSent;

    // Required empty constructor for Firestore
    public Contract() {
    }

    // Constructor
    public Contract(String contractId, String jobId, String jobTitle,
                     String clientId, String contractorId, String contractText) {
        this.contractId = contractId;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.clientId = clientId;
        this.contractorId = contractorId;
        this.contractText = contractText;
        this.generatedDate = System.currentTimeMillis();
        this.clientEmailSent = false;
        this.contractorEmailSent = false;
    }

    // Getters and Setters
    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getContractorId() {
        return contractorId;
    }

    public void setContractorId(String contractorId) {
        this.contractorId = contractorId;
    }

    public String getContractText() {
        return contractText;
    }

    public void setContractText(String contractText) {
        this.contractText = contractText;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public long getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(long generatedDate) {
        this.generatedDate = generatedDate;
    }

    public boolean isClientEmailSent() {
        return clientEmailSent;
    }

    public void setClientEmailSent(boolean clientEmailSent) {
        this.clientEmailSent = clientEmailSent;
    }

    public boolean isContractorEmailSent() {
        return contractorEmailSent;
    }

    public void setContractorEmailSent(boolean contractorEmailSent) {
        this.contractorEmailSent = contractorEmailSent;
    }
}
