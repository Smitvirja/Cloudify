package com.example.notes_app.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.notes_app.Models.Notes;
import com.example.notes_app.R;
import com.example.notes_app.listeners.NotesListner;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class recayclerviewAdapter extends RecyclerView.Adapter<recayclerviewAdapter.NoteViewHolder> {

    private List<Notes> notesList;
    private final NotesListner notesListener;
    private final NotesListner actionListener;
    private boolean multiSelect = false;
    private final List<Notes> selectedNotes = new ArrayList<>();
    private ActionMode actionMode;

    public recayclerviewAdapter(NotesListner notesListener, NotesListner actionListener) {
        this.notesList = new ArrayList<>();
        this.notesListener = notesListener;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card, parent, false);

        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Notes note = notesList.get(position);
        boolean isSelected = selectedNotes.contains(note);
        holder.bind(note, isSelected);

        holder.notesContainer.setOnClickListener(v -> {
            if (multiSelect) {
                selectItem(holder, note);
            } else {
                notesListener.onNoteClicked(note, position);
            }
        });

        holder.notesContainer.setOnLongClickListener(v -> {
            if (!multiSelect) {
                multiSelect = true;
                if (holder.itemView.getContext() instanceof AppCompatActivity) {
                    actionMode = ((AppCompatActivity) holder.itemView.getContext()).startSupportActionMode(actionModeCallback);
                }
            }
            selectItem(holder, note);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public void setNotes(List<Notes> notes) {
        this.notesList = notes;
        notifyItemRangeChanged(0, notesList.size()); // ✅ Preserves selection
    }

    // Selecting/Deselecting Notes
    private void selectItem(NoteViewHolder holder, Notes note) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note);
        } else {
            selectedNotes.add(note);
        }

        // ✅ Update the UI
        holder.bind(note, selectedNotes.contains(note));

        // ✅ Ensure ActionMode starts when selecting first note
        if (!selectedNotes.isEmpty()) {
            if (actionMode == null) {
                actionMode = ((AppCompatActivity) holder.itemView.getContext()).startSupportActionMode(actionModeCallback);
            }
            actionMode.setTitle(selectedNotes.size() + " selected");
        } else {
            if (actionMode != null) {
                actionMode.finish();
            }
        }

        Log.d("adaptor", "Selected notes count: " + selectedNotes.size());
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView viewTitle, viewSubtitle, viewDate, viewNote;
        LinearLayout notesContainer;
        RoundedImageView imageNote;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            viewTitle = itemView.findViewById(R.id.viewtitle);
            viewSubtitle = itemView.findViewById(R.id.viewsub);
            viewDate = itemView.findViewById(R.id.viewdate);
            viewNote = itemView.findViewById(R.id.note);
            notesContainer = itemView.findViewById(R.id.layoutnote);
            imageNote = itemView.findViewById(R.id.imagenote);
        }

        void bind(Notes note, boolean isSelected) {
            viewTitle.setText(note.getTitle());
            viewSubtitle.setText(note.getSubtitle().isEmpty() ? "No Subtitle" : note.getSubtitle());
            viewDate.setText(note.getDate());
            viewNote.setText(note.getNote());

            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(25);
            background.setColor(Color.parseColor(note.getColor()));

            // Get predefined color from colors.xml
            int strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.colorAccent);

            if (isSelected) {
                background.setStroke(8, strokeColor); // Use your color instead of CYAN
            } else {
                background.setStroke(0, Color.TRANSPARENT);
            }

            notesContainer.setBackground(background);

            if (note.getImageUrl() != null && !note.getImageUrl().isEmpty()) {
                imageNote.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(note.getImageUrl()).into(imageNote);
            } else {
                imageNote.setVisibility(View.GONE);
            }
        }

    }
    public void removeSelectedNotes(List<Notes> selectedNotes) {
        this.notesList.removeAll(selectedNotes);
        this.selectedNotes.clear(); // ✅ Clear selection after deletion
        notifyDataSetChanged(); // ✅ Refresh UI properly
    }

    // Action Mode Callback
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.note_selection_menu, menu);

            // ✅ Use the ViewHolder's context safely
            Context context = actionMode != null && selectedNotes.size() > 0
                    ? ((RecyclerView) actionMode.getMenu()).getContext()
                    : null;

            if (context != null) {
                TextView customTitle = new TextView(context);
                customTitle.setText(selectedNotes.size() + " selec");
                customTitle.setTextColor(Color.WHITE);
                customTitle.setTextSize(18);
                customTitle.setTypeface(Typeface.DEFAULT_BOLD);
                customTitle.setBackgroundColor(Color.TRANSPARENT);
                customTitle.setPadding(16, 8, 16, 8);

                mode.setCustomView(customTitle);
            }

            return true;
        }






        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            int selectedCount = selectedNotes.size();
            TextView customTitle = (TextView) mode.getCustomView();

            if (customTitle != null) {
                customTitle.setText(selectedCount + " selec");
            }

            return true;
        }




        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.delete) {
                if (!selectedNotes.isEmpty() && actionListener != null) {
                    actionListener.onRequestDeleteNotes(selectedNotes);
                    mode.finish();
                }
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            multiSelect = false;

            // ✅ Instead of clearing list immediately, update UI first
            List<Notes> tempSelected = new ArrayList<>(selectedNotes);
            selectedNotes.clear();

            for (Notes note : tempSelected) {
                int index = notesList.indexOf(note);
                if (index != -1) {
                    notifyItemChanged(index);
                }
            }

            actionMode = null;
            Log.d("DeleteNotes", "onDestroyActionMode called. Selected notes count: " + selectedNotes.size());
        }


    };
}