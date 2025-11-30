package com.example.volunhub.student.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunhub.databinding.FragmentStudentHomeBinding;
import com.example.volunhub.models.Service;
import com.google.firebase.firestore.DocumentSnapshot;
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
    private NavController navController;
    private boolean isLoading = false;

    // --- Pagination Variables ---
    private static final long PAGE_SIZE = 10; // Load 5 at a time (adjusted from 10 to match your earlier request)
    private DocumentSnapshot lastVisibleDocument;
    private boolean isSearchActive = false;

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
        loadInitialServices(); // Corrected to call loadInitialServices
    }

    private void setupRecyclerView() {
        adapter = new ServiceAdapter(serviceList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.recyclerStudentHomeServices.setLayoutManager(layoutManager);
        binding.recyclerStudentHomeServices.setAdapter(adapter);

        // --- CLICK LISTENER ---
        adapter.setOnItemClickListener(service -> {
            Log.d(TAG, "Clicked on service: " + service.getTitle());
            StudentHomeFragmentDirections.ActionHomeToServiceDetail action =
                    StudentHomeFragmentDirections.actionHomeToServiceDetail(
                            service.getDocumentId()
                    );
            // We pass "NOT_APPLIED" because the home page doesn't know the status yet.
            // The detail page will fetch the real status.
            action.setInitialStatus("NOT_APPLIED");
            navController.navigate(action);
        });

        // --- SCROLL LISTENER FOR PAGINATION ---
        binding.recyclerStudentHomeServices.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && !isSearchActive) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount) {
                            Log.d(TAG, "Scrolled to bottom. Loading more services...");
                            loadMoreServices();
                        }
                    }
                }
            }
        });
    }

    private void setupSearch() {
        // Use TextWatcher for real-time search
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String searchText = getSafeText(s);
                if (searchText.isEmpty()) {
                    isSearchActive = false;
                    loadInitialServices(); // Reload page 1
                } else {
                    isSearchActive = true;
                    searchServicesFirestore(searchText); // Search database
                }
            }
        });
    }

    private void loadInitialServices() {
        Log.d(TAG, "Loading initial services...");
        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("services")
                .whereEqualTo("status", "Active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.d(TAG, "No services found.");
                        binding.emptyView.setVisibility(View.VISIBLE); // Assuming you have this ID in XML
                        binding.recyclerStudentHomeServices.setVisibility(View.GONE);
                        isLoading = false;
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }

                    binding.emptyView.setVisibility(View.GONE);
                    binding.recyclerStudentHomeServices.setVisibility(View.VISIBLE);

                    Log.d(TAG, "Loaded " + querySnapshot.size() + " initial services");
                    serviceList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Service service = doc.toObject(Service.class);
                        if (service != null) {
                            service.setDocumentId(doc.getId());
                            serviceList.add(service);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // Save the last document for pagination
                    lastVisibleDocument = querySnapshot.getDocuments()
                            .get(querySnapshot.size() - 1);
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading services", e);
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    // --- NEW METHOD TO LOAD THE NEXT PAGE ---
    private void loadMoreServices() {
        if (lastVisibleDocument == null) {
            Log.d(TAG, "No last document, can't load more.");
            return;
        }

        Log.d(TAG, "Loading more services...");
        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("services")
                .whereEqualTo("status", "Active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(lastVisibleDocument)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.d(TAG, "No more services to load.");
                        isLoading = false;
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }

                    Log.d(TAG, "Loaded " + querySnapshot.size() + " more services");
                    int startPosition = serviceList.size();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Service service = doc.toObject(Service.class);
                        if (service != null) {
                            service.setDocumentId(doc.getId());
                            serviceList.add(service);
                        }
                    }

                    adapter.notifyItemRangeInserted(startPosition, querySnapshot.size());

                    lastVisibleDocument = querySnapshot.getDocuments()
                            .get(querySnapshot.size() - 1);
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading more services", e);
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    private void searchServicesFirestore(String searchText) {
        Log.d(TAG, "Searching for: " + searchText);
        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);

        String queryText = searchText.toLowerCase();

        db.collection("services")
                .whereEqualTo("status", "Active")
                .orderBy("searchTitle") // Ensure you have this field and index
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    serviceList.clear();

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        binding.emptyView.setVisibility(View.VISIBLE);
                        binding.recyclerStudentHomeServices.setVisibility(View.GONE);
                    } else {
                        binding.emptyView.setVisibility(View.GONE);
                        binding.recyclerStudentHomeServices.setVisibility(View.VISIBLE);

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Service service = doc.toObject(Service.class);
                            if (service != null) {
                                service.setDocumentId(doc.getId());
                                serviceList.add(service);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching services", e);
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Safely gets text from an EditText, trims it, and handles nulls.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }
}