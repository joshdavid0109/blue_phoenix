// src/main/java/com/example/bluephoenix/models/ForumPost.java
package com.example.bluephoenix.models; // Adjust package as needed

public class ForumPost {
    private String title;
    private String author;
    private String date;
    private int commentCount;

    public ForumPost(String title, String author, String date, int commentCount) {
        this.title = title;
        this.author = author;
        this.date = date;
        this.commentCount = commentCount;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public int getCommentCount() {
        return commentCount;
    }
}