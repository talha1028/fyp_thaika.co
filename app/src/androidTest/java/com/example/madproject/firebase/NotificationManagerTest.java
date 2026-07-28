package com.example.madproject.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.madproject.models.Notification;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented (live Firestore) tests for {@link NotificationManager}. Hits the real
 * "madproject-5c465" Firebase project - see {@link FirebaseTestUtils} for details.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationManagerTest {

    private final NotificationManager notificationManager = NotificationManager.getInstance();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        FirebaseTestUtils.signIn();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdIds) {
            Tasks.await(notificationManager.deleteNotification(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdIds.clear();
    }

    private Notification newNotification(String id, String userId, String type) {
        return new Notification(id, userId, "Test Title", "Test message body " + id, type, "apitest_related");
    }

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
    public void testCreateAndGetNotification_roundTrip() throws Exception {
        String userId = "apitest_user_" + FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Notification notification = newNotification(id, userId, "job");

        Tasks.await(notificationManager.createNotification(notification), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        DocumentSnapshot snapshot = Tasks.await(notificationManager.getNotification(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        Notification fetched = snapshot.toObject(Notification.class);
        assertNotNull(fetched);
        assertEquals(id, fetched.getNotificationId());
        assertFalse(fetched.isRead());
    }

    @Test
    public void testMarkAsRead_updatesIsReadAndReadAt() throws Exception {
        String userId = "apitest_user_" + FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Notification notification = newNotification(id, userId, "job");
        Tasks.await(notificationManager.createNotification(notification), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        Tasks.await(notificationManager.markAsRead(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(notificationManager.getNotification(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Notification fetched = snapshot.toObject(Notification.class);
        assertNotNull(fetched);
        assertTrue(fetched.isRead());
        assertTrue(fetched.getReadAt() > 0);
    }

    @Test
    public void testDeleteNotification_thenVerifyGone() throws Exception {
        String userId = "apitest_user_" + FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Notification notification = newNotification(id, userId, "job");
        Tasks.await(notificationManager.createNotification(notification), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Tasks.await(notificationManager.deleteNotification(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(notificationManager.getNotification(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
    }

    @Test
    public void testGetNotificationsByType_filtersCorrectly() throws Exception {
        String userId = "apitest_user_" + FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Notification notification = newNotification(id, userId, "payment");
        Tasks.await(notificationManager.createNotification(notification), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        QuerySnapshot snapshot = Tasks.await(notificationManager.getNotificationsByType(userId, "payment"),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        assertEquals(id, snapshot.getDocuments().get(0).getId());
    }

    @Test
    public void testGetUnreadCount_countsCorrectly() throws Exception {
        String userId = "apitest_user_" + FirebaseTestUtils.newTestId();
        String id1 = FirebaseTestUtils.newTestId();
        String id2 = FirebaseTestUtils.newTestId();
        Tasks.await(notificationManager.createNotification(newNotification(id1, userId, "job")), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id1);
        Tasks.await(notificationManager.createNotification(newNotification(id2, userId, "bid")), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id2);

        CountDownLatch latch = new CountDownLatch(1);
        int[] result = new int[1];
        notificationManager.getUnreadCount(userId, count -> {
            result[0] = count;
            latch.countDown();
        });

        assertTrue(latch.await(FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(2, result[0]);
    }

    @Test
    public void testMarkAllAsRead_marksAllUnreadForUser() throws Exception {
        String userId = "apitest_user_" + FirebaseTestUtils.newTestId();
        String id1 = FirebaseTestUtils.newTestId();
        String id2 = FirebaseTestUtils.newTestId();
        Tasks.await(notificationManager.createNotification(newNotification(id1, userId, "job")), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id1);
        Tasks.await(notificationManager.createNotification(newNotification(id2, userId, "bid")), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id2);

        // markAllAsRead is fire-and-forget (void), so poll for the expected end state.
        notificationManager.markAllAsRead(userId);

        waitUntil(() -> {
            DocumentSnapshot s1 = Tasks.await(notificationManager.getNotification(id1), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            DocumentSnapshot s2 = Tasks.await(notificationManager.getNotification(id2), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Notification n1 = s1.toObject(Notification.class);
            Notification n2 = s2.toObject(Notification.class);
            return n1 != null && n2 != null && n1.isRead() && n2.isRead();
        }, 10, 500);
    }
}
