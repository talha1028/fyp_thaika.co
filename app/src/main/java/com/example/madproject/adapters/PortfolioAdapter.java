package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;

import java.util.List;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.PortfolioViewHolder> {

    private Context context;
    private List<String> imageUrls;
    private OnPortfolioItemClickListener listener;

    public interface OnPortfolioItemClickListener {
        void onItemClick(String imageUrl, int position);
        void onDeleteClick(String imageUrl, int position);
    }

    public PortfolioAdapter(Context context, List<String> imageUrls, OnPortfolioItemClickListener listener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PortfolioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_portfolio, parent, false);
        return new PortfolioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PortfolioViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);

        // Load image using placeholder for now
        // TODO: Load image using Glide/Picasso
        // Glide.with(context).load(imageUrl).into(holder.ivPortfolioImage);

        // Set placeholder
        holder.ivPortfolioImage.setImageResource(R.drawable.ic_portfolio);

        holder.ivPortfolioImage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(imageUrl, position);
            }
        });

        holder.ivPortfolioImage.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(imageUrl, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public void updateList(List<String> newList) {
        this.imageUrls = newList;
        notifyDataSetChanged();
    }

    static class PortfolioViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPortfolioImage;

        public PortfolioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPortfolioImage = itemView.findViewById(R.id.ivPortfolioImage);
        }
    }
}
