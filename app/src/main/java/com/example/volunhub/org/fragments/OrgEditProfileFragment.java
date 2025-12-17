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
            return;
        }

        orgDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        setupOrgFieldSpinner();
        loadCurrentProfileData();

        binding.buttonSaveOrgProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void setupOrgFieldSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, Constants.ORG_FIELDS);
        binding.autoCompleteEditOrgField.setAdapter(adapter);
    }

    private void loadCurrentProfileData() {
        orgDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                binding.editTextEditOrgName.setText(documentSnapshot.getString("orgCompanyName"));
                binding.editTextEditOrgDesc.setText(documentSnapshot.getString("orgDescription"));
                binding.autoCompleteEditOrgField.setText(documentSnapshot.getString("orgField"), false);
                binding.editTextEditOrgEmail.setText(documentSnapshot.getString("email"));

                String contact = documentSnapshot.getString("contact");
                if (contact != null) {
                    if (contact.startsWith("+60")) {
                        binding.editTextEditOrgContact.setText(contact.substring(3));
                    } else {
                        binding.editTextEditOrgContact.setText(contact);
                    }
                }
            }
        });
    }

    private void saveProfileChanges() {
        String orgName = getSafeText(binding.editTextEditOrgName.getText());
        String orgField = getSafeText(binding.autoCompleteEditOrgField.getText());
        String orgDesc = getSafeText(binding.editTextEditOrgDesc.getText());
        String rawContact = getSafeText(binding.editTextEditOrgContact.getText());

        if (orgName.isEmpty() || orgField.isEmpty() || orgDesc.isEmpty() || rawContact.isEmpty()) {
            Toast.makeText(getContext(), "All fields including contact are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalContact;
        if (rawContact.startsWith("0")) {
            finalContact = "+60" + rawContact.substring(1);
        } else {
            finalContact = "+60" + rawContact;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("orgCompanyName", orgName);
        updates.put("orgField", orgField);
        updates.put("orgDescription", orgDesc);
        updates.put("contact", finalContact);

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

    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}