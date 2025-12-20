package com.example.volunhub.org.service;

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

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgServiceBinding;
import com.example.volunhub.models.Service;
import com.example.volunhub.org.adapters.OrgServiceAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays a list of volunteer services posted by the organization.
 * It allows the user to manage individual service listings.
 */
public class OrgServiceFragment extends Fragment {

    private static final String TAG = "OrgServiceFragment";
    private FragmentOrgServiceBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private OrgServiceAdapter adapter;
    private final List<Service> serviceList = new ArrayList<>();
    private TextView emptyStateText;

    public OrgServiceFragment() {}

    /**
     * Inflates the fragment layout using ViewBinding.
     * @param inflater The LayoutInflater object to inflate views.
     * @param container The parent view group.
     * @param savedInstanceState Saved state bundle.
     * @return The root View of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgServiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes Firestore, Auth, and UI elements after the view is created.
     * @param view The created View.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Retrieve the empty state view from the binding root
        emptyStateText = binding.getRoot().findViewById(R.id.tv_empty_state);

        setupRecyclerView();
        loadPostedServices();
    }

    /**
     * Configures the RecyclerView and handles navigation to manage services when an item is clicked.
     */
    private void setupRecyclerView() {
        adapter = new OrgServiceAdapter(serviceList);
        binding.recyclerOrgService.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerOrgService.setAdapter(adapter);

        adapter.setOnItemClickListener(service -> {
            Log.d(TAG, "Clicked on service: " + service.getTitle());

            NavController navController = Navigation.findNavController(requireView());

            // Navigate to the Manage Service screen using Safe Args
            OrgServiceFragmentDirections.ActionOrgServiceToManageService action =
                    OrgServiceFragmentDirections.actionOrgServiceToManageService(
                            service.getDocumentId()
                    );

            navController.navigate(action);
        });
    }

    /**
     * Fetches all services posted by the current organization from Firestore.
     * Sorts services by creation date in descending order.
     */
    private void loadPostedServices() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        db.collection("services")
                .whereEqualTo("orgId", orgId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Check if fragment is still attached
                    if (binding == null) return;

                    serviceList.clear();

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Service service = doc.toObject(Service.class);
                            if (service != null) {
                                service.setDocumentId(doc.getId());
                                serviceList.add(service);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyStateUI();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading services", e));
    }

    /**
     * Updates the visibility of the empty state message based on the list size.
     */
    private void updateEmptyStateUI() {
        if (emptyStateText != null) {
            if (serviceList.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                binding.recyclerOrgService.setVisibility(View.GONE);
            } else {
                emptyStateText.setVisibility(View.GONE);
                binding.recyclerOrgService.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Cleans up the ViewBinding reference when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}