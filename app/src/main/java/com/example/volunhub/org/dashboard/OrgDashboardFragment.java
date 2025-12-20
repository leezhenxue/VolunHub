package com.example.volunhub.org.dashboard;

import android.os.Bundle;
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

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgDashboardBinding;
import com.example.volunhub.org.adapters.RecentActivityAdapter;
import com.example.volunhub.models.RecentActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The main dashboard for the Organization.
 * Displays quick stats, upcoming events, and a feed of recent activities.
 */
public class OrgDashboardFragment extends Fragment {

    private static final String TAG = "OrgDashboardFragment";
    private FragmentOrgDashboardBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ArrayList<RecentActivity> activityList;
    private RecentActivityAdapter activityAdapter;

    public OrgDashboardFragment() {}

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return Return the View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes the dashboard components and loads data after the view is created.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup Recent Activity Recycler
        activityList = new ArrayList<>();
        activityAdapter = new RecentActivityAdapter(activityList);
        binding.recyclerRecentActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerRecentActivity.setAdapter(activityAdapter);

        // Load dashboard data
        loadQuickStats();
        loadUpcomingEvent();
        loadRecentActivity();

        // Navigate to "Post Service" screen
        binding.fabOrgPostService.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_org_dashboard_to_post_service);
        });
    }

    /**
     * Loads numeric stats: Pending Applications, Active Jobs, Total Volunteers Applied.
     */
    private void loadQuickStats() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        // Count Pending applications
        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(snap -> {
                    if (binding == null) return;
                    binding.textStatsPending.setText(String.valueOf(snap.size()));
                });

        // Count Active services
        db.collection("services")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(snap -> {
                    if (binding == null) return;
                    binding.textStatsJobs.setText(String.valueOf(snap.size()));
                });

        // Count Total volunteers
        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (binding == null) return;
                    binding.textStatsVolunteers.setText(String.valueOf(snap.size()));
                });
    }

    /**
     * Loads the next upcoming service event based on date.
     */
    private void loadUpcomingEvent() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        db.collection("services")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "Active")
                .whereGreaterThan("serviceDate", Timestamp.now())
                .orderBy("serviceDate", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (binding == null) return;
                    if (snap.isEmpty()) {
                        binding.textUpcomingEvent.setText(R.string.dashboard_no_upcoming_event);
                        return;
                    }

                    var doc = snap.getDocuments().get(0);
                    Timestamp date = doc.getTimestamp("serviceDate");
                    String title = doc.getString("title");

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy (hh:mm a)", Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));

                    String formatted = (date != null) ? sdf.format(date.toDate()) : "";
                    binding.textUpcomingEvent.setText(title + " – " + formatted);
                })
                .addOnFailureListener(e -> {
                    if (binding != null) binding.textUpcomingEvent.setText(R.string.error_loading_data);
                    Log.e(TAG, "Upcoming event error", e);
                });
    }

    /**
     * Loads recent applications and recent service posts, combines them, and sorts by time.
     */
    private void loadRecentActivity() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        activityList.clear();

        // 1. Get recent applications
        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snap, error) -> {
                    if (binding == null) return;
                    if (error != null || snap == null) {
                        Log.e(TAG, "Error loading recent activity", error);
                        return;
                    }

                    activityList.clear();

                    for (var doc : snap.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("appliedAt");
                        if (ts == null) ts = Timestamp.now();

                        String title = doc.getString("serviceTitle");
                        String status = doc.getString("status");

                        activityList.add(new RecentActivity(
                                "Application: " + title + " (" + status + ")",
                                ts
                        ));
                    }

                    // 2. Load service postings next
                    loadServiceActivity(orgId);
                });
    }

    /**
     * Helper method to load service posting activities and update the adapter.
     *
     * @param orgId The Organization ID to filter services.
     */
    private void loadServiceActivity(String orgId) {
        db.collection("services")
                .whereEqualTo("orgId", orgId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(serviceSnap -> {
                    if (binding == null) return;

                    for (var doc : serviceSnap.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("createdAt");
                        if (ts == null) ts = Timestamp.now();

                        activityList.add(new RecentActivity(
                                "New Service Posted: " + doc.getString("title"),
                                ts
                        ));
                    }

                    // Sort combined list: Newest → Oldest
                    Collections.sort(activityList,
                            Comparator.comparing(RecentActivity::getTimestamp).reversed()
                    );

                    activityAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Cleans up the binding when the view is destroyed to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}