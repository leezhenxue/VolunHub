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

    private static final String TAG = "ViewStudentProfileFrag";
    private FragmentViewStudentProfileBinding binding;
    private FirebaseFirestore db;
    private String studentId;

    public ViewStudentProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the studentId passed from the navigation action
        if (getArguments() != null) {
            // "studentId" MUST match the argument name in your nav graph
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

        if (studentId != null) {
            loadStudentProfile();
        } else {
            Log.e(TAG, "Student ID is null. Cannot load profile.");
            binding.textViewStudentName.setText("Error: Profile not found");
        }
    }

    private void loadStudentProfile() {
        db.collection("users").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate the views
                        binding.textViewStudentName.setText(documentSnapshot.getString("studentName"));

                        String age = String.valueOf(documentSnapshot.getLong("studentAge"));
                        String gender = documentSnapshot.getString("studentGender");
                        binding.textViewStudentAgeGender.setText(age + ", " + gender);

                        binding.textViewStudentIntro.setText(documentSnapshot.getString("studentIntroduction"));
                        binding.textViewStudentExp.setText(documentSnapshot.getString("studentExperience"));

                        // Load profile image using Glide
                        if (getContext() != null) {
                            Glide.with(getContext())
                                    .load(documentSnapshot.getString("profileImageUrl"))
                                    .placeholder(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(binding.imageViewStudentPhoto);
                        }
                    } else {
                        Log.w(TAG, "Student document not found.");
                        binding.textViewStudentName.setText("Student profile not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading student profile", e);
                    binding.textViewStudentName.setText("Error loading profile.");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}