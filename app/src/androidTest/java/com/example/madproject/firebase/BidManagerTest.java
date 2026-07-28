package com.example.madproject.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.madproject.models.Bid;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented (live Firestore) tests for {@link BidManager}. Hits the real
 * "madproject-5c465" Firebase project - see {@link FirebaseTestUtils} for details.
 */
@RunWith(AndroidJUnit4.class)
public class BidManagerTest {

    private final BidManager bidManager = BidManager.getInstance();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        FirebaseTestUtils.signIn();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdIds) {
            Tasks.await(bidManager.deleteBid(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdIds.clear();
    }

    private Bid newBid(String id, String jobId) {
        return new Bid(id, jobId, "Test Job", "apitest_contractor_" + id,
                "Test Contractor", 4500.0, 10, "Proposal text for " + id);
    }

    /**
     * Polls {@code condition} until it returns true or the attempt budget is exhausted.
     * Used for {@link BidManager#rejectOtherBids}, which is fire-and-forget (returns void,
     * not a Task) so it cannot be wrapped in Tasks.await directly.
     */
    private void waitUntil(Callable<Boolean> condition, int maxAttempts, long sleepMillis) throws Exception {
        for (int i = 0; i < maxAttempts; i++) {
            if (Boolean.TRUE.equals(condition.call())) {
                return;
            }
            Thread.sleep(sleepMillis);
        }
        fail("Condition was not met within the timeout budget");
    }

    @Test
    public void testCreateAndGetBid_roundTrip() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Bid bid = newBid(id, jobId);

        Tasks.await(bidManager.createBid(bid), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        DocumentSnapshot snapshot = Tasks.await(bidManager.getBid(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        Bid fetched = snapshot.toObject(Bid.class);
        assertNotNull(fetched);
        assertEquals(id, fetched.getBidId());
        assertEquals("pending", fetched.getStatus());
        assertEquals(4500.0, fetched.getBidAmount(), 0.001);
    }

    @Test
    public void testUpdateBid_and_updateField() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Bid bid = newBid(id, jobId);
        Tasks.await(bidManager.createBid(bid), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        bid.setBidAmount(4200.0);
        Tasks.await(bidManager.updateBid(bid), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Tasks.await(bidManager.updateField(id, "completionDays", 12), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(bidManager.getBid(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Bid fetched = snapshot.toObject(Bid.class);
        assertNotNull(fetched);
        assertEquals(4200.0, fetched.getBidAmount(), 0.001);
        assertEquals(12, fetched.getCompletionDays());
    }

    @Test
    public void testDeleteBid_thenVerifyGone() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Bid bid = newBid(id, jobId);
        Tasks.await(bidManager.createBid(bid), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Tasks.await(bidManager.deleteBid(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(bidManager.getBid(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
    }

    @Test
    public void testAcceptBid_and_rejectBid_statusTransitions() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String acceptedId = FirebaseTestUtils.newTestId();
        String rejectedId = FirebaseTestUtils.newTestId();
        Tasks.await(bidManager.createBid(newBid(acceptedId, jobId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(acceptedId);
        Tasks.await(bidManager.createBid(newBid(rejectedId, jobId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(rejectedId);

        Tasks.await(bidManager.acceptBid(acceptedId), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Tasks.await(bidManager.rejectBid(rejectedId), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot acceptedSnap = Tasks.await(bidManager.getBid(acceptedId), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        DocumentSnapshot rejectedSnap = Tasks.await(bidManager.getBid(rejectedId), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals("accepted", acceptedSnap.toObject(Bid.class).getStatus());
        assertEquals("rejected", rejectedSnap.toObject(Bid.class).getStatus());
    }

    @Test
    public void testRejectOtherBids_rejectsPendingBidsExceptAccepted() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String acceptedId = FirebaseTestUtils.newTestId();
        String otherId1 = FirebaseTestUtils.newTestId();
        String otherId2 = FirebaseTestUtils.newTestId();

        Tasks.await(bidManager.createBid(newBid(acceptedId, jobId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(acceptedId);
        Tasks.await(bidManager.createBid(newBid(otherId1, jobId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(otherId1);
        Tasks.await(bidManager.createBid(newBid(otherId2, jobId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(otherId2);

        Tasks.await(bidManager.acceptBid(acceptedId), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // rejectOtherBids is fire-and-forget (void), so poll for the expected end state.
        bidManager.rejectOtherBids(jobId, acceptedId);

        waitUntil(() -> {
            DocumentSnapshot s1 = Tasks.await(bidManager.getBid(otherId1), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            DocumentSnapshot s2 = Tasks.await(bidManager.getBid(otherId2), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Bid b1 = s1.toObject(Bid.class);
            Bid b2 = s2.toObject(Bid.class);
            return b1 != null && b2 != null
                    && "rejected".equals(b1.getStatus())
                    && "rejected".equals(b2.getStatus());
        }, 10, 500);

        DocumentSnapshot acceptedSnap = Tasks.await(bidManager.getBid(acceptedId), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals("accepted", acceptedSnap.toObject(Bid.class).getStatus());
    }

    @Test
    public void testCheckExistingBid_detectsDuplicateBid() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String contractorId = "apitest_contractor_" + FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Bid bid = newBid(id, jobId);
        bid.setContractorId(contractorId);
        Tasks.await(bidManager.createBid(bid), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        QuerySnapshot snapshot = Tasks.await(bidManager.checkExistingBid(jobId, contractorId),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        assertEquals(id, snapshot.getDocuments().get(0).getId());
    }
}
