package com.example.volunhub.org.fragments.service;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volunhub.databinding.FragmentOrgServiceBinding;
import com.example.volunhub.models.Service;
import com.example.volunhub.org.OrgServiceAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.volunhub.R;

import java.util.ArrayList;
import java.util.List;

public class OrgServiceFragment extends Fragment {

    private static final String TAG = "OrgServiceFragment";
    private FragmentOrgServiceBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private OrgServiceAdapter adapter;
    final private List<Service> serviceList = new ArrayList<>();
    private TextView emptyStateText;

    // constructor
    public OrgServiceFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgServiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        emptyStateText = binding.getRoot().findViewById(R.id.tv_empty_state);

        setupRecyclerView();
        loadPostedServices();
    }

    private void setupRecyclerView() {
        adapter = new OrgServiceAdapter(serviceList);
        binding.recyclerOrgService.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerOrgService.setAdapter(adapter);

        // --- 2. THIS CODE IS NOW CORRECT ---
        adapter.setOnItemClickListener(service -> {
            Log.d(TAG, "Clicked on service: " + service.getTitle());

            NavController navController = Navigation.findNavController(requireView());

            // This class will now be found
            OrgServiceFragmentDirections.ActionOrgServiceToManageService action =
                    OrgServiceFragmentDirections.actionOrgServiceToManageService(
                            service.getDocumentId()
                    );

            navController.navigate(action);
        });
    }

    private void loadPostedServices() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Querying services for orgId: " + orgId);

        db.collection("services")
                .whereEqualTo("orgId", orgId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null) {
                        Log.d(TAG, "Query snapshot is null.");
                        return;
                    }
                    Log.d(TAG, "Found " + querySnapshot.size() + " services...");
                    serviceList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Service service = doc.toObject(Service.class);
                        if (service != null) {
                            service.setDocumentId(doc.getId());
                            serviceList.add(service);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (emptyStateText != null) {
                        if (serviceList.isEmpty()) {
                            // Qimin: list is empty, showing text
                            emptyStateText.setVisibility(View.VISIBLE);
                            binding.recyclerOrgService.setVisibility(View.GONE);
                        } else {
                            emptyStateText.setVisibility(View.GONE);
                            binding.recyclerOrgService.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e ->
                    Log.e(TAG, "Error loading services", e)
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}