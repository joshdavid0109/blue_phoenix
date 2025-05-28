package com.example.bluephoenix.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp; // Import Timestamp for Firestore dates

// Ensure this matches your package structure
public class ForumPost implements Parcelable {
    private String id; // Firestore Document ID
    private String title;
    private String message; // Full post text
    private String authorId; // ID of the user who created the post
    private String author; // Display name of the author
    private String date; // Formatted date string for display (e.g., "Jan 01, 2023 at 10:30 AM")
    private Timestamp timestamp; // Firestore Timestamp for sorting and accurate time
    private int numberOfComments;

    public ForumPost() {
        // Default constructor required for Firestore
    }

    // Constructor for creating a NEW post (ID will be null initially)
    public ForumPost(String id, String title, String message, String authorId, String author, String date, Timestamp timestamp, int numberOfComments) {
        this.id = id; // Can be null when creating a new post
        this.title = title;
        this.message = message;
        this.authorId = authorId;
        this.author = author;
        this.date = date;
        this.timestamp = timestamp;
        this.numberOfComments = numberOfComments;
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public int getNumberOfComments() {
        return numberOfComments;
    }

    // --- Setters (needed for Firestore mapping and updates) ---
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setNumberOfComments(int numberOfComments) {
        this.numberOfComments = numberOfComments;
    }

    // --- Parcelable Implementation ---
    protected ForumPost(Parcel in) {
        id = in.readString();
        title = in.readString();
        message = in.readString(); // Read content
        authorId = in.readString(); // Read authorId
        author = in.readString();
        date = in.readString();
        timestamp = in.readParcelable(Timestamp.class.getClassLoader()); // Read Timestamp
        numberOfComments = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(message); // Write content
        dest.writeString(authorId); // Write authorId
        dest.writeString(author);
        dest.writeString(date);
        dest.writeParcelable(timestamp, flags); // Write Timestamp
        dest.writeInt(numberOfComments);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ForumPost> CREATOR = new Creator<ForumPost>() {
        @Override
        public ForumPost createFromParcel(Parcel in) {
            return new ForumPost(in);
        }

        @Override
        public ForumPost[] newArray(int size) {
            return new ForumPost[size];
        }
    };
}