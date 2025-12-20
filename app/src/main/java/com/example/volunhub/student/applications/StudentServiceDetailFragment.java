package com.example.volunhub.student.applications;

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

import com.bumptech.glide.Glide;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentStudentServiceDetailBinding;
import com.example.volunhub.models.Service;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Fragment that displays the complete details of a volunteer service.
 * Allows students to apply for services, cancel applications, and save services to their list.
 */
public class StudentServiceDetailFragment extends Fragment {

    private static final String TAG = "StudentServiceDetailFragment";
    private FragmentStudentServiceDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String serviceId;
    private Service currentService;
    private boolean isSaved = false;
    private String currentApplicationStatus = "NOT_APPLIED";
    private String currentApplicationDocId = null;

    public StudentServiceDetailFragment() {}

    /**
     * Initializes fragment arguments to retrieve the service ID.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            serviceId = getArguments().getString("serviceId");
        }
    }

    /**
     * Inflates the layout using ViewBinding.
     * @param inflater LayoutInflater object.
     * @param container Parent view container.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentServiceDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Sets up Firestore and triggers initial data loading and UI configurations.
     * @param view The created View.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadServiceDetails();
        checkIfSaved();

        binding.buttonApplyService.setOnClickListener(v -> applyToService());
        binding.buttonSaveService.setOnClickListener(v -> toggleSaveStatus());

        binding.textDetailOrgName.setOnClickListener(v -> {
            if (currentService != null) {
                StudentServiceDetailFragmentDirections.ActionServiceDetailToViewOrgProfile action =
                        StudentServiceDetailFragmentDirections.actionServiceDetailToViewOrgProfile(currentService.getOrgId());
                Navigation.findNavController(v).navigate(action);
            }
        });
    }

    /**
     * Checks the user's Firestore document to see if this service is in their saved list.
     */
    private void checkIfSaved() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(myId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null || !documentSnapshot.exists()) return;

