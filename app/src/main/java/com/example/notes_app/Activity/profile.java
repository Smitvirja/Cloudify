package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.notes_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
public class profile extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private  Context context;

    private FirebaseFirestore firebaseFirestore;

    private TextView usernameTextView, emailTextView, joinedDateTextView, notesCountTextView;
    private ImageView profilePictureImageView;
    private Button logoutButton, editProfileButton, adminPanelButton;

    private EditText emailInput;
    private Button resetButton;


    private static final int PICK_IMAGE_REQUEST = 1;

    private SharedPref sharedPreferences;
    private Button toggleTheme;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize SharedPref once
        SharedPref sharedPreferences = new SharedPref(this);

        // Set the theme before calling super.onCreate()
        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        toggleTheme = findViewById(R.id.theme1);

        // Initialize Firebase services
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        adminPanelButton = findViewById(R.id.admin_panel);


        // Set the button text based on the current theme state
        if (sharedPreferences.loadNightModeState()) {
            toggleTheme.setText("Switch to Light Mode");
        } else {
            toggleTheme.setText("Switch to Dark Mode");
        }

        // Set an onClickListener for the button
        toggleTheme.setOnClickListener(v -> {
            // Toggle the night mode state
            boolean isNightMode = sharedPreferences.loadNightModeState();
            if (isNightMode) {
                sharedPreferences.setNightModeState(false); // Switch to light mode
                toggleTheme.setText("Switch to Dark Mode");
            } else {
                sharedPreferences.setNightModeState(true); // Switch to dark mode
                toggleTheme.setText("Switch to Light Mode");
            }
            Log.d("ProfilePageDebug", "Broadcast sent for theme change.");
            Intent intent = new Intent("com.example.notes_app.THEME_CHANGED");
            sendBroadcast(intent);


            // Recreate the activity to apply the theme change
            recreate();
        });

        // Initialize UI elements
        usernameTextView = findViewById(R.id.usernameText);
        emailTextView = findViewById(R.id.emailText);
        joinedDateTextView = findViewById(R.id.joinedDateText);
        notesCountTextView = findViewById(R.id.notesCountText);
        profilePictureImageView = findViewById(R.id.profilePicture);
        logoutButton = findViewById(R.id.logoutButton);
        editProfileButton = findViewById(R.id.editProfileButton);

        // Load user profile data
        loadUserProfile();

        // Handle logout
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View customView = LayoutInflater.from(context).inflate(R.layout.custom_dialog, null);
                TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
                dialogTitleTask.setText(R.string.are_you_sure_you_want_to_Logout);

                android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                        .setView(customView)
                        .create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }

                // Set up the buttons
                MaterialButton positiveButton = customView.findViewById(R.id.positive_button);
                MaterialButton negativeButton = customView.findViewById(R.id.negative_button);
                positiveButton.setOnClickListener(v -> {
                    logout();
                    dialog.dismiss();
                });
                negativeButton.setOnClickListener(v -> dialog.cancel());

                // Show the dialog
                dialog.show();
            }
        });

        // Handle edit profile
        editProfileButton.setOnClickListener(v -> showEditUsernameDialog());

        // Handle profile picture change
        profilePictureImageView.setOnClickListener(v -> openImagePicker());

        Button resetPasswordButton = findViewById(R.id.pass_res); // Add this to your XML layout
        resetPasswordButton.setOnClickListener(v -> showPasswordResetDialog());

        adminPanelButton.setOnClickListener(v -> openAdminPanel());
        context = this;
    }

    private void restartApp() {
        Intent i = new Intent(getApplicationContext(), profile.class);
        startActivity(i);
        finish();
        Log.d(TAG, "restartApp: Changed theme successfully");
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
                        checkAdminStatus(documentSnapshot);
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

        usernameTextView.setText(username != null ? username : "Unknown");
        emailTextView.setText(email != null ? email : "Unknown");
        joinedDateTextView.setText(joinedDate != null ? "Joined: " + joinedDate : "Joined date unknown");
        notesCountTextView.setText(notesCount != null ? "Total Notes: " + notesCount : "Total Notes: 0");
    }

    private void showEditUsernameDialog() {
        if (context == null) {
            throw new IllegalStateException("Context is null in showEditUsernameDialog");
        }

        View customView = LayoutInflater.from(profile.this).inflate(R.layout.edit_profile_dialog, null);
        EditText usernameChange = customView.findViewById(R.id.username_cha);
        TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
        dialogTitleTask.setText(R.string.username);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context, R.style.AppTheme_Dialog)
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
                                    Toast.makeText(profile.this, "Username already taken, please choose another.", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Username is unique, proceed with the update
                                    updateUsernameInFirestore(newUsername);
                                    usernameTextView.setText(newUsername);
                                    dialog.dismiss();
                                }
                            } else {
                                // Handle Firestore query failure
                                Toast.makeText(profile.this, "Error checking username: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(profile.this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
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
                .addOnSuccessListener(aVoid -> Toast.makeText(profile.this, "Username updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(profile.this, "Error updating username: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

        Intent loginIntent = new Intent(profile.this, LoginPage.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageToFirebase(imageUri);
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference("profile_images/" + UUID.randomUUID().toString());

        storageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveProfileImageUrl(imageUrl);
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(profile.this, "Upload failed", Toast.LENGTH_SHORT).show());
    }

    private void saveProfileImageUrl(String imageUrl) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        firebaseFirestore.collection("Users").document(userId)
                .update("profile_icon", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(profile.this, "Profile image updated", Toast.LENGTH_SHORT).show();
                    loadProfileImage();  // Load the updated profile image into the ImageView
                })
                .addOnFailureListener(e -> Toast.makeText(profile.this, "Error updating profile image", Toast.LENGTH_SHORT).show());
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
                                .into(profilePictureImageView);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(profile.this, "Error loading profile image", Toast.LENGTH_SHORT).show());
    }


    private void showPasswordResetDialog() {

        View customView = LayoutInflater.from(context).inflate(R.layout.edit_profile_dialog, null);
        TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
        TextView reset = customView.findViewById(R.id.dialog_title);
        TextView text = customView.findViewById(R.id.username_cha);
        Button send= customView.findViewById(R.id.positive_button);

        dialogTitleTask.setText(R.string.email);
        reset.setText(R.string.reset);
        send.setText(R.string.send);

        // Create the AlertDialog with the custom layout
        android.app.AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setView(customView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Set up the buttons
        MaterialButton positiveButton = customView.findViewById(R.id.positive_button);
        MaterialButton negativeButton = customView.findViewById(R.id.negative_button);
        positiveButton.setOnClickListener(v -> {
            String email = text.getText().toString().trim();

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

            dialog.dismiss();
        });
        negativeButton.setOnClickListener(v -> dialog.cancel());

        // Show the dialog
        dialog.show();

    }

    private void checkAdminStatus(DocumentSnapshot documentSnapshot) {
        Boolean isAdmin = documentSnapshot.getBoolean("admin");
        if (isAdmin != null && isAdmin) {
            adminPanelButton.setVisibility(View.VISIBLE); // Show the button for admin users
        }
    }

    private void openAdminPanel() {
        // Navigate to the Admin Panel activity or perform admin-specific actions
        Intent adminIntent = new Intent(this, admin_page.class);
        startActivity(adminIntent);
    }

}
