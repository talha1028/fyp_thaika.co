package com.example.madproject.models;

public class Payment {
    private String paymentId;
    private String jobId;
    private String jobTitle;
    private String clientId;
    private String contractorId;
    private String contractorName;
    private double amount;
    private String paymentType;   // "deposit" or "final"
    private String paymentMethod; // "jazzcash", "easypaisa", "bank_transfer", "cash"
    private String transactionRef;
    private String status;        // "completed"
    private long timestamp;

    public Payment() {}

    public Payment(String paymentId, String jobId, String jobTitle, String clientId,
                   String contractorId, String contractorName, double amount,
                   String paymentType, String paymentMethod, String transactionRef) {
        this.paymentId = paymentId;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.clientId = clientId;
        this.contractorId = contractorId;
        this.contractorName = contractorName;
        this.amount = amount;
        this.paymentType = paymentType;
        this.paymentMethod = paymentMethod;
        this.transactionRef = transactionRef;
        this.status = "completed";
        this.timestamp = System.currentTimeMillis();
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getContractorId() { return contractorId; }
    public void setContractorId(String contractorId) { this.contractorId = contractorId; }

    public String getContractorName() { return contractorName; }
    public void setContractorName(String contractorName) { this.contractorName = contractorName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