                    List<String> savedIds = (List<String>) documentSnapshot.get("savedServices");
                    isSaved = savedIds != null && savedIds.contains(serviceId);
                    updateSaveButtonUI();
                });
    }

    /**
     * Adds or removes the service from the user's saved list in Firestore.
     */
    private void toggleSaveStatus() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        binding.buttonSaveService.setEnabled(false);

        if (isSaved) {
            db.collection("users").document(myId)
                    .update("savedServices", FieldValue.arrayRemove(serviceId))
                    .addOnSuccessListener(aVoid -> {
                        if (binding == null) return;
                        isSaved = false;
                        updateSaveButtonUI();
                        Toast.makeText(getContext(), "Removed from Saved List", Toast.LENGTH_SHORT).show();
                    })
                    .addOnCompleteListener(task -> {
                        if (binding != null) binding.buttonSaveService.setEnabled(true);
                    });
        } else {
            db.collection("users").document(myId)
                    .update("savedServices", FieldValue.arrayUnion(serviceId))
                    .addOnSuccessListener(aVoid -> {
                        if (binding == null) return;
                        isSaved = true;
                        updateSaveButtonUI();
                        Toast.makeText(getContext(), "Added to Saved List", Toast.LENGTH_SHORT).show();
                    })
                    .addOnCompleteListener(task -> {
                        if (binding != null) binding.buttonSaveService.setEnabled(true);
                    });
        }
    }

    /**
     * Updates the text and icon of the Save button based on the current save state.
     */
    private void updateSaveButtonUI() {
        if (isSaved) {
            binding.buttonSaveService.setText("Saved");
            binding.buttonSaveService.setIconResource(R.drawable.ic_bookmark_added);
        } else {
            binding.buttonSaveService.setText("Save for Later");
            binding.buttonSaveService.setIconResource(R.drawable.ic_bookmark_add);
        }
    }

    /**
     * Fetches details of the specific service from Firestore and populates the UI.
     */
    private void loadServiceDetails() {
        if (serviceId == null) return;

        db.collection("services").document(serviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        currentService = documentSnapshot.toObject(Service.class);
                        if (currentService != null) {
                            binding.textDetailTitle.setText(currentService.getTitle());
                            binding.textDetailOrgName.setText(currentService.getOrgName());

                            if (currentService.getServiceDate() != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                                binding.textDetailDate.setText(sdf.format(currentService.getServiceDate()));
                            } else {
                                binding.textDetailDate.setText("Date TBD");
                            }

                            String contact = currentService.getContactNumber();
                            binding.textDetailContactNumber.setText((contact != null && !contact.isEmpty()) ? contact : "No contact info");

                            binding.textDetailVolunteers.setText(currentService.getVolunteersApplied() + " / " + currentService.getVolunteersNeeded());
                            binding.textDetailDescription.setText(currentService.getDescription());
                            binding.textDetailRequirements.setText(currentService.getRequirements());

                            loadOrgLogo(currentService.getOrgId());
                            checkAndSetButtonState();
                        }
                    } else {
                        showServiceUnavailableDialog();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading details", Toast.LENGTH_SHORT).show());
    }

    /**
     * Checks if the student has an existing application and updates the dialog logic if the service is deleted.
     */
    private void showServiceUnavailableDialog() {
        if (mAuth.getCurrentUser() == null || serviceId == null) {
            showSimpleUnavailableDialog();
            return;
        }

        db.collection("applications")
                .whereEqualTo("studentId", mAuth.getCurrentUser().getUid())
                .whereEqualTo("serviceId", serviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (binding == null) return;
                    if (!querySnapshot.isEmpty()) {
                        showUnavailableDialogWithRemoveOption(querySnapshot.getDocuments().get(0).getId());
                    } else {
                        showSimpleUnavailableDialog();
                    }
                });
    }

    /**
     * Shows a dialog without extra options and navigates the user back.
     */
    private void showSimpleUnavailableDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.service_unavailable_title)
                .setMessage(R.string.service_unavailable_message)
                .setPositiveButton(android.R.string.ok, (d, w) -> Navigation.findNavController(requireView()).navigateUp())
                .setCancelable(false)
                .show();
    }

    /**
     * Shows a dialog allowing the student to remove an application for a deleted service.
     * @param applicationDocId The Firestore document ID of the application.
     */
    private void showUnavailableDialogWithRemoveOption(String applicationDocId) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.service_unavailable_title)
                .setMessage(R.string.service_unavailable_message)
                .setPositiveButton(R.string.remove_application, (d, w) -> removeApplication(applicationDocId))
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red)));
        dialog.show();
    }

    /**
     * Removes the application document from Firestore.
     * @param applicationDocId The document ID to delete.
     */
    private void removeApplication(String applicationDocId) {
        db.collection("applications").document(applicationDocId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (binding == null) return;
                    Toast.makeText(getContext(), R.string.application_removed_success, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                });
    }

    /**
     * Queries Firestore for application status to determine the Apply button's state.
     */
    private void checkAndSetButtonState() {
        if (mAuth.getCurrentUser() == null || serviceId == null) return;

        db.collection("applications")
                .whereEqualTo("studentId", mAuth.getCurrentUser().getUid())
                .whereEqualTo("serviceId", serviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (binding == null) return;
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        currentApplicationStatus = doc.getString("status");
                        currentApplicationDocId = doc.getId();
                    } else {
                        currentApplicationStatus = "NOT_APPLIED";
                        currentApplicationDocId = null;
                    }
                    updateApplyButtonUI(currentApplicationStatus);
                });
    }

    /**
     * Updates the Apply button text and color based on current application status.
     * @param status The status string (Pending, Accepted, Rejected, NOT_APPLIED).
     */
    private void updateApplyButtonUI(String status) {
        if (binding == null) return;

        binding.progressBarButtonLoading.setVisibility(View.GONE);
        binding.buttonApplyService.setVisibility(View.VISIBLE);
        binding.buttonApplyService.setEnabled(true);

        int purple = ContextCompat.getColor(requireContext(), android.R.color.holo_purple);
        int red = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light);
        int gray = ContextCompat.getColor(requireContext(), android.R.color.darker_gray);

        binding.buttonApplyService.setBackgroundColor(purple);

        switch (status) {
            case "Pending":
                binding.buttonApplyService.setText("Cancel Application");
                binding.buttonApplyService.setBackgroundColor(red);
                break;
            case "Accepted":
                binding.buttonApplyService.setText("Application Accepted");
                binding.buttonApplyService.setBackgroundColor(gray);
                binding.buttonApplyService.setEnabled(false);
                break;
            case "Rejected":
                binding.buttonApplyService.setText("Application Rejected");
                binding.buttonApplyService.setBackgroundColor(gray);
                binding.buttonApplyService.setEnabled(false);
                break;
            default:
                binding.buttonApplyService.setText("Apply Now");
                break;
        }
    }

    /**
     * Loads the organization's logo from their user profile document.
     * @param orgId The organization's UID.
     */
    private void loadOrgLogo(String orgId) {
        if (orgId == null) return;

        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null || !documentSnapshot.exists()) return;

                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    if (getContext() != null) {
                        Glide.with(getContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.default_profile_picture)
                                .error(R.drawable.default_profile_picture)
                                .circleCrop()
                                .into(binding.imageViewStudentServiceDetailCompanyLogo);
                    }
                });
    }

    /**
     * Primary action handler for the Apply button (Create or Cancel application).
     */
    private void applyToService() {
        if ("Pending".equals(currentApplicationStatus)) {
            cancelApplication();
        } else if ("NOT_APPLIED".equals(currentApplicationStatus)) {
            createApplication();
        }
    }

    /**
     * Creates a new application document in Firestore and logs performance for NFR testing.
     */
    private void createApplication() {
        String studentId = mAuth.getCurrentUser().getUid();
        long startTime = System.currentTimeMillis();
        Log.d("NFRTest", "P2 - Action Started: Apply clicked at " + startTime);

        Map<String, Object> newApp = new HashMap<>();
        newApp.put("studentId", studentId);
        newApp.put("serviceId", serviceId);
        newApp.put("orgId", currentService.getOrgId());
        newApp.put("orgName", currentService.getOrgName());
        newApp.put("serviceTitle", currentService.getTitle());
        newApp.put("status", "Pending");
        newApp.put("appliedAt", Timestamp.now());
        newApp.put("serviceDate", currentService.getServiceDate());

        db.collection("applications").add(newApp)
                .addOnSuccessListener(ref -> {
                    if (binding == null) return;
                    long duration = System.currentTimeMillis() - startTime;
                    Log.d("NFRTest", "P2 - Feedback shown. Duration: " + duration + "ms");

                    Toast.makeText(getContext(), "Application submitted!", Toast.LENGTH_SHORT).show();
                    checkAndSetButtonState();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error submitting", Toast.LENGTH_SHORT).show());
    }

    /**
     * Deletes the student's pending application from Firestore.
     */
    private void cancelApplication() {
        if (currentApplicationDocId == null) return;

        db.collection("applications").document(currentApplicationDocId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (binding == null) return;
                    Toast.makeText(getContext(), "Application Cancelled", Toast.LENGTH_SHORT).show();
                    checkAndSetButtonState();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to cancel", Toast.LENGTH_SHORT).show());
    }

    /**
     * Cleans up the ViewBinding reference when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}