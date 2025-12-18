package com.example.volunhub.student.profile;

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

    private static final String TAG = "ViewOrgProfile";
    private FragmentViewOrgProfileBinding binding;
    private FirebaseFirestore db;
    private String orgId;

    public ViewOrgProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the organization ID from arguments
        if (getArguments() != null) {
            orgId = getArguments().getString("orgId");
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

        if (orgId != null && !orgId.isEmpty()) {
            loadOrgProfileData(orgId);
        } else {
            Log.e(TAG, "No organization ID provided");
            binding.textViewOrgName.setText(R.string.error_profile_not_found);
        }
    }

    private void loadOrgProfileData(String orgId) {
        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {

                        // 1. Basic Info
                        String orgName = documentSnapshot.getString("orgCompanyName");
                        binding.textViewOrgName.setText(
                                (orgName != null && !orgName.isEmpty()) ? orgName : "Organization Name"
                        );

                        String orgField = documentSnapshot.getString("orgField");
                        binding.textViewOrgField.setText(
                                (orgField != null && !orgField.isEmpty()) ? orgField : "Industry Unknown"
                        );

                        // 2. Contact Info
                        String email = documentSnapshot.getString("email");
                        binding.textViewOrgEmail.setText(
                                (email != null && !email.isEmpty()) ? email : "No email provided"
                        );

                        // Handle potentially different field names for contact
                        String contact = documentSnapshot.getString("contact");
                        if (contact == null) contact = documentSnapshot.getString("contactNumber");

                        binding.textViewOrgPhone.setText(
                                (contact != null && !contact.isEmpty()) ? contact : "No contact number"
                        );

                        // 3. Description
                        String desc = documentSnapshot.getString("orgDescription");
                        binding.textViewOrgDesc.setText(
                                (desc != null && !desc.isEmpty()) ? desc : "No description provided."
                        );

                        // 4. Logo
                        if (getContext() != null) {
                            Glide.with(getContext())
                                    .load(documentSnapshot.getString("profileImageUrl"))
                                    .placeholder(R.drawable.ic_org_dashboard)
                                    .centerCrop()
                                    .into(binding.imageViewOrgLogo);
                        }
                    } else {
                        Log.w(TAG, "Organization document not found");
                        binding.textViewOrgName.setText("Organization not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading organization profile", e);
                    binding.textViewOrgName.setText("Error loading data");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}