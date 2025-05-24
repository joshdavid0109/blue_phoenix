package com.example.bluephoenix.models;

public class ReviewerButton {
    private String buttonText;
    // You can add other properties like an ID, a destination, etc.

    public ReviewerButton(String buttonText) {
        this.buttonText = buttonText;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }
}