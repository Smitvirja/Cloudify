package com.example.notes_app.listeners;

import com.example.notes_app.Models.Notes;

import java.util.List;

public interface NotesListner {
    void onNoteClicked(Notes note, int position);
    void onNoteLongClicked(Notes note, int position); // Add this method
    void onRequestDeleteNotes(List<Notes> selectedNotes);



}
