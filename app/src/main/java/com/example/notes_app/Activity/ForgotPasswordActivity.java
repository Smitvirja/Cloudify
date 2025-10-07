package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.notes_app.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editTextEmail;

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
        setContentView(R.layout.activity_forgot_password);

        editTextEmail = findViewById(R.id.email_forget);
        Button buttonResetPassword = findViewById(R.id.reset_button);

        findViewById(R.id.back_to_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgotPasswordActivity.this, LoginPage.class);
                startActivity(intent);
            }
        });

        buttonResetPassword.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
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
}
