package com.example.volunhub.org.fragments.service;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.databinding.FragmentOrgAcceptedApplicantsBinding;
import com.example.volunhub.models.Applicant;
import com.example.volunhub.org.ApplicantAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OrgAcceptedApplicantsFragment extends Fragment {

    private static final String TAG = "OrgAcceptedFragment";
    private FragmentOrgAcceptedApplicantsBinding binding;
    private FirebaseFirestore db;
    private ApplicantAdapter adapter;
    final private List<Applicant> applicantList = new ArrayList<>();
    private String serviceId;

    public OrgAcceptedApplicantsFragment() {}

    // Changed to return its own instance
    public static OrgAcceptedApplicantsFragment newInstance(String serviceId) {
        OrgAcceptedApplicantsFragment fragment = new OrgAcceptedApplicantsFragment();
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
        // Changed binding class
        binding = FragmentOrgAcceptedApplicantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        loadAcceptedApplicants(); // Changed method name
    }

    private void setupRecyclerView() {
        // Define click listener
        ApplicantAdapter.ApplicantClickListener listener = new ApplicantAdapter.ApplicantClickListener() {
            @Override
            public void onAcceptClick(Applicant applicant) {
                // This tab has no "Accept" button
            }
            @Override
            public void onRejectClick(Applicant applicant) {
                // This tab has no "Reject" button
            }
            @Override
            public void onProfileClick(Applicant applicant) {
                Log.d(TAG, "Profile clicked: " + applicant.getStudentName());
                // TODO: Navigate to ViewStudentProfileFragment
            }
        };

        // Pass "Accepted" as the tabMode
        adapter = new ApplicantAdapter(getContext(), applicantList, "Accepted", listener);
        binding.recyclerAcceptedApplicants.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerAcceptedApplicants.setAdapter(adapter);
    }

    private void loadAcceptedApplicants() {
        if (serviceId == null) {
            Log.e(TAG, "Service ID is null, cannot load applicants.");
            return;
        }

        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Accepted") // Changed status
                .get()
                .addOnSuccessListener(applicationSnapshots -> {
                    if (applicationSnapshots.isEmpty()) {
                        Log.d(TAG, "No accepted applicants found.");
                        binding.textEmptyAccepted.setVisibility(View.VISIBLE); // Changed empty text
                        return;
                    }

                    binding.textEmptyAccepted.setVisibility(View.GONE);

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
                    Log.e(TAG, "Error loading accepted applicants", e)
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}