package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.madproject.R;
import com.example.madproject.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<Message> messageList;
    private String currentUserId;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                           FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);

        // Hide all layouts first
        holder.receivedMessageLayout.setVisibility(View.GONE);
        holder.sentMessageLayout.setVisibility(View.GONE);
        holder.imageMessageLayout.setVisibility(View.GONE);

        String timeText = formatTime(message.getTimestamp());

        if (message.getMessageType().equals("image") && message.getAttachmentUrl() != null) {
            // Show image message
            holder.imageMessageLayout.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(message.getAttachmentUrl())
                    .placeholder(R.drawable.ic_gallery)
                    .error(R.drawable.ic_gallery)
                    .centerCrop()
                    .into(holder.ivMessageImage);
            if (message.getMessageText() != null && !message.getMessageText().isEmpty()) {
                holder.tvImageCaption.setVisibility(View.VISIBLE);
                holder.tvImageCaption.setText(message.getMessageText());
            } else {
                holder.tvImageCaption.setVisibility(View.GONE);
            }
        } else if (isSent) {
            // Show sent message
            holder.sentMessageLayout.setVisibility(View.VISIBLE);
            holder.tvSentMessage.setText(message.getMessageText());
            holder.tvSentTime.setText(timeText);

            // Show read status
            if (message.isRead()) {
                holder.ivMessageStatus.setVisibility(View.VISIBLE);
                holder.ivMessageStatus.setImageResource(R.drawable.ic_check);
                holder.ivMessageStatus.setColorFilter(0xFF4CAF50); // Green for read
            } else {
                holder.ivMessageStatus.setVisibility(View.VISIBLE);
                holder.ivMessageStatus.setImageResource(R.drawable.ic_check);
                holder.ivMessageStatus.setColorFilter(0xFF9E9E9E); // Grey for sent but not read
            }
        } else {
            // Show received message
            holder.receivedMessageLayout.setVisibility(View.VISIBLE);
            holder.tvReceivedMessage.setText(message.getMessageText());
            holder.tvReceivedTime.setText(timeText);

            Glide.with(context)
                    .load(message.getSenderPhotoUrl())
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .circleCrop()
                    .into(holder.ivSenderImage);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout receivedMessageLayout, sentMessageLayout, imageMessageLayout;
        CircleImageView ivSenderImage;
        TextView tvReceivedMessage, tvReceivedTime;
        TextView tvSentMessage, tvSentTime;
        ImageView ivMessageStatus, ivMessageImage;
        TextView tvImageCaption;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            receivedMessageLayout = itemView.findViewById(R.id.receivedMessageLayout);
            sentMessageLayout = itemView.findViewById(R.id.sentMessageLayout);
            imageMessageLayout = itemView.findViewById(R.id.imageMessageLayout);

            ivSenderImage = itemView.findViewById(R.id.ivSenderImage);
            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);

            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentTime = itemView.findViewById(R.id.tvSentTime);
            ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);

            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            tvImageCaption = itemView.findViewById(R.id.tvImageCaption);
        }
    }
}
