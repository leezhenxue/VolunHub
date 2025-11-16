package com.example.volunhub.student.fragments.application;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.databinding.FragmentStudentHistoryBinding;
import com.example.volunhub.models.Application;
import com.google.firebase.Timestamp; // <-- Import Timestamp
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class StudentHistoryFragment extends Fragment {

    private static final String TAG = "StudentHistoryFragment";
    private FragmentStudentHistoryBinding binding;
    private StudentApplicationAdapter adapter; // Re-use the same adapter
    private List<Application> historyList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
    }

    private void loadHistory() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // This query requires a Firestore Index.
        // Run the app, get the error link from Logcat, and create the index.
        db.collection("applications")
                .whereEqualTo("studentId", myId)
                .whereEqualTo("status", "Accepted")
                .whereLessThan("serviceDate", Timestamp.now()) // "serviceDate" is in the past
                .orderBy("serviceDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No history found.");
                        binding.textEmptyHistory.setVisibility(View.VISIBLE);
                    } else {
                        binding.textEmptyHistory.setVisibility(View.GONE);
                        historyList.clear();
                        historyList.addAll(querySnapshot.toObjects(Application.class));
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading history", e);
                    // This error is likely a "FAILED_PRECONDITION"
                    // Check your Logcat for the link to create the index!
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}