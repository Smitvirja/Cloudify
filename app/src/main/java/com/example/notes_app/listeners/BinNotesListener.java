package com.example.notes_app.listeners;

import com.example.notes_app.Models.Notes;

public interface BinNotesListener {
    void onNoteClicked(Notes note, int position);
    void onRestoreClicked(Notes note, int position);
    void onDeleteClicked(Notes note, int position);
//    void onNoteLongClicked(Notes note, int position);
}
