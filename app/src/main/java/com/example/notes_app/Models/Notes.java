package com.example.notes_app.Models;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Notes implements Serializable {
    @NonNull
    private String id; // Change to Long if Firestore stores it as Long

    private String title;
    private String subtitle;
    private String note;
    private String date;
    private String color;
    private String imageUrl;
    private String userId;
    private boolean deleted;

    // No-argument constructor required for Firestore
    public Notes() {
        // Default constructor for Firestore
    }

    // Parameterized constructor
    public Notes(String id, String title, String subtitle, String note, String date, String color, String imageUrl, String userId , boolean deleted) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.note = note;
        this.date = date;
        this.color = color;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.deleted = deleted;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
