package com.example.volunhub.models;

// This class will hold info from BOTH the application and the user document
public class Applicant {

    // From the "applications" document
    private String applicationId; // The ID of the application document
    private String studentId;

    // From the "users" document
    private String studentName;
    private String studentIntroduction;
    private String profileImageUrl;
    // You can add more fields here like 'studentExperience'

    // Empty constructor for Firestore
    public Applicant() {}

    // --- Getters ---
    public String getApplicationId() { return applicationId; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentIntroduction() { return studentIntroduction; }
    public String getProfileImageUrl() { return profileImageUrl; }

    // --- Setters ---
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setStudentIntroduction(String studentIntroduction) { this.studentIntroduction = studentIntroduction; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}
