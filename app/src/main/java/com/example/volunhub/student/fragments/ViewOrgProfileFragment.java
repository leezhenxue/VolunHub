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
            Log.e("ViewOrgProfile", "No organization ID provided");
        }
    }

    private void loadOrgProfileData(String orgId) {
        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && "organization".equals(documentSnapshot.getString("role"))) {
                        // Populate organization data - 使用正确的ID名称
                        // XML中的 ID: @+id/text_view_org_name -> Java: binding.textViewOrgName
                        binding.textViewOrgName.setText(documentSnapshot.getString("orgCompanyName"));

                        // XML中的 ID: @+id/text_view_org_field -> Java: binding.textViewOrgField
                        binding.textViewOrgField.setText(documentSnapshot.getString("orgField"));

                        // XML中的 ID: @+id/text_view_org_desc -> Java: binding.textViewOrgDesc
                        binding.textViewOrgDesc.setText(documentSnapshot.getString("orgDescription"));

                        // Load organization logo
                        // XML中的 ID: @+id/image_view_org_logo -> Java: binding.imageViewOrgLogo
                        if (getContext() != null) {
                            Glide.with(getContext())
                                    .load(documentSnapshot.getString("profileImageUrl"))
                                    .placeholder(R.drawable.ic_org_dashboard)
                                    .centerCrop()
                                    .into(binding.imageViewOrgLogo);
                        }
                    } else {
                        Log.w("ViewOrgProfile", "Organization document not found or not an organization");
                    }
                })
                .addOnFailureListener(e -> Log.e("ViewOrgProfile", "Error loading organization profile", e));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}