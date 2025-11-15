package com.example.volunhub.org.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
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

        setupLogoutMenu(); // Setup the logout button
        loadProfileData(); // Load the org's info

        // --- This is the navigation to your "Manage Profile" page ---
        binding.fabOrgEditProfile.setOnClickListener(v -> {
            // TODO: You must add "OrgEditProfileFragment" and this action to your org_nav_graph.xml
            // NavController navController = Navigation.findNavController(v);
            // navController.navigate(R.id.action_org_profile_to_edit_profile);
            Log.d(TAG, "Edit profile clicked!"); // Placeholder for now
        });
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.textOrgProfileName.setText(documentSnapshot.getString("orgCompanyName"));
                        binding.textOrgProfileField.setText(documentSnapshot.getString("orgField"));
                        binding.textOrgProfileDesc.setText(documentSnapshot.getString("orgDescription"));

                        if (getContext() != null) {
                            Glide.with(getContext())
                                    .load(documentSnapshot.getString("profileImageUrl"))
                                    .placeholder(R.drawable.ic_org_dashboard)
                                    .into(binding.imageOrgProfileLogo);
                        }
                    } else {
                        Log.w(TAG, "Org document not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading org profile", e));
    }

    private void setupLogoutMenu() {
        MenuHost menuHost = requireActivity();
        LifecycleOwner lifecycleOwner = getViewLifecycleOwner();

        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.toolbar_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull android.view.MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.toolbar_logout) {
                    if (getActivity() != null) {
                        ((OrgHomeActivity) getActivity()).returnToMain();
                    }
                    return true;
                }
                return false;
            }
        }, lifecycleOwner, Lifecycle.State.RESUMED);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}