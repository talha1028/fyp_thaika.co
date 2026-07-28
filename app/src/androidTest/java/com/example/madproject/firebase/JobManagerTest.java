package com.example.madproject.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.madproject.models.Job;
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
 * Instrumented (live Firestore) tests for {@link JobManager}. Hits the real
 * "madproject-5c465" Firebase project - see {@link FirebaseTestUtils} for details.
 */
@RunWith(AndroidJUnit4.class)
public class JobManagerTest {

    private final JobManager jobManager = JobManager.getInstance();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        FirebaseTestUtils.signIn();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdIds) {
            Tasks.await(jobManager.deleteJob(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdIds.clear();
    }

    private Job newJob(String id) {
        return new Job(id, "apitest_client", "Test Client", "Test Job " + id,
                "A job created by an instrumented test", "Plumbing", 5000.0, "2 weeks", "Karachi");
    }

    @Test
    public void testCreateAndGetJob_roundTrip() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        Job job = newJob(id);

        Tasks.await(jobManager.createJob(job), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        DocumentSnapshot snapshot = Tasks.await(jobManager.getJob(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        Job fetched = snapshot.toObject(Job.class);
        assertNotNull(fetched);
        assertEquals(id, fetched.getJobId());
        assertEquals("open", fetched.getStatus());
        assertEquals(5000.0, fetched.getBudget(), 0.001);
    }

    @Test
    public void testUpdateJob_and_updateField() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        Job job = newJob(id);
        Tasks.await(jobManager.createJob(job), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        job.setBudget(7500.0);
        Tasks.await(jobManager.updateJob(job), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Tasks.await(jobManager.updateField(id, "location", "Islamabad"), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(jobManager.getJob(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Job fetched = snapshot.toObject(Job.class);
        assertNotNull(fetched);
        assertEquals(7500.0, fetched.getBudget(), 0.001);
        assertEquals("Islamabad", fetched.getLocation());
    }

    @Test
    public void testDeleteJob_thenVerifyGone() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        Job job = newJob(id);
        Tasks.await(jobManager.createJob(job), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Tasks.await(jobManager.deleteJob(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(jobManager.getJob(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
    }

    @Test
    public void testIncrementTotalBids_atomicIncrement() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        Job job = newJob(id);
        Tasks.await(jobManager.createJob(job), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        Tasks.await(jobManager.incrementTotalBids(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Tasks.await(jobManager.incrementTotalBids(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Tasks.await(jobManager.incrementTotalBids(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(jobManager.getJob(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Job fetched = snapshot.toObject(Job.class);
        assertNotNull(fetched);
        assertEquals(3, fetched.getTotalBids());
    }

    @Test
    public void testAssignContractor_updatesStatusAndAssignmentFields() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        Job job = newJob(id);
        Tasks.await(jobManager.createJob(job), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        Tasks.await(jobManager.assignContractor(id, "apitest_contractor", "Test Contractor",
                        "apitest_bid", 4800.0),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(jobManager.getJob(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Job fetched = snapshot.toObject(Job.class);
        assertNotNull(fetched);
        assertEquals("in_progress", fetched.getStatus());
        assertEquals("apitest_contractor", fetched.getAssignedContractorId());
        assertEquals("apitest_bid", fetched.getAcceptedBidId());
        assertEquals(4800.0, fetched.getAcceptedBidAmount(), 0.001);
    }

    @Test
    public void testCompleteJob_setsStatusCompleted() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        Job job = newJob(id);
        Tasks.await(jobManager.createJob(job), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        Tasks.await(jobManager.completeJob(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(jobManager.getJob(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Job fetched = snapshot.toObject(Job.class);
        assertNotNull(fetched);
        assertEquals("completed", fetched.getStatus());
    }

    @Test
    public void testGetJobsByBudgetRange_findsJobWithinRange() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        Job job = newJob(id);
        job.setBudget(6000.0);
        Tasks.await(jobManager.createJob(job), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        QuerySnapshot snapshot = Tasks.await(jobManager.getJobsByBudgetRange(5000.0, 7000.0),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        boolean found = false;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            if (id.equals(doc.getId())) {
                found = true;
                break;
            }
        }
        assertTrue("Expected job within budget range to be returned", found);
    }
}
