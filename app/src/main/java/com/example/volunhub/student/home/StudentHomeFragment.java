package com.example.volunhub.student.home;

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
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunhub.databinding.FragmentStudentHomeBinding;
import com.example.volunhub.models.Service;
import com.example.volunhub.student.adapters.ServiceAdapter;
//import com.example.volunhub.student.fragments.StudentHomeFragmentDirections;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.Timestamp;

import java.util.Collections;

import com.google.firebase.firestore.ListenerRegistration;




public class StudentHomeFragment extends Fragment {

    private static final String TAG = "StudentHomeFragment";
    private FragmentStudentHomeBinding binding;
    private ServiceAdapter adapter;
    private final List<Service> serviceList = new ArrayList<>();
    private FirebaseFirestore db;
    private NavController navController;
    private boolean isLoading = false;
    private ListenerRegistration servicesListener;
    private long startTime;



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

        startTime = System.currentTimeMillis();
        Log.d("NFRTest", "P1 - Start Fetching Services: " + startTime);

        setupRecyclerView();
        setupSearch();
        loadInitialServices();
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
        lastVisibleDocument = null;
        binding.progressBar.setVisibility(View.VISIBLE);

        Timestamp now = Timestamp.now();

        db.collection("services")
                .whereEqualTo("status", "Active")
                .whereGreaterThanOrEqualTo("serviceDate", now)
                .orderBy("serviceDate", Query.Direction.ASCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (binding == null) {
                        return;
                    }
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        Log.d("NFRTest", "P1 - List Loaded (Empty). Duration: " + duration + "ms");
                        // [NFR 7 TEST] PROOF OF PAGINATION (PAGE 1)
                        int count = querySnapshot.size();
                        Log.d("NFRTest", "SC1 - Pagination: Initial Batch Loaded. Count: " + count + " items. (Limit set to " + PAGE_SIZE + ")");
                        binding.emptyView.setVisibility(View.VISIBLE);
                        binding.recyclerStudentHomeServices.setVisibility(View.GONE);
                        isLoading = false;
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }

                    binding.emptyView.setVisibility(View.GONE);
                    binding.recyclerStudentHomeServices.setVisibility(View.VISIBLE);

                    serviceList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Service service = doc.toObject(Service.class);
                        if (service != null) {
                            service.setDocumentId(doc.getId());
                            serviceList.add(service);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // ---------------------------------------------------------
                    // [NFR 1 & 7 TEST] CAPTURE EVIDENCE HERE
                    // ---------------------------------------------------------
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    int count = querySnapshot.size();

                    // Log for NFR 1 (Performance)
                    Log.d("NFRTest", "P1 - List Rendered Successfully. Duration: " + duration + "ms");

                    // Log for NFR 7 (Scalability - Page 1)
                    Log.d("NFRTest", "SC1 - Pagination: Initial Batch Loaded. Count: " + count + " items. (Limit set to " + PAGE_SIZE + ")");

                    lastVisibleDocument = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
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

        Timestamp now = Timestamp.now();

        db.collection("services")
                .whereEqualTo("status", "Active")
                .whereGreaterThanOrEqualTo("serviceDate", now)
                .orderBy("serviceDate", Query.Direction.ASCENDING)
                .startAfter(lastVisibleDocument)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (binding == null) return;
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

                    // ---------------------------------------------------------
                    // [NFR 7 TEST] PROOF OF PAGINATION (PAGE 2)
                    // ---------------------------------------------------------
                    int count = querySnapshot.size();
                    Log.d("NFRTest", "SC1 - Pagination: Next Batch Loaded. Count: " + count + " items.");

                    // Save the last document for next pagination
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
        lastVisibleDocument = null;
        binding.progressBar.setVisibility(View.VISIBLE);

        String queryText = searchText.toLowerCase();

        Timestamp now = Timestamp.now();

        db.collection("services")
                .whereEqualTo("status", "Active")
                .orderBy("searchTitle") // Ensure this field exists in your DB!
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (binding == null) return;
                    serviceList.clear();

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        binding.emptyView.setVisibility(View.VISIBLE);
                        binding.recyclerStudentHomeServices.setVisibility(View.GONE);
                        isLoading = false;
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }

                    List<DocumentSnapshot> docs = new ArrayList<>(querySnapshot.getDocuments());
                    List<DocumentSnapshot> filtered = new ArrayList<>();

                    // --- FIX 1: Use "serviceDate" instead of "startDateTime" ---
                    for (DocumentSnapshot d : docs) {
                        // Try to get the date as a Date object first (standard Firestore mapping)
                        Timestamp serviceTimestamp = d.getTimestamp("serviceDate");

                        // Filter: Keep only future events
                        if (serviceTimestamp != null && serviceTimestamp.compareTo(now) >= 0) {
                            filtered.add(d);
                        }
                    }

                    // --- FIX 2: Sort using "serviceDate" ---
                    Collections.sort(filtered, (d1, d2) -> {
                        Timestamp t1 = d1.getTimestamp("serviceDate");
                        Timestamp t2 = d2.getTimestamp("serviceDate");

                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;

                        // Ascending order (Earliest first)
                        return t1.compareTo(t2);
                    });

                    // Convert filtered snapshots to Service objects
                    for (DocumentSnapshot doc : filtered) {
                        Service service = doc.toObject(Service.class);
                        if (service != null) {
                            service.setDocumentId(doc.getId());
                            serviceList.add(service);
                        }
                    }

                    // Update UI based on results
                    if (serviceList.isEmpty()) {
                        binding.emptyView.setVisibility(View.VISIBLE);
                        binding.recyclerStudentHomeServices.setVisibility(View.GONE);
                    } else {
                        binding.emptyView.setVisibility(View.GONE);
                        binding.recyclerStudentHomeServices.setVisibility(View.VISIBLE);
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

        if (servicesListener != null) {
            servicesListener.remove();
            servicesListener = null;
        }
        binding = null;
    }

    /**
     * Safely gets text from an EditText, trims it, and handles nulls.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }
}