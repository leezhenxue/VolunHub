package com.example.volunhub.student.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.volunhub.databinding.FragmentStudentServiceDetailBinding;
import com.example.volunhub.models.Service;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StudentServiceDetailFragment extends Fragment {

    private static final String TAG = "ServiceDetailFragment";
    private FragmentStudentServiceDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String serviceId;
    private Service currentService; // To store the loaded service

    public StudentServiceDetailFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            serviceId = getArguments().getString("serviceId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentServiceDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadServiceDetails();

        binding.buttonApplyService.setOnClickListener(v -> applyToService());
    }

    private void loadServiceDetails() {
        if (serviceId == null) return;

        db.collection("services").document(serviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Store the service object
                        currentService = documentSnapshot.toObject(Service.class);
                        if (currentService != null) {
                            // Populate the UI
                            binding.textDetailTitle.setText(currentService.getTitle());
                            binding.textDetailOrgName.setText(currentService.getOrgName());
                        }
                    } else {
                        Toast.makeText(getContext(), "Service not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyToService() {
        String studentId = mAuth.getCurrentUser().getUid();
        if (currentService == null || studentId == null) {
            Toast.makeText(getContext(), "Error: Cannot apply", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- THIS IS THE KEY ---
        // 1. Create the new application map
        Map<String, Object> newApplication = new HashMap<>();
        newApplication.put("studentId", studentId);
        newApplication.put("serviceId", serviceId);
        newApplication.put("orgId", currentService.getOrgId());
        newApplication.put("orgName", currentService.getOrgName());
        newApplication.put("serviceTitle", currentService.getTitle());
        newApplication.put("status", "Pending");
        newApplication.put("appliedAt", Timestamp.now());

        // 2. ADD THE SERVICEDATE
        newApplication.put("serviceDate", currentService.getServiceDate());

        // 3. Save the new application to Firestore
        db.collection("applications")
                .add(newApplication) // "add()" creates an auto-ID
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Application submitted!", Toast.LENGTH_SHORT).show();

                    // Go back to the home screen
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error submitting application", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error adding document", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}