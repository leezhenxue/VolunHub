package com.example.volunhub.models;

/**
 * A custom model class that combines data from the "applications" collection and the "users" collection.
 * This is used by the Organization to view a Student Applicant.
 * It is NOT saved directly to Firestore as a single document.
 */
public class Applicant {

    // From the "applications" document
    private String applicationId;
    private String studentId;

    // From the "users" document
    private String studentName;
    private String studentIntroduction;
    private String profileImageUrl;

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
