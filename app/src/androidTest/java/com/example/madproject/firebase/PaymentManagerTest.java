package com.example.madproject.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.madproject.models.Payment;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented (live Firestore) tests for {@link PaymentManager}. Hits the real
 * "madproject-5c465" Firebase project - see {@link FirebaseTestUtils} for details.
 *
 * PaymentManager has a much smaller surface than the other managers: it only
 * exposes createPayment, getPaymentsByJob, and getPaymentsByClient - there is no
 * single-document getPayment, no updatePayment/updateField, and no deletePayment.
 * Tests below are adapted to that reality:
 *  - "read" is verified through the two list queries (there is no getPayment(id)).
 *  - there is no "update" test since PaymentManager provides no update capability.
 *  - cleanup/"delete" uses {@link FirebaseTestUtils#deleteDocument(String, String)}
 *    directly against Firestore, since PaymentManager itself cannot delete.
 */
@RunWith(AndroidJUnit4.class)
public class PaymentManagerTest {

    private static final String COLLECTION = "payments";

    private final PaymentManager paymentManager = PaymentManager.getInstance();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        FirebaseTestUtils.signIn();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdIds) {
            FirebaseTestUtils.deleteDocument(COLLECTION, id);
        }
        createdIds.clear();
    }

    private Payment newPayment(String id, String jobId, String clientId, double amount, String type) {
        return new Payment(id, jobId, "Test Job", clientId, "apitest_contractor",
                "Test Contractor", amount, type, "bank_transfer", "apitest_txn_" + id);
    }

    @Test
    public void testCreatePayment_and_getPaymentsByJob_roundTrip() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String clientId = "apitest_client_" + FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Payment payment = newPayment(id, jobId, clientId, 1500.0, "deposit");

        Tasks.await(paymentManager.createPayment(payment), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        QuerySnapshot snapshot = Tasks.await(paymentManager.getPaymentsByJob(jobId),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        Payment fetched = snapshot.getDocuments().get(0).toObject(Payment.class);
        assertEquals(id, fetched.getPaymentId());
        assertEquals(1500.0, fetched.getAmount(), 0.001);
        assertEquals("deposit", fetched.getPaymentType());
        assertEquals("completed", fetched.getStatus());
    }

    @Test
    public void testGetPaymentsByClient_findsPayment() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String clientId = "apitest_client_" + FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Payment payment = newPayment(id, jobId, clientId, 2000.0, "final");
        Tasks.await(paymentManager.createPayment(payment), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        QuerySnapshot snapshot = Tasks.await(paymentManager.getPaymentsByClient(clientId),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        assertEquals(id, snapshot.getDocuments().get(0).getId());
    }

    @Test
    public void testDeletePayment_thenVerifyGoneFromQuery() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String clientId = "apitest_client_" + FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Payment payment = newPayment(id, jobId, clientId, 1000.0, "deposit");
        Tasks.await(paymentManager.createPayment(payment), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // PaymentManager exposes no delete method; delete directly against Firestore.
        FirebaseTestUtils.deleteDocument(COLLECTION, id);

        QuerySnapshot snapshot = Tasks.await(paymentManager.getPaymentsByJob(jobId),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.isEmpty());
    }

    @Test
    public void testGetPaymentsByJob_depositAndFinalBothReturned() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String clientId = "apitest_client_" + FirebaseTestUtils.newTestId();
        String depositId = FirebaseTestUtils.newTestId();
        String finalId = FirebaseTestUtils.newTestId();

        Tasks.await(paymentManager.createPayment(newPayment(depositId, jobId, clientId, 1500.0, "deposit")),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(depositId);
        Tasks.await(paymentManager.createPayment(newPayment(finalId, jobId, clientId, 3500.0, "final")),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(finalId);

        QuerySnapshot snapshot = Tasks.await(paymentManager.getPaymentsByJob(jobId),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(2, snapshot.size());

        double total = 0.0;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Payment payment = doc.toObject(Payment.class);
            total += payment.getAmount();
        }
        assertEquals(5000.0, total, 0.001);
    }
}
