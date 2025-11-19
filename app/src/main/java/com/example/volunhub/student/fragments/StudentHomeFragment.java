package com.example.volunhub.student.fragments;



import android.os.Bundle;

import android.text.Editable;

import android.text.TextWatcher;

import android.util.Log;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;



import androidx.annotation.NonNull;

import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;

import androidx.navigation.NavController;

import androidx.navigation.Navigation;

import androidx.recyclerview.widget.LinearLayoutManager;



import com.example.volunhub.databinding.FragmentStudentHomeBinding;

import com.example.volunhub.models.Service;

import com.google.firebase.firestore.DocumentSnapshot;

import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.Query;



import java.util.ArrayList;

import java.util.List;

import java.util.stream.Collectors;



public class StudentHomeFragment extends Fragment {



    private static final String TAG = "StudentHomeFragment";

    private FragmentStudentHomeBinding binding;

    private ServiceAdapter adapter;

    private final List<Service> serviceList = new ArrayList<>(); // List for display

    private final List<Service> allServicesList = new ArrayList<>(); // Full list from DB

    private FirebaseFirestore db;

    private NavController navController;

    private boolean isLoading = false;



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

        navController = Navigation.findNavController(view);



        setupRecyclerView();

        setupSearch();

        loadAllServices();

    }



    private void setupRecyclerView() {

        adapter = new ServiceAdapter(serviceList); // Adapter uses the display list

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        binding.recyclerStudentHomeServices.setLayoutManager(layoutManager);

        binding.recyclerStudentHomeServices.setAdapter(adapter);



        adapter.setOnItemClickListener(service -> {

            Log.d(TAG, "Clicked on service: " + service.getTitle());

            StudentHomeFragmentDirections.ActionHomeToServiceDetail action =

                    StudentHomeFragmentDirections.actionHomeToServiceDetail(

                            service.getDocumentId()

                    );

            navController.navigate(action);

        });

    }



    private void setupSearch() {

        binding.editTextSearch.addTextChangedListener(new TextWatcher() {

            @Override

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}



            @Override

            public void onTextChanged(CharSequence s, int start, int before, int count) {}



            @Override

            public void afterTextChanged(Editable s) {

                String searchText = getSafeText(s);

                filterServices(searchText);

            }

        });

    }



    private void loadAllServices() {

        Log.d(TAG, "Loading all active services...");

        isLoading = true;

        binding.progressBar.setVisibility(View.VISIBLE); // Show progress bar



        db.collection("services")

                .whereEqualTo("status", "Active") // Only fetch active services

                .orderBy("createdAt", Query.Direction.DESCENDING)

                .get()

                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot == null || querySnapshot.isEmpty()) {

                        Log.d(TAG, "No active services found.");

                        binding.emptyView.setVisibility(View.VISIBLE); // Show empty message

                        binding.recyclerStudentHomeServices.setVisibility(View.GONE);

                        return;

                    }



                    Log.d(TAG, "Loaded " + querySnapshot.size() + " active services");

                    allServicesList.clear(); // Clear the master list

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        Service service = doc.toObject(Service.class);

                        if (service != null) {

                            service.setDocumentId(doc.getId());

                            allServicesList.add(service);

                        }

                    }



                    // Initially, the display list is the same as the full list

                    filterServices(""); // Call filter with empty to populate the list



                    isLoading = false;

                    binding.progressBar.setVisibility(View.GONE);

                })

                .addOnFailureListener(e -> {

                    Log.e(TAG, "Error loading services", e);

                    isLoading = false;

                    binding.progressBar.setVisibility(View.GONE);

                    // Optionally show an error message to the user

                });

    }



    private void filterServices(String searchText) {

        Log.d(TAG, "Filtering with text: '" + searchText + "'");

        serviceList.clear(); // Clear the current display list



        if (searchText.isEmpty()) {

            // If search is empty, show all services

            serviceList.addAll(allServicesList);

            Log.d(TAG, "Search empty. Displaying all " + serviceList.size() + " services.");

        } else {

            // Filter the master list and add results to the display list

            String lowerCaseQuery = searchText.toLowerCase();

            List<Service> filteredList = allServicesList.stream()

                    .filter(service -> service.getTitle().toLowerCase().contains(lowerCaseQuery))

                    .collect(Collectors.toList());

            serviceList.addAll(filteredList);

            Log.d(TAG, "Found " + serviceList.size() + " services matching query.");

        }



        // Update the UI based on whether the list is empty

        if (serviceList.isEmpty()) {

            binding.emptyView.setVisibility(View.VISIBLE);

            binding.recyclerStudentHomeServices.setVisibility(View.GONE);

        } else {

            binding.emptyView.setVisibility(View.GONE);

            binding.recyclerStudentHomeServices.setVisibility(View.VISIBLE);

        }



        adapter.notifyDataSetChanged(); // Refresh the RecyclerView

    }



    @Override

    public void onDestroyView() {

        super.onDestroyView();

        binding = null;

    }



    private String getSafeText(android.text.Editable editable) {

        return (editable == null) ? "" : editable.toString().trim();

    }

}
