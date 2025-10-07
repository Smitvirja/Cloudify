package com.example.notes_app.Models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey
    @ColumnInfo(name = "id")
    private String taskId;

    @ColumnInfo(name = "title")
    private String taskTitle;

    @ColumnInfo(name = "date")
    private String date;

    @ColumnInfo(name = "description")
    private String taskDescription;

    @ColumnInfo(name = "isCompleted")
    private boolean complete;

    @ColumnInfo(name = "lastAlarm")
    private String lastAlarm;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "taskEvent")
    private String taskEvent;

    @ColumnInfo(name = "event")
    private String event;

    @ColumnInfo(name = "userId")
    private String userId;

    @ColumnInfo(name = "deadline")
    private Long deadline;



    // Default constructor
    public Task() {}

    // Constructor
    public Task(String taskId, String taskTitle, String date, String taskDescription, boolean complete,
                String lastAlarm, String status, String taskEvent, String event, String userId, Long deadline) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.date = date;
        this.taskDescription = taskDescription;
        this.complete = complete;
        this.lastAlarm = lastAlarm;
        this.status = status;
        this.taskEvent = taskEvent;
        this.event = event;
        this.userId = userId;
        this.deadline = deadline;  // Initialize deadline
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }


    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public String getLastAlarm() {
        return lastAlarm;
    }

    public void setLastAlarm(String lastAlarm) {
        this.lastAlarm = lastAlarm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTaskEvent() {
        return taskEvent;
    }

    public void setTaskEvent(String taskEvent) {
        this.taskEvent = taskEvent;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getDeadline() {  // Getter for deadline
        return deadline;
    }

    public void setDeadline(Long deadline) {  // Setter for deadline
        this.deadline = deadline;
    }

    // Methods to convert Joda-Time objects to/from Strings
    public String getDateAsString() {
        return date != null ? date : "";
    }

    public void setDateFromString(String dateString) {
        this.date = dateString; // Assuming dateString is in "dd-MM-yyyy" format
    }

    public String getLastAlarmAsString() {
        return lastAlarm != null ? lastAlarm : "";
    }

    public void setLastAlarmFromString(String timeString) {
        this.lastAlarm = timeString; // Assuming timeString is in "HH:mm" format
    }
}
