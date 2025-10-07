package com.example.notes_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;


import com.example.notes_app.Activity.CreatingNoteActivity;
import com.example.notes_app.Activity.LoginPage;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.example.notes_app.dao.NoteDao;
import com.example.notes_app.dao.TaskDao;
import com.example.notes_app.entities.TaskEntity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class utility {

    private static final String TAG = "Utility";
    private static final String DEFAULT_USERNAME = "User";

    public interface UsernameCallback {
        void onUsernameFetched(String username);
    }
    public static void fetchUsername(UsernameCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "UID: " + userId);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("Users").document(userId);

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    String username = (document != null && document.exists()) ? document.getString("username") : null;
                    callback.onUsernameFetched(username != null && !username.isEmpty() ? username : DEFAULT_USERNAME);
                } else {
                    Log.e(TAG, "Error getting document", task.getException());
                    callback.onUsernameFetched(DEFAULT_USERNAME);
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Firestore query failed", e);
                callback.onUsernameFetched(DEFAULT_USERNAME);
            });
        } else {
            callback.onUsernameFetched(DEFAULT_USERNAME);
        }
    }

    public static void setupMainActivityBottomNavMenu(BottomAppBar bottomAppBar, Activity activity) {
        bottomAppBar.setOnMenuItemClickListener(item -> {
            Log.d("Utility", "Menu item clicked: " + item.getItemId());
            int id = item.getItemId();

            if (id == R.id.setting) {

                return true;
            } else if (id == R.id.theam) { // Corrected typo from 'theam' to 'theme'

                // Handle Theme action
                return true;
            } else if (id == R.id.logout) {
                Log.d("Utility", "Logout clicked");

                // Clear SharedPreferences for user login status
                SharedPreferences sharedPreferences = activity.getSharedPreferences("UserLoginPrefs", Context.MODE_PRIVATE);
                sharedPreferences.edit()
                        .putBoolean("isLoggedIn", false) // Clear logged-in status
                        .putBoolean("isAdmin", false) // Clear admin status
                        .apply();

                // Sign out from Firebase Auth
                FirebaseAuth.getInstance().signOut();

                // Redirect to LoginPage
                Intent loginIntent = new Intent(activity, LoginPage.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(loginIntent);
                activity.finish();
                return true;
            }
            else if (id == R.id.notequickadd) {
                Log.d("Utility", "Note quick add clicked");
                Intent noteQuickAddIntent = new Intent(activity, CreatingNoteActivity.class);
                activity.startActivity(noteQuickAddIntent);
                return true;
            } else if (id == R.id.reminderquickadd) {
                Log.d("Utility", "Reminder quick add clicked");
                if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
                    androidx.appcompat.app.AppCompatActivity appCompatActivity = (androidx.appcompat.app.AppCompatActivity) activity;
                    CreateTaskBottomSheetFragment fragment = new CreateTaskBottomSheetFragment();
                    fragment.show(appCompatActivity.getSupportFragmentManager(), "CreateTaskBottomSheet");
                }
                return true;
            } else if (id == R.id.binmenu) {
//                Log.d("Utility", "Bin menu clicked");
//                Intent binIntent = new Intent(activity, Bin.class);
//                activity.startActivity(binIntent);
                return true;
            }
            return false;
        });
    }



    @SuppressLint("ClickableViewAccessibility")
    public static void setupPasswordVisibilityToggle(final EditText editText, boolean initialVisibility) {
        editText.setTransformationMethod(initialVisibility ? HideReturnsTransformationMethod.getInstance() : PasswordTransformationMethod.getInstance());

        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRightWidth = editText.getCompoundDrawables()[2] != null ? editText.getCompoundDrawables()[2].getBounds().width() : 0;
                int touchableAreaWidth = drawableRightWidth + editText.getPaddingEnd();
                if (event.getRawX() >= (editText.getRight() - touchableAreaWidth)) {
                    togglePasswordVisibility(editText);
                    return true;
                }
            }
            return false;
        });
    }

    private static void togglePasswordVisibility(EditText editText) {
        boolean isPasswordVisible = editText.getTransformationMethod() instanceof HideReturnsTransformationMethod;
        editText.setTransformationMethod(isPasswordVisible ? PasswordTransformationMethod.getInstance() : HideReturnsTransformationMethod.getInstance());
        editText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, isPasswordVisible ? R.drawable.baseline_visibility_off_24 : R.drawable.baseline_visibility_24, 0);
        editText.postInvalidate();
        editText.setSelection(editText.getText().length());
    }

    public static class TaskSyncManager {
        private final FirebaseFirestore firestore;
        private final TaskDao taskDao;

        public TaskSyncManager(FirebaseFirestore firestore, TaskDao taskDao) {
            this.firestore = firestore;
            this.taskDao = taskDao;
        }

        public void syncTasks() {
            firestore.collection("tasks")
                    .whereEqualTo("userId", FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .whereEqualTo("isDeleted", false)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<TaskEntity> tasks = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                TaskEntity taskEntity = document.toObject(TaskEntity.class);
                                if (taskEntity != null) {
                                    taskEntity.setId(document.getId());
                                    tasks.add(taskEntity);
                                }
                            }
                            Executors.newSingleThreadExecutor().execute(() -> {
                                taskDao.deleteAllTasksForUser(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                for (TaskEntity taskEntity : tasks) {
                                    taskDao.insertOrUpdate(taskEntity);
                                }
                            });
                        }
                    });
        }
    }

    public static class NoteSyncManager {
        private final FirebaseFirestore firestore;
        private final NoteDao noteDao;

        public NoteSyncManager(FirebaseFirestore firestore, NoteDao noteDao) {
            this.firestore = firestore;
            this.noteDao = noteDao;
        }
    }
}
