package com.example.madproject.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.madproject.models.Material;
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
 * Instrumented (live Firestore) tests for {@link MaterialManager}. Hits the real
 * "madproject-5c465" Firebase project - see {@link FirebaseTestUtils} for details.
 */
@RunWith(AndroidJUnit4.class)
public class MaterialManagerTest {

    private final MaterialManager materialManager = MaterialManager.getInstance();
    private final List<String> createdIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() {
        FirebaseTestUtils.signIn();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdIds) {
            Tasks.await(materialManager.deleteMaterial(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdIds.clear();
    }

    private Material newMaterial(String id, String jobId, double quantity, double unitPrice) {
        return new Material(id, jobId, "Test Project", "Cement Bags", "Cement",
                quantity, "bags", unitPrice, "Test Supplier");
    }

    @Test
    public void testCreateAndGetMaterial_roundTrip() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Material material = newMaterial(id, jobId, 20.0, 10.0);

        Tasks.await(materialManager.createMaterial(material), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        DocumentSnapshot snapshot = Tasks.await(materialManager.getMaterial(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        Material fetched = snapshot.toObject(Material.class);
        assertNotNull(fetched);
        assertEquals(id, fetched.getMaterialId());
        assertEquals(200.0, fetched.getTotalCost(), 0.001);
        assertEquals("in_stock", fetched.getStatus());
    }

    @Test
    public void testUpdateMaterial_and_updateField() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Material material = newMaterial(id, jobId, 20.0, 10.0);
        Tasks.await(materialManager.createMaterial(material), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        material.setSupplier("New Supplier");
        Tasks.await(materialManager.updateMaterial(material), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Tasks.await(materialManager.updateField(id, "description", "Updated description"),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(materialManager.getMaterial(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Material fetched = snapshot.toObject(Material.class);
        assertNotNull(fetched);
        assertEquals("New Supplier", fetched.getSupplier());
        assertEquals("Updated description", fetched.getDescription());
    }

    @Test
    public void testDeleteMaterial_thenVerifyGone() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Material material = newMaterial(id, jobId, 20.0, 10.0);
        Tasks.await(materialManager.createMaterial(material), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Tasks.await(materialManager.deleteMaterial(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(materialManager.getMaterial(id), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
    }

    @Test
    public void testGetLowStockMaterials_thresholdLogic() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Material material = newMaterial(id, jobId, 20.0, 10.0);
        material.setLowStockThreshold(5.0);
        // setQuantity() recomputes status against the threshold (checkStockStatus()).
        material.setQuantity(3.0);
        Tasks.await(materialManager.createMaterial(material), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        assertEquals("low_stock", material.getStatus());

        QuerySnapshot snapshot = Tasks.await(materialManager.getLowStockMaterials(jobId),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        assertEquals(id, snapshot.getDocuments().get(0).getId());
    }

    @Test
    public void testGetOutOfStockMaterials_zeroQuantity() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id = FirebaseTestUtils.newTestId();
        Material material = newMaterial(id, jobId, 20.0, 10.0);
        // setQuantity(0) forces "out_of_stock" regardless of threshold (checkStockStatus()).
        material.setQuantity(0.0);
        Tasks.await(materialManager.createMaterial(material), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id);

        QuerySnapshot snapshot = Tasks.await(materialManager.getOutOfStockMaterials(jobId),
                FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(1, snapshot.size());
        assertEquals(id, snapshot.getDocuments().get(0).getId());
    }

    @Test
    public void testCalculateTotalInventoryValue_sumsCostsAcrossMaterials() throws Exception {
        String jobId = FirebaseTestUtils.newTestId();
        String id1 = FirebaseTestUtils.newTestId();
        String id2 = FirebaseTestUtils.newTestId();
        Material material1 = newMaterial(id1, jobId, 10.0, 5.0);   // totalCost = 50.0
        Material material2 = newMaterial(id2, jobId, 4.0, 2.5);    // totalCost = 10.0
        Tasks.await(materialManager.createMaterial(material1), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id1);
        Tasks.await(materialManager.createMaterial(material2), FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        createdIds.add(id2);

        CountDownLatch latch = new CountDownLatch(1);
        double[] result = new double[1];
        materialManager.calculateTotalInventoryValue(jobId, total -> {
            result[0] = total;
            latch.countDown();
        });

        assertTrue(latch.await(FirebaseTestUtils.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(60.0, result[0], 0.001);
    }
}
