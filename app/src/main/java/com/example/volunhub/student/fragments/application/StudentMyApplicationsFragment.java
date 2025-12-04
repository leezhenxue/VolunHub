package com.example.volunhub.student.fragments.application;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentStudentMyApplicationsBinding;
import com.example.volunhub.models.Application;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentMyApplicationsFragment extends Fragment {

    private static final String TAG = "StudentMyAppsFragment";
    private FragmentStudentMyApplicationsBinding binding;
    private StudentApplicationAdapter adapter;
    private List<Application> applicationList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration applicationsListener;

    public StudentMyApplicationsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentMyApplicationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadMyApplications();
    }

    private void setupRecyclerView() {
        adapter = new StudentApplicationAdapter(getContext(), applicationList);
        binding.recyclerStudentMyApplications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerStudentMyApplications.setAdapter(adapter);

        adapter.setOnItemClickListener(application -> {
            // Check if service has been removed
            if (application.isServiceRemoved()) {
                // Show enhanced dialog with option to remove application
                showServiceRemovedDialog(application);
            } else {
                // Navigate to service detail as normal
                StudentApplicationsFragmentDirections.ActionAppsHostToServiceDetail action =
                        StudentApplicationsFragmentDirections.actionAppsHostToServiceDetail(
                                application.getServiceId()
                        );
                Navigation.findNavController(requireView()).navigate(action);
            }
        });
    }

    private void loadMyApplications() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // Real-time listener version - Get ALL applications for this student (no status filter)
        applicationsListener = db.collection("applications")
                .whereEqualTo("studentId", myId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Error loading applications", error);
                        return;
                    }

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.d(TAG, "No applications found.");

                        binding.textEmptyApplications.setVisibility(View.VISIBLE);
                        applicationList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    // If have data
                    binding.textEmptyApplications.setVisibility(View.GONE);
                    
                    // Step 1: Convert all documents to Application objects
                    List<Application> allApplications = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Application application = doc.toObject(Application.class);
                        if (application != null) {
                            application.setDocumentId(doc.getId());
                            allApplications.add(application);
                            Log.d("DebugApp", "Loaded application: docId=" + doc.getId() + 
                                  ", serviceId=" + application.getServiceId() + 
                                  ", status=" + application.getStatus() + 
                                  ", serviceTitle=" + application.getServiceTitle());
                        } else {
                            Log.w("DebugApp", "Failed to convert document to Application: " + doc.getId());
                        }
                    }

                    Log.d("DebugApp", "Found " + allApplications.size() + " raw applications from Firestore");

                    // Step 2: Deduplicate by serviceId (remove duplicates caused by previous bug)
                    List<Application> deduplicatedApplications = deduplicateApplications(allApplications);
                    Log.d("DebugApp", "After deduplication: " + deduplicatedApplications.size() + " applications");
                    
                    // Step 3: Clear current list and process deduplicated applications
                    applicationList.clear();
                    
                    // Step 4: Check if services exist for each application (but always display them)
                    for (Application application : deduplicatedApplications) {
                        checkServiceExists(application);
                    }
                });
    }

    /**
     * Removes duplicate applications for the same serviceId.
     * If multiple applications exist for the same serviceId:
     * - Prefer non-Pending status over Pending
     * - If both are non-Pending or both are Pending, keep the one with the latest appliedAt timestamp
     * 
     * @param allApplications List of all applications (may contain duplicates)
     * @return Deduplicated list with one application per serviceId
     */
    private List<Application> deduplicateApplications(List<Application> allApplications) {
        // Use HashMap to track the best application for each serviceId
        Map<String, Application> serviceIdToApplication = new HashMap<>();

        for (Application app : allApplications) {
            String serviceId = app.getServiceId();
            
            // Skip if serviceId is null
            if (serviceId == null || serviceId.isEmpty()) {
                Log.w(TAG, "Application with null serviceId found, skipping: " + app.getDocumentId());
                continue;
            }

            // Check if we already have an application for this serviceId
            Application existingApp = serviceIdToApplication.get(serviceId);

            if (existingApp == null) {
                // First time seeing this serviceId, add it
                serviceIdToApplication.put(serviceId, app);
            } else {
                // Duplicate found - decide which one to keep
                Application appToKeep = chooseBestApplication(existingApp, app);
                serviceIdToApplication.put(serviceId, appToKeep);
                
                Log.d(TAG, "Duplicate application found for serviceId: " + serviceId + 
                      ". Keeping: " + appToKeep.getStatus() + " (docId: " + appToKeep.getDocumentId() + 
                      "), Discarding: " + (appToKeep == existingApp ? app.getStatus() : existingApp.getStatus()) + 
                      " (docId: " + (appToKeep == existingApp ? app.getDocumentId() : existingApp.getDocumentId()) + ")");
            }
        }

        // Return the deduplicated list
        return new ArrayList<>(serviceIdToApplication.values());
    }

    /**
     * Chooses the best application between two applications for the same serviceId.
     * Priority:
     * 1. Non-Pending status over Pending
     * 2. If both have same status priority, keep the one with latest appliedAt timestamp
     * 
     * @param app1 First application
     * @param app2 Second application
     * @return The application to keep
     */
    private Application chooseBestApplication(Application app1, Application app2) {
        String status1 = app1.getStatus();
        String status2 = app2.getStatus();

        // Priority 1: Prefer non-Pending over Pending
        boolean isPending1 = "Pending".equals(status1);
        boolean isPending2 = "Pending".equals(status2);

        if (isPending1 && !isPending2) {
            // app2 is not Pending, keep it
            return app2;
        } else if (!isPending1 && isPending2) {
            // app1 is not Pending, keep it
            return app1;
        }

        // Priority 2: Both have same priority (both Pending or both non-Pending)
        // Keep the one with the latest appliedAt timestamp
        java.util.Date date1 = app1.getAppliedAt();
        java.util.Date date2 = app2.getAppliedAt();

        if (date1 == null && date2 == null) {
            // Both null, keep the first one
            return app1;
        } else if (date1 == null) {
            // date1 is null, keep app2
            return app2;
        } else if (date2 == null) {
            // date2 is null, keep app1
            return app1;
        } else {
            // Compare dates - keep the one with latest timestamp
            return date1.after(date2) ? app1 : app2;
        }
    }

    /**
     * Checks if the service associated with an application still exists in Firestore.
     * ALWAYS displays the application, even if service check fails or service doesn't exist.
     * This ensures users can see their applications immediately, even if service data is temporarily unavailable.
     */
    private void checkServiceExists(Application application) {
        String serviceId = application.getServiceId();
        String docId = application.getDocumentId();
        String status = application.getStatus();
        
        Log.d("DebugApp", "Checking service for application: docId=" + docId + 
              ", serviceId=" + serviceId + ", status=" + status);

        // If serviceId is null, mark as removed but still display
        if (serviceId == null || serviceId.isEmpty()) {
            Log.d("DebugApp", "Skipping service check for app " + docId + " because serviceId is null");
            application.setServiceRemoved(true);
            // Ensure serviceTitle is set for display
            if (application.getServiceTitle() == null || application.getServiceTitle().isEmpty()) {
                // This shouldn't happen, but set a placeholder if it does
                Log.w("DebugApp", "Application has no serviceTitle, setting placeholder");
            }
            applicationList.add(application);
            adapter.notifyDataSetChanged();
            Log.d("DebugApp", "Added application with null serviceId to list. Total items: " + applicationList.size());
            return;
        }

        // Always add the application immediately, then update service status asynchronously
        // This ensures the application appears in the list even if service check is slow
        applicationList.add(application);
        adapter.notifyDataSetChanged();
        Log.d("DebugApp", "Added application to list immediately. Total items: " + applicationList.size() + 
              ". Now checking service existence...");

        // Check service existence asynchronously (non-blocking)
        db.collection("services").document(serviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Service has been deleted
                        Log.d("DebugApp", "Service not found for app " + docId + 
                              ". ServiceId: " + serviceId + ". Marking as removed.");
                        application.setServiceRemoved(true);
                        // Update the UI to reflect the change
                        int position = applicationList.indexOf(application);
                        if (position != -1) {
                            adapter.notifyItemChanged(position);
                        }
                    } else {
                        Log.d("DebugApp", "Service found for app " + docId + ". ServiceId: " + serviceId);
                        // Service exists, ensure it's not marked as removed
                        if (application.isServiceRemoved()) {
                            application.setServiceRemoved(false);
                            int position = applicationList.indexOf(application);
                            if (position != -1) {
                                adapter.notifyItemChanged(position);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // On error, don't mark as removed - assume service exists but is temporarily unavailable
                    // This prevents hiding valid applications due to network issues
                    Log.w("DebugApp", "Error checking service existence for app " + docId + 
                          ". ServiceId: " + serviceId + ". Error: " + e.getMessage() + 
                          ". Keeping application visible (assuming service exists).");
                    // Don't mark as removed on error - keep it visible
                    // The application is already in the list, so no need to add it again
                });
    }

    /**
     * Shows an AlertDialog when user clicks on an application with a removed service.
     * Provides option to remove the application from the list.
     */
    private void showServiceRemovedDialog(Application application) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.service_unavailable_title)
                .setMessage(R.string.service_unavailable_message)
                .setPositiveButton(R.string.remove_application, (d, which) -> {
                    removeApplication(application);
                })
                .setNegativeButton(R.string.cancel, (d, which) -> {
                    d.dismiss();
                })
                .create();

        dialog.setOnShowListener(d -> {
            // Set positive button text color to red
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error_red)
            );
        });

        dialog.show();
    }

    /**
     * Removes the application from Firestore and updates the list.
     */
    private void removeApplication(Application application) {
        if (application.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Cannot remove application", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store the application and its position for UI update
        String documentId = application.getDocumentId();
        int position = applicationList.indexOf(application);

        db.collection("applications").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Application removed successfully: " + documentId);
                    Toast.makeText(getContext(), R.string.application_removed_success, Toast.LENGTH_SHORT).show();
                    
                    // Immediately update UI by removing the item from the list
                    if (position != -1) {
                        // Item found in list, remove it
                        applicationList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, applicationList.size()); // Update remaining items
                        
                        // Show empty state if list is now empty
                        if (applicationList.isEmpty()) {
                            binding.textEmptyApplications.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // Item not found in list (shouldn't happen, but handle gracefully)
                        Log.w(TAG, "Application not found in list at position, refreshing entire list");
                        // Fallback: reload the entire list
                        loadMyApplications();
                    }
                    
                    // Note: Real-time listener will also update the list, but immediate UI update
                    // provides better user experience
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing application", e);
                    Toast.makeText(getContext(), R.string.application_removed_error, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (applicationsListener != null) {
            applicationsListener.remove();
        }
        binding = null;
    }


}