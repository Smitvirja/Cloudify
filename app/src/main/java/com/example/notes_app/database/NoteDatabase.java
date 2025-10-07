package com.example.notes_app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.notes_app.dao.NoteDao;
import com.example.notes_app.entities.NoteEntity;

@Database(entities = {NoteEntity.class}, version = 2, exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {

    private static  NoteDatabase INSTANCE;

    public abstract NoteDao noteDao();

    public static NoteDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (NoteDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    NoteDatabase.class, "notes_database")
                                    .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

