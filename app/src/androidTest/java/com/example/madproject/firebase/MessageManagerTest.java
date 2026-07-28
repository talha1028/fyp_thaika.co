package com.example.madproject.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.madproject.models.Message;
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
 * Instrumented (live Firestore) tests for {@link MessageManager}. Hits the real
 * "madproject-5c465" Firebase project - see {@link FirebaseTestUtils} for details.
 */
@RunWith(AndroidJUnit4.class)
public class MessageManagerTest {

    private final MessageManager messageManager = MessageManager.getInstance();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        FirebaseTestUtils.signIn();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdIds) {
            Tasks.await(messageManager.deleteMessage(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdIds.clear();
    }

    private Message newMessage(String id, String chatId, String receiverId) {
        return new Message(id, chatId, "apitest_sender", "Test Sender", receiverId,
                "Hello from an instrumented test " + id);
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
    public void testCreateAndGetMessage_roundTrip() throws Exception {
        String chatId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Message message = newMessage(id, chatId, "apitest_receiver");

        Tasks.await(messageManager.createMessage(message), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        DocumentSnapshot snapshot = Tasks.await(messageManager.getMessage(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        Message fetched = snapshot.toObject(Message.class);
        assertNotNull(fetched);
        assertEquals(id, fetched.getMessageId());
        assertFalse(fetched.isRead());
    }

    @Test
    public void testMarkAsRead_updatesIsReadAndReadAt() throws Exception {
        String chatId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Message message = newMessage(id, chatId, "apitest_receiver");
        Tasks.await(messageManager.createMessage(message), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        Tasks.await(messageManager.markAsRead(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(messageManager.getMessage(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Message fetched = snapshot.toObject(Message.class);
        assertNotNull(fetched);
        assertTrue(fetched.isRead());
        assertTrue(fetched.getReadAt() > 0);
    }

    @Test
    public void testDeleteMessage_thenVerifyGone() throws Exception {
        String chatId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Message message = newMessage(id, chatId, "apitest_receiver");
        Tasks.await(messageManager.createMessage(message), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Tasks.await(messageManager.deleteMessage(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(messageManager.getMessage(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
    }

    @Test
    public void testGetUnreadMessages_findsUnreadForReceiver() throws Exception {
        String chatId = FirebaseTestUtils.newTestId();
        String receiverId = "apitest_receiver_" + FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Message message = newMessage(id, chatId, receiverId);
        Tasks.await(messageManager.createMessage(message), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        QuerySnapshot snapshot = Tasks.await(messageManager.getUnreadMessages(receiverId),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        assertEquals(id, snapshot.getDocuments().get(0).getId());
    }

    @Test
    public void testGetUnreadCount_countsCorrectly() throws Exception {
        String chatId = FirebaseTestUtils.newTestId();
        String receiverId = "apitest_receiver_" + FirebaseTestUtils.newTestId();
        String id1 = FirebaseTestUtils.newTestId();
        String id2 = FirebaseTestUtils.newTestId();
        Tasks.await(messageManager.createMessage(newMessage(id1, chatId, receiverId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id1);
        Tasks.await(messageManager.createMessage(newMessage(id2, chatId, receiverId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id2);

        CountDownLatch latch = new CountDownLatch(1);
        int[] result = new int[1];
        messageManager.getUnreadCount(receiverId, count -> {
            result[0] = count;
            latch.countDown();
        });

        assertTrue(latch.await(FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(2, result[0]);
    }

    @Test
    public void testMarkAllAsRead_marksAllUnreadMessagesInChat() throws Exception {
        String chatId = FirebaseTestUtils.newTestId();
        String receiverId = "apitest_receiver_" + FirebaseTestUtils.newTestId();
        String id1 = FirebaseTestUtils.newTestId();
        String id2 = FirebaseTestUtils.newTestId();
        Tasks.await(messageManager.createMessage(newMessage(id1, chatId, receiverId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id1);
        Tasks.await(messageManager.createMessage(newMessage(id2, chatId, receiverId)), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id2);

        // markAllAsRead is fire-and-forget (void), so poll for the expected end state.
        messageManager.markAllAsRead(chatId, receiverId);

        waitUntil(() -> {
            DocumentSnapshot s1 = Tasks.await(messageManager.getMessage(id1), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            DocumentSnapshot s2 = Tasks.await(messageManager.getMessage(id2), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Message m1 = s1.toObject(Message.class);
            Message m2 = s2.toObject(Message.class);
            return m1 != null && m2 != null && m1.isRead() && m2.isRead();
        }, 10, 500);
    }
}
