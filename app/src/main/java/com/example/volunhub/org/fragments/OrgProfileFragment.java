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

        setupLogoutMenu();
        loadProfileData();

        // 修复：在 layout 文件中添加 FAB 后，这里就可以访问了
        // 确保在 fragment_org_profile.xml 中有 fab_org_edit_profile
        if (binding.fabOrgEditProfile != null) {
            binding.fabOrgEditProfile.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_org_profile_to_edit_profile);
            });
        }
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String orgName = documentSnapshot.getString("orgCompanyName");
                        String email = documentSnapshot.getString("email");
                        String orgField = documentSnapshot.getString("orgField");
                        String orgDesc = documentSnapshot.getString("orgDescription");

                        if (orgName != null) {
                            binding.textOrgProfileName.setText(orgName);
                        }
                        if (email != null) {
                            binding.textOrgProfileEmail.setText(email);
                        }
                        if (orgField != null) {
                            binding.textOrgProfileField.setText(orgField);
                        }
                        if (orgDesc != null) {
                            binding.textOrgProfileDesc.setText(orgDesc);
                        }

                        if (getContext() != null) {
                            String imageUrl = documentSnapshot.getString("profileImageUrl");
                            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                                Glide.with(getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_org_dashboard)
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