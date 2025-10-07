package com.example.notes_app.dao;
import com.example.notes_app.Models.Notes;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.notes_app.entities.NoteEntity;

import java.util.List;

@Dao

public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(NoteEntity note);


    @Insert
    void insertNote(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("DELETE FROM notes")
    void deleteAllNotes();

    @Query("DELETE FROM notes WHERE userId = :userId")
    void deleteAllNotesForUser(String userId);

    @Query("SELECT * FROM notes")
    LiveData<List<NoteEntity>> getAllNotes();

    @Query("SELECT * FROM notes WHERE id = :noteId")
    LiveData<NoteEntity> getNoteById(String noteId);

    @Query("SELECT * FROM notes WHERE userId = :userId")
    LiveData<List<NoteEntity>> getNotesByUserId(String userId);

    @Query("SELECT * FROM notes WHERE deleted = :includeDeleted")
    LiveData<List<NoteEntity>> getNotesByDeletionStatus(boolean includeDeleted);

    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :searchQuery || '%' OR subtitle LIKE '%' || :searchQuery || '%' OR note LIKE '%' || :searchQuery || '%') AND deleted = 0")
    LiveData<List<NoteEntity>> searchNotes(String searchQuery);


    // Pagination and sorting
    @Query("SELECT * FROM notes ORDER BY title ASC LIMIT :limit OFFSET :offset")
    LiveData<List<NoteEntity>> getNotesWithPagination(int limit, int offset);

    // Count notes by userId
    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId")
    LiveData<Integer> countNotesByUserId(String userId);

    // Count all notes
    @Query("SELECT COUNT(*) FROM notes")
    LiveData<Integer> countAllNotes();

    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteNoteById(String noteId);

    @Query("SELECT * FROM notes WHERE deleted = 1 AND (title LIKE '%' || :searchQuery || '%' OR note LIKE '%' || :searchQuery || '%')")
    LiveData<List<NoteEntity>> searchDeletedNotes(String searchQuery);

    @Query("DELETE FROM notes WHERE id IN (:noteIds)")
    void deleteNotesByIds(List<String> noteIds);
}

