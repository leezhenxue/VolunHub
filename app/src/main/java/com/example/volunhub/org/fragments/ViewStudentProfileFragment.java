package com.example.volunhub.org.fragments;

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
    private FragmentViewStudentProfileBinding binding;
    private FirebaseFirestore db;
    private String studentId;

    public ViewStudentProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the student ID from arguments
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
            Log.e("ViewStudentProfile", "No student ID provided");
        }
    }

    private void loadStudentProfileData(String studentId) {
        db.collection("users").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && "student".equals(documentSnapshot.getString("role"))) {
                        // Populate student data
                        binding.textViewStudentName.setText(documentSnapshot.getString("studentName"));

                        // Set age and gender (if available)
                        String age = String.valueOf(documentSnapshot.getLong("studentAge"));
                        String gender = documentSnapshot.getString("gender");
                        if (gender != null && !gender.isEmpty()) {
                            binding.textViewStudentAgeGender.setText(age + ", " + gender);
                        } else {
                            binding.textViewStudentAgeGender.setText(age);
                        }

                        binding.textViewStudentIntro.setText(documentSnapshot.getString("studentIntroduction"));
                        binding.textViewStudentExp.setText(documentSnapshot.getString("volunteerExperience"));

                        // Load profile picture
                        if (getContext() != null) {
                            Glide.with(getContext())
                                    .load(documentSnapshot.getString("profileImageUrl"))
                                    .placeholder(R.drawable.ic_profile)
                                    .centerCrop()
                                    .into(binding.imageViewStudentPhoto);
                        }
                    } else {
                        Log.w("ViewStudentProfile", "Student document not found or not a student");
                    }
                })
                .addOnFailureListener(e -> Log.e("ViewStudentProfile", "Error loading student profile", e));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}