package com.example.volunhub.org.fragments.service;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.volunhub.databinding.FragmentOrgManageServiceBinding;
import com.example.volunhub.models.Service;
import com.example.volunhub.org.OrgManageViewPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrgManageServiceFragment extends Fragment {

    private static final String TAG = "OrgManageService";
    private FirebaseFirestore db;
    private FragmentOrgManageServiceBinding binding;
    private OrgManageViewPagerAdapter viewPagerAdapter;
    private String serviceId; // To hold the serviceId

    public OrgManageServiceFragment() {} // constructor

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. --- Get the serviceId from the Navigation Arguments ---
        if (getArguments() != null) {
            // "serviceId" must match the name in your org_nav_graph.xml
            serviceId = getArguments().getString("serviceId");
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

        // TODO: Use the 'serviceId' to load the service details from Firestore
        // and display them in the TextViews at the top of this fragment's layout.
        // e.g., db.collection("services").document(serviceId).get()...
        loadServiceDetails();

        // 2. --- Pass the serviceId to the ViewPager adapter ---
        viewPagerAdapter = new OrgManageViewPagerAdapter(requireActivity(), serviceId);

        binding.viewPagerOrg.setAdapter(viewPagerAdapter);

        // 3. Link the TabLayout to the ViewPager
        new TabLayoutMediator(binding.tabLayoutOrg, binding.viewPagerOrg,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Pending");
                            break;
                        case 1:
                            tab.setText("Accepted");
                            break;
                        case 2:
                            tab.setText("Rejected");
                            break;
                    }
                }
        ).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
                            // Populate the TextViews you just added
                            binding.textManageTitle.setText(service.getTitle());
                            binding.textManageDescription.setText(service.getDescription());
                            // TODO: Add an "Edit" button logic here
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
}