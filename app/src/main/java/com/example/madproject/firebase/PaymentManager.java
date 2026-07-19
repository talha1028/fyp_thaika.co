package com.example.madproject.firebase;

import com.example.madproject.models.Payment;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class PaymentManager {
    private static PaymentManager instance;
    private final FirebaseFirestore db;
    private static final String COLLECTION = "payments";

    private PaymentManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized PaymentManager getInstance() {
        if (instance == null) instance = new PaymentManager();
        return instance;
    }

    public Task<Void> createPayment(Payment payment) {
        return db.collection(COLLECTION)
                .document(payment.getPaymentId())
                .set(payment);
    }

    public Task<QuerySnapshot> getPaymentsByJob(String jobId) {
        return db.collection(COLLECTION)
                .whereEqualTo("jobId", jobId)
                .get();
    }

    public Task<QuerySnapshot> getPaymentsByClient(String clientId) {
        return db.collection(COLLECTION)
                .whereEqualTo("clientId", clientId)
                .get();
    }
}
