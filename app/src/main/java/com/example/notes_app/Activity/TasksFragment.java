package com.example.notes_app.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.notes_app.Models.Task;
import com.example.notes_app.R;
import com.example.notes_app.adapter.CompletedTaskAdapter;
import com.example.notes_app.adapter.TaskAdapter;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.example.notes_app.bottomSheetFragment.ShowCalendarViewBottomSheet;
import com.example.notes_app.dao.TaskDao;
import com.example.notes_app.database.TaskDatabase;
import com.example.notes_app.databinding.ActivityTasksFragmentBinding;
import com.example.notes_app.notification.NotificationScheduler;
import com.example.notes_app.utility.TaskSyncManager;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment implements CreateTaskBottomSheetFragment.SetRefreshListener {

    private ActivityTasksFragmentBinding binding;
    private TaskAdapter pendingTaskAdapter;
    private ImageView noDataImage;
    private CompletedTaskAdapter completedTaskAdapter;
    private List<Task> pendingTasks = new ArrayList<>();
    private List<Task> completedTasks = new ArrayList<>();
    private boolean isCompletedTasksVisible = false;
    private TaskDatabase taskDatabase;
    private TaskDao taskDao;
    private TaskSyncManager taskSyncManager;
    private MutableLiveData<List<Task>> completedTasksLiveData = new MutableLiveData<>();
    private static final int RC_NOTIFICATION = 123;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ActivityTasksFragmentBinding.inflate(inflater, container, false);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        taskDatabase = TaskDatabase.getInstance(getContext());
        taskDao = taskDatabase.taskDao();
        taskSyncManager = new TaskSyncManager(FirebaseFirestore.getInstance(), taskDao);

        NotificationScheduler notificationScheduler = new NotificationScheduler();

        // Use the binding root view instead of inflating manually
        View view = binding.getRoot();

        noDataImage = view.findViewById(R.id.noDataImage);

        ImageView menuIcon = view.findViewById(R.id.menuIcon);

        setUpViews();
        setUpAdapters();


        binding.calendar.setOnClickListener(view1 -> {
            ShowCalendarViewBottomSheet showCalendarViewBottomSheet = new ShowCalendarViewBottomSheet();
            showCalendarViewBottomSheet.show(getParentFragmentManager(), showCalendarViewBottomSheet.getTag());
        });

        binding.buttonToggleCompleted.setOnClickListener(v -> toggleCompletedTasks());


        completedTasksLiveData.observe(getViewLifecycleOwner(), tasks -> {
            completedTasks.clear();
            completedTasks.addAll(tasks);
            completedTaskAdapter.notifyDataSetChanged();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, RC_NOTIFICATION);
            }
        } else {
            Toast.makeText(getContext(), "Notification permission not required", Toast.LENGTH_SHORT).show();
        }

        FloatingActionButton optionOne = binding.optionOne;
        FloatingActionButton optionTwo = binding.optionTwo;
        FloatingActionButton optionThree = binding.optionThree;
        FloatingActionsMenu fabMenu = binding.layoutquickactiontodo;

        optionOne.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreatingNoteActivity.class);
            startActivity(intent);
            fabMenu.collapse();  // Close the FAB menu after option one is clicked
        });

        optionTwo.setOnClickListener(v -> {
            Log.d("Utility", "Reminder quick add clicked");

            CreateTaskBottomSheetFragment fragment = new CreateTaskBottomSheetFragment();
            fragment.setRefreshListener(this);  // 'this' refers to TasksFragment which implements SetRefreshListener
            fragment.show(getParentFragmentManager(), "CreateTaskBottomSheet");

            fabMenu.collapse();  // Close the FAB menu after option two is clicked
        });

        optionThree.setOnClickListener(v -> {
            // Close the FAB menu after option three is clicked
        });

        menuIcon.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(getContext(), v);
            MenuInflater inflater1 = getActivity().getMenuInflater();
            inflater1.inflate(R.menu.task_menu, popupMenu.getMenu());

            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.refres) {
                    refresh();
                    return true;
                } else if (item.getItemId() == R.id.Delete) {
                    clearCompletedTasks();
                    return true;
                } else {
                    return false;
                }
            });
        });
        fetchAndSyncTasks();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchAndSyncTasks(); // Ensure tasks are updated when the user returns to this fragment
    }


    public void updateNoDataImageVisibility() {
        if (pendingTasks.isEmpty()) {
            noDataImage.setVisibility(View.VISIBLE);
        } else {
            noDataImage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setUpViews() {
        Glide.with(this).load(R.drawable.addyourfirsttask).into(binding.noDataImage);
    }

    private void setUpAdapters() {
        pendingTaskAdapter = new TaskAdapter(getContext(), pendingTasks, this, this::onTaskLongClick, this);
        binding.taskRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.taskRecycler.setAdapter(pendingTaskAdapter);

        completedTaskAdapter = new CompletedTaskAdapter(getContext(), completedTasks);
        binding.recyclerViewCompleted.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewCompleted.setAdapter(completedTaskAdapter);
    }

    private void fetchAndSyncTasks() {
        new FetchTasksTask().execute(getCurrentUserId());
    }

    private class FetchTasksTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... userId) {
            fetchTasksFromFirestore(userId[0]);
            return null;
        }

        private void fetchTasksFromFirestore(String userId) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("tasks")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<Task> tasks = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Task taskItem = document.toObject(Task.class);
                                tasks.add(taskItem);
                            }

                            getActivity().runOnUiThread(() -> {
                                pendingTasks.clear();
                                completedTasks.clear();

                                for (Task taskItem : tasks) {
                                    if (taskItem.isComplete()) {
                                        completedTasks.add(taskItem);
                                    } else {
                                        pendingTasks.add(taskItem);
                                    }
                                }

                                pendingTaskAdapter.notifyDataSetChanged();
                                completedTaskAdapter.notifyDataSetChanged();
                                updateNoDataImageVisibility(); // Update image based on tasks

                                // **Ensure card visibility updates properly**
                                if (completedTasks.isEmpty()) {
                                    binding.cardViewCompleted.setVisibility(View.GONE);

                                    binding.buttonToggleCompleted.setVisibility(View.GONE);

                                } else {
                                    binding.cardViewCompleted.setVisibility(View.VISIBLE);

                                    binding.buttonToggleCompleted.setVisibility(View.VISIBLE);

                                }
                            });
                        } else {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Error getting tasks", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
        }
    }


    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    private void onTaskLongClick(Task task) {
        Log.d("TasksFragment", "Task long-clicked: " + task.getTaskId());
        CreateTaskBottomSheetFragment bottomSheet = new CreateTaskBottomSheetFragment();
        bottomSheet.setTaskId(task.getTaskId(), true, this);
        bottomSheet.setRefreshListener(this);
        bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
    }

    @Override
    public void refresh() {
        fetchAndSyncTasks();
    }

    private void toggleCompletedTasks() {
        boolean hasCompletedTasks = !completedTasks.isEmpty();
        if (hasCompletedTasks) {
            // Hide only the RecyclerView that shows completed tasks
            binding.recyclerViewCompleted.setVisibility(isCompletedTasksVisible ? View.GONE : View.VISIBLE);

            // Update the button text based on visibility state
            binding.buttonToggleCompleted.setText(isCompletedTasksVisible ? "Show Completed Tasks" : "Hide Completed Tasks");

            // Toggle the visibility state flag
            isCompletedTasksVisible = !isCompletedTasksVisible;
        }
    }


    private void clearCompletedTasks() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("complete", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> taskIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            taskIds.add(document.getId());
                        }

                        if (taskIds.isEmpty()) {
                            Toast.makeText(getContext(), "No completed tasks found", Toast.LENGTH_SHORT).show();
                        } else {
                            for (String taskId : taskIds) {
                                firestore.collection("tasks").document(taskId).delete();
                            }
                            Toast.makeText(getContext(), "Completed tasks cleared", Toast.LENGTH_SHORT).show();

                            // ** Fetch completed tasks again to update UI **
                            firestore.collection("tasks")
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("complete", true)
                                    .get()
                                    .addOnCompleteListener(updatedTask -> {
                                        if (updatedTask.isSuccessful()) {
                                            completedTasks.clear();
                                            for (QueryDocumentSnapshot document : updatedTask.getResult()) {
                                                completedTasks.add(document.toObject(Task.class)); // Assuming Task is your model
                                            }
                                            completedTaskAdapter.notifyDataSetChanged();

                                            if (completedTasks.isEmpty()) {
                                                binding.cardViewCompleted.setVisibility(View.GONE);

                                                binding.buttonToggleCompleted.setVisibility(View.GONE);

                                            } else {
                                                binding.cardViewCompleted.setVisibility(View.VISIBLE);

                                                binding.buttonToggleCompleted.setVisibility(View.VISIBLE);

                                            }

                                            updateNoDataImageVisibility();
                                        }
                                    });
                        }
                    }
                });

    }

    public void onTaskCompleted(Task task) {
        pendingTasks.remove(task);
        completedTasks.add(task);

        FirebaseFirestore.getInstance().collection("tasks")
                .document(task.getTaskId())
                .update("isComplete", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TasksFragment", "Task marked as complete in Firestore");

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            pendingTaskAdapter.notifyDataSetChanged();
                            completedTaskAdapter.notifyDataSetChanged();
                            updateNoDataImageVisibility();
                            updateCompletedTaskVisibility();
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e("TasksFragment", "Error updating task completion status in Firestore", e));
    }

    private void updateCompletedTaskVisibility() {
        if (completedTasks.isEmpty()) {
            binding.cardViewCompleted.setVisibility(View.GONE);
            binding.buttonToggleCompleted.setVisibility(View.GONE);
        } else {
            binding.cardViewCompleted.setVisibility(View.VISIBLE);
            binding.buttonToggleCompleted.setVisibility(View.VISIBLE);
        }
    }

}
