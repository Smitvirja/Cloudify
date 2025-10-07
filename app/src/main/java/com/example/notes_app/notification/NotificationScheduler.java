package com.example.notes_app.notification;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.concurrent.TimeUnit;

public class NotificationScheduler {
    public static void scheduleTaskDeadlineNotification(Context context, String taskName, long deadlineInMillis) {
        long delayMillis = calculateDelayUntilNotification(deadlineInMillis);

        Data inputData = new Data.Builder()
                .putString("task_name", taskName)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                taskName,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }

    private static long calculateDelayUntilNotification(long taskDeadlineMillis) {
        long currentTimeMillis = System.currentTimeMillis();
        long notificationTimeMillis = taskDeadlineMillis - TimeUnit.MINUTES.toMillis(5);
        return Math.max(notificationTimeMillis - currentTimeMillis, 0);
    }
}
