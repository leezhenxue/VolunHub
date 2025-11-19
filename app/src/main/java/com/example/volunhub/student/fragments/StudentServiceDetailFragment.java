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

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentStudentServiceDetailBinding;
import com.example.volunhub.models.Service;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentServiceDetailFragment extends Fragment {

    private static final String TAG = "ServiceDetailFragment";
    private FragmentStudentServiceDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String serviceId;
    private Service currentService; // To store the loaded service
    private boolean isSaved = false;

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

        binding.textDetailOrgName.setOnClickListener(v -> {
            if (currentService != null) {
                // Use the generated Directions class
                StudentServiceDetailFragmentDirections.ActionServiceDetailToViewOrgProfile action =
                        StudentServiceDetailFragmentDirections.actionServiceDetailToViewOrgProfile(
                                currentService.getOrgId() // Pass the Org ID
                        );

                Navigation.findNavController(v).navigate(action);
            }
        });

        checkIfSaved();
        binding.buttonSaveService.setOnClickListener(v -> toggleSaveStatus());
    }

    private void checkIfSaved() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(myId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get the array from Firestore
                        List<String> savedIds = (List<String>) documentSnapshot.get("savedServices");

                        // Check if our current serviceId is in that list
                        if (savedIds != null && savedIds.contains(serviceId)) {
                            isSaved = true;
                            updateSaveButtonUI(); // Change button look
                        } else {
                            isSaved = false;
                            updateSaveButtonUI();
                        }
                    }
                });
    }

    private void toggleSaveStatus() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // Disable button to prevent double clicks while loading
        binding.buttonSaveService.setEnabled(false);

        if (isSaved) {
            // --- REMOVE FROM LIST ---
            db.collection("users").document(myId)
                    .update("savedServices", FieldValue.arrayRemove(serviceId))
                    .addOnSuccessListener(aVoid -> {
                        isSaved = false;
                        updateSaveButtonUI();
                        Toast.makeText(getContext(), "Removed from Saved List", Toast.LENGTH_SHORT).show();
                    })
                    .addOnCompleteListener(task -> binding.buttonSaveService.setEnabled(true));
        } else {
            // --- ADD TO LIST ---
            db.collection("users").document(myId)
                    .update("savedServices", FieldValue.arrayUnion(serviceId))
                    .addOnSuccessListener(aVoid -> {
                        isSaved = true;
                        updateSaveButtonUI();
                        Toast.makeText(getContext(), "Added to Saved List", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // If the field doesn't exist yet, update() might fail.
                        // You might need 'set(..., SetOptions.merge())' for the very first save,
                        // but usually update() works if the document exists.
                        Log.e(TAG, "Error saving", e);
                    })
                    .addOnCompleteListener(task -> binding.buttonSaveService.setEnabled(true));
        }
    }

    private void updateSaveButtonUI() {
        if (isSaved) {
            binding.buttonSaveService.setText("Saved");
            binding.buttonSaveService.setIconResource(R.drawable.ic_bookmark_added); // Filled icon
        } else {
            binding.buttonSaveService.setText("Save for Later");
            binding.buttonSaveService.setIconResource(R.drawable.ic_bookmark_add); // Outline icon
        }
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
                            loadOrgLogo(currentService.getOrgId());
                            binding.textDetailDescription.setText("Description\n" + currentService.getDescription());
                            binding.textDetailRequirements.setText("Requirements\n" + currentService.getRequirements());
                        }
                    } else {
                        Toast.makeText(getContext(), "Service not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadOrgLogo(String orgId) {
        if (orgId == null) return;

        // We go to the "users" collection to find the profileImageUrl
        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("profileImageUrl");

                        // 3. Use Glide to load the URL into the ImageView
                        if (getContext() != null) {
                            com.bumptech.glide.Glide.with(getContext())
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_org_dashboard) // Show this while loading
                                    .error(R.drawable.ic_org_dashboard)       // Show this if URL is null/broken
                                    .circleCrop()                             // Optional: Make it round
                                    .into(binding.imageViewStudentServiceDetailCompanyLogo);
                        }
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