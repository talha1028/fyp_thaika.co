package com.example.madproject;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.adapters.ReviewAdapter;
import com.example.madproject.firebase.ReviewManager;
import com.example.madproject.models.Review;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllReviewsActivity extends AppCompatActivity {

    private static final String TAG = "AllReviewsActivity";

    private RecyclerView rvReviews;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView tvAverageRating, tvTotalReviews;

    private String contractorId;
    private String contractorName;

    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);

        // Get contractor ID from intent
        contractorId = getIntent().getStringExtra("contractorId");
        contractorName = getIntent().getStringExtra("contractorName");

        if (contractorId == null || contractorId.isEmpty()) {
            Toast.makeText(this, "Error: Contractor not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        loadReviews();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (contractorName != null && !contractorName.isEmpty()) {
                getSupportActionBar().setTitle(contractorName + "'s Reviews");
            } else {
                getSupportActionBar().setTitle("Reviews");
            }
        }

        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupRecyclerView() {
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        rvReviews.setAdapter(reviewAdapter);
    }

    private void loadReviews() {
        showLoading(true);

        ReviewManager.getInstance()
                .getReviewsByContractor(contractorId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);

                    reviewList.clear();

                    float totalRating = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            reviewList.add(review);
                            totalRating += review.getRating();
                            count++;
                        }
                    }

                    // Sort by date (newest first)
                    Collections.sort(reviewList, (r1, r2) ->
                            Long.compare(r2.getReviewDate(), r1.getReviewDate()));

                    reviewAdapter.notifyDataSetChanged();
                    updateEmptyState();

                    // Update summary
                    if (tvTotalReviews != null) {
                        tvTotalReviews.setText(count + " Reviews");
                    }

                    if (tvAverageRating != null) {
                        if (count > 0) {
                            float average = totalRating / count;
                            tvAverageRating.setText(String.format("%.1f", average));
                        } else {
                            tvAverageRating.setText("N/A");
                        }
                    }

                    Log.d(TAG, "Loaded " + reviewList.size() + " reviews");
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading reviews: " + e.getMessage());
                    Toast.makeText(this, "Error loading reviews", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            rvReviews.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (reviewList.isEmpty()) {
            rvReviews.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        } else {
            rvReviews.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
