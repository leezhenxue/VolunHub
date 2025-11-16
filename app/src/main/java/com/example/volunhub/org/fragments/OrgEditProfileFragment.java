package com.example.volunhub.org.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.volunhub.Constants;
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
            // No user, something is wrong
            return;
        }

        // Get a reference to this org's document
        orgDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        setupOrgFieldSpinner();
        loadCurrentProfileData();

        binding.buttonSaveOrgProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void setupOrgFieldSpinner() {
        // Use the same adapter from your SignUpActivity
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, Constants.ORG_FIELDS);
        binding.autoCompleteEditOrgField.setAdapter(adapter);
    }

    private void loadCurrentProfileData() {
        orgDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Pre-fill the form with existing data
                binding.editTextEditOrgName.setText(documentSnapshot.getString("orgCompanyName"));
                binding.editTextEditOrgDesc.setText(documentSnapshot.getString("orgDescription"));
                binding.autoCompleteEditOrgField.setText(documentSnapshot.getString("orgField"), false);
                // TODO: Load the profile image
            }
        });
    }

    private void saveProfileChanges() {
        String orgName = getSafeText(binding.editTextEditOrgName.getText());
        String orgField = getSafeText(binding.autoCompleteEditOrgField.getText());
        String orgDesc = getSafeText(binding.editTextEditOrgDesc.getText());

        if (orgName.isEmpty() || orgField.isEmpty() || orgDesc.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a map of *only* the fields we want to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("orgCompanyName", orgName);
        updates.put("orgField", orgField);
        updates.put("orgDescription", orgDesc);

        orgDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    // Navigate back to the profile page
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating profile.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating document", e);
                });
    }

    /**
     * Safely gets text from an EditText, trims it, and handles nulls.
     * @param editable The Editable text from binding.editText.getText()
     * @return A trimmed String, or an empty String ("") if it was null.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}