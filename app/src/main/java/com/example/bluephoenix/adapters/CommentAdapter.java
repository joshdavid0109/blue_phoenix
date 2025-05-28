package com.example.bluephoenix.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluephoenix.R; // Make sure this points to your R file
import com.example.bluephoenix.models.Comment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()); // Corrected missing character in your original code

    public CommentAdapter(List<Comment> commentsList) {
        this.commentsList = commentsList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentsList.get(position);
        holder.userNameTextView.setText(comment.getAuthorName());
        holder.commentTextTextView.setText(comment.getContent());
        if (comment.getTimestamp() != null) {
            holder.timestampTextView.setText(dateFormat.format(comment.getTimestamp().toDate()));
        } else {
            holder.timestampTextView.setText("N/A");
        }
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView;
        TextView commentTextTextView;
        TextView timestampTextView;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.comment_user_name);
            commentTextTextView = itemView.findViewById(R.id.comment_text);
            timestampTextView = itemView.findViewById(R.id.comment_timestamp);
        }
    }
}