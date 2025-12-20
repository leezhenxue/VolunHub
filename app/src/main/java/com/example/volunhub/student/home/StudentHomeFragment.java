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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The main dashboard for students.
 * Displays a list of available volunteer services with pagination and search functionality.
 */
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
    private static final long PAGE_SIZE = 10;
    private DocumentSnapshot lastVisibleDocument;
    private boolean isSearchActive = false;

    public StudentHomeFragment() {}

    /**
     * Inflates the layout for this fragment.
     * @param inflater LayoutInflater object.
     * @param container Parent view.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes the view, sets up RecyclerView, and loads initial data.
     * @param view The created view.
     * @param savedInstanceState Saved state bundle.
     */
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

    /**
     * Configures the RecyclerView with a layout manager, adapter, and scroll listener for pagination.
     */
    private void setupRecyclerView() {
        adapter = new ServiceAdapter(serviceList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.recyclerStudentHomeServices.setLayoutManager(layoutManager);
        binding.recyclerStudentHomeServices.setAdapter(adapter);

        // Click Listener: Navigate to Service Detail
        adapter.setOnItemClickListener(service -> {
            Log.d(TAG, "Clicked on service: " + service.getTitle());
            StudentHomeFragmentDirections.ActionHomeToServiceDetail action =
                    StudentHomeFragmentDirections.actionHomeToServiceDetail(
                            service.getDocumentId()
                    );
            navController.navigate(action);
        });

        // Scroll Listener: Trigger loadMoreServices when near bottom
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

    /**
     * Sets up the search bar to filter services in real-time.
     */
    private void setupSearch() {
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

    /**
     * Loads the first page of active services from Firestore.
     * Captures performance metrics for NFR testing.
     */
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
                    if (binding == null) return;

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        Log.d("NFRTest", "P1 - List Loaded (Empty). Duration: " + duration + "ms");

                        int count = (querySnapshot == null) ? 0 : querySnapshot.size();
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

                    // [NFR 1 & 7 TEST] CAPTURE EVIDENCE HERE
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    int count = querySnapshot.size();

                    Log.d("NFRTest", "P1 - List Rendered Successfully. Duration: " + duration + "ms");
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

    /**
     * Loads the next batch of services (Pagination) when scrolling to the bottom.
     */
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

                    // [NFR 7 TEST] PROOF OF PAGINATION (PAGE 2)
                    int count = querySnapshot.size();
                    Log.d("NFRTest", "SC1 - Pagination: Next Batch Loaded. Count: " + count + " items.");

                    lastVisibleDocument = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading more services", e);
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    /**
     * Performs a Firestore query to find services matching the search text.
     * @param searchText The text entered by the user.
     */
    private void searchServicesFirestore(String searchText) {
        Log.d(TAG, "Searching for: " + searchText);
        isLoading = true;
        lastVisibleDocument = null;
        binding.progressBar.setVisibility(View.VISIBLE);

        String queryText = searchText.toLowerCase();
        Timestamp now = Timestamp.now();

        db.collection("services")
                .whereEqualTo("status", "Active")
                .orderBy("searchTitle")
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

                    // Filter future events locally since complex queries are limited
                    for (DocumentSnapshot d : docs) {
                        Timestamp serviceTimestamp = d.getTimestamp("serviceDate");
                        if (serviceTimestamp != null && serviceTimestamp.compareTo(now) >= 0) {
                            filtered.add(d);
                        }
                    }

                    // Sort by service date (earliest first)
                    Collections.sort(filtered, (d1, d2) -> {
                        Timestamp t1 = d1.getTimestamp("serviceDate");
                        Timestamp t2 = d2.getTimestamp("serviceDate");

                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return t1.compareTo(t2);
                    });

                    for (DocumentSnapshot doc : filtered) {
                        Service service = doc.toObject(Service.class);
                        if (service != null) {
                            service.setDocumentId(doc.getId());
                            serviceList.add(service);
                        }
                    }

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

    /**
     * Cleans up the binding and listeners when the view is destroyed.
     */
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
     * @param editable The editable object from the EditText.
     * @return The trimmed string or empty string.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }
}