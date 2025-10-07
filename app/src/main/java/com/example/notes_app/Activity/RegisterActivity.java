package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.utility;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputEmail;
    private EditText inputUsername;
    private EditText inputPassword;
    private EditText inputConfirmPassword;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private SharedPref sharedPreferences;

    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = new SharedPref(this);

        // Apply the saved theme
        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }


        setContentView(R.layout.activity_register);

        initializeUI();
        setupListeners();
    }


    private final BroadcastReceiver themeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.notes_app.THEME_CHANGED".equals(intent.getAction())) {
                // Apply the theme when the broadcast is received
                applyTheme();
                Log.d("home activity", "Broadcast sent for theme change..");
                recreate(); // Recreate activity to apply the new theme
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent("com.example.notes_app.THEME_CHANGED");
        Log.d(TAG, "ProfilePageDebug: Broadcast sent for theme change on back press.");
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isReceiverRegistered) {
            registerReceiver(themeReceiver, new IntentFilter("com.example.notes_app.THEME_CHANGED"));
            isReceiverRegistered = true;
            Log.d("HomeActivity", "Receiver registered.");
        }
        applyTheme(); // Ensure theme is applied when returning
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(themeReceiver);
            isReceiverRegistered = false;
            Log.d("HomeActivity", "Receiver unregistered.");
        }
    }



    private void applyTheme() {
        // Retrieve the current theme from SharedPreferences and apply it
        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
    }

    private void initializeUI() {
        inputEmail = findViewById(R.id.Re_mail);
        inputUsername = findViewById(R.id.re_username);
        inputPassword = findViewById(R.id.re_pass1);
        inputConfirmPassword = findViewById(R.id.re_passconf);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        utility.setupPasswordVisibilityToggle(inputPassword, false);
        utility.setupPasswordVisibilityToggle(inputConfirmPassword, false);
    }

    private void setupListeners() {
        Button confirmButton = findViewById(R.id.re_submit);
        TextView loginButton = findViewById(R.id.login);

        loginButton.setOnClickListener(v -> navigateToLogin());

        confirmButton.setOnClickListener(v -> {
            if (validateInput()) {
                registerUser();
            }
        });
    }

    private void navigateToLogin() {
        startActivity(new Intent(RegisterActivity.this, LoginPage.class));
        finish();
    }

    private boolean validateInput() {
        return validateEmail() && validateUsername() && validatePassword();
    }

    private boolean validateEmail() {
        String emailInput = inputEmail.getText().toString().trim();

        if (emailInput.isEmpty()) {
            inputEmail.setError("Email field can't be empty");
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            inputEmail.setError("Invalid email format");
            return false;
        } else {
            inputEmail.setError(null);
            return true;
        }
    }

    private boolean validateUsername() {
        String usernameInput = inputUsername.getText().toString().trim();

        if (usernameInput.isEmpty()) {
            inputUsername.setError("Username field can't be empty");
            return false;
        } else if (usernameInput.length() > 15) {
            inputUsername.setError("Username is too long");
            return false;
        } else {
            inputUsername.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String passwordInput = inputPassword.getText().toString().trim();
        String confirmPasswordInput = inputConfirmPassword.getText().toString().trim();

        if (passwordInput.isEmpty()) {
            inputPassword.setError("Password field can't be empty");
            return false;
        } else if (passwordInput.length() < 6) {
            inputPassword.setError("Password must be at least 6 characters");
            return false;
        } else if (!passwordInput.equals(confirmPasswordInput)) {
            inputConfirmPassword.setError("Passwords do not match");
            return false;
        } else {
            inputPassword.setError(null);
            inputConfirmPassword.setError(null);
            return true;
        }
    }

    private void registerUser() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String username = inputUsername.getText().toString().trim();
        long joinedDate = System.currentTimeMillis();  // Get the current timestamp for joined date

        // Create the user with Firebase Authentication
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("RegisterActivity", "Registration successful");

                        // Get the current user
                        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                        if (currentUser != null) {
                            // Save the user to Firestore, no need to check if they are logged in
                            saveUserToFirestore(currentUser.getUid(), username, email);
                            // Send email verification after successful registration
                            sendEmailVerification();
                            Toast.makeText(RegisterActivity.this, "Registration successful! Please verify your email.", Toast.LENGTH_SHORT).show();
                            navigateToLogin();
                        }
                    } else {
                        handleRegistrationFailure(task.getException());
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String email) {
        // Create a new user with admin set to false and joined_date as server timestamp
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("uid", userId);
        newUser.put("username", username);
        newUser.put("email", email);
        newUser.put("admin", false);
        newUser.put("joined_date", FieldValue.serverTimestamp()); // Use server timestamp for joined_date

        // Save the new user to Firestore
        db.collection("Users").document(userId).set(newUser)
                .addOnSuccessListener(aVoid -> Log.d("RegisterActivity", "User added to Firestore with joined_date as server timestamp"))
                .addOnFailureListener(e -> Log.e("RegisterActivity", "Error adding user to Firestore", e));
    }


    private void sendEmailVerification() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Verification email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Failed to send verification email", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void handleRegistrationFailure(Exception e) {
        Log.e("RegisterActivity", "Registration failed", e);
        Toast.makeText(RegisterActivity.this, "Registration failed: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
    }
}
