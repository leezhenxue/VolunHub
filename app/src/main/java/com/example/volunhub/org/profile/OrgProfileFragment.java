package com.example.volunhub.org.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgProfileBinding;
import com.example.volunhub.org.OrgHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Displays the Organization's profile details.
 * Also handles the Logout functionality via the top toolbar menu.
 */
public class OrgProfileFragment extends Fragment {

    private static final String TAG = "OrgProfileFragment";
    private FragmentOrgProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public OrgProfileFragment() {}

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater The LayoutInflater object.
     * @param container The parent view.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes the view, sets up the toolbar menu, and loads profile data.
     *
     * @param view The created view.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupToolbarMenu();
        loadProfileData();

        if (binding.fabOrgEditProfile != null) {
            binding.fabOrgEditProfile.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_org_profile_to_edit_profile);
            });
        }
    }

    /**
     * Manually attaches the logout menu to the Activity's toolbar.
     * This is required because this fragment does not use setHasOptionsMenu.
     */
    private void setupToolbarMenu() {
        if (getActivity() == null) return;

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.toolbar_menu);
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.toolbar_logout) {
                    ((OrgHomeActivity) getActivity()).returnToMain();
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Fetches and displays the organization's profile data from Firestore.
     */
    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null || !documentSnapshot.exists()) return;

                    // Header Info
                    String orgName = documentSnapshot.getString("orgCompanyName");
                    binding.textOrgProfileName.setText(orgName != null ? orgName : "Organization Name");

                    String orgField = documentSnapshot.getString("orgField");
                    binding.textOrgProfileField.setText(orgField != null ? orgField : "Field not specified");

                    // Contact Info
                    String email = documentSnapshot.getString("email");
                    binding.textOrgProfileEmail.setText(email != null ? email : "No email");

                    String contact = documentSnapshot.getString("contactNumber");
                    binding.textOrgProfilePhone.setText(contact != null ? contact : "No contact number");

                    // Description
                    String orgDesc = documentSnapshot.getString("orgDescription");
                    binding.textOrgProfileDesc.setText(orgDesc != null ? orgDesc : "No description provided yet.");

                    // Logo
                    if (getContext() != null) {
                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                            Glide.with(getContext())
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_org_dashboard)
                                    .centerCrop()
                                    .into(binding.imageOrgProfileLogo);
                        } else {
                            binding.imageOrgProfileLogo.setImageResource(R.drawable.default_profile_picture);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading org profile", e));
    }

    /**
     * Cleans up the binding and clears the toolbar menu when leaving the fragment.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear the toolbar menu when leaving this fragment to prevent logout button on other screens
        if (getActivity() != null) {
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.getMenu().clear();
            }
        }
        binding = null;
    }
}