package com.example.volunhub.org.service;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.databinding.FragmentOrgPendingApplicantsBinding;
import com.example.volunhub.models.Applicant;
import com.example.volunhub.org.adapters.ApplicantAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of pending applicants and allows managing them (Accept/Reject).
 */
public class OrgPendingApplicantsFragment extends Fragment {

    private static final String TAG = "OrgPendingFragment";
    private FragmentOrgPendingApplicantsBinding binding;
    private FirebaseFirestore db;
    private ApplicantAdapter adapter;
    private final List<Applicant> applicantList = new ArrayList<>();
    private String serviceId;

    public OrgPendingApplicantsFragment() {}

    /**
     * Creates a new instance of this fragment.
     * @param serviceId The ID of the service.
     * @return New instance of OrgPendingApplicantsFragment.
     */
    public static OrgPendingApplicantsFragment newInstance(String serviceId) {
        OrgPendingApplicantsFragment fragment = new OrgPendingApplicantsFragment();
        Bundle args = new Bundle();
        args.putString("SERVICE_ID", serviceId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Retrieves arguments.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            serviceId = getArguments().getString("SERVICE_ID");
        }
    }

    /**
     * Inflates the layout.
     * @param inflater LayoutInflater object.
     * @param container Parent view.
     * @param savedInstanceState Saved state bundle.
     * @return The View.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgPendingApplicantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes view and loads data.
     * @param view The created view.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        setupAcceptAllButton();
        loadPendingApplicants();
    }

    /**
     * Configures the "Accept All" button listener.
     */
    private void setupAcceptAllButton() {
        binding.btnAcceptAll.setOnClickListener(v -> acceptAllEligible());
    }

    /**
     * Configures the RecyclerView and adapter click listeners.
     */
    private void setupRecyclerView() {
        ApplicantAdapter.ApplicantClickListener listener = new ApplicantAdapter.ApplicantClickListener() {
            @Override
            public void onAcceptClick(Applicant applicant) {
                updateApplicationStatus(applicant, "Accepted");
            }
            @Override
            public void onRejectClick(Applicant applicant) {
                updateApplicationStatus(applicant, "Rejected");
            }
        };

        adapter = new ApplicantAdapter(getContext(), applicantList, "Pending", listener);
        binding.recyclerPendingApplicants.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerPendingApplicants.setAdapter(adapter);
    }

    /**
     * Updates the status of an application in Firestore (Accepted/Rejected).
     * @param applicant The applicant to update.
     * @param newStatus The new status string.
     */
    private void updateApplicationStatus(Applicant applicant, String newStatus) {
        if (serviceId == null || applicant.getApplicationId() == null) {
            Toast.makeText(getContext(), "Error: Missing ID", Toast.LENGTH_SHORT).show();
            return;
        }

        final DocumentReference serviceRef = db.collection("services").document(serviceId);
        final DocumentReference appRef = db.collection("applications").document(applicant.getApplicationId());

        db.runTransaction(transaction -> {
            DocumentSnapshot appSnapshot = transaction.get(appRef);
            if (!appSnapshot.exists()) throw new IllegalStateException("Application not found");

            DocumentSnapshot serviceSnapshot = transaction.get(serviceRef);
            if (!serviceSnapshot.exists()) throw new IllegalStateException("Service not found");

            transaction.update(appRef, "status", newStatus);

            if (newStatus.equals("Accepted")) {
                long applied = serviceSnapshot.getLong("volunteersApplied") != null ? serviceSnapshot.getLong("volunteersApplied") : 0;
                long needed = serviceSnapshot.getLong("volunteersNeeded") != null ? serviceSnapshot.getLong("volunteersNeeded") : 0;
                long newCount = applied + 1;

                transaction.update(serviceRef, "volunteersApplied", newCount);

                if (newCount >= needed) {
                    transaction.update(serviceRef, "status", "Closed");
                }
            }
            return null;
        }).addOnSuccessListener(result -> {
            Toast.makeText(getContext(), "Application " + newStatus.toLowerCase() + " successfully", Toast.LENGTH_SHORT).show();

            int position = applicantList.indexOf(applicant);
            if (position != -1) {
                applicantList.remove(position);
                adapter.notifyItemRemoved(position);
                if (applicantList.isEmpty()) binding.textEmptyPending.setVisibility(View.VISIBLE);
                notifyParentToUpdateCounts();
            } else {
                loadPendingApplicants();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to update status", e);
            Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Loads the list of pending applicants from Firestore.
     */
    private void loadPendingApplicants() {
        if (serviceId == null) return;

        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(applicationSnapshots -> {
                    if(binding == null) return;
                    if (applicationSnapshots.isEmpty()) {
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
                .addOnFailureListener(e -> Log.e(TAG, "Error loading pending applicants", e));
    }

    /**
     * Batch accepts all eligible applicants until the service is full.
     */
    private void acceptAllEligible() {
        if (serviceId == null || applicantList.isEmpty()) {
            Toast.makeText(getContext(), "No pending applicants", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference serviceRef = db.collection("services").document(serviceId);
        serviceRef.get().addOnSuccessListener(serviceSnapshot -> {
            if (binding == null || !serviceSnapshot.exists()) return;

            Long needed = serviceSnapshot.getLong("volunteersNeeded");
            Long applied = serviceSnapshot.getLong("volunteersApplied");
            if (needed == null) needed = 0L;
            if (applied == null) applied = 0L;

            long slotsRemaining = needed - applied;
            if (slotsRemaining <= 0) {
                Toast.makeText(getContext(), "Service is already full!", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Applicant> applicantsToAccept = new ArrayList<>();
            int countToAccept = Math.min(applicantList.size(), (int) slotsRemaining);

            for (int i = 0; i < countToAccept; i++) {
                applicantsToAccept.add(applicantList.get(i));
            }

            WriteBatch batch = db.batch();
            for (Applicant applicant : applicantsToAccept) {
                DocumentReference appRef = db.collection("applications").document(applicant.getApplicationId());
                batch.update(appRef, "status", "Accepted");
            }

            long newApplied = applied + applicantsToAccept.size();
            batch.update(serviceRef, "volunteersApplied", newApplied);

            if (newApplied >= needed) {
                batch.update(serviceRef, "status", "Closed");
            }

            batch.commit().addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Auto-accepted " + applicantsToAccept.size() + " applicants", Toast.LENGTH_SHORT).show();
                loadPendingApplicants();
                notifyParentToUpdateCounts();
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    /**
     * Sends a signal to the parent fragment to refresh the tab counts.
     */
    private void notifyParentToUpdateCounts() {
        Bundle result = new Bundle();
        result.putBoolean("refresh", true);
        getParentFragmentManager().setFragmentResult("KEY_REFRESH_COUNTS", result);
    }

    /**
     * Cleans up the binding when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}