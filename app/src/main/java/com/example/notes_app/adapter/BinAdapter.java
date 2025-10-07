package com.example.notes_app.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.notes_app.Models.Notes;
import com.example.notes_app.R;
import com.example.notes_app.listeners.BinNotesListener;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BinAdapter extends RecyclerView.Adapter<BinAdapter.BinViewHolder> {

    private List<Notes> deletedNotes;
    private final BinNotesListener binNotesListener;

    public BinAdapter(BinNotesListener binNotesListener) {
        this.deletedNotes = new ArrayList<>();
        this.binNotesListener = binNotesListener;
    }

    @NonNull
    @Override
    public BinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card, parent, false);
        return new BinViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BinViewHolder holder, int position) {
        Notes note = deletedNotes.get(position);
        holder.bind(note);
        holder.notesContainer.setOnClickListener(v -> binNotesListener.onNoteClicked(note, position));
        holder.notesContainer.setOnLongClickListener(v -> {
            showPopupMenu(v, note, position);
            return true; // Return true to indicate the long click was handled
        });
    }

    @Override
    public int getItemCount() {
        return deletedNotes.size();
    }

    public void setDeletedNotes(List<Notes> notes) {
        Log.d("BinAdapter", "Setting deleted notes. Size: " + notes.size());
        this.deletedNotes = notes;
        notifyDataSetChanged();
    }


    @SuppressLint("NonConstantResourceId")
    private void showPopupMenu(View view, Notes note, int position) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.bin_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> handleMenuItemClick(item.getItemId(), note, position));

        popupMenu.show();
    }

    private boolean handleMenuItemClick(int itemId, Notes note, int position) {
        if (itemId == R.id.restore) {
            binNotesListener.onRestoreClicked(note, position);
            return true;
        } else if (itemId == R.id.permanent_delete) {
            binNotesListener.onDeleteClicked(note, position);
            return true;
        } else {
            return false;
        }
    }


    static class BinViewHolder extends RecyclerView.ViewHolder {
        TextView viewTitle, viewSubtitle, viewDate, viewNote;
        LinearLayout notesContainer;
        RoundedImageView imageNote;

        public BinViewHolder(@NonNull View itemView) {
            super(itemView);
            viewTitle = itemView.findViewById(R.id.viewtitle);
            viewSubtitle = itemView.findViewById(R.id.viewsub);
            viewDate = itemView.findViewById(R.id.viewdate);
            viewNote = itemView.findViewById(R.id.note);
            notesContainer = itemView.findViewById(R.id.layoutnote);
            imageNote = itemView.findViewById(R.id.imagenote);
        }

        void bind(Notes note) {
            if (note != null) {
                viewTitle.setText(note.getTitle() != null ? note.getTitle() : "");

                if (note.getSubtitle() != null && !note.getSubtitle().trim().isEmpty()) {
                    viewSubtitle.setVisibility(View.VISIBLE);
                    viewSubtitle.setText(note.getSubtitle());
                } else {
                    viewSubtitle.setVisibility(View.GONE);
                }

                viewDate.setText(note.getDate() != null ? note.getDate() : "");
                viewNote.setText(note.getNote() != null ? note.getNote() : "");

                GradientDrawable background = (GradientDrawable) notesContainer.getBackground();
                if (note.getColor() != null && !note.getColor().isEmpty()) {
                    background.setColor(Color.parseColor(note.getColor()));
                } else {
                    background.setColor(getRandomColor());
                }
                notesContainer.setBackground(background);

                if (note.getImageUrl() != null && !note.getImageUrl().isEmpty()) {
                    imageNote.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(note.getImageUrl())
                            .into(imageNote);
                } else {
                    imageNote.setVisibility(View.GONE);
                }
            }
        }

        private int getRandomColor() {
            int[] colorArray = itemView.getResources().getIntArray(R.array.card_colors);
            Random random = new Random();
            return colorArray.length > 0 ? colorArray[random.nextInt(colorArray.length)] : Color.WHITE;
        }
    }
}
