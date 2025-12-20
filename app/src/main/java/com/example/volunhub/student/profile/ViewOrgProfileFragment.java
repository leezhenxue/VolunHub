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

/**
 * Fragment that allows students to view the public profile of an Organization.
 * It displays company details, contact information, and their description.
 */
public class ViewOrgProfileFragment extends Fragment {

    private static final String TAG = "ViewOrgProfile";
    private FragmentViewOrgProfileBinding binding;
    private FirebaseFirestore db;
    private String orgId;

    public ViewOrgProfileFragment() {}

    /**
     * Retrieves the organization ID from the fragment arguments.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orgId = getArguments().getString("orgId");
        }
    }

    /**
     * Inflates the layout for this fragment using ViewBinding.
     * @param inflater The LayoutInflater object to inflate views in the fragment.
     * @param container The parent view that the fragment's UI is attached to.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentViewOrgProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes Firestore and triggers data loading after the view is created.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState Saved state bundle.
     */
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

    /**
     * Fetches organization data from Firestore and populates the UI fields.
     * @param orgId The unique ID of the organization to load.
     */
    private void loadOrgProfileData(String orgId) {
        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Check if fragment is still attached to avoid NullPointerException
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

                        // Support legacy or alternate contact field names
                        String contact = documentSnapshot.getString("contactNumber");
                        if (contact == null) contact = documentSnapshot.getString("contact");

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
                        binding.textViewOrgName.setText(R.string.error_profile_not_found);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading organization profile", e);
                    if (binding != null) {
                        binding.textViewOrgName.setText(R.string.error_loading_data);
                    }
                });
    }

    /**
     * Cleans up the binding object when the fragment's view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}