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
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;

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

    // Loads all applications for the current student.
// 1) Deduplicates by serviceId
// 2) Filters out past services (serviceDate before "now")
// 3) Sorts remaining services by serviceDate ASC (upcoming service at top)
    private void loadMyApplications() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // Optional: remove old listener to avoid multiple active listeners
        if (applicationsListener != null) {
            applicationsListener.remove();
        }

        applicationsListener = db.collection("applications")
                .whereEqualTo("studentId", myId)
                // Initial order by appliedAt; final order will be done on the client side.
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

                    // We have data – hide empty-state message.
                    binding.textEmptyApplications.setVisibility(View.GONE);

                    // Step 1: Convert all documents to Application objects.
                    List<Application> allApplications = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Application application = doc.toObject(Application.class);
                        if (application != null) {
                            application.setDocumentId(doc.getId());
                            allApplications.add(application);
                        }
                    }

                    // Step 2: Deduplicate by serviceId (keep the best application per service).
                    List<Application> deduplicatedApplications = deduplicateApplications(allApplications);

                    // Step 3: Filter out past services and sort future services by serviceDate ASC.
                    List<Application> futureSortedApplications =
                            filterAndSortApplications(deduplicatedApplications);

                    // Step 4: Replace current list with filtered + sorted list.
                    applicationList.clear();
                    applicationList.addAll(futureSortedApplications);
                    adapter.notifyDataSetChanged();

                    // Show empty-state if everything was filtered out (only past services).
                    if (applicationList.isEmpty()) {
                        binding.textEmptyApplications.setVisibility(View.VISIBLE);
                    }

                    // Step 5: For each visible application, check service status asynchronously.
                    for (int i = 0; i < applicationList.size(); i++) {
                        Application app = applicationList.get(i);
                        checkServiceStatusForExistingApp(app, i);
                    }
                });
    }

    // Filters out past services and sorts remaining services by serviceDate ASC.
// Requirement:
//  - Only show services whose serviceDate is after current time.
//  - Upcoming services appear at the top of the list.
    private List<Application> filterAndSortApplications(List<Application> applications) {
        List<Application> filteredList = new ArrayList<>();
        Date now = new Date(); // Current time

        // 1. Filter: keep only applications whose serviceDate is after "now".
        for (Application app : applications) {
            Date serviceDate = app.getServiceDate();   // Make sure Application has this getter
            if (serviceDate != null && serviceDate.after(now)) {
                filteredList.add(app);
            }
        }

        // 2. Sort: ascending by serviceDate (upcoming first).
        Collections.sort(filteredList, new Comparator<Application>() {
            @Override
            public int compare(Application app1, Application app2) {
                Date d1 = app1.getServiceDate();
                Date d2 = app2.getServiceDate();

                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;   // null goes to the end
                if (d2 == null) return -1;

                // Ascending order
                return d1.compareTo(d2);
            }
        });

        return filteredList;
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
     * Asynchronously checks the service status for an application ALREADY displayed in the list.
     * Updates only the specific item row if its status changes.
     * (Replaces the old checkServiceExists method)
     */
    private void checkServiceStatusForExistingApp(Application application, int position) {
        String serviceId = application.getServiceId();
        String docId = application.getDocumentId();

        // Boundary check: Ensure position is valid
        if (position < 0 || position >= applicationList.size()) return;

        // Handle null serviceId case
        if (serviceId == null || serviceId.isEmpty()) {
            if (!application.isServiceRemoved()) {
                application.setServiceRemoved(true);
                // Only update this specific item
                adapter.notifyItemChanged(position);
            }
            return;
        }

        // Asynchronously check if service exists
        db.collection("services").document(serviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    // Re-check position validity as list might have changed during async call
                    if (position >= applicationList.size()) return;
                    // Ensure we are updating the correct object
                    Application currentAppAtIndex = applicationList.get(position);
                    if (!currentAppAtIndex.getDocumentId().equals(docId)) return;

                    boolean statusChanged = false;
                    if (!documentSnapshot.exists()) {
                        // Service deleted
                        if (!application.isServiceRemoved()) {
                            Log.d("DebugApp", "Marking service as removed for app: " + docId);
                            application.setServiceRemoved(true);
                            statusChanged = true;
                        }
                    } else {
                        // Service exists
                        if (application.isServiceRemoved()) {
                            Log.d("DebugApp", "Marking service as existing for app: " + docId);
                            application.setServiceRemoved(false);
                            statusChanged = true;
                        }
                    }

                    // Only notify if status actually changed
                    if (statusChanged) {
                        adapter.notifyItemChanged(position);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("DebugApp", "Error checking service existence: " + e.getMessage());
                    // Do not mark as removed on error, assume it exists.
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
                    if (binding == null) return;
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