package com.example.volunhub.org.fragments.service;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
// --- 1. Add these imports ---
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.AggregateSource;

import com.example.volunhub.databinding.FragmentOrgManageServiceBinding;
import com.example.volunhub.models.Service;
import com.example.volunhub.org.OrgManageViewPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrgManageServiceFragment extends Fragment {

    private static final String TAG = "OrgManageService";
    private FirebaseFirestore db;
    private FragmentOrgManageServiceBinding binding;
    private String serviceId;

    public OrgManageServiceFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Uses Safe Args to get the ID
            serviceId = OrgManageServiceFragmentArgs.fromBundle(getArguments()).getServiceId();
            Log.d(TAG, "Received serviceId: " + serviceId);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgManageServiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // 1. Load Service Details (Title, Desc, Stats)
        loadServiceDetails();

        // 2. Setup Adapter
        OrgManageViewPagerAdapter viewPagerAdapter = new OrgManageViewPagerAdapter(requireActivity(), serviceId);
        binding.viewPagerOrg.setAdapter(viewPagerAdapter);

        // 3. Link Tabs (Set initial titles)
        new TabLayoutMediator(binding.tabLayoutOrg, binding.viewPagerOrg,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText("Pending"); break;
                        case 1: tab.setText("Accepted"); break;
                        case 2: tab.setText("Rejected"); break;
                    }
                }
        ).attach();

        // 4. --- NEW: Fetch and update the counts on the tabs ---
        updateTabCounts();
    }

    private void loadServiceDetails() {
        if (serviceId == null) {
            Log.e(TAG, "Service ID is null, cannot load details.");
            return;
        }

        db.collection("services").document(serviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Service service = documentSnapshot.toObject(Service.class);
                        if (service != null) {
                            binding.textManageTitle.setText(service.getTitle());
                            binding.textManageDescription.setText(service.getDescription());

                            String reqText = "Requirements: " + service.getRequirements();
                            binding.textManageRequirements.setText(reqText);

                            String stats = "Applicants: " + service.getVolunteersApplied() + " / " + service.getVolunteersNeeded();
                            binding.textManageStats.setText(stats);
                        }
                    } else {
                        Log.w(TAG, "Service document not found.");
                        binding.textManageTitle.setText("Error: Service not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading service details", e);
                    binding.textManageTitle.setText("Error loading data");
                });
    }

    // --- 5. NEW METHOD: Counts applicants for each status ---
    private void updateTabCounts() {
        if (serviceId == null) return;

        // Count Pending
        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Pending")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    updateTabTitle(0, "Pending", snapshot.getCount());
                });

        // Count Accepted
        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Accepted")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    updateTabTitle(1, "Accepted", snapshot.getCount());
                });

        // Count Rejected
        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Rejected")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    updateTabTitle(2, "Rejected", snapshot.getCount());
                });
    }

    // --- 6. Helper to update the Tab Text ---
    private void updateTabTitle(int position, String title, long count) {
        TabLayout.Tab tab = binding.tabLayoutOrg.getTabAt(position);
        if (tab != null) {
            // Example result: "Pending (3)"
            if (count > 0) {
                tab.setText(title + " (" + count + ")");
            } else {
                tab.setText(title);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}