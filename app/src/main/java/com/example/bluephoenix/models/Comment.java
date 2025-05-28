package com.example.bluephoenix.models;

import androidx.annotation.NonNull;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName; // IMPORTANT: Ensure this import is there!

public class Comment {
    private String authorId;
    private String authorName;
    private String content;
    private Timestamp timestamp;

    public Comment() { }

    public Comment(String authorId, String authorName, String content, Timestamp timestamp) {
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters
    public String getAuthorId() { return authorId; }

    @PropertyName("author") // Annotation for getter
    public String getAuthorName() { return authorName; }

    @PropertyName("comment") // Annotation for getter
    public String getContent() { return content; }

    public Timestamp getTimestamp() { return timestamp; }

    // Setters (Crucial: Add @PropertyName here too)
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    @PropertyName("author") // Annotation for setter
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    @PropertyName("comment") // Annotation for setter
    public void setContent(String content) { this.content = content; }

    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    @NonNull
    @Override
    public String toString() {
        return "Author: " + authorName + ", Content: " + content + ", Timestamp: " + timestamp + ", Author ID: " + authorId; // Enhanced for debugging
    }
}