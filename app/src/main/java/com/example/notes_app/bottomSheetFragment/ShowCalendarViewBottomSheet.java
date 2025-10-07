package com.example.notes_app.bottomSheetFragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.notes_app.Models.Task;
import com.example.notes_app.R;
import com.example.notes_app.databinding.FragmentCalendarViewBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ShowCalendarViewBottomSheet extends BottomSheetDialogFragment {

    private FragmentCalendarViewBinding binding;
    private List<Task> tasks = new ArrayList<>();
    private FirebaseFirestore firestore;

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    @SuppressLint("ResourceType")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        binding = FragmentCalendarViewBinding.inflate(getLayoutInflater());
        dialog.setContentView(binding.getRoot());
        firestore = FirebaseFirestore.getInstance();

        // Set CalendarView background color
        binding.calendarView.setHeaderColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        binding.calendarView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));  // Change background

        getSavedTasks();

        binding.back.setOnClickListener(view -> dialog.dismiss());
        return dialog;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Release reference to prevent memory leaks
    }

    private void getSavedTasks() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("tasks")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                Task taskData = document.toObject(Task.class);
                                // Exclude completed tasks from being displayed on the calendar
                                if (!taskData.isComplete()) {
                                    tasks.add(taskData);
                                }
                            }
                            binding.calendarView.setEvents(getHighlightedDays());
                        }
                    } else {
                        // Handle error
                    }
                });
    }

    public List<EventDay> getHighlightedDays() {
        List<EventDay> events = new ArrayList<>();

        for (Task task : tasks) {
            Calendar calendar = Calendar.getInstance();
            String[] items1 = task.getDate().split("-");
            String dd = items1[0];
            String month = items1[1];
            String year = items1[2];

            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dd));
            calendar.set(Calendar.MONTH, Integer.parseInt(month) - 1);
            calendar.set(Calendar.YEAR, Integer.parseInt(year));
            events.add(new EventDay(calendar, R.drawable.dot));
        }
        return events;
    }
}
