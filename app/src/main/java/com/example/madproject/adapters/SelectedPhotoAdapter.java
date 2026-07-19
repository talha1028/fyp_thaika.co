package com.example.madproject.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.madproject.R;

import java.util.List;

public class SelectedPhotoAdapter extends RecyclerView.Adapter<SelectedPhotoAdapter.PhotoViewHolder> {

    private final Context context;
    private final List<Uri> uriList;
    private final OnRemoveClickListener listener;

    public interface OnRemoveClickListener {
        void onRemove(int position);
    }

    public SelectedPhotoAdapter(Context context, List<Uri> uriList, OnRemoveClickListener listener) {
        this.context = context;
        this.uriList = uriList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Glide.with(context)
                .load(uriList.get(position))
                .centerCrop()
                .into(holder.ivPhoto);

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemove(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return uriList.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        ImageButton btnRemove;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
