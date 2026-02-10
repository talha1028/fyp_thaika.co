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
import com.example.madproject.models.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContractorAdapter extends RecyclerView.Adapter<ContractorAdapter.ContractorViewHolder> {

    private Context context;
    private List<User> contractorList;
    private OnContractorClickListener listener;

    public interface OnContractorClickListener {
        void onContractorClick(User contractor);
    }

    public ContractorAdapter(Context context, List<User> contractorList, OnContractorClickListener listener) {
        this.context = context;
        this.contractorList = contractorList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContractorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contractor, parent, false);
        return new ContractorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContractorViewHolder holder, int position) {
        User contractor = contractorList.get(position);

        // Set contractor name
        holder.tvContractorName.setText(contractor.getFullName());

        // Set category
        if (contractor.getCategory() != null) {
            holder.tvCategory.setText(contractor.getCategory());
            holder.tvCategory.setVisibility(View.VISIBLE);
        } else {
            holder.tvCategory.setVisibility(View.GONE);
        }

        // Set rating
        if (contractor.getRating() > 0) {
            holder.tvRating.setText(String.format("%.1f (%d reviews)",
                    contractor.getRating(), contractor.getTotalReviews()));
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setText("No reviews yet");
            holder.tvRating.setVisibility(View.VISIBLE);
        }

        // Set experience
        if (contractor.getExperienceYears() > 0) {
            holder.tvExperience.setText(contractor.getExperienceYears() + " years experience");
            holder.tvExperience.setVisibility(View.VISIBLE);
        } else {
            holder.tvExperience.setVisibility(View.GONE);
        }

        // Set completed projects
        holder.tvCompletedProjects.setText(contractor.getCompletedProjects() + " projects completed");

        // Set hourly rate
        if (contractor.getHourlyRate() > 0) {
            holder.tvHourlyRate.setText("Rs. " + formatCurrency(contractor.getHourlyRate()) + "/hr");
            holder.tvHourlyRate.setVisibility(View.VISIBLE);
        } else {
            holder.tvHourlyRate.setVisibility(View.GONE);
        }

        // Set location
        if (contractor.getCity() != null && !contractor.getCity().isEmpty()) {
            holder.tvLocation.setText(contractor.getCity());
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContractorClick(contractor);
            }
        });

        // TODO: Load profile image using Glide/Picasso
        // if (contractor.getProfilePictureUrl() != null) {
        //     Glide.with(context).load(contractor.getProfilePictureUrl()).into(holder.ivContractorImage);
        // }
    }

    @Override
    public int getItemCount() {
        return contractorList.size();
    }

    private String formatCurrency(double amount) {
        if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    public void updateList(List<User> newList) {
        this.contractorList = newList;
        notifyDataSetChanged();
    }

    static class ContractorViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        CircleImageView ivContractorImage;
        TextView tvContractorName, tvCategory, tvRating, tvExperience;
        TextView tvCompletedProjects, tvHourlyRate, tvLocation;

        public ContractorViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivContractorImage = itemView.findViewById(R.id.ivContractorImage);
            tvContractorName = itemView.findViewById(R.id.tvContractorName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvCompletedProjects = itemView.findViewById(R.id.tvCompletedProjects);
            tvHourlyRate = itemView.findViewById(R.id.tvHourlyRate);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }
    }
}
