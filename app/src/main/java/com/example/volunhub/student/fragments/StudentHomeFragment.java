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

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.util.Date;
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
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_START_TIME = "startDateTime";
    private static final String FIELD_SEARCH_TITLE = "searchTitle";
    private static final String FIELD_SERVICE_DATE = "serviceDate";
    private ListenerRegistration servicesListener;



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
        listenInitialServicesRealtime();  // Corrected to call loadInitialServices
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

        Date now = new Date();

        db.collection("services")
                .whereEqualTo(FIELD_STATUS, "Active")
                .whereGreaterThanOrEqualTo(FIELD_SERVICE_DATE, now)
                .orderBy(FIELD_SERVICE_DATE, Query.Direction.ASCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
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

        Date now = new Date();

        db.collection("services")
                .whereEqualTo("status", "Active")
                .whereGreaterThanOrEqualTo("serviceDate", now)
                .orderBy("serviceDate", Query.Direction.ASCENDING)
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
                .whereEqualTo(FIELD_STATUS, "Active")
                .orderBy(FIELD_SEARCH_TITLE)
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
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
                    for (DocumentSnapshot d : docs) {
                        Timestamp start = d.getTimestamp(FIELD_START_TIME);
                        if (start != null && start.compareTo(now) >= 0) {
                            filtered.add(d);
                        }
                    }

                    // 按开始时间升序排序
                    Collections.sort(filtered, (d1, d2) -> {
                        Timestamp t1 = d1.getTimestamp(FIELD_START_TIME);
                        Timestamp t2 = d2.getTimestamp(FIELD_START_TIME);
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

    private void listenInitialServicesRealtime() {
        Log.d(TAG, "Listening initial services (realtime)...");
        binding.progressBar.setVisibility(View.VISIBLE);

        // "now" is used to hide past services (only show services with serviceDate >= now)
        Date now = new Date();

        // IMPORTANT: Remove old listener before creating a new one
        // Otherwise you may have multiple listeners running at the same time
        if (servicesListener != null) {
            servicesListener.remove();
            servicesListener = null;
        }

        // Realtime query:
        // 1) Only Active services
        // 2) Only future services (serviceDate >= now)
        // 3) Sort by serviceDate ascending (earliest upcoming at the top)
        // 4) Limit for pagination (first page)
        Query query = db.collection("services")
                .whereEqualTo("status", "Active")
                .whereGreaterThanOrEqualTo("serviceDate", now)
                .orderBy("serviceDate", Query.Direction.ASCENDING)
                .limit(PAGE_SIZE);

        // Use snapshot listener to get realtime updates:
        // - If someone edits serviceDate/status in Firebase Console, UI updates automatically
        // - If a document no longer matches (e.g., status changes), it disappears automatically
        servicesListener = query.addSnapshotListener((querySnapshot, e) -> {
            binding.progressBar.setVisibility(View.GONE);

            if (e != null) {
                Log.e(TAG, "Realtime listen failed", e);
                return;
            }

            // If no matching services, show empty state
            if (querySnapshot == null || querySnapshot.isEmpty()) {
                serviceList.clear();
                adapter.notifyDataSetChanged();

                binding.emptyView.setVisibility(View.VISIBLE);
                binding.recyclerStudentHomeServices.setVisibility(View.GONE);

                lastVisibleDocument = null;
                return;
            }

            // If we have results, show RecyclerView and hide empty state
            binding.emptyView.setVisibility(View.GONE);
            binding.recyclerStudentHomeServices.setVisibility(View.VISIBLE);

            // Replace the list with latest snapshot data
            serviceList.clear();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Service service = doc.toObject(Service.class);
                if (service != null) {
                    // Store Firestore docId for navigation/details
                    service.setDocumentId(doc.getId());
                    serviceList.add(service);
                }
            }

            // Refresh UI
            adapter.notifyDataSetChanged();

            // Save last doc for "load more" pagination
            lastVisibleDocument = querySnapshot.getDocuments()
                    .get(querySnapshot.size() - 1);
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