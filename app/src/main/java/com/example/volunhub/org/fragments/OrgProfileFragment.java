package com.example.volunhub.org.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgProfileBinding;
import com.example.volunhub.org.OrgHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrgProfileFragment extends Fragment {

    private static final String TAG = "OrgProfileFragment";
    private FragmentOrgProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public OrgProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 1. SETUP LOGOUT MANUALLY (Since we removed setSupportActionBar)
        setupToolbarMenu();

        loadProfileData();

        // Navigate to Edit Page
        if (binding.fabOrgEditProfile != null) {
            binding.fabOrgEditProfile.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_org_profile_to_edit_profile);
            });
        }
    }

    // --- NEW METHOD: Manually adding the menu to the toolbar ---
    private void setupToolbarMenu() {
        if (getActivity() == null) return;

        // Find the toolbar from the Activity
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);

        if (toolbar != null) {
            // Clear any old menu items first (so they don't duplicate)
            toolbar.getMenu().clear();

            // Inflate your menu (ensure you have res/menu/toolbar_menu.xml)
            toolbar.inflateMenu(R.menu.toolbar_menu);

            // Set the click listener
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.toolbar_logout) {
                    ((OrgHomeActivity) getActivity()).returnToMain();
                    return true;
                }
                return false;
            });
        }
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 1. Header Info
                        String orgName = documentSnapshot.getString("orgCompanyName");
                        binding.textOrgProfileName.setText(
                                (orgName != null && !orgName.isEmpty()) ? orgName : "Organization Name"
                        );

                        String orgField = documentSnapshot.getString("orgField");
                        binding.textOrgProfileField.setText(
                                (orgField != null && !orgField.isEmpty()) ? orgField : "Field not specified"
                        );

                        // 2. Contact Card Info
                        String email = documentSnapshot.getString("email");
                        binding.textOrgProfileEmail.setText(
                                (email != null && !email.isEmpty()) ? email : "No email"
                        );

                        String contact = documentSnapshot.getString("contact");
                        if (contact == null) contact = documentSnapshot.getString("contactNumber");

                        binding.textOrgProfilePhone.setText(
                                (contact != null && !contact.isEmpty()) ? contact : "No contact number"
                        );

                        // 3. About Us Description
                        String orgDesc = documentSnapshot.getString("orgDescription");
                        binding.textOrgProfileDesc.setText(
                                (orgDesc != null && !orgDesc.isEmpty()) ? orgDesc : "No description provided yet."
                        );

                        // 4. Logo Image
                        if (getContext() != null) {
                            String imageUrl = documentSnapshot.getString("profileImageUrl");
                            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                                Glide.with(getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_org_dashboard)
                                        .centerCrop()
                                        .into(binding.imageOrgProfileLogo);
                            } else {
                                binding.imageOrgProfileLogo.setImageResource(R.drawable.ic_org_dashboard);
                            }
                        }
                    } else {
                        Log.w(TAG, "Org document not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading org profile", e));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // --- CRITICAL: CLEAR THE MENU WHEN LEAVING ---
        // If we don't do this, the "Logout" button might stay visible on the Dashboard!
        if (getActivity() != null) {
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.getMenu().clear();
            }
        }

        binding = null;
    }
}