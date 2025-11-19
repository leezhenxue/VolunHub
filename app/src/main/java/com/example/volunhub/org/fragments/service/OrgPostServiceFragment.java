package com.example.volunhub.org.fragments.service;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.volunhub.databinding.FragmentOrgPostServiceBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class OrgPostServiceFragment extends Fragment {

    private static final String TAG = "OrgPostServiceFragment";
    private FragmentOrgPostServiceBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Date selectedServiceDate; // To store the chosen date

    public OrgPostServiceFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgPostServiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupDatePicker();
        binding.buttonPostService.setOnClickListener(v -> postService());
    }

    private void setupDatePicker() {
        binding.editTextServiceDate.setOnClickListener(v -> showDatePicker());
        binding.inputLayoutServiceDate.setEndIconOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select service date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Convert the selected UTC milliseconds to a Date object
            selectedServiceDate = new Date(selection);
            // Format the date for display in the EditText
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            binding.editTextServiceDate.setText(formatter.format(selectedServiceDate));
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void postService() {
        String title = getSafeText(binding.editTextTitle.getText());
        String description = getSafeText(binding.editTextDescription.getText());
        String requirements = getSafeText(binding.editTextRequirements.getText());
        String volunteersNeededStr = getSafeText(binding.editTextVolunteersNeeded.getText());

        if (TextUtils.isEmpty(title)) {
            binding.inputLayoutTitle.setError("Title is required");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            binding.inputLayoutDescription.setError("Description is required");
            return;
        }
        if (TextUtils.isEmpty(volunteersNeededStr)) {
            binding.inputLayoutVolunteersNeeded.setError("Volunteers needed is required");
            return;
        }
        if (selectedServiceDate == null) {
            binding.inputLayoutServiceDate.setError("Service date is required");
            return;
        }

        int volunteersNeeded;
        try {
            volunteersNeeded = Integer.parseInt(volunteersNeededStr);
            if (volunteersNeeded <= 0) {
                binding.inputLayoutVolunteersNeeded.setError("Must be at least 1");
                return;
            }
        } catch (NumberFormatException e) {
            binding.inputLayoutVolunteersNeeded.setError("Invalid number");
            return;
        }

        // Get current user's orgId and orgName
        String orgId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (orgId == null) {
            Toast.makeText(getContext(), "Error: Organization not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // You might need to fetch orgName from your Firestore "users" collection
        // For simplicity, let's assume you have it or fetch it before this
        // Or pass it as an argument if coming from OrgDashboardFragment
        String orgName = "Unknown Organization"; // TODO: Fetch actual org name from Firestore

        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("orgId", orgId);
        serviceData.put("orgName", orgName); // Make sure this is correct
        serviceData.put("title", title);
        serviceData.put("description", description);
        serviceData.put("requirements", requirements);
        serviceData.put("volunteersNeeded", volunteersNeeded);
        serviceData.put("volunteersApplied", 0); // Always start with 0
        serviceData.put("serviceDate", selectedServiceDate);
        serviceData.put("createdAt", FieldValue.serverTimestamp()); // Firestore will set this
        serviceData.put("status", "Active");
        serviceData.put("searchTitle", title.toLowerCase());

        db.collection("services")
                .add(serviceData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Service posted successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                    // Navigate back to the OrgServiceFragment
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack(); // Go back to previous screen
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error posting service: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Error adding document", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Safely gets text from an EditText, trims it, and handles nulls.
     * @param editable The Editable text from binding.editText.getText()
     * @return A trimmed String, or an empty String ("") if it was null.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }
}
