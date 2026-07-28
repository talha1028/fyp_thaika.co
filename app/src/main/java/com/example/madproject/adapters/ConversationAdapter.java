package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.madproject.R;
import com.example.madproject.helpers.NameFormatter;
import com.example.madproject.models.Conversation;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private Context context;
    private List<Conversation> conversationList;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(Context context, List<Conversation> conversationList, OnConversationClickListener listener) {
        this.context = context;
        this.conversationList = conversationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);

        // Set user name
        holder.tvUserName.setText(NameFormatter.capitalize(conversation.getOtherUserName()));

        // Set last message
        String lastMsg = conversation.getLastMessage();
        if (lastMsg != null && !lastMsg.isEmpty()) {
            holder.tvLastMessage.setText(lastMsg);
        } else {
            holder.tvLastMessage.setText("No messages yet");
            holder.tvLastMessage.setTextColor(0xFF9E9E9E);
        }

        // Set time
        holder.tvTime.setText(formatTime(conversation.getLastMessageTime()));

        // Set unread count
        int unreadCount = conversation.getUnreadCount();
        if (unreadCount > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(unreadCount));
            // Bold the last message if unread
            holder.tvLastMessage.setTextColor(0xFF212121);
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
            holder.tvLastMessage.setTextColor(0xFF757575);
        }

        // Set online indicator
        if (conversation.isOnline()) {
            holder.vOnlineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.vOnlineIndicator.setVisibility(View.GONE);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conversation);
            }
        });

        Glide.with(context)
                .load(conversation.getOtherUserProfilePic())
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .into(holder.ivProfileImage);
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    private String formatTime(long timestamp) {
        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(timestamp);

        Calendar now = Calendar.getInstance();

        // Same day - show time
        if (isSameDay(messageTime, now)) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        // Yesterday
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(messageTime, yesterday)) {
            return "Yesterday";
        }

        // Within same week - show day name
        if (now.get(Calendar.WEEK_OF_YEAR) == messageTime.get(Calendar.WEEK_OF_YEAR) &&
                now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        // Older - show date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivProfileImage;
        View vOnlineIndicator;
        TextView tvUserName, tvTime, tvLastMessage, tvUnreadCount;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            vOnlineIndicator = itemView.findViewById(R.id.vOnlineIndicator);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}
