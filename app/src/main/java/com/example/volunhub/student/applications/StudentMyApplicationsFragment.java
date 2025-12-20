package com.example.volunhub.student.applications;

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
import com.example.volunhub.student.adapters.StudentApplicationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;

/**
 * Fragment that displays a list of active applications submitted by the student.
 * Handles deduplication, filtering past services, and checking if services still exist.
 */
public class StudentMyApplicationsFragment extends Fragment {

    private static final String TAG = "StudentMyAppsFragment";
    private FragmentStudentMyApplicationsBinding binding;
    private StudentApplicationAdapter adapter;
    private final List<Application> applicationList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration applicationsListener;

    public StudentMyApplicationsFragment() {}

    /**
     * Inflates the layout for this fragment.
     * @param inflater LayoutInflater object to inflate views.
     * @param container Parent view that the fragment is attached to.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentMyApplicationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes Firestore and triggers the data loading process.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadMyApplications();
    }

    /**
     * Sets up the RecyclerView and handles clicks on application items.
     */
    private void setupRecyclerView() {
        adapter = new StudentApplicationAdapter(getContext(), applicationList);
        binding.recyclerStudentMyApplications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerStudentMyApplications.setAdapter(adapter);

        adapter.setOnItemClickListener(application -> {
            if (application.isServiceRemoved()) {
                showServiceRemovedDialog(application);
            } else {
                StudentApplicationsFragmentDirections.ActionAppsHostToServiceDetail action =
                        StudentApplicationsFragmentDirections.actionAppsHostToServiceDetail(application.getServiceId());
                Navigation.findNavController(requireView()).navigate(action);
            }
        });
    }

    /**
     * Attaches a real-time listener to the applications collection for the current student.
     * Performs deduplication, filtering, and sorting before displaying.
     */
    private void loadMyApplications() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        if (applicationsListener != null) {
            applicationsListener.remove();
        }

        applicationsListener = db.collection("applications")
                .whereEqualTo("studentId", myId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading applications", error);
                        return;
                    }

                    if (binding == null) return;

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        binding.textEmptyApplications.setVisibility(View.VISIBLE);
                        applicationList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    binding.textEmptyApplications.setVisibility(View.GONE);

                    List<Application> allApplications = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Application application = doc.toObject(Application.class);
                        if (application != null) {
                            application.setDocumentId(doc.getId());
                            allApplications.add(application);
                        }
                    }

                    List<Application> deduplicated = deduplicateApplications(allApplications);
                    List<Application> processedList = filterAndSortApplications(deduplicated);

                    applicationList.clear();
                    applicationList.addAll(processedList);
                    adapter.notifyDataSetChanged();

                    if (applicationList.isEmpty()) {
                        binding.textEmptyApplications.setVisibility(View.VISIBLE);
                    }

                    for (int i = 0; i < applicationList.size(); i++) {
                        checkServiceStatusForExistingApp(applicationList.get(i), i);
                    }
                });
    }

    /**
     * Filters out services that have already occurred and sorts the remaining by date.
     * @param applications List of deduplicated applications.
     * @return A sorted list of future-dated applications.
     */
    private List<Application> filterAndSortApplications(List<Application> applications) {
        List<Application> filteredList = new ArrayList<>();
        Date now = new Date();

        for (Application app : applications) {
            Date serviceDate = app.getServiceDate();
            if (serviceDate != null && serviceDate.after(now)) {
                filteredList.add(app);
            }
        }

        Collections.sort(filteredList, (app1, app2) -> {
            Date d1 = app1.getServiceDate();
            Date d2 = app2.getServiceDate();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d1.compareTo(d2);
        });

        return filteredList;
    }

    /**
     * Removes multiple applications for the same service, keeping the most relevant one.
     * @param allApplications The raw list of applications from Firestore.
     * @return A list containing only one application per service ID.
     */
    private List<Application> deduplicateApplications(List<Application> allApplications) {
        Map<String, Application> serviceIdToApplication = new HashMap<>();

        for (Application app : allApplications) {
            String serviceId = app.getServiceId();
            if (serviceId == null || serviceId.isEmpty()) continue;

            Application existingApp = serviceIdToApplication.get(serviceId);
            if (existingApp == null) {
                serviceIdToApplication.put(serviceId, app);
            } else {
                serviceIdToApplication.put(serviceId, chooseBestApplication(existingApp, app));
            }
        }
        return new ArrayList<>(serviceIdToApplication.values());
    }

    /**
     * Logical helper to decide which application to keep based on status and timestamp.
     * @param app1 First application compared.
     * @param app2 Second application compared.
     * @return The application with higher priority (Non-Pending > Pending).
     */
    private Application chooseBestApplication(Application app1, Application app2) {
        boolean isPending1 = "Pending".equals(app1.getStatus());
        boolean isPending2 = "Pending".equals(app2.getStatus());

        if (isPending1 && !isPending2) return app2;
        if (!isPending1 && isPending2) return app1;

        Date date1 = app1.getAppliedAt();
        Date date2 = app2.getAppliedAt();

        if (date1 == null) return app2;
        if (date2 == null) return app1;

        return date1.after(date2) ? app1 : app2;
    }

    /**
     * Verifies if the service associated with an application still exists in the database.
     * @param application The application object to verify.
     * @param position The position in the RecyclerView for partial updates.
     */
    private void checkServiceStatusForExistingApp(Application application, int position) {
        String serviceId = application.getServiceId();
        String docId = application.getDocumentId();

        if (position < 0 || position >= applicationList.size() || serviceId == null) return;

        db.collection("services").document(serviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null || position >= applicationList.size()) return;
                    if (!applicationList.get(position).getDocumentId().equals(docId)) return;

                    boolean removed = !documentSnapshot.exists();
                    if (application.isServiceRemoved() != removed) {
                        application.setServiceRemoved(removed);
                        adapter.notifyItemChanged(position);
                    }
                });
    }

    /**
     * Shows a dialog notifying the user that a service was deleted by the organization.
     * @param application The selected application.
     */
    private void showServiceRemovedDialog(Application application) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.service_unavailable_title)
                .setMessage(R.string.service_unavailable_message)
                .setPositiveButton(R.string.remove_application, (d, which) -> removeApplication(application))
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red)));
        dialog.show();
    }

    /**
     * Deletes the application record from Firestore.
     * @param application The application to be deleted.
     */
    private void removeApplication(Application application) {
        String documentId = application.getDocumentId();
        if (documentId == null) return;

        db.collection("applications").document(documentId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), R.string.application_removed_success, Toast.LENGTH_SHORT).show();
                    // Real-time listener will handle list removal automatically.
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), R.string.application_removed_error, Toast.LENGTH_SHORT).show());
    }

    /**
     * Detaches the Firestore listener and cleans up the UI binding.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (applicationsListener != null) applicationsListener.remove();
        binding = null;
    }
}