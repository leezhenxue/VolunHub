package com.example.volunhub.org.fragments.service;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentViewStudentProfileBinding;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewStudentProfileFragment extends Fragment {

    private static final String TAG = "ViewStudentProfile";
    private FragmentViewStudentProfileBinding binding;
    private FirebaseFirestore db;
    private String studentId;

    public ViewStudentProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the student ID passed from the previous screen
        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentViewStudentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (studentId != null && !studentId.isEmpty()) {
            loadStudentProfileData(studentId);
        } else {
            Log.e(TAG, "No student ID provided");
            binding.textViewStudentName.setText(getString(R.string.error_profile_not_found));
        }
    }

    private void loadStudentProfileData(String studentId) {
        db.collection("users").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        // 1. Basic Info
                        binding.textViewStudentName.setText(documentSnapshot.getString("studentName"));

                        // Safe Age Handling: Prevents crash if age is number or string
                        String ageText;
                        try {
                            Object ageObj = documentSnapshot.get("studentAge");
                            ageText = (ageObj != null) ? String.valueOf(ageObj) : "N/A";
                        } catch (Exception e) {
                            ageText = "N/A";
                        }

                        // Combine Age & Gender
                        String gender = documentSnapshot.getString("studentGender");
                        String genderText = (gender != null && !gender.isEmpty()) ? gender : "N/A";
                        binding.textViewStudentAgeGender.setText(ageText + " Years Old • " + genderText);

                        // 2. Contact Info (Matches your new XML Redesign)
                        String email = documentSnapshot.getString("email");
                        binding.textViewStudentEmail.setText(
                                (email != null && !email.isEmpty()) ? email : "No email provided"
                        );

                        String phone = documentSnapshot.getString("contactNumber");
                        binding.textViewStudentPhone.setText(
                                (phone != null && !phone.isEmpty()) ? phone : "No contact number"
                        );

                        // 3. Details
                        String intro = documentSnapshot.getString("studentIntroduction");
                        binding.textViewStudentIntro.setText(
                                (intro != null && !intro.isEmpty()) ? intro : "No introduction provided."
                        );

                        String exp = documentSnapshot.getString("studentExperience");
                        binding.textViewStudentExp.setText(
                                (exp != null && !exp.isEmpty()) ? exp : "No experience listed."
                        );

                        // 4. Load Profile Picture
                        if (getContext() != null) {
                            Glide.with(getContext())
                                    .load(documentSnapshot.getString("profileImageUrl"))
                                    .placeholder(R.drawable.default_profile_picture)
                                    .centerCrop()
                                    .into(binding.imageViewStudentPhoto);
                        }
                    } else {
                        Log.w(TAG, "Student document not found");
                        binding.textViewStudentName.setText("Student not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading student profile", e);
                    binding.textViewStudentName.setText("Error loading data");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}