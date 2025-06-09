// com.example.bluephoenix.models.ReviewerButton.java
package com.example.bluephoenix.models;

public class ReviewerButton {
    private String buttonText;
    private String documentId; // This will store the actual Firestore Document ID for the chapter

    public ReviewerButton(String buttonText, String documentId) {
        this.buttonText = buttonText;
        this.documentId = documentId;
    }

    // If you have a constructor that only takes buttonText, you might want to remove it
    // or ensure it sets documentId to null if it's not relevant for all buttons.
    // For content display, documentId is crucial.
    public ReviewerButton(String buttonText) {
        this.buttonText = buttonText;
        this.documentId = buttonText; // A default if documentId is often the same as buttonText
    }


    public String getButtonText() {
        return buttonText;
    }

    public String getDocumentId() {
        return documentId;
    }
}