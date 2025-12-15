package com.example.volunhub.org.fragments.service;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
        setHasOptionsMenu(true);
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

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            // Qimin: Hooking up the existing Toolbar back arrow
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);

            Toolbar toolbar = activity.findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setNavigationOnClickListener(v -> {
                    // Qimin: handling back button
                    Log.d("Qimin_Nav", "Back button clicked");
                    getParentFragmentManager().popBackStack();
                });
            } else {
                Log.d("Qimin_Nav", "Using standard ActionBar back behavior");
            }
        }

        if (studentId != null) {
            loadStudentProfile();
        } else {
            Log.e(TAG, "Student ID is null. Cannot load profile.");
            binding.textViewStudentName.setText(getString(R.string.error_profile_not_found));
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Qimin: handling back button via ActionBar home
            Log.d("Qimin_Nav", "ActionBar home pressed");
            getParentFragmentManager().popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadStudentProfile() {
        db.collection("users").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate the views
                        binding.textViewStudentName.setText(documentSnapshot.getString("studentName"));

                        String gender = documentSnapshot.getString("studentGender");
                        String ageText;
                        try {
                            // Qimin: Fixed crash by reading age as String
                            Object ageObj = documentSnapshot.get("studentAge");
                            if (ageObj != null) {
                                ageText = String.valueOf(ageObj);
                            } else {
                                ageText = getString(R.string.not_available);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to read studentAge safely", e);
                            ageText = getString(R.string.not_available);
                        }
                        String genderText = gender != null ? gender : getString(R.string.not_available);
                        binding.textViewStudentAgeGender.setText(ageText + ", " + genderText);

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
                        binding.textViewStudentName.setText(getString(R.string.error_student_profile_not_found));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading student profile", e);
                    binding.textViewStudentName.setText(getString(R.string.error_loading_profile));
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}