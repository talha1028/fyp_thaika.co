package com.example.madproject.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.madproject.models.User;
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
 * Instrumented (live Firestore) tests for {@link UserManager}. Hits the real
 * "madproject-5c465" Firebase project - see {@link FirebaseTestUtils} for details.
 */
@RunWith(AndroidJUnit4.class)
public class UserManagerTest {

    private final UserManager userManager = UserManager.getInstance();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        FirebaseTestUtils.signIn();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdIds) {
            Tasks.await(userManager.deleteUser(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdIds.clear();
    }

    private User newClientUser(String id) {
        return new User(id, id + "@test.com", "Test Client " + id, "03001234567", "client");
    }

    private User newContractorUser(String id, String category) {
        User user = newClientUser(id);
        user.setUserType("contractor");
        user.setCategory(category);
        return user;
    }

    @Test
    public void testCreateAndGetUser_roundTrip() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        User user = newClientUser(id);

        Tasks.await(userManager.createUser(user), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        DocumentSnapshot snapshot = Tasks.await(userManager.getUser(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        User fetched = snapshot.toObject(User.class);
        assertNotNull(fetched);
        assertEquals(id, fetched.getUserId());
        assertEquals(user.getEmail(), fetched.getEmail());
        assertEquals("client", fetched.getUserType());
    }

    @Test
    public void testUpdateUser_and_updateField() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        User user = newClientUser(id);
        Tasks.await(userManager.createUser(user), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        user.setCity("Lahore");
        Tasks.await(userManager.updateUser(user), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Tasks.await(userManager.updateField(id, "bio", "Updated bio"), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(userManager.getUser(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        User fetched = snapshot.toObject(User.class);
        assertNotNull(fetched);
        assertEquals("Lahore", fetched.getCity());
        assertEquals("Updated bio", fetched.getBio());
    }

    @Test
    public void testDeleteUser_thenVerifyGone() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        User user = newClientUser(id);
        Tasks.await(userManager.createUser(user), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Tasks.await(userManager.deleteUser(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(userManager.getUser(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
        // Already deleted above; no need for tearDown to delete again, but harmless if id isn't tracked.
    }

    @Test
    public void testUpdateRating_persistsRatingAndTotalReviews() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        User contractor = newContractorUser(id, "Electrician");
        Tasks.await(userManager.createUser(contractor), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        Tasks.await(userManager.updateRating(id, 4.5, 10), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(userManager.getUser(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        User fetched = snapshot.toObject(User.class);
        assertNotNull(fetched);
        assertEquals(4.5, fetched.getRating(), 0.001);
        assertEquals(10, fetched.getTotalReviews());
    }

    @Test
    public void testGetContractorsByCategory_findsCreatedContractor() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        String category = "apitest_category_" + id;
        User contractor = newContractorUser(id, category);
        Tasks.await(userManager.createUser(contractor), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        QuerySnapshot snapshot = Tasks.await(userManager.getContractorsByCategory(category), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        assertEquals(id, snapshot.getDocuments().get(0).getId());
    }

    @Test
    public void testSearchUsersByName_prefixMatch() throws Exception {
        String id = FirebaseTestUtils.newTestId();
        String uniqueName = "ApitestZZZ_" + id;
        User user = newClientUser(id);
        user.setFullName(uniqueName);
        Tasks.await(userManager.createUser(user), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        QuerySnapshot snapshot = Tasks.await(userManager.searchUsersByName(uniqueName), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.size() >= 1);
        boolean found = false;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            if (id.equals(doc.getId())) {
                found = true;
                break;
            }
        }
        assertTrue("Expected to find created user in search results", found);
    }
}
