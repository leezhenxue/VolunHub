package com.example.volunhub.student.applications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.databinding.FragmentStudentHistoryBinding;
import com.example.volunhub.models.Application;
import com.example.volunhub.student.adapters.StudentApplicationAdapter;
import com.google.firebase.Timestamp; // <-- Import Timestamp
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;


public class StudentHistoryFragment extends Fragment {

    private static final String TAG = "StudentHistoryFragment";
    private FragmentStudentHistoryBinding binding;
    private StudentApplicationAdapter adapter; // Re-use the same adapter
    private List<Application> historyList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration historyListener;

    public StudentHistoryFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadHistory();
    }

    private void setupRecyclerView() {
        adapter = new StudentApplicationAdapter(getContext(), historyList);
        binding.recyclerStudentHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerStudentHistory.setAdapter(adapter);

        adapter.setOnItemClickListener(application -> {
            // 1. Use the DIRECTIONS from the HOST fragment
            // (Make sure you import this specific class)
            StudentApplicationsFragmentDirections.ActionAppsHostToServiceDetail action =
                    StudentApplicationsFragmentDirections.actionAppsHostToServiceDetail(
                            application.getServiceId()
                    );

            // 2. Find the NavController
            // Since we are inside a ViewPager, standard findNavController(view) works fine
            // because the ViewPager is part of the nav host.
            Navigation.findNavController(requireView()).navigate(action);
        });

    }

    private void loadHistory() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // If a Firestore listener already exists, remove it first
        // This prevents duplicate listeners when switching tabs.
        if (historyListener != null) {
            historyListener.remove();
            historyListener = null;
        }

        // Create a real-time Firestore listener
        historyListener = db.collection("applications")
                .whereEqualTo("studentId", myId)              // Only this student's applications
                .whereEqualTo("status", "Accepted")          // Only accepted applications
                .whereLessThan("serviceDate", Timestamp.now())// Service date is in the past → completed
                .orderBy("serviceDate", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {

                    // Handle Firestore listener errors
                    if (e != null) {
                        Log.e(TAG, "Error listening to history", e);
                        binding.textEmptyHistory.setVisibility(View.VISIBLE);
                        return;
                    }

                    // If the result is empty, show the empty message
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.d(TAG, "No history found.");
                        historyList.clear();
                        adapter.notifyDataSetChanged();
                        binding.textEmptyHistory.setVisibility(View.VISIBLE);
                    }
                    else {
                        // Data exists → update list in real-time
                        binding.textEmptyHistory.setVisibility(View.GONE);
                        historyList.clear();
                        historyList.addAll(querySnapshot.toObjects(Application.class));

                        // Sort by serviceDate DESC (most recent past service on top)
                        Collections.sort(historyList, new Comparator<Application>() {
                            @Override
                            public int compare(Application a1, Application a2) {
                                Date d1 = a1.getServiceDate(); // make sure Application has this getter
                                Date d2 = a2.getServiceDate();

                                if (d1 == null && d2 == null) return 0;
                                if (d1 == null) return 1;   // null = older → put at bottom
                                if (d2 == null) return -1;

                                // d2.compareTo(d1) = DESC (latest date first)
                                return d2.compareTo(d1);
                            }
                        });

                        // Notify adapter to refresh RecyclerView
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove the real-time listener to avoid memory leaks
        if (historyListener != null) {
            historyListener.remove();
            historyListener = null;
        }

        // Clear view binding reference
        binding = null;
    }
}