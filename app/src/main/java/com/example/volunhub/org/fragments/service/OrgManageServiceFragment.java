package com.example.volunhub.org.fragments.service;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
// --- 1. Add these imports ---
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.AggregateSource;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgManageServiceBinding;
import com.example.volunhub.models.Service;
import com.example.volunhub.org.EditJobActivity;
import com.example.volunhub.org.OrgManageViewPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
public class OrgManageServiceFragment extends Fragment {

    private static final String TAG = "OrgManageService";
    private FirebaseFirestore db;
    private FragmentOrgManageServiceBinding binding;
    private String serviceId;
    private Service currentService; // Store the loaded service for edit functionality

    public OrgManageServiceFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Uses Safe Args to get the ID
            serviceId = OrgManageServiceFragmentArgs.fromBundle(getArguments()).getServiceId();
            Log.d(TAG, "Received serviceId: " + serviceId);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgManageServiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // 1. Load Service Details (Title, Desc, Stats)
        loadServiceDetails();

        // 2. Setup Adapter
        OrgManageViewPagerAdapter viewPagerAdapter = new OrgManageViewPagerAdapter(requireActivity(), serviceId);
        binding.viewPagerOrg.setAdapter(viewPagerAdapter);

        // 3. Link Tabs (Set initial titles)
        new TabLayoutMediator(binding.tabLayoutOrg, binding.viewPagerOrg,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText("Pending"); break;
                        case 1: tab.setText("Accepted"); break;
                        case 2: tab.setText("Rejected"); break;
                    }
                }
        ).attach();

        // 4. --- NEW: Fetch and update the counts on the tabs ---
        updateTabCounts();

        // 5. Set up delete button click listener
        binding.buttonDeleteService.setOnClickListener(v -> showDeleteConfirmationDialog());

        // 6. Set up edit button click listener
        binding.buttonEditService.setOnClickListener(v -> openEditServiceActivity());
    }

    private void loadServiceDetails() {
        if (serviceId == null) {
            Log.e(TAG, "Service ID is null, cannot load details.");
            return;
        }

        db.collection("services").document(serviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Service service = documentSnapshot.toObject(Service.class);
                        if (service != null) {
                            // Store service for edit functionality
                            currentService = service;
                            
                            binding.textManageTitle.setText(service.getTitle());
                            binding.textManageDescription.setText(service.getDescription());

                            String reqText = "Requirements: " + service.getRequirements();
                            binding.textManageRequirements.setText(reqText);

                            String stats = "Applicants: " + service.getVolunteersApplied() + " / " + service.getVolunteersNeeded();
                            binding.textManageStats.setText(stats);

                            // Qimin: showing the date/time the same way students see it
                            if (service.getServiceDate() != null) {
                                SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
                                String dateText = formatter.format(service.getServiceDate());
                                binding.textManageDate.setText(dateText);
                                Log.d("Qimin_Debug", "Loaded service date: " + dateText);
                            } else {
                                binding.textManageDate.setText("Date not set");
                                Log.d("Qimin_Debug", "Service date missing, showing fallback text");
                            }

                            // Qimin: I am showing contact number and hiding if missing
                            String contactNum = service.getContactNum();
                            if (contactNum != null && !contactNum.trim().isEmpty()) {
                                binding.textManageContact.setVisibility(View.VISIBLE);
                                binding.textManageContact.setText("Contact: " + contactNum);
                                Log.d("Qimin_Debug", "Contact number is: " + contactNum);
                            } else {
                                binding.textManageContact.setText("No contact info");
                                binding.textManageContact.setVisibility(View.GONE);
                                Log.d("Qimin_Debug", "Contact number missing, hiding view");
                            }
                        }
                    } else {
                        Log.w(TAG, "Service document not found.");
                        binding.textManageTitle.setText("Error: Service not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading service details", e);
                    binding.textManageTitle.setText("Error loading data");
                });
    }

    // --- 5. NEW METHOD: Counts applicants for each status ---
    private void updateTabCounts() {
        if (serviceId == null) return;

        // Count Pending
        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Pending")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    updateTabTitle(0, "Pending", snapshot.getCount());
                });

        // Count Accepted
        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Accepted")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    updateTabTitle(1, "Accepted", snapshot.getCount());
                });

        // Count Rejected
        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Rejected")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    updateTabTitle(2, "Rejected", snapshot.getCount());
                });
    }

    // --- 6. Helper to update the Tab Text ---
    private void updateTabTitle(int position, String title, long count) {
        TabLayout.Tab tab = binding.tabLayoutOrg.getTabAt(position);
        if (tab != null) {
            // Example result: "Pending (3)"
            if (count > 0) {
                tab.setText(title + " (" + count + ")");
            } else {
                tab.setText(title);
            }
        }
    }

    /**
     * Shows a confirmation dialog before deleting the service.
     */
    private void showDeleteConfirmationDialog() {
        if (serviceId == null) {
            Toast.makeText(getContext(), "Error: Service ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteService())
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Opens the EditJobActivity with current service details.
     */
    private void openEditServiceActivity() {
        if (serviceId == null) {
            Toast.makeText(getContext(), "Error: Service ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // If service is not loaded yet, load it first
        if (currentService == null) {
            db.collection("services").document(serviceId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Service service = documentSnapshot.toObject(Service.class);
                            if (service != null) {
                                currentService = service;
                                launchEditActivity(service);
                            } else {
                                Toast.makeText(getContext(), "Error: Could not load service data", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Error: Service not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading service for edit", e);
                        Toast.makeText(getContext(), "Error loading service data", Toast.LENGTH_SHORT).show();
                    });
        } else {
            launchEditActivity(currentService);
        }
    }

    /**
     * Launches EditJobActivity with service data.
     */
    private void launchEditActivity(Service service) {
        Intent intent = new Intent(getContext(), EditJobActivity.class);
        
        // Put all service details into Intent
        intent.putExtra("serviceId", serviceId);
        intent.putExtra("title", service.getTitle());
        intent.putExtra("desc", service.getDescription());
        intent.putExtra("description", service.getDescription()); // Alternative key
        intent.putExtra("requirements", service.getRequirements());
        intent.putExtra("volunteersNeeded", String.valueOf(service.getVolunteersNeeded()));
        intent.putExtra("contactNum", service.getContactNum());
        
        // Put service date as timestamp
        if (service.getServiceDate() != null) {
            intent.putExtra("serviceDate", service.getServiceDate().getTime());
        }
        
        startActivity(intent);
    }

    /**
     * Deletes the service from Firestore.
     * On success: Shows a success toast and navigates back to the previous screen.
     * On failure: Shows an error toast.
     */
    private void deleteService() {
        if (serviceId == null) {
            Toast.makeText(getContext(), "Error: Service ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("services").document(serviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Service deleted successfully: " + serviceId);
                    Toast.makeText(getContext(), R.string.delete_success, Toast.LENGTH_SHORT).show();
                    // Navigate back to the previous screen
                    Navigation.findNavController(requireView()).popBackStack();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting service", e);
                    String errorMsg = getString(R.string.delete_error);
                    if (e.getMessage() != null) {
                        errorMsg += ": " + e.getMessage();
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}