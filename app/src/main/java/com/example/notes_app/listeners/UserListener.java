package com.example.notes_app.listeners;

import com.example.notes_app.Models.users;

public interface UserListener {
        void onResetPasswordClicked(users user);
        void onDeleteUserClicked(users user);
}
