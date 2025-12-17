package com.example.volunhub.student.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentStudentServiceDetailBinding;
import com.example.volunhub.models.Service;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentServiceDetailFragment extends Fragment {

    private static final String TAG = "StudentServiceDetailFragment";
    private FragmentStudentServiceDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String serviceId;
    private Service currentService; // To store the loaded service
    private boolean isSaved = false;
    private String currentApplicationStatus = "NOT_APPLIED";
    private String currentApplicationDocId = null;

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

                        if (currentService.getServiceDate() != null) {
                            // Import SimpleDateFormat and Locale
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault());
                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                            binding.textDetailDate.setText(sdf.format(currentService.getServiceDate()));
                        } else {
                            binding.textDetailDate.setText("Date TBD");
                        }
                        // Qimin: I am showing the contact number when it exists
                        String contact = currentService.getContactNumber();
                        if (contact != null && !contact.trim().isEmpty()) {
                            binding.textDetailContact.setVisibility(View.VISIBLE);
                            binding.textDetailContact.setText("Contact: " + contact);
                            Log.d("Qimin_Debug", "Contact number is: " + contact);
                        } else {
                            binding.textDetailContact.setText("No contact info");
                            binding.textDetailContact.setVisibility(View.GONE);
                            Log.d("Qimin_Debug", "Contact number missing for this service");
                        }
                        String stats = currentService.getVolunteersApplied() + " / " + currentService.getVolunteersNeeded();
                        binding.textDetailVolunteers.setText(stats);
                        loadOrgLogo(currentService.getOrgId());
                        binding.textDetailDescription.setText("Description\n" + currentService.getDescription());
                        binding.textDetailRequirements.setText("Requirements\n" + currentService.getRequirements());
                        // --- Set Contact Number ---
                        contact = currentService.getContactNumber(); // make sure Service model has this getter

                        if (contact != null && !contact.isEmpty()) {
                            binding.textDetailContactNumber.setText(contact);
                            binding.textDetailContactNumber.setVisibility(View.VISIBLE);
                        } else {
                            binding.textDetailContactNumber.setText("Not Available");
                            binding.textDetailContactNumber.setVisibility(View.VISIBLE);
                        }

                        checkAndSetButtonState();
                    }
                } else {
                    // Service document does not exist (likely deleted by organization)
                    Log.w(TAG, "Service document not found. ServiceId: " + serviceId + " - This volunteer opportunity may have been removed.");
                    showServiceUnavailableDialog();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading service details", e);
                Toast.makeText(getContext(), "Error loading service details", Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * Shows an AlertDialog when the service is unavailable (deleted by organization).
     * If the user has an application for this service, provides option to remove it.
     */
    private void showServiceUnavailableDialog() {
        // Check if user has an application for this service
        if (mAuth.getCurrentUser() == null || serviceId == null) {
            // No user or serviceId, just show simple dialog
            showSimpleUnavailableDialog();
            return;
        }

        String studentId = mAuth.getCurrentUser().getUid();
        db.collection("applications")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("serviceId", serviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // User has an application, show dialog with remove option
                        DocumentSnapshot appDoc = querySnapshot.getDocuments().get(0);
                        String applicationDocId = appDoc.getId();
                        showUnavailableDialogWithRemoveOption(applicationDocId);
                    } else {
                        // No application found, show simple dialog
                        showSimpleUnavailableDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking application", e);
                    // On error, show simple dialog
                    showSimpleUnavailableDialog();
                });
    }

    /**
     * Shows a simple dialog without remove option (when user has no application).
     */
    private void showSimpleUnavailableDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.service_unavailable_title)
                .setMessage(R.string.service_unavailable_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Shows a dialog with option to remove the application.
     */
    private void showUnavailableDialogWithRemoveOption(String applicationDocId) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.service_unavailable_title)
                .setMessage(R.string.service_unavailable_message)
                .setPositiveButton(R.string.remove_application, (d, which) -> {
                    removeApplication(applicationDocId);
                })
                .setNegativeButton(R.string.cancel, (d, which) -> {
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .create();

        dialog.setOnShowListener(d -> {
            // Set positive button text color to red
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error_red)
            );
        });

        dialog.show();
    }

    /**
     * Removes the application from Firestore and navigates back.
     */
    private void removeApplication(String applicationDocId) {
        db.collection("applications").document(applicationDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Application removed successfully: " + applicationDocId);
                    Toast.makeText(getContext(), R.string.application_removed_success, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing application", e);
                    Toast.makeText(getContext(), R.string.application_removed_error, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                });
    }

    private void checkAndSetButtonState() {
        if (mAuth.getCurrentUser() == null || serviceId == null) return;
        String studentId = mAuth.getCurrentUser().getUid();

        // Query Firestore for the user's existing application for this service
        db.collection("applications")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("serviceId", serviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Application found!
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        currentApplicationStatus = doc.getString("status");
                        currentApplicationDocId = doc.getId(); // Store the ID for cancellation

                    } else {
                        // No application found
                        currentApplicationStatus = "NOT_APPLIED";
                        currentApplicationDocId = null;
                    }

                    // Update the UI based on the status we just found
                    updateApplyButtonUI(currentApplicationStatus);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking application status", e);
                    // Default to apply if check fails
                    updateApplyButtonUI("NOT_APPLIED");
                });
    }

    private void updateApplyButtonUI(String status) {
        binding.progressBarButtonLoading.setVisibility(View.GONE);
        binding.buttonApplyService.setVisibility(View.VISIBLE);
        binding.buttonApplyService.setEnabled(true);

        // Use system colors for now
        int purple = getResources().getColor(android.R.color.holo_purple, null);
        int red = getResources().getColor(android.R.color.holo_red_light, null);
        int gray = getResources().getColor(android.R.color.darker_gray, null);

        binding.buttonApplyService.setBackgroundColor(purple); // Reset to purple

        switch (status) {
            case "Pending":
                binding.buttonApplyService.setText("Cancel Application");
                binding.buttonApplyService.setBackgroundColor(red); // Red for cancel
                break;
            case "Accepted":
                binding.buttonApplyService.setText("Application Accepted");
                binding.buttonApplyService.setBackgroundColor(gray); // Gray for disabled
                binding.buttonApplyService.setEnabled(false);
                break;
            case "Rejected":
                binding.buttonApplyService.setText("Application Rejected");
                binding.buttonApplyService.setBackgroundColor(gray); // Gray for disabled
                binding.buttonApplyService.setEnabled(false);
                break;
            case "NOT_APPLIED":
            default:
                binding.buttonApplyService.setText("Apply Now");
                break;
        }
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
        // We no longer check for null here, as checkAndSetButtonState handles it
        if (currentApplicationStatus.equals("Pending")) {
            cancelApplication(); // If Pending, the button means CANCEL
        } else if (currentApplicationStatus.equals("NOT_APPLIED")) {
            createApplication(); // If Not Applied, the button means APPLY
        }
    }

    // --- NEW METHOD: Create the Application ---
    private void createApplication() {
        String studentId = mAuth.getCurrentUser().getUid();

        Map<String, Object> newApplication = new HashMap<>();
        newApplication.put("studentId", studentId);
        newApplication.put("serviceId", serviceId);
        newApplication.put("orgId", currentService.getOrgId());
        newApplication.put("orgName", currentService.getOrgName());
        newApplication.put("serviceTitle", currentService.getTitle());
        newApplication.put("status", "Pending");
        newApplication.put("appliedAt", Timestamp.now());
        newApplication.put("serviceDate", currentService.getServiceDate());

        // Save the new application to Firestore
        db.collection("applications")
                .add(newApplication)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Application submitted!", Toast.LENGTH_SHORT).show();

                    // Refresh status and UI
                    checkAndSetButtonState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error submitting application", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error adding document", e);
                });
    }

    // --- NEW METHOD: Cancel the Application ---
    private void cancelApplication() {
        if (currentApplicationDocId == null) return;

        db.collection("applications").document(currentApplicationDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Application Cancelled", Toast.LENGTH_SHORT).show();
                    // Reset UI to "Apply" state
                    checkAndSetButtonState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to cancel application.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting document", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}