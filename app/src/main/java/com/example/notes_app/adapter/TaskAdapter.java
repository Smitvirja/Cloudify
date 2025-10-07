package com.example.notes_app.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import com.example.notes_app.Activity.TasksFragment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes_app.Models.Task;
import com.example.notes_app.R;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.example.notes_app.databinding.ItemTaskBinding;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final Context context;
    private final List<Task> taskList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EE dd MMM yyyy", Locale.US);
    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd-M-yyyy", Locale.US);
    private final FirebaseFirestore firestore;
    private final CreateTaskBottomSheetFragment.SetRefreshListener setRefreshListener;
    private final OnTaskLongClickListener onTaskLongClickListener;
    private final TasksFragment tasksFragment;

    public interface OnTaskLongClickListener {
        void onTaskLongClick(Task task);
    }

    // Update constructor to accept TasksFragment as a parameter
    public TaskAdapter(Context context, List<Task> taskList, CreateTaskBottomSheetFragment.SetRefreshListener setRefreshListener,
                       OnTaskLongClickListener onTaskLongClickListener, TasksFragment tasksFragment) {
        this.context = context;
        this.taskList = taskList;
        this.setRefreshListener = setRefreshListener;
        this.firestore = FirebaseFirestore.getInstance();
        this.onTaskLongClickListener = onTaskLongClickListener;
        this.tasksFragment = tasksFragment; // Store reference to TasksFragment
    }

    public void updateTaskList(List<Task> newTaskList) {
        this.taskList.clear();
        this.taskList.addAll(newTaskList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ItemTaskBinding binding = ItemTaskBinding.inflate(inflater, viewGroup, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        bindTaskDataToViews(holder, task);

        // Set up long-click listener for each task item
        holder.itemView.setOnLongClickListener(v -> {
            onTaskLongClickListener.onTaskLongClick(task);
            return true; // Return true to indicate that the long-click event is handled
        });

        holder.binding.options.setOnClickListener(view -> showPopUpMenu(view, position));
    }

    private void bindTaskDataToViews(TaskViewHolder holder, Task task) {
        holder.binding.title.setText(task.getTaskTitle());
        holder.binding.description.setText(task.getTaskDescription());
        holder.binding.time.setText(task.getLastAlarm());
        holder.binding.Status.setText(task.isComplete() ? "COMPLETED" : "UPCOMING");

        try {
            if (task.getDate() != null && !task.getDate().isEmpty()) {
                Date date = inputDateFormat.parse(task.getDate());
                String outputDateString = dateFormat.format(date);
                String[] items = outputDateString.split(" ");
                holder.binding.day.setText(items[0]);
                holder.binding.date.setText(items[1]);
                holder.binding.month.setText(items[2]);
            } else {
                clearDateFields(holder);
            }
        } catch (Exception e) {
            e.printStackTrace();
            clearDateFields(holder);
        }
    }

    private void clearDateFields(TaskViewHolder holder) {
        holder.binding.day.setText("");
        holder.binding.date.setText("");
        holder.binding.month.setText("");
    }

    private void showPopUpMenu(View view, int position) {
        final Task task = taskList.get(position);
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.getMenuInflater().inflate(R.menu.menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menuDelete) {
                showDeleteConfirmationDialog(task.getTaskId(), position);
                return true;
            } else if (id == R.id.menuUpdate) {
                showUpdateTaskBottomSheet(task.getTaskId());
                return true;
            } else if (id == R.id.menuComplete) {
                markTaskAsComplete(task); // Mark task as complete
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showDeleteConfirmationDialog(String taskId, int position) {
        // Inflate the custom layout
        View customView = LayoutInflater.from(context).inflate(R.layout.custom_dialog, null);
        TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
        dialogTitleTask.setText(R.string.are_you_sure_you_want_to_delete_this_task);

        // Create the AlertDialog with the custom layout
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setView(customView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Set up the buttons
        MaterialButton positiveButton = customView.findViewById(R.id.positive_button);
        MaterialButton negativeButton = customView.findViewById(R.id.negative_button);
        positiveButton.setOnClickListener(v -> {
            deleteTask(taskId, position);
            dialog.dismiss();
        });
        negativeButton.setOnClickListener(v -> dialog.cancel());

        // Show the dialog
        dialog.show();
    }

    private void showCompleteDialog() {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_completed_theme, null);

        // Create and configure the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Set the background of the dialog's window to be transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Find the button by its ID and set its click listener
        Button completeButton = dialogView.findViewById(R.id.closeButton);
        if (completeButton != null) {
            completeButton.setOnClickListener(v -> {
                // Notify the TasksFragment to update visibility of completed tasks
                tasksFragment.updateNoDataImageVisibility();
                dialog.dismiss();
            });
        } else {
            Log.e("TaskAdapter", "Complete button is null. Check your layout file.");
        }
        // Show the dialog
        dialog.show();
    }

    private void deleteTask(String taskId, int position) {
        firestore.collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    taskList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, taskList.size());

                    // Notify the fragment to refresh the visibility of noDataImage
                    tasksFragment.updateNoDataImageVisibility();

                    Toast.makeText(context, R.string.task_deleted, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, R.string.error_deleting_task, Toast.LENGTH_SHORT).show());
    }

    private void showUpdateTaskBottomSheet(String taskId) {
        CreateTaskBottomSheetFragment bottomSheetFragment = new CreateTaskBottomSheetFragment();
        bottomSheetFragment.setTaskId(taskId, true, setRefreshListener);
        bottomSheetFragment.show(tasksFragment.getParentFragmentManager(), bottomSheetFragment.getTag());
    }

    private void markTaskAsComplete(Task task) {
        firestore.collection("tasks")
                .document(task.getTaskId())
                .update("complete", true)
                .addOnSuccessListener(aVoid -> {
                    task.setComplete(true);
                    showCompleteDialog();
                    // Notify the fragment to refresh the task lists
                    tasksFragment.onTaskCompleted(task);
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskAdapter", "Error marking task as complete", e);
                    Toast.makeText(context, "Error marking task as complete", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskBinding binding;

        TaskViewHolder(@NonNull ItemTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public void addTask(Task task) {
        this.taskList.add(task);
        notifyItemInserted(taskList.size() - 1);
    }
}