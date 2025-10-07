package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.adapter.UserAdapter;
import com.example.notes_app.listeners.UserListener;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;



public class admin_page extends AppCompatActivity {

    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private List<users> userList;
    private FirebaseFirestore db;
    private LineChart lineChart;
    private BarChart barChart;

    private TextView totalUsersText, mostActiveUsersText;

    private SharedPref sharedPreferences;
    private boolean isReceiverRegistered = false;
    private static final String TAG = "AdminPage";

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
        setContentView(R.layout.activity_admin_page);

        db = FirebaseFirestore.getInstance();

        totalUsersText=findViewById(R.id.totalUsersText);
        mostActiveUsersText=findViewById(R.id.mostActiveUsersText);

        setupRecyclerView();
        setupCharts();
        loadUsers();
        loadUserAnalytics();
        fetchMostActiveUsers();
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

    private void setupRecyclerView() {
        userRecyclerView = findViewById(R.id.adminview);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(new UserListener() {
            @Override
            public void onResetPasswordClicked(users user) {
                resetUserPassword(user);
            }

            @Override
            public void onDeleteUserClicked(users user) {
                deleteUser(user);
            }
        });
        userRecyclerView.setAdapter(userAdapter);
    }

    private void setupCharts() {
        lineChart = findViewById(R.id.lineChart);
        barChart = findViewById(R.id.barChart);
    }



    private void loadUserAnalytics() {
        db.collection("Users").get()
                .addOnSuccessListener(value -> {
                    List<Entry> lineEntries = new ArrayList<>();
                    List<BarEntry> barEntries = new ArrayList<>();
                    Map<String, Integer> activityMap = new HashMap<>();

                    for (QueryDocumentSnapshot document : value) {
                        Long lastLoginMillis = document.getLong("lastLogin");
                        if (lastLoginMillis != null) {
                            // Convert milliseconds to a readable date format
                            String lastLoginDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(new Date(lastLoginMillis));

                            // Count user activity per date
                            activityMap.put(lastLoginDate, activityMap.getOrDefault(lastLoginDate, 0) + 1);
                        }
                    }

                    int index = 0;
                    for (Map.Entry<String, Integer> entry : activityMap.entrySet()) {
                        lineEntries.add(new Entry(index, entry.getValue()));
                        barEntries.add(new BarEntry(index, entry.getValue()));
                        index++;
                    }

                    LineDataSet lineDataSet = new LineDataSet(lineEntries, "User Activity");
                    LineData lineData = new LineData(lineDataSet);
                    lineChart.setData(lineData);
                    lineChart.invalidate();

                    BarDataSet barDataSet = new BarDataSet(barEntries, "Active Users");
                    BarData barData = new BarData(barDataSet);
                    barChart.setData(barData);
                    barChart.invalidate();
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error loading analytics", e));
    }


    private void fetchMostActiveUsers() {
        db.collection("Users")
                .orderBy("lastLogin", Query.Direction.DESCENDING) // Get most recently logged-in users
                .limit(3) // Get top 3 active users
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    StringBuilder activeUsers = new StringBuilder();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String username = document.getString("username");
                        activeUsers.append(username).append("\n");
                    }

                    if (activeUsers.length() > 0) {
                        mostActiveUsersText.setText("Most Active Users:\n" + activeUsers);
                    } else {
                        mostActiveUsersText.setText("Most Active Users: No data");
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error fetching active users", e));
    }


    private void loadUsers() {
        db.collection("Users").get()
                .addOnSuccessListener(value -> {
                    if (value != null && !value.isEmpty()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : value) {
                            userList.add(document.toObject(users.class));
                        }
                        userAdapter.setUsers(userList);

                        // ✅ Update total users count
                        totalUsersText.setText("Total Users: " + userList.size());
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error loading users", e));
    }

    private void resetUserPassword(users user) {
        String email = user.getEmail();
        if (email != null && !email.isEmpty()) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void deleteUser(users user) {
        db.collection("Users").document(user.getUid()).delete()
                .addOnSuccessListener(aVoid -> {
                    userList.remove(user);
                    userAdapter.setUsers(userList);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error deleting user", e));
    }
}