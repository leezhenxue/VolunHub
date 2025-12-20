package com.example.volunhub.models;

import com.google.firebase.Timestamp;

/**
 * A simple model to represent a single activity item in the Organization's dashboard.
 * This is used for display purposes in the "Recent Activity" list in OrgDashboardFragment.
 */
public class RecentActivity {
    private String message;
    private Timestamp timestamp;

    public RecentActivity(String message, Timestamp timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    // --- Getters ---
    public String getMessage() { return message; }
    public Timestamp getTimestamp() { return timestamp; }
}