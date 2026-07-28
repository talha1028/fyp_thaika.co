package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Job;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {

    private Context context;
    private List<Job> jobList;
    private OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(Job job);
    }

    public JobAdapter(Context context, List<Job> jobList, OnJobClickListener listener) {
        this.context = context;
        this.jobList = jobList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_job_card, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobList.get(position);

        // Set job title
        holder.tvJobTitle.setText(job.getTitle() != null ? job.getTitle() : "");

        // Set category
        holder.tvCategory.setText(job.getCategory() != null ? job.getCategory() : "");

        // Format and set budget
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "PK"));
        String budgetText = "PKR " + formatCurrency(job.getBudget());
        holder.tvBudget.setText(budgetText);

        // Set location
        holder.tvLocation.setText(job.getLocation() != null ? job.getLocation() : "");

        // Set posted date
        String dateText = getRelativeTime(job.getPostedDate());
        holder.tvPostedDate.setText(dateText);

        // Set status with color
        String status = job.getStatus() != null ? job.getStatus() : "";
        holder.tvStatus.setText(status.toUpperCase());
        setStatusColor(holder.tvStatus, status);

        // Set bid count
        String bidsText = job.getTotalBids() + " bids";
        holder.tvBidCount.setText(bidsText);

        // Set click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onJobClick(job);
            }
        });
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    private String formatCurrency(double amount) {
        if (amount >= 10000000) { // 1 Crore
            return String.format("%.1f Cr", amount / 10000000);
        } else if (amount >= 100000) { // 1 Lakh
            return String.format("%.1f L", amount / 100000);
        } else if (amount >= 1000) { // 1 Thousand
            return String.format("%.1f K", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    private void setStatusColor(TextView textView, String status) {
        int color;
        switch (status.toLowerCase()) {
            case "open":
                color = 0xFF4CAF50; // Green
                break;
            case "in_progress":
                color = 0xFFFFA726; // Orange
                break;
            case "completed":
                color = 0xFF2196F3; // Blue
                break;
            case "cancelled":
                color = 0xFFF44336; // Red
                break;
            default:
                color = 0xFF757575; // Grey
                break;
        }
        textView.setTextColor(color);
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvJobTitle, tvCategory, tvBudget, tvLocation, tvPostedDate, tvStatus, tvBidCount;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPostedDate = itemView.findViewById(R.id.tvPostedDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvBidCount = itemView.findViewById(R.id.tvBidCount);
        }
    }
}