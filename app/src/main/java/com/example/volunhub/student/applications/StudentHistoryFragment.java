package com.example.volunhub.student.applications;

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

import com.example.volunhub.databinding.FragmentStudentHistoryBinding;
import com.example.volunhub.models.Application;
import com.example.volunhub.student.adapters.StudentApplicationAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Fragment that displays a history of completed volunteer services.
 * Filters for applications that were accepted and whose service date has passed.
 */
public class StudentHistoryFragment extends Fragment {

    private static final String TAG = "StudentHistoryFragment";
    private FragmentStudentHistoryBinding binding;
    private StudentApplicationAdapter adapter;
    private final List<Application> historyList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration historyListener;

    public StudentHistoryFragment() {}

    /**
     * Inflates the layout for this fragment using ViewBinding.
     * @param inflater The LayoutInflater object.
     * @param container The parent view group.
     * @param savedInstanceState Saved state bundle.
     * @return The root View of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes Firestore, Auth, and triggers UI setup after the view is created.
     * @param view The created View.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadHistory();
    }

    /**
     * Configures the RecyclerView and handles navigation to service details when an item is clicked.
     */
    private void setupRecyclerView() {
        adapter = new StudentApplicationAdapter(getContext(), historyList);
        binding.recyclerStudentHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerStudentHistory.setAdapter(adapter);

        adapter.setOnItemClickListener(application -> {
            // Navigation via the Host Fragment's Directions
            StudentApplicationsFragmentDirections.ActionAppsHostToServiceDetail action =
                    StudentApplicationsFragmentDirections.actionAppsHostToServiceDetail(
                            application.getServiceId()
                    );
            Navigation.findNavController(requireView()).navigate(action);
        });
    }

    /**
     * Attaches a real-time listener to Firestore to fetch accepted applications from the past.
     * Sorts the results by date in descending order (most recent first).
     */
    private void loadHistory() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // Cleanup existing listener to prevent leaks or duplicates
        if (historyListener != null) {
            historyListener.remove();
        }

        historyListener = db.collection("applications")
                .whereEqualTo("studentId", myId)
                .whereEqualTo("status", "Accepted")
                .whereLessThan("serviceDate", Timestamp.now()) // Only services in the past
                .orderBy("serviceDate", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to history", e);
                        if (binding != null) binding.textEmptyHistory.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (binding == null) return;

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.d(TAG, "No history found.");
                        historyList.clear();
                        adapter.notifyDataSetChanged();
                        binding.textEmptyHistory.setVisibility(View.VISIBLE);
                    } else {
                        binding.textEmptyHistory.setVisibility(View.GONE);
                        historyList.clear();
                        historyList.addAll(querySnapshot.toObjects(Application.class));

                        // Manual sort to ensure consistency with Date objects
                        Collections.sort(historyList, (a1, a2) -> {
                            Date d1 = a1.getServiceDate();
                            Date d2 = a2.getServiceDate();

                            if (d1 == null && d2 == null) return 0;
                            if (d1 == null) return 1;
                            if (d2 == null) return -1;

                            return d2.compareTo(d1); // Descending order
                        });

                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * Removes the Firestore listener and cleans up the UI binding reference.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (historyListener != null) {
            historyListener.remove();
        }
        binding = null;
    }
}