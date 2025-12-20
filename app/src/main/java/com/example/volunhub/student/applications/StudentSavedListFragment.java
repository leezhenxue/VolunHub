package com.example.volunhub.student.applications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.databinding.FragmentStudentSavedListBinding;
import com.example.volunhub.models.Service;
import com.example.volunhub.student.adapters.ServiceAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment responsible for displaying the list of volunteer services saved by the student.
 * It listens for changes in the user's saved ID list and fetches full service details accordingly.
 */
public class StudentSavedListFragment extends Fragment {

    private static final String TAG = "StudentSavedFragment";
    private FragmentStudentSavedListBinding binding;
    private ServiceAdapter adapter;
    private final List<Service> savedList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration savedListener;

    public StudentSavedListFragment() {}

    /**
     * Inflates the layout for this fragment using ViewBinding.
     * @param inflater The LayoutInflater object to inflate views.
     * @param container The parent view group.
     * @param savedInstanceState Saved state bundle.
     * @return The root View of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSavedListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes Firestore, Auth, and triggers UI setup after the view is created.
     * @param view The created View.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadSavedServiceIds();
    }

    /**
     * Configures the RecyclerView and handles navigation to service details when an item is clicked.
     */
    private void setupRecyclerView() {
        adapter = new ServiceAdapter(savedList);
        binding.recyclerStudentSaved.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerStudentSaved.setAdapter(adapter);

        adapter.setOnItemClickListener(service -> {
            // Navigation via the Host Fragment's Directions
            StudentApplicationsFragmentDirections.ActionAppsHostToServiceDetail action =
                    StudentApplicationsFragmentDirections.actionAppsHostToServiceDetail(
                            service.getDocumentId()
                    );
            Navigation.findNavController(requireView()).navigate(action);
        });
    }

    /**
     * Attaches a real-time listener to the user's profile to track changes in the 'savedServices' ID list.
     */
    private void loadSavedServiceIds() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // Cleanup existing listener to prevent leaks or duplicates
        if (savedListener != null) {
            savedListener.remove();
        }

        savedListener = db.collection("users")
                .document(myId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to saved services", e);
                        return;
                    }

                    if (binding == null) return;

                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        updateUIWithEmptyState();
                        return;
                    }

                    List<String> savedIds = new ArrayList<>();
                    Object savedServicesField = documentSnapshot.get("savedServices");

                    if (savedServicesField instanceof List) {
                        try {
                            //noinspection unchecked
                            savedIds = (List<String>) savedServicesField;
                        } catch (ClassCastException ex) {
                            Log.e(TAG, "Error casting savedServices", ex);
                        }
                    }

                    if (savedIds != null && !savedIds.isEmpty()) {
                        binding.textEmptySaved.setVisibility(View.GONE);
                        fetchServicesFromIds(savedIds);
                    } else {
                        updateUIWithEmptyState();
                    }
                });
    }

    /**
     * Fetches detailed service data from Firestore using a list of document IDs.
     * @param serviceIds List of service unique identifiers.
     */
    private void fetchServicesFromIds(List<String> serviceIds) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (String id : serviceIds) {
            tasks.add(db.collection("services").document(id).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            if (binding == null) return;

            savedList.clear();
            for (Object snapshot : results) {
                DocumentSnapshot doc = (DocumentSnapshot) snapshot;
                if (doc.exists()) {
                    Service service = doc.toObject(Service.class);
                    if (service != null) {
                        service.setDocumentId(doc.getId());
                        // Add to top of list for "most recent saved" view
                        savedList.add(0, service);
                    }
                }
            }
            adapter.notifyDataSetChanged();

            if (savedList.isEmpty()) {
                binding.textEmptySaved.setVisibility(View.VISIBLE);
            } else {
                binding.textEmptySaved.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Resets the UI components to reflect an empty list state.
     */
    private void updateUIWithEmptyState() {
        savedList.clear();
        adapter.notifyDataSetChanged();
        binding.textEmptySaved.setVisibility(View.VISIBLE);
    }

    /**
     * Removes the Firestore listener and cleans up UI binding to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (savedListener != null) {
            savedListener.remove();
        }
        binding = null;
    }
}