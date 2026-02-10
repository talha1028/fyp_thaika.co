package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Bid;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class BidAdapter extends RecyclerView.Adapter<BidAdapter.BidViewHolder> {

    private Context context;
    private List<Bid> bidList;
    private OnBidActionListener listener;
    private String currentUserId;
    private String jobClientId;

    public interface OnBidActionListener {
        void onAcceptBid(Bid bid);
        void onRejectBid(Bid bid);
        void onViewProfile(Bid bid);
        void onContactContractor(Bid bid);
    }

    public BidAdapter(Context context, List<Bid> bidList, String currentUserId, String jobClientId, OnBidActionListener listener) {
        this.context = context;
        this.bidList = bidList;
        this.currentUserId = currentUserId;
        this.jobClientId = jobClientId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bid_card, parent, false);
        return new BidViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BidViewHolder holder, int position) {
        Bid bid = bidList.get(position);

        // Set contractor name
        holder.tvContractorName.setText(bid.getContractorName());

        // Set category
        if (bid.getContractorCategory() != null) {
            holder.tvContractorCategory.setText(bid.getContractorCategory());
        } else {
            holder.tvContractorCategory.setVisibility(View.GONE);
        }

        // Set rating
        if (bid.getContractorRating() > 0) {
            holder.tvRating.setText(String.format("⭐ %.1f", bid.getContractorRating()));
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }

        // Set completed projects
        holder.tvCompletedProjects.setText(bid.getContractorCompletedProjects() + " projects completed");

        // Format and set bid amount
        String bidAmountText = "Rs. " + formatCurrency(bid.getBidAmount());
        holder.tvBidAmount.setText(bidAmountText);

        // Set completion days
        holder.tvCompletionDays.setText(bid.getCompletionDays() + " days");

        // Set proposal
        holder.tvProposal.setText(bid.getProposal());

        // Set submitted date
        String dateText = getRelativeTime(bid.getSubmittedDate());
        holder.tvSubmittedDate.setText("Submitted " + dateText);

        // Set status badge
        holder.tvBidStatus.setText(bid.getStatus().toUpperCase());
        setBidStatusColor(holder.tvBidStatus, bid.getStatus());

        // Show/hide action buttons based on status AND if current user is job owner
        boolean isJobOwner = currentUserId != null && currentUserId.equals(jobClientId);
        boolean isPending = "pending".equals(bid.getStatus());

        if (isJobOwner && isPending) {
            // Only job owner can accept/reject pending bids
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.tvBidStatus.setVisibility(View.GONE);
        } else {
            // Hide accept/reject buttons for contractors or non-pending bids
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.tvBidStatus.setVisibility(View.VISIBLE);
        }

        // Set click listeners
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAcceptBid(bid);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRejectBid(bid);
            }
        });

        holder.btnViewProfile.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewProfile(bid);
            }
        });

        holder.btnContact.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactContractor(bid);
            }
        });

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewProfile(bid);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bidList.size();
    }

    private String formatCurrency(double amount) {
        if (amount >= 10000000) {
            return String.format("%.1f Cr", amount / 10000000);
        } else if (amount >= 100000) {
            return String.format("%.1f L", amount / 100000);
        } else if (amount >= 1000) {
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

    private void setBidStatusColor(TextView textView, String status) {
        int color;
        switch (status.toLowerCase()) {
            case "accepted":
                color = 0xFF4CAF50; // Green
                break;
            case "rejected":
                color = 0xFFF44336; // Red
                break;
            default:
                color = 0xFFFFA726; // Orange
                break;
        }
        textView.setTextColor(color);
    }

    static class BidViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        CircleImageView ivContractorPhoto;
        TextView tvContractorName, tvContractorCategory, tvRating, tvCompletedProjects;
        TextView tvBidAmount, tvCompletionDays, tvProposal, tvSubmittedDate, tvBidStatus;
        Button btnAccept, btnReject, btnViewProfile, btnContact;

        public BidViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivContractorPhoto = itemView.findViewById(R.id.ivContractorPhoto);
            tvContractorName = itemView.findViewById(R.id.tvContractorName);
            tvContractorCategory = itemView.findViewById(R.id.tvContractorCategory);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvCompletedProjects = itemView.findViewById(R.id.tvCompletedProjects);
            tvBidAmount = itemView.findViewById(R.id.tvBidAmount);
            tvCompletionDays = itemView.findViewById(R.id.tvCompletionDays);
            tvProposal = itemView.findViewById(R.id.tvProposal);
            tvSubmittedDate = itemView.findViewById(R.id.tvSubmittedDate);
            tvBidStatus = itemView.findViewById(R.id.tvBidStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
            btnContact = itemView.findViewById(R.id.btnContact);
        }
    }
}