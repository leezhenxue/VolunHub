package com.example.volunhub.student.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentStudentHomeBinding;
import com.example.volunhub.models.Service;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class StudentHomeFragment extends Fragment {

    private static final String TAG = "StudentHomeFragment";
    private FragmentStudentHomeBinding binding;
    private ServiceAdapter adapter;
    private final List<Service> serviceList = new ArrayList<>();
    private FirebaseFirestore db;

    // constructor
    public StudentHomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        setupSearch();
        loadServices();
    }

    private void setupRecyclerView() {
        adapter = new ServiceAdapter(serviceList);
        binding.recyclerStudentHomeServices.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerStudentHomeServices.setAdapter(adapter);

        // This is where you make the items clickable
        adapter.setOnItemClickListener(service -> {
            Log.d(TAG, "Clicked on service: " + service.getTitle());

            // Use the generated Directions class to navigate
            StudentHomeFragmentDirections.ActionHomeToServiceDetail action =
                    StudentHomeFragmentDirections.actionHomeToServiceDetail(
                            service.getDocumentId() // Pass the service ID
                    );

            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(action);
        });
    }

    private void setupSearch() {
        binding.editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String searchText = binding.editTextSearch.getText().toString().trim();
                searchServices(searchText);
                return true;
            }
            return false;
        });
    }

    private void loadServices() {
        Log.d(TAG, "Loading initial services...");
        db.collection("services")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20) // Load the first 20
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    serviceList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Service service = doc.toObject(Service.class);
                        if (service != null) {
                            service.setDocumentId(doc.getId()); // Don't forget to set the ID
                            serviceList.add(service);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading services", e);
                });
    }

    private void searchServices(String searchText) {
        Log.d(TAG, "Searching for: " + searchText);
        // This is the prefix search query
        db.collection("services")
                .orderBy("title")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    serviceList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Service service = doc.toObject(Service.class);
                        if (service != null) {
                            service.setDocumentId(doc.getId());
                            serviceList.add(service);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching services", e);
                    // This will likely fail until you create the index
                    // Check Logcat for the FAILED_PRECONDITION link!
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}