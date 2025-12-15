package com.example.volunhub.org.fragments.service;

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
import androidx.navigation.Navigation; // <-- Import this
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.databinding.FragmentOrgPendingApplicantsBinding;
import com.example.volunhub.models.Applicant;
import com.example.volunhub.org.ApplicantAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;

public class OrgPendingApplicantsFragment extends Fragment {

    private static final String TAG = "OrgPendingFragment";
    private FragmentOrgPendingApplicantsBinding binding;
    private FirebaseFirestore db;
    private ApplicantAdapter adapter;
    final private List<Applicant> applicantList = new ArrayList<>();
    private String serviceId;

    public OrgPendingApplicantsFragment() {}

    public static OrgPendingApplicantsFragment newInstance(String serviceId) {
        OrgPendingApplicantsFragment fragment = new OrgPendingApplicantsFragment();
        Bundle args = new Bundle();
        args.putString("SERVICE_ID", serviceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            serviceId = getArguments().getString("SERVICE_ID");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgPendingApplicantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        setupAcceptAllButton();
        loadPendingApplicants();
    }

    private void setupAcceptAllButton() {
        binding.btnAcceptAll.setOnClickListener(v -> acceptAllEligible());
    }

    private void setupRecyclerView() {
        ApplicantAdapter.ApplicantClickListener listener = new ApplicantAdapter.ApplicantClickListener() {
            @Override
            public void onAcceptClick(Applicant applicant) {
                Log.d(TAG, "Accept clicked: " + applicant.getStudentName());
                updateApplicationStatus(applicant, "Accepted");
            }
            @Override
            public void onRejectClick(Applicant applicant) {
                Log.d(TAG, "Reject clicked: " + applicant.getStudentName());
                updateApplicationStatus(applicant, "Rejected");
            }

            // --- 2. THIS IS THE IMPLEMENTATION ---
            @Override
            public void onProfileClick(Applicant applicant) {
                Log.d(TAG, "Profile clicked: " + applicant.getStudentName());

                // Find the NavController from the parent fragment
                NavController navController = Navigation.findNavController(requireParentFragment().requireView());

                // Create the action using the parent's generated Directions class
                OrgManageServiceFragmentDirections.ActionManageServiceToViewStudent action =
                        OrgManageServiceFragmentDirections.actionManageServiceToViewStudent(
                                applicant.getStudentId()
                        );

                // Navigate
                navController.navigate(action);
            }
        };

        adapter = new ApplicantAdapter(getContext(), applicantList, "Pending", listener);
        binding.recyclerPendingApplicants.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerPendingApplicants.setAdapter(adapter);
    }

    private void updateApplicationStatus(Applicant applicant, String newStatus) {
        // Validate inputs
        if (serviceId == null) {
            Log.e(TAG, "Service ID is null, cannot update application status.");
            Toast.makeText(getContext(), "Error: Service ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String applicationId = applicant.getApplicationId();
        if (applicationId == null || applicationId.isEmpty()) {
            Log.e(TAG, "Application ID is null or empty, cannot update application status.");
            Toast.makeText(getContext(), "Error: Application ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Updating application status. ApplicationId: " + applicationId + ", NewStatus: " + newStatus);

        // Get document references - CRITICAL: Use existing document ID, NEVER create new document
        final DocumentReference serviceRef = db.collection("services").document(serviceId);
        final DocumentReference appRef = db.collection("applications").document(applicationId);

        // Use transaction to ensure atomic update
        db.runTransaction(transaction -> {
            // 1. READ: Verify application document exists
            DocumentSnapshot appSnapshot = transaction.get(appRef);
            if (!appSnapshot.exists()) {
                throw new IllegalStateException("Application document does not exist: " + applicationId);
            }

            // Verify current status is "Pending" (optional check for safety)
            String currentStatus = appSnapshot.getString("status");
            if (currentStatus == null || !currentStatus.equals("Pending")) {
                Log.w(TAG, "Application status is not Pending. Current: " + currentStatus);
                // Continue anyway, but log warning
            }

            // 2. READ: Get current service state
            DocumentSnapshot serviceSnapshot = transaction.get(serviceRef);
            if (!serviceSnapshot.exists()) {
                throw new IllegalStateException("Service document does not exist: " + serviceId);
            }
            long applied = serviceSnapshot.getLong("volunteersApplied");
            long needed = serviceSnapshot.getLong("volunteersNeeded");

            // 3. WRITE: Update Application Status - CRITICAL: Use update(), NOT add() or set()
            transaction.update(appRef, "status", newStatus);

            // 4. WRITE: Update Service Counter & Check Full (only for Accepted)
            if (newStatus.equals("Accepted")) {
                long newCount = applied + 1;
                transaction.update(serviceRef, "volunteersApplied", newCount);

                // Auto-close the service if full
                if (newCount >= needed) {
                    transaction.update(serviceRef, "status", "Closed");
                }
            }

            return null; // Success
        }).addOnSuccessListener(result -> {
            // 5. UI Cleanup: Remove item from Pending list immediately
            Log.d(TAG, "Application status updated successfully! ApplicationId: " + applicationId);
            Toast.makeText(getContext(), "Application " + newStatus.toLowerCase() + " successfully", Toast.LENGTH_SHORT).show();
            
            // Find and remove the applicant from the list
            int position = applicantList.indexOf(applicant);
            if (position != -1) {
                applicantList.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, applicantList.size()); // Notify remaining items
                
                // Show empty state if list is now empty
                if (applicantList.isEmpty()) {
                    binding.textEmptyPending.setVisibility(View.VISIBLE);
                }
            } else {
                Log.w(TAG, "Applicant not found in list, refreshing entire list");
                // Fallback: reload the entire list if item not found
                loadPendingApplicants();
            }
        }).addOnFailureListener(e -> {
            // Error handling
            Log.e(TAG, "Failed to update application status. ApplicationId: " + applicationId, e);
            
            String errorMessage = "Failed to update application status";
            if (e.getMessage() != null) {
                errorMessage += ": " + e.getMessage();
            }
            
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    private void loadPendingApplicants() {
        if (serviceId == null) {
            Log.e(TAG, "Service ID is null, cannot load applicants.");
            return;
        }

        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(applicationSnapshots -> {
                    if (applicationSnapshots.isEmpty()) {
                        Log.d(TAG, "No pending applicants found.");
                        binding.textEmptyPending.setVisibility(View.VISIBLE);
                        return;
                    }

                    binding.textEmptyPending.setVisibility(View.GONE);
                    List<Task<com.google.firebase.firestore.DocumentSnapshot>> userTasks = new ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot appDoc : applicationSnapshots.getDocuments()) {
                        String studentId = appDoc.getString("studentId");
                        if (studentId != null) {
                            userTasks.add(db.collection("users").document(studentId).get());
                        }
                    }

                    Tasks.whenAllSuccess(userTasks).addOnSuccessListener(userSnapshots -> {
                        applicantList.clear();
                        for (int i = 0; i < userSnapshots.size(); i++) {
                            com.google.firebase.firestore.DocumentSnapshot userDoc =
                                    (com.google.firebase.firestore.DocumentSnapshot) userSnapshots.get(i);
                            com.google.firebase.firestore.DocumentSnapshot appDoc =
                                    applicationSnapshots.getDocuments().get(i);

                            if (userDoc.exists()) {
                                Applicant applicant = new Applicant();
                                applicant.setApplicationId(appDoc.getId());
                                applicant.setStudentId(userDoc.getId());
                                applicant.setStudentName(userDoc.getString("studentName"));
                                applicant.setStudentIntroduction(userDoc.getString("studentIntroduction"));
                                applicant.setProfileImageUrl(userDoc.getString("profileImageUrl"));
                                applicantList.add(applicant);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    });
                })
                .addOnFailureListener(e ->
                    Log.e(TAG, "Error loading pending applicants", e)
                );
    }

    /**
     * Accepts all eligible applicants up to the remaining capacity.
     * Uses batch update to efficiently process multiple applications.
     */
    
    private void acceptAllEligible() {
        // Validation: Check if serviceId is available
        if (serviceId == null) {
            Log.e(TAG, "Service ID is null, cannot accept applicants.");
            Toast.makeText(getContext(), "Error: Service ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        // Qimin: Check if we have enough slots before accepting everyone.
        if (applicantList.isEmpty()) {
            Toast.makeText(getContext(), "No pending applicants", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 1: Get service capacity information
        DocumentReference serviceRef = db.collection("services").document(serviceId);
        serviceRef.get()
                .addOnSuccessListener(serviceSnapshot -> {
                    if (!serviceSnapshot.exists()) {
                        Log.e(TAG, "Service document does not exist: " + serviceId);
                        Toast.makeText(getContext(), "Error: Service not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get capacity information
                    Long volunteersNeeded = serviceSnapshot.getLong("volunteersNeeded");
                    Long volunteersApplied = serviceSnapshot.getLong("volunteersApplied");

                    if (volunteersNeeded == null) volunteersNeeded = 0L;
                    if (volunteersApplied == null) volunteersApplied = 0L;

                    long slotsRemaining = volunteersNeeded - volunteersApplied;

                    // Validation: Check if service is full
                    if (slotsRemaining <= 0) {
                        Toast.makeText(getContext(), "Service is already full!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Step 2: Select applicants to accept (only up to slotsRemaining)
                    List<Applicant> applicantsToAccept = new ArrayList<>();
                    int countToAccept = Math.min(applicantList.size(), (int) slotsRemaining);
                    
                    for (int i = 0; i < countToAccept; i++) {
                        applicantsToAccept.add(applicantList.get(i));
                    }

                    if (applicantsToAccept.isEmpty()) {
                        Toast.makeText(getContext(), "No eligible applicants to accept", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d(TAG, "Accepting " + applicantsToAccept.size() + " applicants. Slots remaining: " + slotsRemaining);

                    // Step 3: Batch update using WriteBatch
                    WriteBatch batch = db.batch();

                    // Update each application status to "Accepted"
                    for (Applicant applicant : applicantsToAccept) {
                        String applicationId = applicant.getApplicationId();
                        if (applicationId != null && !applicationId.isEmpty()) {
                            DocumentReference appRef = db.collection("applications").document(applicationId);
                            batch.update(appRef, "status", "Accepted");
                        } else {
                            Log.w(TAG, "Skipping applicant with null applicationId: " + applicant.getStudentName());
                        }
                    }

                    // Update service volunteersApplied count
                    long newVolunteersApplied = volunteersApplied + applicantsToAccept.size();
                    batch.update(serviceRef, "volunteersApplied", newVolunteersApplied);

                    // Auto-close the service if full
                    if (newVolunteersApplied >= volunteersNeeded) {
                        batch.update(serviceRef, "status", "Closed");
                        Log.d(TAG, "Service is now full, closing it.");
                    }

                    // Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Batch update successful. Accepted " + applicantsToAccept.size() + " applicants.");
                                Toast.makeText(getContext(), 
                                        "Auto-accepted " + applicantsToAccept.size() + " applicants", 
                                        Toast.LENGTH_SHORT).show();
                                
                                // Step 4: Refresh the list
                                loadPendingApplicants();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Batch update failed", e);
                                Toast.makeText(getContext(), 
                                        "Error accepting applicants: " + e.getMessage(), 
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching service information", e);
                    Toast.makeText(getContext(), "Error loading service information", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}