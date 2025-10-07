package com.example.notes_app.bottomSheetFragment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.notes_app.Activity.TasksFragment;
import com.example.notes_app.Models.Task;
import com.example.notes_app.R;
import com.example.notes_app.databinding.FragmentCreateTaskBinding;
import com.example.notes_app.notification.NotificationWorker;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CreateTaskBottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentCreateTaskBinding binding;
    private String taskId;
    private boolean isEdit;
    private FirebaseFirestore firestore;
    private SetRefreshListener refreshListener;
    private TimePickerDialog timePickerDialog;

    @NonNull
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        binding = FragmentCreateTaskBinding.inflate(getLayoutInflater());
        dialog.setContentView(binding.getRoot());
        firestore = FirebaseFirestore.getInstance();

        setupViews();
        if (isEdit) showTaskFromId();

        return dialog;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupViews() {
        binding.addTask.setOnClickListener(view -> {
            if (validateFields()) {
                if (isEdit) updateTask();
                else createTask();
            }
        });

        binding.taskDate.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                showDatePickerDialog();
            }
            return true;
        });

        binding.taskTime.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                showTimePickerDialog();
            }
            return true;
        });
    }

    private void showDatePickerDialog() {
        LocalDate currentDate = LocalDate.now();

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), R.style.CustomDatePickerDialog,
                (view, year, monthOfYear, dayOfMonth) -> {
                    LocalDate date = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                    binding.taskDate.setText(date.toString("dd-MM-yyyy"));
                },
                currentDate.getYear(), currentDate.getMonthOfYear() - 1, currentDate.getDayOfMonth());

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }


    private void showTimePickerDialog() {
        LocalTime currentTime = LocalTime.now();

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(requireActivity(), R.style.CustomTimePickerDialog);

        TimePickerDialog timePickerDialog = new TimePickerDialog(contextThemeWrapper,
                (view, hourOfDay, minute) -> {
                    int selectedHour = hourOfDay;
                    String amPm = "AM";

                    if (!view.is24HourView()) {
                        if (selectedHour >= 12) {
                            amPm = "PM";
                            if (selectedHour > 12) {
                                selectedHour -= 12;
                            }
                        } else if (selectedHour == 0) {
                            selectedHour = 12;
                        }
                    }

                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", selectedHour, minute, amPm);
                    binding.taskTime.setText(formattedTime);
                },
                currentTime.getHourOfDay(), currentTime.getMinuteOfHour(), false);

        timePickerDialog.show();
    }


    private boolean validateFields() {
        if (binding.addTaskTitle.getText().toString().trim().isEmpty()) {
            showToast("Please enter a valid title");
            return false;
        } else if (binding.taskDate.getText().toString().trim().isEmpty()) {
            showToast("Please enter date");
            return false;
        } else if (binding.taskTime.getText().toString().trim().isEmpty()) {
            showToast("Please enter time");
            return false;
        } else {
            return true;
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Clear the binding reference to avoid memory leaks
    }

    private void createTask() {
        String taskTitle = binding.addTaskTitle.getText().toString();
        LocalDate date;
        LocalTime time;

        try {
            date = LocalDate.parse(binding.taskDate.getText().toString(), DateTimeFormat.forPattern("dd-MM-yyyy"));
            time = LocalTime.parse(binding.taskTime.getText().toString(), DateTimeFormat.forPattern("hh:mm a"));
        } catch (IllegalArgumentException e) {
            showToast("Invalid date or time format.");
            return;
        }

        // Combine date and time to get the deadline in milliseconds
        long deadlineInMillis = date.toDateTime(time).getMillis();
        long notificationTimeInMillis = deadlineInMillis;  // Send notification exactly at deadline
        long currentTimeMillis = System.currentTimeMillis();

        // Check if the deadline is in the future
        if (deadlineInMillis <= currentTimeMillis) {
            showToast("The deadline must be in the future.");
            return;
        }

        Task newTask = new Task(
                null, // taskId will be set later
                taskTitle,
                date.toString("dd-MM-yyyy"),
                binding.addTaskDescription.getText().toString(),
                false, // default value for complete
                time.toString("HH:mm"),
                null, // status default value
                null, // taskEvent default value
                binding.taskEvent.getText().toString(),
                getCurrentUserId(),
                deadlineInMillis
        );

        firestore.collection("tasks").add(newTask)
                .addOnSuccessListener(documentReference -> {
                    newTask.setTaskId(documentReference.getId());
                    firestore.collection("tasks").document(newTask.getTaskId()).set(newTask)
                            .addOnSuccessListener(aVoid -> {
                                if (refreshListener != null) {
                                    refreshListener.refresh();
                                }
                                scheduleNotification(taskTitle, notificationTimeInMillis);
                                Log.d("CreateTask", "New Task: " + newTask.getTaskTitle());
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Log.w("CreateTask", "Error updating task document", e);
                                showToast("Error creating task");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w("CreateTask", "Error adding task document", e);
                    showToast("Error creating task");
                });
    }

    private void updateTask() {
        String taskTitle = binding.addTaskTitle.getText().toString();
        LocalDate date;
        LocalTime time;

        try {
            date = LocalDate.parse(binding.taskDate.getText().toString(), DateTimeFormat.forPattern("dd-MM-yyyy"));
            time = LocalTime.parse(binding.taskTime.getText().toString(), DateTimeFormat.forPattern("hh:mm a"));
        } catch (IllegalArgumentException e) {
            showToast("Invalid date or time format.");
            return;
        }

        long deadlineInMillis = date.toDateTime(time).getMillis();
        long notificationTimeInMillis = deadlineInMillis;
        long currentTimeMillis = System.currentTimeMillis();

        if (deadlineInMillis <= currentTimeMillis) {
            showToast("The deadline must be in the future.");
            return;
        }

        Task updatedTask = new Task(
                taskId,
                taskTitle,
                date.toString("dd-MM-yyyy"),
                binding.addTaskDescription.getText().toString(),
                false,
                time.toString("HH:mm"),
                null,
                null,
                binding.taskEvent.getText().toString(),
                getCurrentUserId(),
                deadlineInMillis
        );

        firestore.collection("tasks").document(taskId).set(updatedTask)
                .addOnSuccessListener(aVoid -> {
                    if (refreshListener != null) {
                        refreshListener.refresh();
                    }
                    scheduleNotification(taskTitle, notificationTimeInMillis);
                    Log.d("UpdateTask", "Task updated successfully: " + updatedTask.getTaskTitle());
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.w("UpdateTask", "Error updating task", e);
                    showToast("Error updating task");
                });
    }

    private void showTaskFromId() {
        firestore.collection("tasks").document(taskId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Task task = documentSnapshot.toObject(Task.class);
                    if (task != null) setDataInUI(task);
                })
                .addOnFailureListener(e -> showToast("Error retrieving task"));
    }

    public void setTaskId(String taskId, boolean isEdit, SetRefreshListener setRefreshListener) {
        this.taskId = taskId;
        this.isEdit = isEdit;
        this.refreshListener = setRefreshListener;
    }

    private void setDataInUI(Task task) {
        binding.addTaskTitle.setText(task.getTaskTitle());
        binding.addTaskDescription.setText(task.getTaskDescription());
        binding.taskDate.setText(task.getDate());
        binding.taskTime.setText(task.getLastAlarm());
        binding.taskEvent.setText(task.getEvent());
    }

    private void scheduleNotification(String taskTitle, long deadlineInMillis) {
        long delay = deadlineInMillis - System.currentTimeMillis();

        if (delay <= 0) {
            Log.d("NotificationScheduler", "Task deadline is in the past, skipping notification for: " + taskTitle);
            return;
        }

        Log.d("NotificationScheduler", "Scheduling notification for: " + taskTitle);

        Data data = new Data.Builder()
                .putString("taskTitle", taskTitle)
                .putLong("deadline", deadlineInMillis)
                .build();

        OneTimeWorkRequest notificationRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();

        WorkManager.getInstance(requireContext()).enqueueUniqueWork(
                "TaskDeadlineNotification_" + taskTitle,
                ExistingWorkPolicy.REPLACE,
                notificationRequest
        );
    }

    private long calculateDelayUntilNotification(long taskDeadlineMillis) {
        long currentTimeMillis = System.currentTimeMillis();
        long notificationTimeMillis = taskDeadlineMillis - TimeUnit.MINUTES.toMillis(5);
        return Math.max(notificationTimeMillis - currentTimeMillis, 0);
    }

    public interface SetRefreshListener {
        void refresh();
    }

    public void setRefreshListener(SetRefreshListener listener) {
        this.refreshListener = listener;
    }

    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
