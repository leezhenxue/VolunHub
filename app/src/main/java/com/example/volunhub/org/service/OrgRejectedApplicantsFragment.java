package com.example.volunhub.org.service;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgRejectedApplicantsBinding;
import com.example.volunhub.models.Applicant;
import com.example.volunhub.org.adapters.ApplicantAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of students whose applications have been rejected for a specific service.
 */
public class OrgRejectedApplicantsFragment extends Fragment {

    private static final String TAG = "OrgRejectedFragment";
    private FragmentOrgRejectedApplicantsBinding binding;
    private FirebaseFirestore db;
    private ApplicantAdapter adapter;
    private final List<Applicant> applicantList = new ArrayList<>();
    private String serviceId;

    public OrgRejectedApplicantsFragment() {}

    /**
     * Creates a new instance of this fragment with the service ID.
     * @param serviceId The ID of the service to show applicants for.
     * @return A new instance of OrgRejectedApplicantsFragment.
     */
    public static OrgRejectedApplicantsFragment newInstance(String serviceId) {
        OrgRejectedApplicantsFragment fragment = new OrgRejectedApplicantsFragment();
        Bundle args = new Bundle();
        args.putString("SERVICE_ID", serviceId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Retrieves arguments when the fragment is created.
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
     * Inflates the layout for this fragment.
     * @param inflater LayoutInflater object.
     * @param container Parent view.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgRejectedApplicantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes the view and loads data.
     * @param view The created view.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        loadRejectedApplicants();
    }

    /**
     * Sets up the RecyclerView adapter and click listeners.
     */
    private void setupRecyclerView() {
        ApplicantAdapter.ApplicantClickListener listener = new ApplicantAdapter.ApplicantClickListener() {
            @Override
            public void onAcceptClick(Applicant applicant) {
                // Not applicable for Rejected tab
            }
            @Override
            public void onRejectClick(Applicant applicant) {
                // Not applicable for Rejected tab
            }
        };

        // Pass "Rejected" as the tabMode to hide buttons
        adapter = new ApplicantAdapter(getContext(), applicantList, "Rejected", listener);
        binding.recyclerRejectedApplicants.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerRejectedApplicants.setAdapter(adapter);
    }

    /**
     * Fetches the list of rejected applicants from Firestore.
     */
    private void loadRejectedApplicants() {
        if (serviceId == null) {
            Log.e(TAG, "Service ID is null, cannot load applicants.");
            return;
        }

        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Rejected")
                .get()
                .addOnSuccessListener(applicationSnapshots -> {
                    if (binding == null) return;
                    if (applicationSnapshots.isEmpty()) {
                        Log.d(TAG, "No rejected applicants found.");
                        binding.textEmptyRejected.setVisibility(View.VISIBLE);
                        return;
                    }

                    binding.textEmptyRejected.setVisibility(View.GONE);

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
                        Log.e(TAG, "Error loading rejected applicants", e)
                );
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