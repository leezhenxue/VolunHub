package com.example.volunhub.org.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrgDashboardFragment extends Fragment {

    private static final String TAG = "OrgDashboardFragment";
    private FragmentOrgDashboardBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public OrgDashboardFragment() {} // constructor

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 1. Fetch the stats
        loadPendingApplicantStats();

        // 2. Set click listener for the FAB
        binding.fabOrgPostService.setOnClickListener(v -> {
            // Navigate to the "Post Service" screen
            NavController navController = Navigation.findNavController(v);

            // This action must be added to your org_nav_graph.xml
            navController.navigate(R.id.action_org_dashboard_to_post_service);
        });
    }

    private void loadPendingApplicantStats() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int pendingCount = querySnapshot.size();
                    if (pendingCount == 0) {
                        binding.textOrgDashboardStatValue.setText("You have no pending applicants.");
                    } else if (pendingCount == 1) {
                        binding.textOrgDashboardStatValue.setText("You have 1 new applicant waiting.");
                    } else {
                        binding.textOrgDashboardStatValue.setText("You have " + pendingCount + " new applicants waiting.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching stats", e);
                    binding.textOrgDashboardStatValue.setText("Could not load stats.");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}