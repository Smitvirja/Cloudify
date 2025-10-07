package com.example.notes_app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.notes_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Setting extends AppCompatActivity {
    private SharedPref sharedPreferences;
    private Switch toggleTheme;
    private FirebaseFirestore firebaseFirestore;

    private FirebaseAuth firebaseAuth;

    private TextView usernameText ,logout,edit_profile,forget_password;
    private ImageView profile,back;

    private  Context context;
    private RelativeLayout heading;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPref sharedPreferences = new SharedPref(this);

        // Set the theme before calling super.onCreate()
        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
        setContentView(R.layout.activity_setting);

        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        back=findViewById(R.id.back);
        profile=findViewById(R.id.profilePicture);
        usernameText=findViewById(R.id.usernameText);
        forget_password=findViewById(R.id.passwordchange);
        logout=findViewById(R.id.logout);
        edit_profile=findViewById(R.id.edit_profile);
        heading=findViewById(R.id.profile_heading);
        Switch toggleTheme = findViewById(R.id.darkModeSwitch);
        context = this;


// Load and set initial switch state
        boolean isNightMode = sharedPreferences.loadNightModeState();
        toggleTheme.setChecked(isNightMode);

// Set listener for switch toggle
        toggleTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.setNightModeState(isChecked); // Save new mode state

            Log.d("ProfilePageDebug", "Broadcast sent for theme change.");
            Intent intent = new Intent("com.example.notes_app.THEME_CHANGED");
            sendBroadcast(intent);

            // Recreate activity to apply theme change
            recreate();
        });

        forget_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View customView = LayoutInflater.from(view.getContext()).inflate(R.layout.edit_profile_dialog, null);
                TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
                TextView reset = customView.findViewById(R.id.dialog_title);
                TextView text = customView.findViewById(R.id.username_cha);
                MaterialButton positiveButton = customView.findViewById(R.id.positive_button);

                dialogTitleTask.setText(R.string.email);
                reset.setText(R.string.reset);
                positiveButton.setText(R.string.send);

                // Create the AlertDialog with the custom layout
                android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(Setting.this, R.style.AppTheme_Dialog)
                        .setView(customView)
                        .create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }

                // Set up the buttons

                MaterialButton negativeButton = customView.findViewById(R.id.negative_button);
                positiveButton.setOnClickListener(v -> {
                    String email = text.getText().toString().trim();

                    if (email.isEmpty()) {
                        Toast.makeText(context, "Email cannot be empty!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Send password reset email
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to send email: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                    dialog.dismiss();
                });
                negativeButton.setOnClickListener(v -> dialog.cancel());

                // Show the dialog
                dialog.show();
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
        edit_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Setting.this, profile.class);
                startActivity(intent);
            }
        });

        heading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Setting.this, profile.class);
                startActivity(intent);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Setting.this, home_page.class);
                startActivity(intent);
                finish();
            }
        });
        loadUserProfile();

    }

    private void loadUserProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        firebaseFirestore.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        displayUserProfile(documentSnapshot);
//                        checkAdminStatus(documentSnapshot);
                        loadProfileImage();  // Load profile image after displaying user info
                    } else {
                        Toast.makeText(this, "User profile not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ProfileActivity", "Error loading profile", e);
                });
    }

    private void loadProfileImage() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        firebaseFirestore.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve the profile image URL
                        String imageUrl = documentSnapshot.getString("profile_icon");

                        // Use Glide to load the image (or default if not available)
                        Glide.with(this)
                                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : R.drawable.accicon) // Default image if no URL
                                .apply(RequestOptions.circleCropTransform()) // Apply circular cropping
                                .placeholder(R.drawable.ic_profile_placeholder) // Placeholder for loading
                                .into(profile);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading profile image", Toast.LENGTH_SHORT).show());
    }

    private void displayUserProfile(DocumentSnapshot documentSnapshot) {
        String username = documentSnapshot.getString("username");
        String email = documentSnapshot.getString("email");
        Timestamp joinedTimestamp = documentSnapshot.getTimestamp("joined_date");
        Long notesCount = documentSnapshot.getLong("notes_count");

        String joinedDate = null;
        if (joinedTimestamp != null) {
            Date date = joinedTimestamp.toDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            joinedDate = dateFormat.format(date);
        }

        usernameText.setText(username != null ? username : "Unknown");
//        emailTextView.setText(email != null ? email : "Unknown");
//        joinedDateTextView.setText(joinedDate != null ? "Joined: " + joinedDate : "Joined date unknown");
//        notesCountTextView.setText(notesCount != null ? "Total Notes: " + notesCount : "Total Notes: 0");
    }

    private void logout() {
        Log.d("Utility", "Logout clicked");

        SharedPreferences sharedPreferences = getSharedPreferences("UserLoginPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean("isLoggedIn", false)
                .putBoolean("isAdmin", false)
                .apply();

        FirebaseAuth.getInstance().signOut();

        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

        Intent loginIntent = new Intent(this, LoginPage.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void showPasswordResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        // Create an EditText for email input
        final EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email");
        builder.setView(emailInput);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Email cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Send password reset email
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.create().show();
    }

    private void showEditUsernameDialog() {

        View customView = LayoutInflater.from(this).inflate(R.layout.edit_profile_dialog, null);
        EditText usernameChange = customView.findViewById(R.id.username_cha);
        TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
        dialogTitleTask.setText(R.string.username);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setView(customView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton positiveButton = customView.findViewById(R.id.positive_button);
        MaterialButton negativeButton = customView.findViewById(R.id.negative_button);

        positiveButton.setOnClickListener(v -> {
            String newUsername = usernameChange.getText().toString().trim();
            if (!newUsername.isEmpty()) {
                // Check if the username is unique before proceeding
                firebaseFirestore.collection("Users")
                        .whereEqualTo("username", newUsername)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    // Username already taken
                                    Toast.makeText(this, "Username already taken, please choose another.", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Username is unique, proceed with the update
                                    updateUsernameInFirestore(newUsername);
                                    usernameText.setText(newUsername);
                                    dialog.dismiss();
                                }
                            } else {
                                // Handle Firestore query failure
                                Toast.makeText(this, "Error checking username: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        negativeButton.setOnClickListener(v -> dialog.cancel());

        // Show the dialog
        dialog.show();
    }

    private void updateUsernameInFirestore(String newUsername) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        firebaseFirestore.collection("Users").document(userId)
                .update("username", newUsername)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error updating username: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}