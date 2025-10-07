package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.notes_app.database.NoteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.notes_app.Models.Notes;
import com.example.notes_app.R;
import com.example.notes_app.dao.NoteDao;
import com.example.notes_app.entities.NoteEntity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CreatingNoteActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    private ImageView imageBack, save, imagenote;
    private TextView date;
    private EditText inNoteTitle, inNoteSubtitle, inNote;
    private LinearLayout noteContainer;
    private View viewSubtitleIndicator;
    private Uri selectedImageUri;
    private String selectNoteColor = "#333333"; // Default color
    private String localImagePath;
    private Notes existingNote;
    private SharedPref sharedPreferences;
    private boolean isReceiverRegistered = false;

    private boolean isNoteSaved = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = new SharedPref(this);

        // Apply the saved theme
        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }


        setContentView(R.layout.activity_creating_note);

        NoteDatabase appDatabase = NoteDatabase.getInstance(this);
        NoteDao noteDao = appDatabase.noteDao();

        // Initialize views
        initializeViews();

        // Initialize Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Set the current date and time
        setCurrentDateTime();

        // Check if we are editing an existing note
        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            existingNote = (Notes) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();

        } else {
            // Ensure default color is set when creating a new note
            selectNoteColor = "#333333"; // Default color
            setSubtitleIndicatorColor(); // Update the indicator color
        }

        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagenote.setImageBitmap(null);
                imagenote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);

                if (existingNote != null) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("notes").document(existingNote.getId())
                            .update("imageUrl", "")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(CreatingNoteActivity.this, "Image removed", Toast.LENGTH_SHORT).show();
                                existingNote.setImageUrl("");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(CreatingNoteActivity.this, "Failed to remove image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }

                selectedImageUri = null;
                localImagePath = null; // Clear local image path
            }
        });

        save.setOnClickListener(view -> {
            if (existingNote != null) {
                updateNoteInFirestore(db, existingNote.getId());
            } else {
                saveNoteToFirestore(db);
            }
        });

        initMiscellaneous();
    }


    private final BroadcastReceiver themeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.notes_app.THEME_CHANGED".equals(intent.getAction())) {
                // Apply the theme when the broadcast is received
                applyTheme();
                Log.d("home activity", "Broadcast sent for theme change..");
                recreate(); // Recreate activity to apply the new theme
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!isReceiverRegistered) {
            registerReceiver(themeReceiver, new IntentFilter("com.example.notes_app.THEME_CHANGED"));
            isReceiverRegistered = true;
            Log.d("HomeActivity", "Receiver registered.");
        }
        applyTheme(); // Ensure theme is applied when returning
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(themeReceiver);
            isReceiverRegistered = false;
            Log.d("HomeActivity", "Receiver unregistered.");
        }
    }

    @Override
    public void onBackPressed() {
        if (isNoteEmpty()) {
            Toast.makeText(CreatingNoteActivity.this, "Empty note removed", Toast.LENGTH_SHORT).show();
            super.onBackPressed();
        } else if (!isNoteSaved) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (existingNote != null) {
                updateNoteInFirestore(db, existingNote.getId());
            } else {
                saveNoteToFirestore(db);
            }
        }
        super.onBackPressed();
    }


    private void applyTheme() {
        // Retrieve the current theme from SharedPreferences and apply it
        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
    }

    // Method to check if the note is completely empty
    private boolean isNoteEmpty() {
        String title = inNoteTitle.getText().toString().trim();
        String subtitle = inNoteSubtitle.getText().toString().trim();
        String noteText = inNote.getText().toString().trim();

        return title.isEmpty() && subtitle.isEmpty() && noteText.isEmpty();
    }

    private void initializeViews() {
        imageBack = findViewById(R.id.imageBack);
        save = findViewById(R.id.save);
        date = findViewById(R.id.textDateTime);
        inNoteTitle = findViewById(R.id.inputNoteTitle);
        inNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inNote = findViewById(R.id.inputNote);
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        imagenote = findViewById(R.id.imagenote);

        imageBack.setOnClickListener(view -> onBackPressed());
    }


    @SuppressLint("SimpleDateFormat")
    private void setCurrentDateTime() {
        String currentDateTime = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a").format(new Date());
        date.setText(currentDateTime);
    }

    private void setViewOrUpdateNote() {
        if (existingNote != null) {
            inNoteTitle.setText(existingNote.getTitle());
            inNoteSubtitle.setText(existingNote.getSubtitle());
            inNote.setText(existingNote.getNote());

            selectNoteColor = existingNote.getColor();
            setSubtitleIndicatorColor();
            setColorTickBasedOnExistingColor(selectNoteColor); // Ensure color tick is updated
            // Load existing image if it exists
            if (existingNote.getImageUrl() != null && !existingNote.getImageUrl().isEmpty()) {
                displaySelectedImage(Uri.parse(existingNote.getImageUrl()));
            }
        }
    }


    private void saveNoteToFirestore(FirebaseFirestore db) {
        if (isNoteSaved) return; // Prevent duplicate saves

        String title = inNoteTitle.getText().toString().trim();
        String subtitle = inNoteSubtitle.getText().toString().trim();
        String noteText = inNote.getText().toString().trim();
        String currentDateTime = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date());
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        NoteDatabase appDatabase = NoteDatabase.getInstance(this);
        NoteDao noteDao = appDatabase.noteDao();

        if (selectNoteColor == null || selectNoteColor.isEmpty()) {
            selectNoteColor = "#333333";
        }

        Notes newNote = new Notes(null, title, subtitle, noteText, currentDateTime, selectNoteColor, localImagePath, userId, false);

        db.collection("notes")
                .add(newNote)
                .addOnSuccessListener(documentReference -> {
                    String noteId = documentReference.getId();
                    newNote.setId(noteId);

                    db.collection("notes").document(noteId).set(newNote)
                            .addOnSuccessListener(aVoid -> {
                                syncNoteWithRoomDatabase(noteDao, newNote);
                                isNoteSaved = true; // Mark note as saved

                                if (selectedImageUri != null) {
                                    uploadImageToFirebaseStorage(noteId);
                                    saveImageLocally(selectedImageUri);
                                }

                                Toast.makeText(CreatingNoteActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(CreatingNoteActivity.this, "Error adding note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreatingNoteActivity.this, "Error adding note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void syncNoteWithRoomDatabase(NoteDao noteDao, Notes note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            NoteEntity noteEntity = new NoteEntity(note.getId(), note.getTitle(), note.getSubtitle(), note.getNote(),
                    note.getDate(), note.getColor(), note.getImageUrl(), note.getUserId(), note.isDeleted());
            noteDao.insertOrUpdate(noteEntity);
        });
    }



    private void updateNoteInFirestore(FirebaseFirestore db, String noteId) {
        if (isNoteSaved) return; // Prevent duplicate updates

        String title = inNoteTitle.getText().toString().trim();
        String subtitle = inNoteSubtitle.getText().toString().trim();
        String noteText = inNote.getText().toString().trim();
        String currentDateTime = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date());
        String userId = existingNote.getUserId();

        if (selectNoteColor == null || selectNoteColor.isEmpty()) {
            selectNoteColor = "#333333";
        }

        Notes updatedNote = new Notes(noteId, title, subtitle, noteText, currentDateTime, selectNoteColor, existingNote.getImageUrl(), userId, existingNote.isDeleted());

        db.collection("notes").document(noteId)
                .set(updatedNote)
                .addOnSuccessListener(aVoid -> {
                    isNoteSaved = true; // Mark note as saved
                    Toast.makeText(CreatingNoteActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                    if (selectedImageUri != null) {
                        uploadImageToFirebaseStorage(noteId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UpdateError", "Error updating note", e);
                    Toast.makeText(CreatingNoteActivity.this, "Error updating note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void uploadImageToFirebaseStorage(String noteId) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("note_images/" + noteId + ".jpg");

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        updateNoteWithImageUrl(noteId, imageUrl);
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(CreatingNoteActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateNoteWithImageUrl(String noteId, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("notes").document(noteId)
                .update("imageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreatingNoteActivity.this, "Note updated with image", Toast.LENGTH_SHORT).show();
                    finishWithResult();
                })
                .addOnFailureListener(e -> Toast.makeText(CreatingNoteActivity.this, "Failed to update note with image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void initMiscellaneous() {
        final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);

        layoutMiscellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        setColorPickerListeners(layoutMiscellaneous);

        layoutMiscellaneous.findViewById(R.id.LayoutAddImage).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CreatingNoteActivity.this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                selectImage();
            }
        });
    }

    private void setColorPickerListeners(LinearLayout layoutMiscellaneous) {
        final ImageView imageColor1 = layoutMiscellaneous.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = layoutMiscellaneous.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = layoutMiscellaneous.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = layoutMiscellaneous.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = layoutMiscellaneous.findViewById(R.id.imageColor5);
        final ImageView imageColor6 = layoutMiscellaneous.findViewById(R.id.imageColor6);
        final ImageView imageColor7 = layoutMiscellaneous.findViewById(R.id.imageColor7);
        final ImageView imageColor8 = layoutMiscellaneous.findViewById(R.id.imageColor8);

        layoutMiscellaneous.findViewById(R.id.viewColor1).setOnClickListener(view -> {
            selectNoteColor = "#333333";
            updateColorSelection(imageColor1, imageColor2, imageColor3, imageColor4, imageColor5, imageColor6, imageColor7, imageColor8);
        });

        layoutMiscellaneous.findViewById(R.id.viewColor2).setOnClickListener(view -> {
            selectNoteColor = "#FF4842";
            updateColorSelection(imageColor2, imageColor1, imageColor3, imageColor4, imageColor5, imageColor6, imageColor7, imageColor8);
        });

        layoutMiscellaneous.findViewById(R.id.viewColor3).setOnClickListener(view -> {
            selectNoteColor = "#f4d03f";
            updateColorSelection(imageColor3, imageColor1, imageColor2, imageColor4, imageColor5, imageColor6, imageColor7, imageColor8);
        });

        layoutMiscellaneous.findViewById(R.id.viewColor4).setOnClickListener(view -> {
            selectNoteColor = "#2196F3";
            updateColorSelection(imageColor4, imageColor1, imageColor2, imageColor3, imageColor5, imageColor6, imageColor7, imageColor8);
        });

        layoutMiscellaneous.findViewById(R.id.viewColor5).setOnClickListener(view -> {
            selectNoteColor = "#00008B";
            updateColorSelection(imageColor5, imageColor1, imageColor2, imageColor3, imageColor4, imageColor6, imageColor7, imageColor8);
        });
        layoutMiscellaneous.findViewById(R.id.viewColor6).setOnClickListener(view -> {
            selectNoteColor = "#DE3163";
            updateColorSelection(imageColor6, imageColor1, imageColor2, imageColor3, imageColor4, imageColor6, imageColor7, imageColor5);
        });
        layoutMiscellaneous.findViewById(R.id.viewColor7).setOnClickListener(view -> {
            selectNoteColor = "#CCCCFF";
            updateColorSelection(imageColor7, imageColor1, imageColor2, imageColor3, imageColor4, imageColor6, imageColor7, imageColor5);
        });
        layoutMiscellaneous.findViewById(R.id.viewColor8).setOnClickListener(view -> {
            selectNoteColor = "#8e44ad";
            updateColorSelection(imageColor8, imageColor1, imageColor2, imageColor3, imageColor4, imageColor6, imageColor7, imageColor5);
        });
    }


    private void updateColorSelection(ImageView selectedColor, ImageView... otherColors) {
        // Clear all tick marks
        for (ImageView color : otherColors) {
            color.setImageResource(0);
        }
        // Set tick mark only on the selected color
        selectedColor.setImageResource(R.drawable.ic_done);
        setSubtitleIndicatorColor();
    }


    private void setSubtitleIndicatorColor() {
        if (selectNoteColor == null || selectNoteColor.isEmpty()) {
            selectNoteColor = "#333333"; // Default color if none is selected
        }
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectNoteColor));
    }

    private void setColorTickBasedOnExistingColor(String color) {
        // Clear all tick marks
        ImageView[] colorImages = {
                findViewById(R.id.imageColor1),
                findViewById(R.id.imageColor2),
                findViewById(R.id.imageColor3),
                findViewById(R.id.imageColor4),
                findViewById(R.id.imageColor5),
                findViewById(R.id.imageColor6),
                findViewById(R.id.imageColor7),
                findViewById(R.id.imageColor8)
        };

        for (ImageView colorImage : colorImages) {
            colorImage.setImageResource(0); // Clear the tick mark
        }
        if (color == null || color.isEmpty()) {
            color = "#333333"; // Default color if none is provided
        }


        // Set tick mark based on the existing color
        switch (color) {
            case "#FF4842":
                ((ImageView) findViewById(R.id.imageColor2)).setImageResource(R.drawable.ic_done);
                break;
            case "#f4d03f":
                ((ImageView) findViewById(R.id.imageColor3)).setImageResource(R.drawable.ic_done);
                break;
            case "#2196F3":
                ((ImageView) findViewById(R.id.imageColor4)).setImageResource(R.drawable.ic_done);
                break;
            case "#00008B":
                ((ImageView) findViewById(R.id.imageColor5)).setImageResource(R.drawable.ic_done);
                break;
            case "#DE3163":
                ((ImageView) findViewById(R.id.imageColor6)).setImageResource(R.drawable.ic_done);
                break;
            case "#CCCCFF":
                ((ImageView) findViewById(R.id.imageColor7)).setImageResource(R.drawable.ic_done);
                break;
            case "#8e44ad":
                ((ImageView) findViewById(R.id.imageColor8)).setImageResource(R.drawable.ic_done);
                break;
            default:
                ((ImageView) findViewById(R.id.imageColor1)).setImageResource(R.drawable.ic_done);
                break;
        }
    }


    private void deleteNote() {
        if (existingNote != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("notes").document(existingNote.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreatingNoteActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                        finishWithResult();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DeleteError", "Error deleting note", e);
                        Toast.makeText(CreatingNoteActivity.this, "Error deleting note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void selectImage() {
        Toast.makeText(CreatingNoteActivity.this, "Opening Image Picker", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectImage();
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    displaySelectedImage(selectedImageUri);
                }
            }
        }
    }

    private void displaySelectedImage(Uri imageUri) {
        Glide.with(this)
                .load(imageUri.toString()) // Use the string representation of the URL
                .into(imagenote); // imagenote is your ImageView
        imagenote.setVisibility(View.VISIBLE);
        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
    }

    private void finishWithResult() {
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void saveImageLocally(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            File file = new File(getFilesDir(), "note_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            // Save file path or URI in your app's local storage/database if needed
            String localImagePath = file.getAbsolutePath();
            // Store this path in your database or note object

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving image locally: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


}
