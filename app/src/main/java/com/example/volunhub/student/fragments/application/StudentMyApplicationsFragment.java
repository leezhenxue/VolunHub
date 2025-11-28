package com.example.volunhub.student.fragments.application;

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

import com.example.volunhub.databinding.FragmentStudentMyApplicationsBinding;
import com.example.volunhub.models.Application;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StudentMyApplicationsFragment extends Fragment {

    private static final String TAG = "StudentMyAppsFragment";
    private FragmentStudentMyApplicationsBinding binding;
    private StudentApplicationAdapter adapter;
    private List<Application> applicationList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public StudentMyApplicationsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentMyApplicationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadMyApplications();
    }

    private void setupRecyclerView() {
        adapter = new StudentApplicationAdapter(getContext(), applicationList);
        binding.recyclerStudentMyApplications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerStudentMyApplications.setAdapter(adapter);
    }

    private void loadMyApplications() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // This query finds all applications that are "Pending" OR "Accepted"
        db.collection("applications")
                .whereEqualTo("studentId", myId)
                .whereIn("status", Arrays.asList("Pending", "Accepted"))
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No applications found.");
                        binding.textEmptyApplications.setVisibility(View.VISIBLE);
                    } else {
                        binding.textEmptyApplications.setVisibility(View.GONE);
                        applicationList.clear();
                        applicationList.addAll(querySnapshot.toObjects(Application.class));
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading applications", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}