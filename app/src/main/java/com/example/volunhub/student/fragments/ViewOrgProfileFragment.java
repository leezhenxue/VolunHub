package com.example.volunhub.student.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentViewOrgProfileBinding;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewOrgProfileFragment extends Fragment {
    private static final String TAG = "ViewOrgProfileFragment";
    private FragmentViewOrgProfileBinding binding;
    private FirebaseFirestore db;
    private String organizationId;

    public ViewOrgProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            organizationId = getArguments().getString("organizationId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentViewOrgProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (organizationId != null) {
            loadOrganizationProfile();
        } else {
            Log.e(TAG, "Organization ID is null");
            binding.textViewOrgName.setText("Profile not found");
        }
    }

    private void loadOrganizationProfile() {
        db.collection("organizations").document(organizationId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.textViewOrgName.setText(documentSnapshot.getString("orgName"));
                        binding.textViewOrgField.setText(documentSnapshot.getString("orgField"));
                        binding.textViewOrgDesc.setText(documentSnapshot.getString("orgDescription"));

                        if (getContext() != null) {
                            Glide.with(getContext())
                                    .load(documentSnapshot.getString("logoImageUrl"))
                                    .placeholder(R.drawable.ic_org_dashboard)
                                    .into(binding.imageViewOrgLogo);
                        }
                    } else {
                        binding.textViewOrgName.setText("Organization not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading organization", e);
                    binding.textViewOrgName.setText("Error loading profile");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}