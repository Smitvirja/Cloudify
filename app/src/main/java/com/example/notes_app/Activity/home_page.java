package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.notes_app.R;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import nl.joery.animatedbottombar.AnimatedBottomBar;
import android.net.Uri;
import android.widget.Toast;


public class home_page extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private AnimatedBottomBar bottomBar;
    private TextView helloTextView;

    private Fragment notesFragment;
    private Fragment tasksFragment;
    private Fragment binFragment;

    private View userImageView;
    private ImageView profile_picture;

    private Fragment currentFragment; // Keep track of the current fragment
    private int currentTabIndex = 0; // Track the current tab index

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

        setContentView(R.layout.activity_home_page);

        // Initialize views
        bottomBar = findViewById(R.id.animatedBottomBar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        helloTextView = findViewById(R.id.hellotexth);
        userImageView = findViewById(R.id.userImage); // This will open the drawer
        FloatingActionButton optionOne = findViewById(R.id.option_one);
        FloatingActionButton optionTwo = findViewById(R.id.option_two);
        FloatingActionButton optionThree = findViewById(R.id.option_three);




        // Initialize fragments
        notesFragment = new NotesFragment();
        tasksFragment = new TasksFragment();
        binFragment = new Bin_fragment();

        // Set default fragment
        if (savedInstanceState == null) {
            Log.d("HomePageDebug", "Setting default fragment: NotesFragment");
            setInitialFragment();
        }




        // Setup Navigation Drawer
        setupNavigationDrawer();

        // Set up bottom bar tab selection listener
        bottomBar.setOnTabSelectListener(new AnimatedBottomBar.OnTabSelectListener() {
            @Override
            public void onTabSelected(int lastIndex, AnimatedBottomBar.Tab lastTab, int newIndex, AnimatedBottomBar.Tab newTab) {
                Fragment selectedFragment = null;

                if (newTab.getId() == R.id.tab_notes) {
                    selectedFragment = notesFragment;
                } else if (newTab.getId() == R.id.tab_tasks) {
                    selectedFragment = tasksFragment;
                } else if (newTab.getId() == R.id.tab_bin) {
                    selectedFragment = binFragment;
                }

                if (selectedFragment != null) {
                    replaceFragment(selectedFragment, newIndex > currentTabIndex);
                    currentTabIndex = newIndex; // Update the current tab index
                }
            }



            @Override
            public void onTabReselected(int index, AnimatedBottomBar.Tab tab) {
                Log.d("AnimatedBottomBar", "Reselected tab: " + tab.getTitle());
            }
        });

        if (getIntent().getBooleanExtra("open_tasks_fragment", false)) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerh, new TasksFragment())  // Replace with your fragment
                    .commit();
        }

        optionOne.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatingNoteActivity.class);
            startActivity(intent);
        });

        optionTwo.setOnClickListener(v -> {
            Log.d("Utility", "Reminder quick add clicked");

            // Get the context from the view and cast it to an AppCompatActivity
            if (v.getContext() instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.AppCompatActivity appCompatActivity = (androidx.appcompat.app.AppCompatActivity) v.getContext();

                // Create and show the bottom sheet
                CreateTaskBottomSheetFragment fragment = new CreateTaskBottomSheetFragment();
                fragment.show(appCompatActivity.getSupportFragmentManager(), "CreateTaskBottomSheet");
            }
        });


        optionThree.setOnClickListener(v -> {
            // Handle Option Three Click
        });

        // Set up greeting text or user profile
        setupUserProfile();
        checkIfUserIsAdmin();

    }


    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Get reference to the Navigation Header
        View headerView = navigationView.getHeaderView(0);
        TextView usernameTextView = headerView.findViewById(R.id.username);
        TextView emailTextView = headerView.findViewById(R.id.email);
