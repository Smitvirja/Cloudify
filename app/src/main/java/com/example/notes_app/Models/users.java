package com.example.notes_app.Models;

public class users {
    private String email, uid, username, profile_icon; // Added profileImageUrl
    private boolean admin;
    private long joinedDate;

    // Constructor that initializes all fields, including profileImageUrl
    public users(String uid, String username, String email, boolean admin, long joinedDate, String profile_icon) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.admin = admin;
        this.joinedDate = joinedDate;
        this.profile_icon = profile_icon; // Initialize profileImageUrl
    }

    // Default constructor required for Firebase deserialization
    public users() {}

    // Getter for UID
    public String getUid() {
        return uid;
    }

    // Setter for UID
    public void setUid(String uid) {
        this.uid = uid;
    }

    // Getter for username
    public String getUsername() {
        return username;
    }

    // Setter for username
    public void setUsername(String username) {
        this.username = username;
    }

    // Getter for email
    public String getEmail() {
        return email;
    }

    // Setter for email
    public void setEmail(String email) {
        this.email = email;
    }

    // Getter for admin status
    public boolean isAdmin() {
        return admin;
    }

    // Setter for admin status
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    // Getter for joined date
    public long getJoinedDate() {
        return joinedDate;
    }

    // Setter for joined date
    public void setJoinedDate(long joinedDate) {
        this.joinedDate = joinedDate;
    }

    // Getter for profile image URL
    public String getProfile_icon() { return profile_icon; }
    public void setProfile_icon(String profile_icon) { this.profile_icon = profile_icon; }
}
