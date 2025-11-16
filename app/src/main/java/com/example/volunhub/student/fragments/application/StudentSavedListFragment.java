package com.example.volunhub.student.fragments.application;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

        // TODO: Add click listener to navigate to StudentServiceDetailFragment
    }

    private void loadSavedServiceIds() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(myId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        // --- 3. This safely handles the 'Unchecked cast' warning ---
                        List<String> savedIds = new ArrayList<>();
                        Object savedServicesField = documentSnapshot.get("savedServices");
                        if (savedServicesField instanceof List) {
                            try {
                                savedIds = (List<String>) savedServicesField;
                            } catch (ClassCastException e) {
                                Log.e(TAG, "Error casting savedServices", e);
                            }
                        }

                        if (savedIds != null && !savedIds.isEmpty()) {
                            fetchServicesFromIds(savedIds);
                        } else {
                            Log.d(TAG, "No saved services.");
                            binding.textEmptySaved.setVisibility(View.VISIBLE);
                        }
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
                        savedList.add(service);
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
        binding = null;
    }
}