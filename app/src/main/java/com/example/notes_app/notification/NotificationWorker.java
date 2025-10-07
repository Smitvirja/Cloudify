package com.example.notes_app.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.notes_app.Activity.TasksFragment;
import com.example.notes_app.Activity.home_page;
import com.example.notes_app.R;

public class NotificationWorker extends Worker {

    private static final String CHANNEL_ID = "task_deadline_channel";
    private static final String CHANNEL_NAME = "Task Deadline";
    private static final String CHANNEL_DESC = "Notifications for task deadlines";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String taskTitle = getInputData().getString("taskTitle");
        long deadline = getInputData().getLong("deadline", 0);

        if (taskTitle != null && deadline > 0) {
            sendNotification(taskTitle, deadline);
        } else {
            Log.e("NotificationWorker", "Task title or deadline is missing");
        }

        return Result.success();
    }

    private void sendNotification(String taskTitle, long deadline) {
        Context context = getApplicationContext();

        // Create the notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create an intent to open the app when the notification is clicked
        Intent intent = new Intent(context, home_page.class);  // Open MainActivity
        intent.putExtra("open_tasks_fragment", true);  // Pass a flag
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Task Deadline")
                .setContentText("Your task \"" + taskTitle + "\" is due.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Show the notification with a unique ID based on the task title's hashcode
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(taskTitle.hashCode(), builder.build());
        } else {
            // Handle the case where notifications are not enabled
            Log.e("NotificationWorker", "Notifications are not enabled");
        }
    }
}
