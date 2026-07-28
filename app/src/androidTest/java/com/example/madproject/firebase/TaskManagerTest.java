package com.example.madproject.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.madproject.models.Task;
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
 * Instrumented (live Firestore) tests for {@link TaskManager}. Hits the real
 * "madproject-5c465" Firebase project - see {@link FirebaseTestUtils} for details.
 *
 * Note: the model class here is {@code com.example.madproject.models.Task}, which
 * shares a simple name with {@code com.google.android.gms.tasks.Task} (the async
 * return type every Manager method produces). To avoid an import collision this
 * file never names the gms Task type directly - every Manager call is passed
 * straight into {@code Tasks.await(...)} inline without an intermediate variable,
 * exactly as TaskManager.java itself does internally with fully-qualified names.
 */
@RunWith(AndroidJUnit4.class)
public class TaskManagerTest {

    private final TaskManager taskManager = TaskManager.getInstance();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        FirebaseTestUtils.signIn();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdIds) {
            Tasks.await(taskManager.deleteTask(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdIds.clear();
    }

    private Task newTask(String id, String jobId) {
        return new Task(id, jobId, "Test Project", "Test Task " + id,
                "A task created by an instrumented test", "Test Worker", 2);
    }

    @Test
    public void testCreateAndGetTask_roundTrip() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Task task = newTask(id, jobId);

        Tasks.await(taskManager.createTask(task), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        DocumentSnapshot snapshot = Tasks.await(taskManager.getTask(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        Task fetched = snapshot.toObject(Task.class);
        assertNotNull(fetched);
        assertEquals(id, fetched.getTaskId());
        assertEquals("not_started", fetched.getStatus());
        assertEquals(0.0, fetched.getProgressPercentage(), 0.001);
    }

    @Test
    public void testUpdateTask_and_updateField() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Task task = newTask(id, jobId);
        Tasks.await(taskManager.createTask(task), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        task.setNumberOfWorkers(5);
        Tasks.await(taskManager.updateTask(task), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Tasks.await(taskManager.updateField(id, "dailyWages", 2500.0), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(taskManager.getTask(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Task fetched = snapshot.toObject(Task.class);
        assertNotNull(fetched);
        assertEquals(5, fetched.getNumberOfWorkers());
        assertEquals(2500.0, fetched.getDailyWages(), 0.001);
    }

    @Test
    public void testDeleteTask_thenVerifyGone() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Task task = newTask(id, jobId);
        Tasks.await(taskManager.createTask(task), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Tasks.await(taskManager.deleteTask(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(taskManager.getTask(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
    }

    @Test
    public void testUpdateProgress_computesPercentageAndAutoCompletesAt100() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Task task = newTask(id, jobId);
        task.setEstimatedQuantity(100.0);
        Tasks.await(taskManager.createTask(task), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        Tasks.await(taskManager.updateProgress(id, 50.0), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        DocumentSnapshot midSnapshot = Tasks.await(taskManager.getTask(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Task midFetched = midSnapshot.toObject(Task.class);
        assertNotNull(midFetched);
        assertEquals(50.0, midFetched.getProgressPercentage(), 0.001);
        assertEquals("not_started", midFetched.getStatus());

        Tasks.await(taskManager.updateProgress(id, 100.0), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        DocumentSnapshot finalSnapshot = Tasks.await(taskManager.getTask(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Task finalFetched = finalSnapshot.toObject(Task.class);
        assertNotNull(finalFetched);
        assertEquals(100.0, finalFetched.getProgressPercentage(), 0.001);
        assertEquals("completed", finalFetched.getStatus());
    }

    @Test
    public void testCompleteTask_setsStatusAndFullProgress() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Task task = newTask(id, jobId);
        Tasks.await(taskManager.createTask(task), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        Tasks.await(taskManager.completeTask(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(taskManager.getTask(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Task fetched = snapshot.toObject(Task.class);
        assertNotNull(fetched);
        assertEquals("completed", fetched.getStatus());
        assertEquals(100.0, fetched.getProgressPercentage(), 0.001);
    }

    @Test
    public void testGetOngoingTasks_and_getCompletedTasks_filterByStatus() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String ongoingId = FirebaseTestUtils.newTestId();
        String completedId = FirebaseTestUtils.newTestId();

        Tasks.await(taskManager.createTask(newTask(ongoingId, jobId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(ongoingId);
        Tasks.await(taskManager.createTask(newTask(completedId, jobId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(completedId);

        Tasks.await(taskManager.updateTaskStatus(ongoingId, "ongoing"), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Tasks.await(taskManager.completeTask(completedId), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        QuerySnapshot ongoingSnapshot = Tasks.await(taskManager.getOngoingTasks(jobId), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        QuerySnapshot completedSnapshot = Tasks.await(taskManager.getCompletedTasks(jobId), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertEquals(1, ongoingSnapshot.size());
        assertEquals(ongoingId, ongoingSnapshot.getDocuments().get(0).getId());
        assertEquals(1, completedSnapshot.size());
        assertEquals(completedId, completedSnapshot.getDocuments().get(0).getId());
    }
}
