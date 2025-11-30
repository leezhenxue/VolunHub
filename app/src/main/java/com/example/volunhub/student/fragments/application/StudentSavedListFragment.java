package com.example.volunhub.student.fragments.application;

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
// --- 1. IMPORT THE ADAPTER ---
import com.example.volunhub.student.fragments.ServiceAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class StudentSavedListFragment extends Fragment {

    private static final String TAG = "StudentSavedFragment";
    private FragmentStudentSavedListBinding binding;
    private ServiceAdapter adapter;
    // --- 2. Made 'final' to fix the warning ---
    private final List<Service> savedList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration savedListener;


    public StudentSavedListFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSavedListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadSavedServiceIds();
    }

    private void setupRecyclerView() {
        adapter = new ServiceAdapter(savedList);
        binding.recyclerStudentSaved.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerStudentSaved.setAdapter(adapter);

        adapter.setOnItemClickListener(application -> {
            // 1. Use the DIRECTIONS from the HOST fragment
            // (Make sure you import this specific class)
            StudentApplicationsFragmentDirections.ActionAppsHostToServiceDetail action =
                    StudentApplicationsFragmentDirections.actionAppsHostToServiceDetail(
                            application.getDocumentId()
                    );

            // 2. Find the NavController
            // Since we are inside a ViewPager, standard findNavController(view) works fine
            // because the ViewPager is part of the nav host.
            Navigation.findNavController(requireView()).navigate(action);
        });
    }

    private void loadSavedServiceIds() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // If a previous Firestore listener exists, remove it to avoid duplicates
        if (savedListener != null) {
            savedListener.remove();
            savedListener = null;
        }

        // Start a real-time listener on the current user's document
        savedListener = db.collection("users")
                .document(myId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    // Handle Firestore listener errors
                    if (e != null) {
                        Log.e(TAG, "Error listening to saved services", e);
                        return;
                    }

                    // If document does not exist, show empty state
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        savedList.clear();                       // your saved-list variable
                        adapter.notifyDataSetChanged();
                        binding.textEmptySaved.setVisibility(View.VISIBLE);
                        Log.d(TAG, "User document not found.");
                        return;
                    }

                    // Safely read the 'savedServices' field (List<String>)
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

                    // If there are saved IDs, fetch service details from Firestore
                    if (savedIds != null && !savedIds.isEmpty()) {
                        binding.textEmptySaved.setVisibility(View.GONE);
                        fetchServicesFromIds(savedIds);          // your existing function
                    } else {
                        // If list is empty, show empty message
                        savedList.clear();
                        adapter.notifyDataSetChanged();
                        binding.textEmptySaved.setVisibility(View.VISIBLE);
                        Log.d(TAG, "No saved services.");
                    }
                });
    }

    private void fetchServicesFromIds(List<String> serviceIds) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (String id : serviceIds) {
            tasks.add(db.collection("services").document(id).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            savedList.clear();
            for (Object snapshot : results) {
                DocumentSnapshot doc = (DocumentSnapshot) snapshot;
                if (doc.exists()) {
                    Service service = doc.toObject(Service.class);
                    if (service != null) {
                        service.setDocumentId(doc.getId());
                        savedList.add(0,service);
                    }
                }
            }
            adapter.notifyDataSetChanged(); // --- 4. This will work now ---

            if(savedList.isEmpty()){
                binding.textEmptySaved.setVisibility(View.VISIBLE);
            } else {
                binding.textEmptySaved.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop real-time updates when the fragment is destroyed
        if (savedListener != null) {
            savedListener.remove();   // cancel Firestore listener to avoid memory leaks
            savedListener = null;
        }

        binding = null;
    }
}