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

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgAcceptedApplicantsBinding;
import com.example.volunhub.models.Applicant;
import com.example.volunhub.org.adapters.ApplicantAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of students whose applications have been accepted for a specific service.
 */
public class OrgAcceptedApplicantsFragment extends Fragment {

    private static final String TAG = "OrgAcceptedFragment";
    private FragmentOrgAcceptedApplicantsBinding binding;
    private FirebaseFirestore db;
    private ApplicantAdapter adapter;
    private final List<Applicant> applicantList = new ArrayList<>();
    private String serviceId;

    public OrgAcceptedApplicantsFragment() {}

    /**
     * Creates a new instance of this fragment with the service ID.
     * @param serviceId The ID of the service to show applicants for.
     * @return A new instance of OrgAcceptedApplicantsFragment.
     */
    public static OrgAcceptedApplicantsFragment newInstance(String serviceId) {
        OrgAcceptedApplicantsFragment fragment = new OrgAcceptedApplicantsFragment();
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
        binding = FragmentOrgAcceptedApplicantsBinding.inflate(inflater, container, false);
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
        loadAcceptedApplicants();
    }

    /**
     * Sets up the RecyclerView adapter and click listeners.
     */
    private void setupRecyclerView() {
        ApplicantAdapter.ApplicantClickListener listener = new ApplicantAdapter.ApplicantClickListener() {
            @Override
            public void onAcceptClick(Applicant applicant) {
                // Not applicable for Accepted tab
            }
            @Override
            public void onRejectClick(Applicant applicant) {
                // Not applicable for Accepted tab
            }
            // Removed onProfileClick because it was defined in the Adapter but not the Interface in previous step
            // Re-adding it here assumes you updated the Interface in ApplicantAdapter.java
            // If the Interface in ApplicantAdapter ONLY has onAccept/onReject, this logic belongs inside the Adapter's bind() method directly.
            // Based on your previous ApplicantAdapter code, the click listener was inside the ViewHolder.
            // So we don't need to override onProfileClick here unless the interface forces it.
        };

        // Pass "Accepted" as the tabMode to hide buttons
        adapter = new ApplicantAdapter(getContext(), applicantList, "Accepted", listener);
        binding.recyclerAcceptedApplicants.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerAcceptedApplicants.setAdapter(adapter);
    }

    /**
     * Fetches the list of accepted applicants from Firestore.
     */
    private void loadAcceptedApplicants() {
        if (serviceId == null) {
            Log.e(TAG, "Service ID is null, cannot load applicants.");
            return;
        }

        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnSuccessListener(applicationSnapshots -> {
                    if (binding == null) return;
                    if (applicationSnapshots.isEmpty()) {
                        Log.d(TAG, "No accepted applicants found.");
                        binding.textEmptyAccepted.setVisibility(View.VISIBLE);
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

    /**
     * Cleans up the binding when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}