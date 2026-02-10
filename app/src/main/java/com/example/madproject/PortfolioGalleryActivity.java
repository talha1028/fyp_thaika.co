package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

public class PortfolioGalleryActivity extends AppCompatActivity {

    private static final String TAG = "PortfolioGallery";

    private RecyclerView rvPortfolio;
    private FloatingActionButton fabAddPortfolio;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    private String contractorId;
    private String currentUserId;
    private boolean isOwnProfile;

    private PortfolioAdapter portfolioAdapter;
    private List<String> portfolioList;

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
            // If no contractor ID, use current user's profile
            contractorId = currentUserId;
            isOwnProfile = true;
        }

        initViews();
        setupRecyclerView();
        loadPortfolio();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Portfolio");
        }

        rvPortfolio = findViewById(R.id.rvPortfolio);
        fabAddPortfolio = findViewById(R.id.fabAddPortfolio);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

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
        // TODO: Implement image picker and upload
        Toast.makeText(this, "Image upload coming soon", Toast.LENGTH_SHORT).show();
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
