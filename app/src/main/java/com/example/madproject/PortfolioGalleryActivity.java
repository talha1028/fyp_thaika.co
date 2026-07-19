package com.example.madproject;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.PortfolioAdapter;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class PortfolioGalleryActivity extends AppCompatActivity {

    private static final String TAG = "PortfolioGallery";

    private RecyclerView rvPortfolio;
    private FloatingActionButton fabAddPortfolio;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private android.widget.TextView tvTotalProjects, tvTotalPhotos, tvTotalLikes;

    private String contractorId;
    private String currentUserId;
    private boolean isOwnProfile;

    private StorageReference storageRef;
    private PortfolioAdapter portfolioAdapter;
    private List<String> portfolioList;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadPortfolioImage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio_gallery);

        // Get contractor ID from intent
        contractorId = getIntent().getStringExtra("contractorId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Check if viewing own profile
        isOwnProfile = currentUserId.equals(contractorId);

        if (contractorId == null || contractorId.isEmpty()) {
            contractorId = currentUserId;
            isOwnProfile = true;
        }

        storageRef = FirebaseStorage.getInstance().getReference();
        initViews();
        setupRecyclerView();
        loadPortfolio();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("");
        }

        rvPortfolio     = findViewById(R.id.rvPortfolio);
        fabAddPortfolio = findViewById(R.id.fabAddPortfolio);
        progressBar     = findViewById(R.id.progressBar);
        emptyState      = findViewById(R.id.emptyState);
        tvTotalProjects = findViewById(R.id.tvTotalProjects);
        tvTotalPhotos   = findViewById(R.id.tvTotalPhotos);
        tvTotalLikes    = findViewById(R.id.tvTotalLikes);
        if (tvTotalLikes != null) tvTotalLikes.setText("—");

        rvPortfolio.setLayoutManager(new GridLayoutManager(this, 2));

        // Only show add button for own profile
        if (fabAddPortfolio != null) {
            fabAddPortfolio.setVisibility(isOwnProfile ? View.VISIBLE : View.GONE);
            fabAddPortfolio.setOnClickListener(v -> addPortfolioItem());
        }
    }

    private void setupRecyclerView() {
        portfolioList = new ArrayList<>();

        portfolioAdapter = new PortfolioAdapter(this, portfolioList, new PortfolioAdapter.OnPortfolioItemClickListener() {
            @Override
            public void onItemClick(String imageUrl, int position) {
                // View full image - could open image viewer
                Toast.makeText(PortfolioGalleryActivity.this,
                        "Image " + (position + 1), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(String imageUrl, int position) {
                if (isOwnProfile) {
                    confirmDeleteImage(imageUrl, position);
                }
            }
        });

        rvPortfolio.setAdapter(portfolioAdapter);
    }

    private void loadPortfolio() {
        showLoading(true);

        UserManager.getInstance().getUserObject(contractorId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                showLoading(false);

                List<String> portfolio = user.getPortfolioImages();
                portfolioList.clear();

                if (portfolio != null && !portfolio.isEmpty()) {
                    portfolioList.addAll(portfolio);
                }

                portfolioAdapter.notifyDataSetChanged();
                updateEmptyState();

                if (tvTotalPhotos   != null) tvTotalPhotos.setText(String.valueOf(portfolioList.size()));
                if (tvTotalProjects != null) tvTotalProjects.setText(String.valueOf(user.getCompletedProjects()));

                Log.d(TAG, "Loaded " + portfolioList.size() + " portfolio images");
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error loading portfolio: " + error);
                Toast.makeText(PortfolioGalleryActivity.this,
                        "Error loading portfolio", Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void addPortfolioItem() {
        imagePickerLauncher.launch("image/*");
    }

    private void uploadPortfolioImage(Uri imageUri) {
        showLoading(true);
        String fileName = "portfolio/" + currentUserId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storageRef.child(fileName);
        ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String url = downloadUri.toString();
                    portfolioList.add(url);
                    UserManager.getInstance()
                            .updateField(currentUserId, "portfolioImages", new ArrayList<>(portfolioList))
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                portfolioAdapter.notifyItemInserted(portfolioList.size() - 1);
                                updateEmptyState();
                                if (tvTotalPhotos != null) tvTotalPhotos.setText(String.valueOf(portfolioList.size()));
                                Toast.makeText(this, "Image added to portfolio", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                portfolioList.remove(url);
                                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDeleteImage(String imageUrl, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to remove this image from your portfolio?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteImage(imageUrl, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage(String imageUrl, int position) {
        // Remove from local list
        portfolioList.remove(position);

        // Update in Firestore
        UserManager.getInstance()
                .updateField(currentUserId, "portfolioImages", new ArrayList<>(portfolioList))
                .addOnSuccessListener(aVoid -> {
                    portfolioAdapter.notifyItemRemoved(position);
                    if (tvTotalPhotos != null) tvTotalPhotos.setText(String.valueOf(portfolioList.size()));
                    Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    // Restore the item
                    portfolioList.add(position, imageUrl);
                    portfolioAdapter.notifyItemInserted(position);
                    Toast.makeText(this, "Failed to remove image", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            rvPortfolio.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (portfolioList.isEmpty()) {
            rvPortfolio.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        } else {
            rvPortfolio.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