//        ImageView profileImageView = headerView.findViewById(R.id.profile_picture);
        profile_picture = (ImageView) headerView.findViewById(R.id.profile_picture);


        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            fetchUserProfile(userId,
                    username -> usernameTextView.setText(username),
                    imageUrl -> {
                        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                            // Load the profile image using Glide
                            Glide.with(this)
                                    .load(imageUrl)
                                    .apply(RequestOptions.circleCropTransform())
                                    .placeholder(R.drawable.alert) // Fallback while loading
                                    .error(R.drawable.accicon) // Default image if loading fails
                                    .into(profile_picture);
                        } else {
                            Log.d("FirestoreDebug", "No profile image found, using default.");
                        }
                    },
                    email -> emailTextView.setText(email)
            );
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.home) {
                Intent intent = new Intent(this, home_page.class);
                startActivity(intent);
            } else if (id == R.id.setting) {
                Intent intent = new Intent(this, Setting.class);
                startActivity(intent);
            }else if (id == R.id.share) {
                String url = "https://github.com/Smitvirja/note_cloudy";  // Replace with your desired URL
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);

                Toast.makeText(home_page.this, "Opening link...", Toast.LENGTH_SHORT).show();
            }else if (id == R.id.about_us) {
                Intent intent = new Intent(this, About_us_page.class);
                startActivity(intent);
            }else if (id == R.id.admin_panel) {
                Intent intent = new Intent(this, admin_page.class);
                startActivity(intent);
            }else if (id == R.id.logout) {
                //logout();
                View customView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null);
                TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
                dialogTitleTask.setText(R.string.are_you_sure_you_want_to_Logout);

                AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
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

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Open Drawer when clicking the user icon
        userImageView.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));
    }

    // Method to set the admin panel visibility to true
    private void updateAdminMenuVisibility(final boolean isAdmin) {
        if (navigationView != null) {
            Menu menu = navigationView.getMenu();
            MenuItem adminPanelItem = menu.findItem(R.id.admin_panel);

            if (isAdmin) {
                adminPanelItem.setVisible(true); // Show the item for admin users
            } else {
                adminPanelItem.setVisible(false); // Hide the item for non-admin users
            }
        }
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




    private void checkIfUserIsAdmin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {

            // Check user's role, if using Firebase, you may have an "isAdmin" field in the user document
            FirebaseFirestore.getInstance().collection("Users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get the "admin" field (true or false)
                            boolean isAdmin = documentSnapshot.getBoolean("admin");
                            updateAdminMenuVisibility(isAdmin);

                            // Update the menu visibility based on admin status
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle errors if needed
                        Log.e("Firestore", "Failed to fetch user data", e);
                    });
        }
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

    private void restartApp() {
        Intent i = new Intent(getApplicationContext(), home_page.class);
        startActivity(i);
        finish();
        Log.d(TAG, "restartApp: Changed theme successfully");
    }


    private void setInitialFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainerh, notesFragment).commit();
        currentFragment = notesFragment; // Track the current fragment
    }

    private void setupUserProfile() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d("FirestoreDebug", "User ID: " + userId);

            fetchUserProfile(userId,
                    username -> {
                        if (helloTextView != null) {
                            helloTextView.setText("Hi, " + username);
                        }
                    },
                    imageUrl -> {
                        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                            // Load the profile image using Glide
                            Glide.with(this)
                                    .load(Uri.parse(imageUrl))
                                    .apply(RequestOptions.circleCropTransform())
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .into((ImageView) userImageView);
                        }
                    },
                    email -> {
                        Log.d("FirestoreDebug", "User Email: " + email);
                        // You can use this email wherever needed, like in a TextView.
                    }
            );
        } else {
            if (helloTextView != null) {
                helloTextView.setText("Hi, User");
            }
        }
    }


    public interface ImageCallback {
        void onImageFetched(String imageUrl);
    }
    public interface EmailCallback {
        void onEmailFetched(String email);
    }


    private void fetchUserProfile(String userId, UsernameCallback usernameCallback, ImageCallback imageCallback, EmailCallback emailCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("Users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String username = document.getString("username");
                    String imageUrl = document.getString("profile_icon");
                    String email = document.getString("email");

                    usernameCallback.onUsernameFetched(username != null ? username : "User");
                    imageCallback.onImageFetched(imageUrl);
                    emailCallback.onEmailFetched(email != null ? email : "No Email");
                } else {
                    Log.d("FirestoreDebug", "No document found for user.");
                    usernameCallback.onUsernameFetched("User");
                    imageCallback.onImageFetched(null);
                    emailCallback.onEmailFetched("No Email");
                }
            } else {
                Log.e("FirestoreDebug", "Error getting document", task.getException());
                usernameCallback.onUsernameFetched("User");
                imageCallback.onImageFetched(null);
                emailCallback.onEmailFetched("No Email");
            }
        }).addOnFailureListener(e -> {
            Log.e("FirestoreDebug", "Firestore query failed", e);
            usernameCallback.onUsernameFetched("User");
            imageCallback.onImageFetched(null);
            emailCallback.onEmailFetched("No Email");
        });
    }


    private void replaceFragment(Fragment fragment, boolean forward) {
        if (currentFragment == fragment) return; // Avoid unnecessary replacements

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (forward) {
            fragmentTransaction.setCustomAnimations(
                    R.anim.enter_from_right,
                    R.anim.exit_to_left
            );
        } else {
            fragmentTransaction.setCustomAnimations(
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
            );
        }

        fragmentTransaction.replace(R.id.fragmentContainerh, fragment);
        fragmentTransaction.commit();
        Log.d("HomePageDebug", "Fragment replaced successfully");
        currentFragment = fragment; // Update the current fragment
    }

    public interface UsernameCallback {
        void onUsernameFetched(String username);
    }
}
