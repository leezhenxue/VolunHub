package com.example.volunhub.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Represents a document in the "services" collection in Firestore.
 * Contains all details about a volunteer opportunity posted by an Organization.
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
    private String searchTitle;
    private String contactNumber;

    @Exclude
    private String documentId;

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    public Service() {}

    public Service(String orgId, String orgName, String title, String description, String requirements,
                   long volunteersNeeded, long volunteersApplied, Date serviceDate, Date createdAt,
                   String status, String searchTitle, String contactNumber) {
        this.orgId = orgId;
        this.orgName = orgName;
        this.title = title;
        this.description = description;
        this.requirements = requirements;
        this.volunteersNeeded = volunteersNeeded;
        this.volunteersApplied = volunteersApplied;
        this.serviceDate = serviceDate;
        this.createdAt = createdAt;
        this.status = status;
        this.searchTitle = searchTitle;
        this.contactNumber = contactNumber;
    }

    public Service(String title, String description, String contactNumber) {
        this.title = title;
        this.description = description;
        this.contactNumber = contactNumber;
    }

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
    public String getContactNumber() { return contactNumber; }
    public String getSearchTitle() { return searchTitle; }
    @Exclude
    public String getDocumentId() { return documentId; }

    // --- Setters ---
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}