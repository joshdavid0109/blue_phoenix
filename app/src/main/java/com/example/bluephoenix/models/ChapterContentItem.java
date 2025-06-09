// app/src/main/java/com/example/bluephoenix/models/ChapterContentItem.java
package com.example.bluephoenix.models;

public class ChapterContentItem {
    public int viewType;
    public String textContent; // Used for chapter title and text content
    public String imageUrl;    // Kept for potential future image support
    public String imageCaption; // Kept for potential future image support

    // Define view types as constants
    public static final int VIEW_TYPE_CHAPTER_TITLE = 0;
    public static final int VIEW_TYPE_TEXT = 1;
    public static final int VIEW_TYPE_IMAGE = 2;

    public ChapterContentItem(int viewType, String text) {
        this.viewType = viewType;
        this.textContent = text;
        this.imageUrl = null;
        this.imageCaption = null;
    }

    public ChapterContentItem(int viewType, String imageUrl, String imageCaption) {
        this.viewType = viewType;
        this.imageUrl = imageUrl;
        this.imageCaption = imageCaption;
        this.textContent = null;
    }
}