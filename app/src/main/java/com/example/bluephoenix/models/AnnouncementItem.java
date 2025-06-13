package com.example.bluephoenix.models; // Replace with your actual package name

public class AnnouncementItem {
    private String textContent;
    private int imageResId; // For local drawables
    // private String imageUrl; // For images from URLs

    private boolean isImage;

    // Constructor for text announcements
    public AnnouncementItem(String textContent) {
        this.textContent = textContent;
        this.isImage = false;
    }

    // Constructor for image announcements (from local drawables)
    public AnnouncementItem(int imageResId) {
        this.imageResId = imageResId;
        this.isImage = true;
    }

    // Constructor for image announcements (from URLs - uncomment if needed)
    /*
    public AnnouncementItem(String imageUrl, boolean isImage) {
        this.imageUrl = imageUrl;
        this.isImage = isImage;
    }
    */

    public String getTextContent() {
        return textContent;
    }

    public int getImageResId() {
        return imageResId;
    }

    // public String getImageUrl() { return imageUrl; }

    public boolean isImage() {
        return isImage;
    }
}