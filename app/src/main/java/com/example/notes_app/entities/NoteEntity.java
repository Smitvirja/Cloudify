package com.example.notes_app.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey
    @NonNull
    private String id;

    private String title;
    private String subtitle;
    private String note;
    private String date;
    private String color;
    private String imageUrl;



    private String localImagePath;
    private String userId;
    private boolean deleted;

    // Default constructor
    public NoteEntity() {
    }

    // Constructor with all fields
    public NoteEntity(@NonNull String id, String title, String subtitle, String note,
                      String date, String color, String imageUrl, String userId, boolean deleted) {
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


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
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

    public String getLocalImagePath() {
        return localImagePath;
    }

    public void setLocalImagePath(String localImagePath) {
        this.localImagePath = localImagePath;
    }
}

