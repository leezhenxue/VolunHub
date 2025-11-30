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
        loadPendingApplicants();
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
        if (serviceId == null) return;

        final DocumentReference serviceRef = db.collection("services").document(serviceId);
        final DocumentReference appRef = db.collection("applications").document(applicant.getApplicationId());

        db.runTransaction(transaction -> {
            // 1. Read the Service document (This locks the document)
            DocumentSnapshot serviceSnapshot = transaction.get(serviceRef);

            // 2. Only check limits if we are attempting to ACCEPT
            if (newStatus.equals("Accepted")) {
                Long needed = serviceSnapshot.getLong("volunteersNeeded");
                Long applied = serviceSnapshot.getLong("volunteersApplied");

                if (needed == null || applied == null) {
                    throw new FirebaseFirestoreException("Service data corrupted", FirebaseFirestoreException.Code.ABORTED);
                }

                // 3. THE CRITICAL CHECK: Is it full?
                if (applied >= needed) {
                    // If full, STOP everything and throw an exception
                    throw new FirebaseFirestoreException("FULL", FirebaseFirestoreException.Code.ABORTED);
                }

                // 4. If not full, increment the count
                transaction.update(serviceRef, "volunteersApplied", applied + 1);
            }

            // 5. Update the application status (we do this for both Accept and Reject)
            transaction.update(appRef, "status", newStatus);

            return null; // Transaction successful
        }).addOnSuccessListener(aVoid -> {
            // --- SUCCESS ---
            Toast.makeText(getContext(), "Applicant " + newStatus, Toast.LENGTH_SHORT).show();

            // Remove from list locally
            int position = applicantList.indexOf(applicant);
            if (position != -1) {
                applicantList.remove(position);
                adapter.notifyItemRemoved(position);
                if (applicantList.isEmpty()) {
                    binding.textEmptyPending.setVisibility(View.VISIBLE);
                }
            }
        }).addOnFailureListener(e -> {
            // 1. Check if it's our custom "FULL" error
            if (e instanceof FirebaseFirestoreException) {
                if ("FULL".equals(e.getMessage())) {
                    Toast.makeText(getContext(), "The service has reached its application limit.", Toast.LENGTH_LONG).show();
                    return; // Stop here so we don't crash
                }
            }

            // 2. Log the actual error so you can see it in Logcat
            Log.e(TAG, "Transaction failed: ", e);
            Toast.makeText(getContext(), "Update failed. Check Logcat.", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}