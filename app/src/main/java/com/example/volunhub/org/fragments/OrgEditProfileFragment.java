package com.example.volunhub.org.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class OrgEditProfileFragment extends Fragment {

    private static final String TAG = "OrgEditProfileFragment";
    private FragmentOrgEditProfileBinding binding;
    private DocumentReference orgDocRef;

    public OrgEditProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        orgDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        setupOrgFieldDropdown();
        loadCurrentProfileData();

        binding.buttonSaveOrgProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void setupOrgFieldDropdown() {
        String[] orgFields = getResources().getStringArray(R.array.org_field_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                orgFields
        );
        binding.autoCompleteEditOrgField.setAdapter(adapter);
        binding.autoCompleteEditOrgField.setKeyListener(null);
        binding.autoCompleteEditOrgField.setOnClickListener(v -> binding.autoCompleteEditOrgField.showDropDown());
    }

    private void loadCurrentProfileData() {
        orgDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String orgName = documentSnapshot.getString("orgCompanyName");
                String email = documentSnapshot.getString("email");
                String orgField = documentSnapshot.getString("orgField");
                String orgDesc = documentSnapshot.getString("orgDescription");

                if (orgName != null) {
                    binding.editTextEditOrgName.setText(orgName);
                }
                if (email != null) {
                    binding.editTextEditOrgEmail.setText(email);
                }
                if (orgField != null) {
                    binding.autoCompleteEditOrgField.setText(orgField, false);
                }
                if (orgDesc != null) {
                    binding.editTextEditOrgDesc.setText(orgDesc);
                }
            }
        });
    }

    private void saveProfileChanges() {
        String orgName = binding.editTextEditOrgName.getText().toString().trim();
        String email = binding.editTextEditOrgEmail.getText().toString().trim();
        String orgField = binding.autoCompleteEditOrgField.getText().toString().trim();
        String orgDesc = binding.editTextEditOrgDesc.getText().toString().trim();

        if (orgName.isEmpty()) {
            binding.inputLayoutEditOrgName.setError("Organization name is required");
            return;
        } else {
            binding.inputLayoutEditOrgName.setError(null);
        }

        if (email.isEmpty()) {
            binding.inputLayoutEditOrgEmail.setError("Email is required");
            return;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEditOrgEmail.setError("Please enter a valid email");
            return;
        } else {
            binding.inputLayoutEditOrgEmail.setError(null);
        }

        if (orgField.isEmpty()) {
            binding.inputLayoutEditOrgField.setError("Industry field is required");
            return;
        } else {
            binding.inputLayoutEditOrgField.setError(null);
        }

        if (orgDesc.isEmpty()) {
            binding.inputLayoutEditOrgDesc.setError("Organization description is required");
            return;
        } else {
            binding.inputLayoutEditOrgDesc.setError(null);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("orgCompanyName", orgName);
        updates.put("email", email);
        updates.put("orgField", orgField);
        updates.put("orgDescription", orgDesc);

        orgDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating profile.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating document", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}