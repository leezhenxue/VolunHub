package com.example.volunhub.student.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentStudentProfileBinding;
import com.example.volunhub.student.StudentHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentProfileFragment extends Fragment {

    private static final String TAG = "StudentProfileFragment";
    private FragmentStudentProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public StudentProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupLogoutMenu();
        loadProfileData();

        binding.fabStudentEditProfile.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_student_profile_to_edit_profile);
        });
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        String studentId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String studentName = documentSnapshot.getString("studentName");
                        String email = documentSnapshot.getString("email");
                        String intro = documentSnapshot.getString("studentIntroduction");

                        if (studentName != null) {
                            binding.textStudentProfileName.setText(studentName);
                        }

                        if (email != null) {
                            binding.textStudentProfileEmail.setText(email);
                        }

                        if (intro != null) {
                            binding.textStudentProfileIntro.setText(intro);
                        }

                        Object ageObj = documentSnapshot.get("studentAge");
                        String gender = documentSnapshot.getString("gender");

                        if (ageObj != null) {
                            String ageText = "Age: " + ageObj.toString();
                            binding.textStudentProfileAge.setText(ageText);
                        } else {
                            binding.textStudentProfileAge.setText("Age: Not specified");
                        }

                        if (gender != null && !gender.trim().isEmpty()) {
                            String genderText = "Gender: " + gender;
                            binding.textStudentProfileGender.setText(genderText);
                        } else {
                            binding.textStudentProfileGender.setText("Gender: Not specified");
                        }

                        String contact = documentSnapshot.getString("contactNumber");
                        if (contact != null && !contact.trim().isEmpty()) {
                            binding.textStudentProfileContact.setText("Contact: " + contact);
                            binding.textStudentProfileContact.setVisibility(View.VISIBLE);
                        } else {
                            binding.textStudentProfileContact.setVisibility(View.GONE);
                        }

                        String experience = documentSnapshot.getString("volunteerExperience");
                        if (experience != null && !experience.trim().isEmpty()) {
                            binding.textStudentProfileExperience.setText(experience);
                        } else {
                            binding.textStudentProfileExperience.setText("No volunteer experience yet.");
                        }

                        if (getContext() != null) {
                            String imageUrl = documentSnapshot.getString("profileImageUrl");
                            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                                Glide.with(getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_profile)
                                        .circleCrop()
                                        .into(binding.imageStudentProfilePicture);
                            } else {
                                binding.imageStudentProfilePicture.setImageResource(R.drawable.ic_profile);
                            }
                        }
                    } else {
                        Log.w(TAG, "Student document not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading student profile", e));
    }

    private void setupLogoutMenu() {
        MenuHost menuHost = requireActivity();
        LifecycleOwner lifecycleOwner = getViewLifecycleOwner();

        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.toolbar_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull android.view.MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.toolbar_logout) {
                    if (getActivity() != null) {
                        ((StudentHomeActivity) getActivity()).returnToMain();
                    }
                    return true;
                }
                return false;
            }
        }, lifecycleOwner, Lifecycle.State.RESUMED);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}