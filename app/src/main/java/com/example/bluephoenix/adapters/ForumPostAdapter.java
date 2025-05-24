// src/main/java/com/example/bluephoenix/adapters/ForumPostAdapter.java
package com.example.bluephoenix.adapters; // Adjust package as needed

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.R; // Make sure this R points to your app's R file
import com.example.bluephoenix.models.ForumPost; // Import your data model

import java.util.List;

public class ForumPostAdapter extends RecyclerView.Adapter<ForumPostAdapter.ForumPostViewHolder> {

    private List<ForumPost> forumPosts;

    public ForumPostAdapter(List<ForumPost> forumPosts) {
        this.forumPosts = forumPosts;
    }

    @NonNull
    @Override
    public ForumPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the single item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forum_post, parent, false);
        return new ForumPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForumPostViewHolder holder, int position) {
        // Get the data for the current position
        ForumPost currentPost = forumPosts.get(position);

        // Bind the data to the views in the ViewHolder
        holder.titleTextView.setText(currentPost.getTitle());
        holder.authorTextView.setText(currentPost.getAuthor());
        holder.dateTextView.setText(currentPost.getDate());
        holder.commentCountTextView.setText(String.valueOf(currentPost.getCommentCount()));

        // You might want to add a click listener here for each item
        holder.itemView.setOnClickListener(v -> {
            // Handle item click, e.g., open a detailed post view
            // Toast.makeText(v.getContext(), "Clicked: " + currentPost.getTitle(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return forumPosts.size();
    }

    public static class ForumPostViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView authorTextView;
        TextView dateTextView;
        TextView commentCountTextView;
        ImageView commentsIcon;

        public ForumPostViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.forum_post_title);
            authorTextView = itemView.findViewById(R.id.forum_author);
            dateTextView = itemView.findViewById(R.id.forum_post_date);
            commentCountTextView = itemView.findViewById(R.id.pad_number_of_comments);
            commentsIcon = itemView.findViewById(R.id.icon_comments);
        }
    }
    public void updateData(List<ForumPost> newPosts) {
        this.forumPosts.clear();
        this.forumPosts.addAll(newPosts);
        notifyDataSetChanged();
    }
}