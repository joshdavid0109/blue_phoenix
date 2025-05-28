package com.example.bluephoenix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView; // Ensure ImageView is imported if you use it for comments icon etc.

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.R; // Make sure your R file is correctly imported
import com.example.bluephoenix.models.ForumPost;

import java.util.List;

public class ForumPostAdapter extends RecyclerView.Adapter<ForumPostAdapter.ForumPostViewHolder> {

    private List<ForumPost> forumPosts;
    private OnItemClickListener listener; // Declare the listener

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(ForumPost post);
    }

    public ForumPostAdapter(List<ForumPost> forumPosts) {
        this.forumPosts = forumPosts;
    }

    // Setter for the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ForumPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forum_post, parent, false);
        return new ForumPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForumPostViewHolder holder, int position) {
        ForumPost post = forumPosts.get(position);
        holder.forumPostTitle.setText(post.getTitle());
        holder.forumAuthor.setText("By " + post.getAuthor());
        holder.forumPostDate.setText(" • " + post.getDate());
        holder.padNumberOfComments.setText(String.valueOf(post.getNumberOfComments()));
        holder.forumPostContent.setText(post.getMessage());

        // Set the click listener on the entire item view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(post); // Notify the listener when an item is clicked
            }
        });
    }

    @Override
    public int getItemCount() {
        return forumPosts.size();
    }

    public void updateData(List<ForumPost> newPosts) {
        this.forumPosts = newPosts;
        notifyDataSetChanged(); // Notify the adapter that the data has changed
    }

    static class ForumPostViewHolder extends RecyclerView.ViewHolder {
        TextView forumPostTitle;
        TextView forumAuthor;
        TextView forumPostDate;
        TextView padNumberOfComments;
        ImageView iconComments; // If you have an icon for comments

        TextView forumPostContent;

        public ForumPostViewHolder(@NonNull View itemView) {
            super(itemView);
            forumPostTitle = itemView.findViewById(R.id.forum_post_title);
            forumAuthor = itemView.findViewById(R.id.forum_author);
            forumPostDate = itemView.findViewById(R.id.forum_post_date);
            padNumberOfComments = itemView.findViewById(R.id.pad_number_of_comments);
            iconComments = itemView.findViewById(R.id.icon_comments);
            forumPostContent = itemView.findViewById(R.id.forum_post_full_content_hidden);
        }
    }
}