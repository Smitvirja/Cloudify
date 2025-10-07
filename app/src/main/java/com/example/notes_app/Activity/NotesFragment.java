package com.example.notes_app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.notes_app.Models.Notes;
import com.example.notes_app.R;
import com.example.notes_app.adapter.recayclerviewAdapter;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.example.notes_app.dao.NoteDao;
import com.example.notes_app.database.NoteDatabase;
import com.example.notes_app.databinding.ActivityNotesFragmentBinding;
import com.example.notes_app.entities.NoteEntity;
import com.example.notes_app.listeners.NotesListner;
import com.example.notes_app.utility.NoteSyncManager;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NotesFragment extends Fragment implements NotesListner {

    private recayclerviewAdapter notesAdapter;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private NoteDao noteDao;
    private NoteSyncManager noteSyncManager;
    private ActivityNotesFragmentBinding binding;
    private GestureDetector gestureDetector;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Notes> notesList = new ArrayList<>();


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ActivityNotesFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initializeRoomDatabase();
        initializeNoteSyncManager();
        setupRecyclerView();
        setupSearchFunctionality();
        loadNotesFromFirestore();

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNotesFromFirestore();
            swipeRefreshLayout.setRefreshing(false);
        });


        FloatingActionButton optionOne = binding.optionOne;
        FloatingActionButton optionTwo = binding.optionTwo;
        FloatingActionButton optionThree = binding.optionThree;
        FloatingActionsMenu fabMenu = binding.layoutquickactiontodo;

        optionOne.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreatingNoteActivity.class);
            startActivity(intent);
            fabMenu.collapse();  // Close the FAB menu after option one is clicked
        });

        optionTwo.setOnClickListener(v -> {
            Log.d("Utility", "Reminder quick add clicked");

            CreateTaskBottomSheetFragment fragment = new CreateTaskBottomSheetFragment();
            fragment.show(getChildFragmentManager(), "CreateTaskBottomSheet");

            fabMenu.collapse();  // Close the FAB menu after option two is clicked
        });

        optionThree.setOnClickListener(v -> {
             // Close the FAB menu after option three is clicked
        });

        Glide.with(this)
                .load(R.drawable.add_first_note) // Replace with your actual image resource
                .into(binding.noDataImagenotes);
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeRoomDatabase() {
        NoteDatabase appDatabase = NoteDatabase.getInstance(getContext());
        noteDao = appDatabase.noteDao();
    }

    private void initializeNoteSyncManager() {
        noteSyncManager = new NoteSyncManager(firestore, noteDao);
    }

    private void setupRecyclerView() {
        binding.noteRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));

        notesList = new ArrayList<>(); // ✅ Initialize the list
        notesAdapter = new recayclerviewAdapter(this, this);
        // ✅ Pass both arguments

        binding.noteRecyclerView.setAdapter(notesAdapter);
    }


    private void setupSearchFunctionality() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    loadNotesFromFirestore();
                }
            }
        });
    }

    private void loadNotesFromFirestore() {
        firestore.collection("notes")
                .whereEqualTo("userId", firebaseAuth.getCurrentUser().getUid())
                .whereEqualTo("deleted", false)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        showToast("Error loading notes: " + e.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<Notes> notesList = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Notes note = document.toObject(Notes.class);
                            if (note != null) {
                                note.setId(document.getId());
                                notesList.add(note);
                                syncNoteWithRoomDatabase(note);
                            }
                        }

                        // Update the adapter with the loaded notes
                        updateNotesAdapter(notesList);


                        View rootView = getView();
                        if (rootView != null) {
                            ImageView noDataImagenotes = rootView.findViewById(R.id.noDataImagenotes);

                            // If no notes, show the ImageView (for adding new notes)
                            if (notesList.isEmpty()) {
                                noDataImagenotes.setVisibility(View.VISIBLE); // Show the ImageView
                            } else {
                                noDataImagenotes.setVisibility(View.GONE); // Hide the ImageView
                            }
                        }
                    }
                });
    }

    private void syncNoteWithRoomDatabase(Notes note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            NoteEntity noteEntity = new NoteEntity(note.getId(), note.getTitle(), note.getSubtitle(), note.getNote(),
                    note.getDate(), note.getColor(), note.getImageUrl(), note.getUserId(), note.isDeleted());
            noteDao.insertOrUpdate(noteEntity);
        });
    }

    private void performSearch(String searchQuery) {
        LiveData<List<NoteEntity>> liveDataNotes = noteDao.searchNotes(searchQuery);
        liveDataNotes.observe(getViewLifecycleOwner(), noteEntities -> {
            List<Notes> notes = convertNoteEntitiesToNotes(noteEntities);
            updateNotesAdapter(notes);
        });
    }

    private List<Notes> convertNoteEntitiesToNotes(List<NoteEntity> noteEntities) {
        List<Notes> notes = new ArrayList<>();
        for (NoteEntity noteEntity : noteEntities) {
            Notes note = new Notes(noteEntity.getId(), noteEntity.getTitle(), noteEntity.getSubtitle(),
                    noteEntity.getNote(), noteEntity.getDate(), noteEntity.getColor(),
                    noteEntity.getImageUrl(), noteEntity.getUserId(), noteEntity.isDeleted());
            notes.add(note);
        }
        return notes;
    }

    private void updateNotesAdapter(List<Notes> notesList) {
        notesAdapter.setNotes(notesList);
        notesAdapter.notifyDataSetChanged();
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNoteClicked(Notes note, int position) {
        Intent intent = new Intent(getContext(), CreatingNoteActivity.class);
        intent.putExtra("note", note);
        intent.putExtra("isViewOrUpdate", true);
        startActivity(intent);
    }

    @Override
    public void onNoteLongClicked(Notes note, int position) {
//        showDeleteConfirmationDialog(note);
    }

//    private void showDeleteConfirmationDialog(Notes note) {
//        // Use getContext() instead of NotesFragment.this
//        View customView = LayoutInflater.from(getContext()).inflate(R.layout.custom_dialog, null);
//        ((TextView) customView.findViewById(R.id.dialog_message)).setText(R.string.are_you_sure_you_want_to_delete_this_note);
//
//        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog)
//                .setView(customView)
//                .create();
//
//        if (dialog.getWindow() != null) {
//            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//        }
//
//        customView.findViewById(R.id.positive_button).setOnClickListener(v -> deleteNoteWithConfirmation(note, dialog));
//        customView.findViewById(R.id.negative_button).setOnClickListener(v -> dialog.cancel());
//
//        dialog.show();
//    }
//
//
//    private void deleteNoteWithConfirmation(Notes note, AlertDialog dialog) {
//        Executors.newSingleThreadExecutor().execute(() -> {
//            NoteEntity noteEntity = createNoteEntityFromNoteObject(note);
//            try {
//                deleteNoteFromFirestore(noteEntity, dialog);
//            } catch (Exception e) {
//                Log.e("MainActivity", "Error during deletion process", e);
//                // Corrected usage of runOnUiThread in Fragment
//                getActivity().runOnUiThread(() -> showToast("Error deleting note"));
//            }
//        });
//    }


    private NoteEntity createNoteEntityFromNoteObject(Notes note) {
        return new NoteEntity(note.getId(), note.getTitle(), note.getSubtitle(), note.getNote(),
                note.getDate(), note.getColor(), note.getImageUrl(), note.getUserId(), true);
    }

    private void deleteNoteFromFirestore(NoteEntity noteEntity, AlertDialog dialog) {
        firestore.collection("notes").document(noteEntity.getId())
                .update("deleted", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d("MainActivity", "Note marked as deleted in Firestore");

                    // Run the Room database update on a background thread
                    Executors.newSingleThreadExecutor().execute(() -> {
                        noteDao.update(noteEntity);

                        // Once the update is complete, update the UI on the main thread
                        // Corrected usage of runOnUiThread in Fragment
                        getActivity().runOnUiThread(() -> {
                            showToast("Note deleted");
                            dialog.dismiss();
                        });
                    });
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "Error updating note in Firestore", e));
    }

    @Override
    public void onRequestDeleteNotes(List<Notes> selectedNotes) {
        if (selectedNotes.isEmpty()) return;

        Log.d("onrequestdelete(fragment)", "Selected notes count: " + selectedNotes.size());

        // ✅ Pass a new copy of the list to avoid reference issues
        showDeleteConfirmationDialog(new ArrayList<>(selectedNotes));
    }


    private void showDeleteConfirmationDialog(List<Notes> selectedNotes) {
        View customView = LayoutInflater.from(getContext()).inflate(R.layout.custom_dialog, null);
        ((TextView) customView.findViewById(R.id.dialog_message)).setText(R.string.are_you_sure_you_want_to_delete_this_note);

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog)
                .setView(customView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        customView.findViewById(R.id.positive_button).setOnClickListener(v -> {
            deleteSelectedNotes(selectedNotes); // Delete notes
            dialog.dismiss();
        });

        customView.findViewById(R.id.negative_button).setOnClickListener(v -> dialog.cancel());

        dialog.show();
    }

    private void deleteSelectedNotes(List<Notes> selectedNotes) {
        Log.d("deleteSelectedNotes", "Before deletion, count: " + selectedNotes.size());

        if (selectedNotes.isEmpty()) {
            Log.e("deleteSelectedNotes", "List is unexpectedly empty!");
            Toast.makeText(getContext(), "No notes selected", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        for (Notes note : selectedNotes) {
            Log.d("deleteSelectedNotes", "Deleting note: " + note.getId());

            // Firestore deletion
            DocumentReference docRef = db.collection("notes").document(note.getId());
            batch.update(docRef, "deleted", true);
        }

        // Commit the Firestore batch update
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "All selected notes marked as deleted");

                    // Remove the notes from the adapter (UI update)
                    notesAdapter.removeSelectedNotes(selectedNotes);

                    // Now delete the notes from Room database
                    deleteNotesFromRoom(selectedNotes);

                    Toast.makeText(getContext(), "Notes moved to bin", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error marking notes as deleted", e);
                    Toast.makeText(getContext(), "Error moving notes to bin", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteNotesFromRoom(List<Notes> selectedNotes) {
        List<String> noteIds = new ArrayList<>();
        for (Notes note : selectedNotes) {
            noteIds.add(note.getId()); // Add note IDs to the list
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            // Call the DAO's delete method
            noteDao.deleteNotesByIds(noteIds);  // Pass the list of note IDs

            // Log to confirm the operation
            Log.d("Room", "Notes deleted from Room database: " + noteIds.size() + " notes deleted.");
        });
    }



}
