package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;  // Import Handler for the delay
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.notes_app.R;
import com.example.notes_app.utility;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import de.timonknispel.ktloadingbutton.KTLoadingButton;

public class LoginPage extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private EditText usernameEditText, passwordEditText;
    private KTLoadingButton loginButton;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "UserLoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private SharedPref sharedPref;

    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = new SharedPref(this);

        // Apply the saved theme
        if (sharedPref.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }


        setContentView(R.layout.activity_login_page);

        // Initialize Firebase Auth and SharedPreferences
        firebaseAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if user is already logged in
        checkAutoLogin();

        // Initialize UI elements
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.loginbutton);
        TextView registerTextView = findViewById(R.id.register);

        // Password toggle utility
        utility.setupPasswordVisibilityToggle(passwordEditText, false);

        // Register new user
        registerTextView.setOnClickListener(view -> startActivity(new Intent(LoginPage.this, RegisterActivity.class)));

        // Login button click
        loginButton.setOnClickListener(view -> handleLogin());

        // Forgot password
        findViewById(R.id.forget).setOnClickListener(view -> startActivity(new Intent(LoginPage.this, ForgotPasswordActivity.class)));
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
        if (sharedPref.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
    }

    private void checkAutoLogin() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (isLoggedIn && currentUser != null) {
            updateLastLogin(currentUser.getUid()); // Update last login when the app opens
            navigateToMainActivity();
            finish();
        }
    }


    private void handleLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username or Password is empty", Toast.LENGTH_SHORT).show();
            loginButton.doResult(false, button -> {
                button.reset();
                return null;
            });
            return;
        }

        // Start loading animation
        loginButton.startLoading();
        loginWithUsername(username, password);
    }

    private void loginWithUsername(String username, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String email = task.getResult().getDocuments().get(0).getString("email");
                        if (email != null) {
                            authenticateUser(email, password);
                        } else {
                            showError("Email not found");
                        }
                    } else {
                        showError("Username or password is incorrect");
                    }
                });
    }

    private void authenticateUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            updateLastLogin(user.getUid()); // Update last login
                        }

                        // Use KTLoadingButton's animation and delay transition
                        loginButton.doResult(true, button -> {
                            // After the success animation (green tick), delay the transition
                            new Handler().postDelayed(() -> startLoginTransition(), 400); // Delay to match the animation duration
                            return null;
                        });
                    } else {
                        showError("Login failed: " + task.getException().getMessage());
                    }
                });
    }

    private void updateLastLogin(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(userId)
                .update("lastLogin", System.currentTimeMillis())
                .addOnFailureListener(e -> Log.w(TAG, "Failed to update last login", e));
    }


    private void startLoginTransition() {
        // Save login status in SharedPreferences
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();

        // Proceed to main activity
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginPage.this, home_page.class);
        startActivity(intent);

        // Apply the fade-in and fade-out transition for the activity switch
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void checkUserRole() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            showError("Failed to authenticate user");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        sharedPreferences.edit()
                                .putBoolean(KEY_IS_LOGGED_IN, true)
                                .apply();

                        loginButton.doResult(true, button -> {
                            navigateToMainActivity();
                            return null;
                        });
                    } else {
                        showError("User data not found");
                    }
                })
                .addOnFailureListener(e -> showError("Failed to retrieve user data"));
    }

    private void showError(String message) {
        loginButton.doResult(false, button -> {
            button.reset();
            return null;
        });

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
