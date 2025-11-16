package com.example.volunhub.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

// This POJO class MUST match your "applications" collection fields
public class Application {
    private String orgId;
    private String orgName;
    private String serviceId;
    private String serviceTitle;
    private String status;
    private String studentId;
    @ServerTimestamp
    private Date appliedAt;

    // Required empty constructor for Firestore
    public Application() {}

    // --- Getters ---
    public String getOrgId() { return orgId; }
    public String getOrgName() { return orgName; }
    public String getServiceId() { return serviceId; }
    public String getServiceTitle() { return serviceTitle; }
    public String getStatus() { return status; }
    public String getStudentId() { return studentId; }
    public Date getAppliedAt() { return appliedAt; }
}