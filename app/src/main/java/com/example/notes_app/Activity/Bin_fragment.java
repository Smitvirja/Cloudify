package com.example.notes_app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.notes_app.R;
import com.example.notes_app.Models.Notes;
import com.example.notes_app.adapter.BinAdapter;
import com.example.notes_app.dao.NoteDao;
import com.example.notes_app.database.NoteDatabase;
import com.example.notes_app.entities.NoteEntity;
import com.example.notes_app.listeners.BinNotesListener;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class Bin_fragment extends Fragment implements BinNotesListener {

    private RecyclerView binRecyclerView;
    private BinAdapter binAdapter;
    private List<Notes> deletedNotes;
    private FirebaseFirestore firestore;
    private NoteDao noteDao;

    private ImageView ClearNotes;
    private EditText searchEditText;
    private BottomAppBar layoutquickactionbin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_bin_fragment, container, false);

        setHasOptionsMenu(true); // Enables options menu for this fragment

        // Initialize Firestore and Room database
        firestore = FirebaseFirestore.getInstance();
        NoteDatabase appDatabase = NoteDatabase.getInstance(requireContext());
        noteDao = appDatabase.noteDao();

        // Initialize RecyclerView and adapter
        binRecyclerView = view.findViewById(R.id.binRecyclerView);
        binRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));
        binAdapter = new BinAdapter(this);
        binRecyclerView.setAdapter(binAdapter);
        ClearNotes= view.findViewById(R.id.clearnotes);

        // Initialize notes list
        deletedNotes = new ArrayList<>();


        // Set up search functionality
        searchEditText = view.findViewById(R.id.searchBinEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ClearNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearAllDeletedNotes();
            }
        });



        // Load deleted notes from Firestore
        loadDeletedNotesFromFirestore();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload or refresh data for the bin here
        loadDeletedNotesFromFirestore();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.bottom_nav_bin, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.clear_notes) {
            Log.d("BinFragment", "Clear notes clicked");
            clearAllDeletedNotes();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearAllDeletedNotes() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("notes")
                .whereEqualTo("userId", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .whereEqualTo("deleted", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> deletedNoteIds = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            deletedNoteIds.add(document.getId());
                        }

                        if (deletedNoteIds.isEmpty()) {

                        } else {
                            for (String noteId : deletedNoteIds) {
                                firestore.collection("notes").document(noteId).delete()
                                        .addOnSuccessListener(aVoid -> Log.d("BinFragment", "Note deleted: " + noteId))
                                        .addOnFailureListener(e -> Log.e("BinFragment", "Error deleting note: " + noteId, e));
                            }

                            Executors.newSingleThreadExecutor().execute(() -> {
                                try {
                                    noteDao.deleteNotesByIds(deletedNoteIds);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "All deleted notes cleared", Toast.LENGTH_SHORT).show();
                                        loadDeletedNotesFromFirestore();
                                    });
                                } catch (Exception e) {
                                    Log.e("BinFragment", "Error clearing notes from Room database", e);
                                }
                            });
                        }
                    } else {
                        Log.e("BinFragment", "Error getting notes", task.getException());
                    }
                });
    }

    private void loadDeletedNotesFromFirestore() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("notes")
                .whereEqualTo("userId", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .whereEqualTo("deleted", true)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("BinFragment", "Error loading deleted notes", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<Notes> newDeletedNotes = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Notes note = document.toObject(Notes.class);
                            if (note != null) {
                                newDeletedNotes.add(note);
                            }
                        }
                        updateDeletedNotes(newDeletedNotes);
                    }
                });
    }

    private void updateDeletedNotes(List<Notes> newDeletedNotes) {
        deletedNotes.clear();
        deletedNotes.addAll(newDeletedNotes);
        binAdapter.setDeletedNotes(deletedNotes);
        binAdapter.notifyDataSetChanged();


    }

    @Override
    public void onNoteClicked(Notes note, int position) {
        // Work in progress
    }

    @Override
    public void onRestoreClicked(Notes note, int position) {
        restoreNoteFromFirestore(note, position);
    }

    @Override
    public void onDeleteClicked(Notes note, int position) {
        permanentlyDeleteNoteFromFirestore(note, position);
    }

    private void restoreNoteFromFirestore(Notes note, int position) {
        note.setDeleted(false);

        firestore.collection("notes").document(note.getId())
                .set(note)
                .addOnSuccessListener(aVoid -> {
                    if (position >= 0 && position < deletedNotes.size()) {
                        deletedNotes.remove(position);
                        binAdapter.notifyItemRemoved(position);
                    }

                    NoteEntity noteEntity = convertToNoteEntity(note);

                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (noteDao != null) {
                            noteDao.insertOrUpdate(noteEntity);
                            loadDeletedNotesFromFirestore();

                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Note restored", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                })
                .addOnFailureListener(e -> requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error restoring note: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void permanentlyDeleteNoteFromFirestore(Notes note, int position) {
        Executors.newSingleThreadExecutor().execute(() -> {
            firestore.collection("notes").document(note.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> requireActivity().runOnUiThread(() -> {
                        if (position >= 0 && position < deletedNotes.size()) {
                            deletedNotes.remove(position);
                            binAdapter.notifyItemRemoved(position);
                            Toast.makeText(requireContext(), "Note deleted permanently", Toast.LENGTH_SHORT).show();
                        }
                    }))
                    .addOnFailureListener(e -> requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Error deleting note: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    ));

            try {
                noteDao.deleteNoteById(note.getId());
            } catch (Exception e) {
                Log.e("BinFragment", "Error deleting note from Room DB", e);
            }
        });
    }

    private NoteEntity convertToNoteEntity(Notes note) {
        NoteEntity noteEntity = new NoteEntity();
        noteEntity.setId(note.getId());
        noteEntity.setTitle(note.getTitle());
        noteEntity.setSubtitle(note.getSubtitle());
        noteEntity.setNote(note.getNote());
        noteEntity.setDate(note.getDate());
        noteEntity.setColor(note.getColor());
        noteEntity.setImageUrl(note.getImageUrl());
        noteEntity.setUserId(note.getUserId());
        noteEntity.setDeleted(note.isDeleted());
        return noteEntity;
    }

    private void performSearch(String searchQuery) {
        LiveData<List<NoteEntity>> liveDataNotes = noteDao.searchDeletedNotes(searchQuery);

        liveDataNotes.observe(getViewLifecycleOwner(), noteEntities -> {
            List<Notes> notes = new ArrayList<>();

            for (NoteEntity noteEntity : noteEntities) {
                Notes note = new Notes();
                note.setId(noteEntity.getId());
                note.setTitle(noteEntity.getTitle());
                note.setSubtitle(noteEntity.getSubtitle());
                note.setNote(noteEntity.getNote());
                note.setDate(noteEntity.getDate());
                note.setColor(noteEntity.getColor());
                note.setImageUrl(noteEntity.getImageUrl());
                note.setUserId(noteEntity.getUserId());
                note.setDeleted(noteEntity.isDeleted());
                notes.add(note);
            }

            binAdapter.setDeletedNotes(notes);
            binAdapter.notifyDataSetChanged();
        });
    }
}
