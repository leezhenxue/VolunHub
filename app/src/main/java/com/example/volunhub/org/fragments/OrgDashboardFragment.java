package com.example.volunhub.org.fragments;

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
import com.example.volunhub.org.models.RecentActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TimeZone;

public class OrgDashboardFragment extends Fragment {

    private static final String TAG = "OrgDashboardFragment";
    private FragmentOrgDashboardBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ArrayList<RecentActivity> activityList;
    private RecentActivityAdapter activityAdapter;

    public OrgDashboardFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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

        // Load all dashboard sections
        loadQuickStats();
        loadUpcomingEvent();
        loadRecentActivity();

        binding.fabOrgPostService.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_org_dashboard_to_post_service);
        });
    }

        private void loadQuickStats() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        // Pending applications
        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(snap -> binding.textStatsPending.setText(String.valueOf(snap.size())));

        // Total active services
        db.collection("services")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(snap -> binding.textStatsJobs.setText(String.valueOf(snap.size())));

        // Total volunteers applied
        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .get()
                .addOnSuccessListener(snap -> binding.textStatsVolunteers.setText(String.valueOf(snap.size())));
    }

    private void loadUpcomingEvent() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        db.collection("services")
                .whereEqualTo("orgId", orgId)
                .whereEqualTo("status", "Active")
                .whereGreaterThan("serviceDate", Timestamp.now())  // FIX
                .orderBy("serviceDate", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        binding.textUpcomingEvent.setText("No upcoming events");
                        return;
                    }

                    var doc = snap.getDocuments().get(0);
                    Timestamp date = doc.getTimestamp("serviceDate");
                    String title = doc.getString("title");

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy (hh:mm a)");
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                    String formatted = sdf.format(date.toDate());

                    binding.textUpcomingEvent.setText(title + " – " + formatted);
                })
                .addOnFailureListener(e -> {
                    binding.textUpcomingEvent.setText("Unable to load events");
                    Log.e(TAG, "Upcoming event error", e);
                });
    }


    private void loadRecentActivity() {
        if (mAuth.getCurrentUser() == null) return;
        String orgId = mAuth.getCurrentUser().getUid();

        activityList.clear();

        db.collection("applications")
                .whereEqualTo("orgId", orgId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snap, error) -> {
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

                    // Load service activity next
                    loadServiceActivity(orgId);
                });
    }


    private void loadServiceActivity(String orgId) {
        db.collection("services")
                .whereEqualTo("orgId", orgId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(serviceSnap -> {

                    for (var doc : serviceSnap.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("createdAt");
                        if (ts == null) ts = Timestamp.now();

                        activityList.add(new RecentActivity(
                                "New Service Posted: " + doc.getString("title"),
                                ts
                        ));
                    }

                    // Sort newest → oldest
                    Collections.sort(activityList,
                            Comparator.comparing(RecentActivity::getTimestamp).reversed()
                    );

                    activityAdapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
