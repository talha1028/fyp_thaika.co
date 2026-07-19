package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.madproject.R;
import com.example.madproject.models.Review;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;

    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // Set reviewer name
        holder.tvReviewerName.setText(review.getClientName());

        // Set review date
        holder.tvReviewDate.setText(getRelativeTime(review.getReviewDate()));

        // Set rating
        holder.tvRating.setText(String.format("%.1f", review.getRating()));

        // Set review comment
        holder.tvReviewComment.setText(review.getReviewText());

        // Set job title if available
        if (review.getJobTitle() != null && !review.getJobTitle().isEmpty()) {
            holder.tvJobTitle.setVisibility(View.VISIBLE);
            holder.tvJobTitle.setText(review.getJobTitle());
        } else {
            holder.tvJobTitle.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(review.getClientPhotoUrl())
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .into(holder.ivReviewerImage);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void updateData(List<Review> newList) {
        this.reviewList = newList;
        notifyDataSetChanged();
    }

    private String getRelativeTime(long timestamp) {
        if (timestamp == 0) {
            return "N/A";
        }

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;

        if (months > 0) {
            return months + " month" + (months > 1 ? "s" : "") + " ago";
        } else if (weeks > 0) {
            return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";
        } else if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivReviewerImage;
        TextView tvReviewerName, tvReviewDate, tvRating, tvReviewComment, tvJobTitle;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReviewerImage = itemView.findViewById(R.id.ivReviewerImage);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewComment = itemView.findViewById(R.id.tvReviewComment);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
        }
    }
}
