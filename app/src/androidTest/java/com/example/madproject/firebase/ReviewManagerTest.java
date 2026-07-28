package com.example.madproject.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.madproject.models.Review;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented (live Firestore) tests for {@link ReviewManager}. Hits the real
 * "madproject-5c465" Firebase project - see {@link FirebaseTestUtils} for details.
 */
@RunWith(AndroidJUnit4.class)
public class ReviewManagerTest {

    private final ReviewManager reviewManager = ReviewManager.getInstance();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        FirebaseTestUtils.signIn();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdIds) {
            Tasks.await(reviewManager.deleteReview(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdIds.clear();
    }

    private Review newReview(String id, String contractorId, String jobId, float rating) {
        return new Review(id, contractorId, "Test Contractor", "apitest_client", "Test Client",
                jobId, "Test Job", rating, "Great work, test review " + id);
    }

    @Test
    public void testCreateAndGetReview_roundTrip() throws Exception {
        String contractorId = "apitest_contractor_" + FirebaseTestUtils.newTestId();
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Review review = newReview(id, contractorId, jobId, 4.5f);

        Tasks.await(reviewManager.createReview(review), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        DocumentSnapshot snapshot = Tasks.await(reviewManager.getReview(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        Review fetched = snapshot.toObject(Review.class);
        assertNotNull(fetched);
        assertEquals(id, fetched.getReviewId());
        assertEquals(4.5f, fetched.getRating(), 0.001f);
        assertTrue(fetched.isVerified());
    }

    @Test
    public void testUpdateReview_and_addResponse() throws Exception {
        String contractorId = "apitest_contractor_" + FirebaseTestUtils.newTestId();
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Review review = newReview(id, contractorId, jobId, 3.0f);
        Tasks.await(reviewManager.createReview(review), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        review.setReviewText("Updated review text");
        Tasks.await(reviewManager.updateReview(review), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Tasks.await(reviewManager.addResponse(id, "Thank you for the feedback!"),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(reviewManager.getReview(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Review fetched = snapshot.toObject(Review.class);
        assertNotNull(fetched);
        assertEquals("Updated review text", fetched.getReviewText());
        assertEquals("Thank you for the feedback!", fetched.getResponse());
        assertTrue(fetched.getResponseDate() > 0);
    }

    @Test
    public void testDeleteReview_thenVerifyGone() throws Exception {
        String contractorId = "apitest_contractor_" + FirebaseTestUtils.newTestId();
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Review review = newReview(id, contractorId, jobId, 5.0f);
        Tasks.await(reviewManager.createReview(review), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Tasks.await(reviewManager.deleteReview(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(reviewManager.getReview(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
    }

    @Test
    public void testGetReviewsByRating_filtersByMinimumRating() throws Exception {
        String contractorId = "apitest_contractor_" + FirebaseTestUtils.newTestId();
        String jobId = FirebaseTestUtils.newTestId();
        String highId = FirebaseTestUtils.newTestId();
        String lowId = FirebaseTestUtils.newTestId();
        Tasks.await(reviewManager.createReview(newReview(highId, contractorId, jobId, 4.8f)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(highId);
        Tasks.await(reviewManager.createReview(newReview(lowId, contractorId, jobId, 2.0f)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(lowId);

        QuerySnapshot snapshot = Tasks.await(reviewManager.getReviewsByRating(contractorId, 4.0f),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        assertEquals(highId, snapshot.getDocuments().get(0).getId());
    }

    @Test
    public void testGetVerifiedReviews_filtersUnverifiedOut() throws Exception {
        String contractorId = "apitest_contractor_" + FirebaseTestUtils.newTestId();
        String jobId = FirebaseTestUtils.newTestId();
        String verifiedId = FirebaseTestUtils.newTestId();
        String unverifiedId = FirebaseTestUtils.newTestId();

        Review verified = newReview(verifiedId, contractorId, jobId, 4.0f);
        Review unverified = newReview(unverifiedId, contractorId, jobId, 4.0f);
        unverified.setVerified(false);

        Tasks.await(reviewManager.createReview(verified), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(verifiedId);
        Tasks.await(reviewManager.createReview(unverified), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(unverifiedId);

        QuerySnapshot snapshot = Tasks.await(reviewManager.getVerifiedReviews(contractorId),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        assertEquals(verifiedId, snapshot.getDocuments().get(0).getId());
    }

    @Test
    public void testCalculateAverageRating_computesCorrectAverage() throws Exception {
        String contractorId = "apitest_contractor_" + FirebaseTestUtils.newTestId();
        String jobId = FirebaseTestUtils.newTestId();
        String id1 = FirebaseTestUtils.newTestId();
        String id2 = FirebaseTestUtils.newTestId();
        Tasks.await(reviewManager.createReview(newReview(id1, contractorId, jobId, 4.0f)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id1);
        Tasks.await(reviewManager.createReview(newReview(id2, contractorId, jobId, 2.0f)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id2);

        CountDownLatch latch = new CountDownLatch(1);
        double[] avg = new double[1];
        int[] count = new int[1];
        reviewManager.calculateAverageRating(contractorId, (averageRating, totalReviews) -> {
            avg[0] = averageRating;
            count[0] = totalReviews;
            latch.countDown();
        });

        assertTrue(latch.await(FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(3.0, avg[0], 0.001);
        assertEquals(2, count[0]);
    }
}
