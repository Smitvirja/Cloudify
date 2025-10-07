package com.example.notes_app.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import com.example.notes_app.Models.Task;
import com.example.notes_app.entities.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {

    // Insert or update a task
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(TaskEntity task);

    // Retrieve all tasks that are not marked as deleted
    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    LiveData<List<TaskEntity>> getAllTasks();

    // Search for tasks by title or description that are not marked as deleted
    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%') AND isDeleted = 0")
    LiveData<List<TaskEntity>> searchTasks(String searchQuery);

    // Update a task
    @Update
    void update(TaskEntity task);

    // Mark a task as deleted by setting the isDeleted flag to true
    @Query("UPDATE tasks SET isDeleted = 1 WHERE id = :taskId")
    void markAsDeleted(String taskId);

    // Delete all tasks associated with a specific user
    @Query("DELETE FROM tasks WHERE userId = :userId")
    void deleteAllTasksForUser(String userId);

    // Delete all completed tasks
    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    void deleteCompletedTasks();

    @Query("DELETE FROM tasks WHERE id IN (:taskIds)")
    void deleteCompletedTasksByIds(List<String> taskIds);

    // Retrieve all completed tasks that are not marked as deleted
    @Query("SELECT * FROM tasks WHERE isCompleted = 1")
    List<TaskEntity> getCompletedTasks(); // Should return List<TaskEntity>

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    void updateTaskCompletion(String taskId, boolean isCompleted);

    // Retrieve all pending tasks that are not marked as deleted
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isDeleted = 0")
    LiveData<List<TaskEntity>> getPendingTasks();

    // Transactional operation example
    @Transaction
    default void deleteTasksAndInsertNew(List<TaskEntity> tasksToDelete, TaskEntity taskToInsert) {
        for (TaskEntity task : tasksToDelete) {
            markAsDeleted(task.getId());
        }
        insertOrUpdate(taskToInsert);
    }
}
