package com.example.volunhub.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Represents a document in the "services" collection.
 * Contains details about a volunteer opportunity posted by an Organization.
 */
public class Service {
    private String orgId;
    private String orgName;
    private String title;
    private String description;
    private String requirements;
    private long volunteersNeeded;
    private long volunteersApplied;
    private Date serviceDate;
    @ServerTimestamp
    private Date createdAt;
    private String status;
    @Exclude
    private String documentId;
    private String searchTitle;

    // Empty constructor for Firestore
    public Service() {}

    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // --- Getters ---
    public String getOrgId() { return orgId; }
    public String getOrgName() { return orgName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRequirements() { return requirements; }
    public long getVolunteersNeeded() { return volunteersNeeded; }
    public long getVolunteersApplied() { return volunteersApplied; }
    public Date getServiceDate() { return serviceDate; }
    public Date getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
}