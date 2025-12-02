package com.example.volunhub.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Represents a document in the "applications" collection.
 * Links a Student to a Service with a status (Pending/Accepted/Rejected).
 */
public class Application {
    private String orgId;
    private String orgName;
    private String serviceId;
    private String serviceTitle;
    private String status;
    private String studentId;
    @ServerTimestamp
    private Date appliedAt;
    @Exclude
    private String documentId;
    @Exclude
    private boolean serviceRemoved = false; // Flag to mark if the service has been deleted

    // Empty constructor for Firestore
    public Application() {}

    // --- Getters ---
    public String getOrgId() { return orgId; }
    public String getOrgName() { return orgName; }
    public String getServiceId() { return serviceId; }
    public String getServiceTitle() { return serviceTitle; }
    public String getStatus() { return status; }
    public String getStudentId() { return studentId; }
    public Date getAppliedAt() { return appliedAt; }
    @Exclude
    public String getDocumentId() { return documentId; }
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    @Exclude
    public boolean isServiceRemoved() { return serviceRemoved; }
    @Exclude
    public void setServiceRemoved(boolean serviceRemoved) { this.serviceRemoved = serviceRemoved; }
}