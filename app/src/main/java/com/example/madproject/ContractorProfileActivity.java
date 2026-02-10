package com.example.madproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.PortfolioAdapter;
import com.example.madproject.adapters.ReviewAdapter;
import com.example.madproject.firebase.ReviewManager;
import com.example.madproject.firebase.UserManager;
import com.example.madproject.models.Review;
import com.example.madproject.models.User;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContractorProfileActivity extends AppCompatActivity {

    private static final String TAG = "ContractorProfile";

    private CircleImageView ivProfileImage;
    private TextView tvName, tvCategory, tvRating, tvExperience, tvHourlyRate, tvBio;
    private TextView tvCompletedProjects, tvTotalReviews, tvLocation;
    private TextView tvViewAllPortfolio, tvViewAllReviews;
    private RecyclerView rvPortfolio, rvReviews;
    private Button btnCall, btnMessage;
    private ProgressBar progressBar;
    private LinearLayout portfolioSection, reviewsSection;

    private String contractorId;
    private User contractor;

    private PortfolioAdapter portfolioAdapter;
    private ReviewAdapter reviewAdapter;
    private List<String> portfolioList;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contractor_profile);

        // Get contractor ID from intent
        contractorId = getIntent().getStringExtra("contractorId");

        if (contractorId == null || contractorId.isEmpty()) {
            Toast.makeText(this, "Error: Contractor not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerViews();
        setupClickListeners();
        loadProfile();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Contractor Profile");
        }

        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvName = findViewById(R.id.tvName);
        tvCategory = findViewById(R.id.tvCategory);
        tvRating = findViewById(R.id.tvRating);
        tvExperience = findViewById(R.id.tvExperience);
        tvHourlyRate = findViewById(R.id.tvHourlyRate);
        tvBio = findViewById(R.id.tvBio);
        tvCompletedProjects = findViewById(R.id.tvCompletedProjects);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        tvLocation = findViewById(R.id.tvLocation);
        tvViewAllPortfolio = findViewById(R.id.tvViewAllPortfolio);
        tvViewAllReviews = findViewById(R.id.tvViewAllReviews);
        btnCall = findViewById(R.id.btnCall);
        btnMessage = findViewById(R.id.btnMessage);
        rvPortfolio = findViewById(R.id.rvPortfolio);
        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        portfolioSection = findViewById(R.id.portfolioSection);
        reviewsSection = findViewById(R.id.reviewsSection);

        rvPortfolio.setLayoutManager(new GridLayoutManager(this, 3));
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRecyclerViews() {
        // Portfolio adapter
        portfolioList = new ArrayList<>();
        portfolioAdapter = new PortfolioAdapter(this, portfolioList, new PortfolioAdapter.OnPortfolioItemClickListener() {
            @Override
            public void onItemClick(String imageUrl, int position) {
                // View full image
                Intent intent = new Intent(ContractorProfileActivity.this, PortfolioGalleryActivity.class);
                intent.putExtra("contractorId", contractorId);
                intent.putExtra("selectedPosition", position);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(String imageUrl, int position) {
                // Only show delete for own profile - not applicable here
            }
        });
        rvPortfolio.setAdapter(portfolioAdapter);

        // Reviews adapter
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupClickListeners() {
        btnMessage.setOnClickListener(v -> {
            if (contractor != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("receiverId", contractorId);
                intent.putExtra("receiverName", contractor.getFullName());
                startActivity(intent);
            }
        });

        btnCall.setOnClickListener(v -> {
            if (contractor != null && contractor.getPhoneNumber() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + contractor.getPhoneNumber()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        if (tvViewAllPortfolio != null) {
            tvViewAllPortfolio.setOnClickListener(v -> {
                Intent intent = new Intent(this, PortfolioGalleryActivity.class);
                intent.putExtra("contractorId", contractorId);
                startActivity(intent);
            });
        }

        if (tvViewAllReviews != null) {
            tvViewAllReviews.setOnClickListener(v -> {
                Intent intent = new Intent(this, AllReviewsActivity.class);
                intent.putExtra("contractorId", contractorId);
                if (contractor != null) {
                    intent.putExtra("contractorName", contractor.getFullName());
                }
                startActivity(intent);
            });
        }
    }

    private void loadProfile() {
        showLoading(true);

        UserManager.getInstance().getUserObject(contractorId, new UserManager.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                showLoading(false);
                contractor = user;
                displayProfile(user);
                loadReviews();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error loading profile: " + error);
                Toast.makeText(ContractorProfileActivity.this,
                        "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayProfile(User user) {
        // Set name
        tvName.setText(user.getFullName());

        // Set category
        if (user.getCategory() != null && !user.getCategory().isEmpty()) {
            tvCategory.setText(user.getCategory());
            tvCategory.setVisibility(View.VISIBLE);
        } else {
            tvCategory.setVisibility(View.GONE);
        }

        // Set rating
        if (user.getRating() > 0) {
            tvRating.setText(String.format("%.1f (%d reviews)", user.getRating(), user.getTotalReviews()));
        } else {
            tvRating.setText("No reviews yet");
        }

        // Set experience
        if (user.getExperienceYears() > 0) {
            tvExperience.setText(user.getExperienceYears() + " years experience");
            tvExperience.setVisibility(View.VISIBLE);
        } else {
            tvExperience.setVisibility(View.GONE);
        }

        // Set hourly rate
        if (user.getHourlyRate() > 0) {
            tvHourlyRate.setText("Rs. " + formatCurrency(user.getHourlyRate()) + "/hr");
            tvHourlyRate.setVisibility(View.VISIBLE);
        } else {
            tvHourlyRate.setVisibility(View.GONE);
        }

        // Set bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            tvBio.setText(user.getBio());
            tvBio.setVisibility(View.VISIBLE);
        } else {
            tvBio.setVisibility(View.GONE);
        }

        // Set completed projects
        if (tvCompletedProjects != null) {
            tvCompletedProjects.setText(user.getCompletedProjects() + " projects");
        }

        // Set total reviews
        if (tvTotalReviews != null) {
            tvTotalReviews.setText(user.getTotalReviews() + " reviews");
        }

        // Set location
        if (tvLocation != null) {
            if (user.getCity() != null && !user.getCity().isEmpty()) {
                tvLocation.setText(user.getCity());
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }
        }

        // Load portfolio images
        List<String> portfolio = user.getPortfolioImages();
        if (portfolio != null && !portfolio.isEmpty()) {
            portfolioList.clear();
            // Show only first 6 images
            int limit = Math.min(portfolio.size(), 6);
            for (int i = 0; i < limit; i++) {
                portfolioList.add(portfolio.get(i));
            }
            portfolioAdapter.notifyDataSetChanged();

            if (portfolioSection != null) portfolioSection.setVisibility(View.VISIBLE);

            // Show "View All" if more than 6 images
            if (tvViewAllPortfolio != null) {
                tvViewAllPortfolio.setVisibility(portfolio.size() > 6 ? View.VISIBLE : View.GONE);
            }
        } else {
            if (portfolioSection != null) portfolioSection.setVisibility(View.GONE);
        }

        // TODO: Load profile image using Glide/Picasso
        // if (user.getProfilePictureUrl() != null) {
        //     Glide.with(this).load(user.getProfilePictureUrl()).into(ivProfileImage);
        // }
    }

    private void loadReviews() {
        ReviewManager.getInstance()
                .getReviewsByContractor(contractorId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            reviewList.add(review);
                        }
                    }

                    // Sort by date (newest first)
                    Collections.sort(reviewList, (r1, r2) ->
                            Long.compare(r2.getReviewDate(), r1.getReviewDate()));

                    // Show only first 3 reviews
                    List<Review> limitedReviews = new ArrayList<>();
                    int limit = Math.min(reviewList.size(), 3);
                    for (int i = 0; i < limit; i++) {
                        limitedReviews.add(reviewList.get(i));
                    }

                    reviewAdapter.updateData(limitedReviews);

                    if (reviewsSection != null) {
                        reviewsSection.setVisibility(reviewList.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    // Show "View All" if more than 3 reviews
                    if (tvViewAllReviews != null) {
                        tvViewAllReviews.setVisibility(reviewList.size() > 3 ? View.VISIBLE : View.GONE);
                    }

                    Log.d(TAG, "Loaded " + reviewList.size() + " reviews");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews: " + e.getMessage());
                });
    }

    private String formatCurrency(double amount) {
        if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
