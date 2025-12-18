package com.example.volunhub.org.service;

import android.app.AlertDialog;
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
//import com.example.volunhub.org.fragments.service.OrgManageServiceFragmentArgs;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.AggregateSource;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgManageServiceBinding;
import com.example.volunhub.models.Service;
import com.example.volunhub.org.adapters.OrgManageViewPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

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
        OrgManageViewPagerAdapter viewPagerAdapter = new OrgManageViewPagerAdapter(this, serviceId);
        binding.viewPagerOrg.setAdapter(viewPagerAdapter);

        // 3. Link Tabs (Set initial titles)
        new TabLayoutMediator(binding.tabLayoutOrg, binding.viewPagerOrg,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText(getString(R.string.tab_pending)); break;
                        case 1: tab.setText(getString(R.string.tab_accepted)); break;
                        case 2: tab.setText(getString(R.string.tab_rejected)); break;
                    }
                }
        ).attach();

        getChildFragmentManager().setFragmentResultListener("KEY_REFRESH_COUNTS", this,
                (requestKey, result) -> {
                    Log.d(TAG, "Received refresh signal from child fragment. Updating tabs...");
                    updateTabCounts();
                });

        // 4. --- NEW: Fetch and update the counts on the tabs ---
        updateTabCounts();

        // 5. Set up delete button click listener
        binding.buttonDeleteService.setOnClickListener(v -> showDeleteConfirmationDialog());

        // 6. Set up edit button click listener
//        binding.buttonEditService.setOnClickListener(v -> openEditServiceActivity());
    }

    private void loadServiceDetails() {
        if (serviceId == null) {
            Log.e(TAG, "Service ID is null, cannot load details.");
            return;
        }

        db.collection("services").document(serviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        Service service = documentSnapshot.toObject(Service.class);
                        if (service != null) {
                            // Store service for edit functionality
                            currentService = service;
                            
                            binding.textManageTitle.setText(service.getTitle());
                            binding.textManageDescription.setText(service.getDescription());

                            String reqText = getString(R.string.label_requirements, service.getRequirements());
                            binding.textManageRequirements.setText(reqText);

                            String stats = getString(R.string.label_applicants, service.getVolunteersApplied(), service.getVolunteersNeeded());
                            binding.textManageStats.setText(stats);

                            if (service.getServiceDate() != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                                String dateText = sdf.format(service.getServiceDate());
                                binding.textManageDate.setText(dateText);
                                Log.d(TAG, "Loaded service date: " + dateText);
                            } else {
                                binding.textManageDate.setText("Date not set");
                                Log.d(TAG, "Service date missing, showing fallback text");
                            }

                            String contactNumber = service.getContactNumber();
                            if (contactNumber != null && !contactNumber.trim().isEmpty()) {
                                binding.textManageContact.setText(contactNumber);
                                Log.d(TAG, "Contact number is: " + contactNumber);
                            } else {
                                binding.textManageContact.setText("No contact info");
                                Log.d(TAG, "Contact number missing, hiding view");
                            }
                        }
                    } else {
                        Log.w(TAG, "Service document not found.");
                        binding.textManageTitle.setText(getString(R.string.error_service_not_found));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading service details", e);
                    binding.textManageTitle.setText(getString(R.string.error_loading_data));
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
                    updateTabTitle(0, getString(R.string.tab_pending), snapshot.getCount());
                });

        // Count Accepted
        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Accepted")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    updateTabTitle(1, getString(R.string.tab_accepted), snapshot.getCount());
                });

        // Count Rejected
        db.collection("applications")
                .whereEqualTo("serviceId", serviceId)
                .whereEqualTo("status", "Rejected")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    updateTabTitle(2, getString(R.string.tab_rejected), snapshot.getCount());
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
            Toast.makeText(getContext(), getString(R.string.error_service_id_missing), Toast.LENGTH_SHORT).show();
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
     * Deletes the service from Firestore.
     * On success: Shows a success toast and navigates back to the previous screen.
     * On failure: Shows an error toast.
     */
    private void deleteService() {
        if (serviceId == null) {
            Toast.makeText(getContext(), getString(R.string.error_service_id_missing), Toast.LENGTH_SHORT).show();
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