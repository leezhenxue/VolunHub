package com.example.volunhub.org.models;

import com.google.firebase.Timestamp;

public class RecentActivity {
    private String message;
    private Timestamp timestamp;

    public RecentActivity(String message, Timestamp timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
