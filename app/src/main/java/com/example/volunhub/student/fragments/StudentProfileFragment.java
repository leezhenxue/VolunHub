package com.example.volunhub.student.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.fragment.app.Fragment;
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

        // 1. Setup Manual Logout Menu
        setupToolbarMenu();

        loadProfileData();

        binding.fabStudentEditProfile.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_student_profile_to_edit_profile);
        });
    }

    // --- Manual Toolbar Menu Setup ---
    private void setupToolbarMenu() {
        if (getActivity() == null) return;

        // Find the toolbar from the StudentHomeActivity layout
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);

        if (toolbar != null) {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.toolbar_menu);
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.toolbar_logout) {
                    // Call returnToMain() from StudentHomeActivity
                    if (getActivity() instanceof StudentHomeActivity) {
                        ((StudentHomeActivity) getActivity()).returnToMain();
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        String studentId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {

                        // 1. Name
                        String studentName = documentSnapshot.getString("studentName");
                        binding.textStudentProfileName.setText(
                                (studentName != null) ? studentName : "Student Name"
                        );

                        // 2. Subtitle: Age & Gender
                        Object ageObj = documentSnapshot.get("studentAge");
                        String ageText = (ageObj != null) ? String.valueOf(ageObj) : "N/A";

                        String gender = documentSnapshot.getString("studentGender"); // Check if DB uses "gender" or "studentGender"
                        if (gender == null) gender = documentSnapshot.getString("gender"); // Fallback
                        String genderText = (gender != null) ? gender : "N/A";

                        binding.textStudentProfileSubtitle.setText(ageText + " Years Old • " + genderText);

                        // 3. Contact Info
                        String email = documentSnapshot.getString("email");
                        binding.textStudentProfileEmail.setText(
                                (email != null) ? email : "No email"
                        );

                        String contact = documentSnapshot.getString("contact");
                        if (contact == null) contact = documentSnapshot.getString("contactNumber");
                        binding.textStudentProfileContact.setText(
                                (contact != null) ? contact : "No contact number"
                        );

                        // 4. About Me
                        String intro = documentSnapshot.getString("studentIntroduction");
                        binding.textStudentProfileIntro.setText(
                                (intro != null && !intro.isEmpty()) ? intro : "No introduction provided."
                        );

                        // 5. Experience
                        String experience = documentSnapshot.getString("studentExperience");
                        // Fallback if key is different in your DB
                        if (experience == null) experience = documentSnapshot.getString("volunteerExperience");

                        binding.textStudentProfileExperience.setText(
                                (experience != null && !experience.isEmpty()) ? experience : "No experience listed."
                        );

                        // 6. Profile Picture
                        if (getContext() != null) {
                            String imageUrl = documentSnapshot.getString("profileImageUrl");
                            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                                Glide.with(getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.default_profile_picture)
                                        .centerCrop()
                                        .into(binding.imageStudentProfilePicture);
                            } else {
                                binding.imageStudentProfilePicture.setImageResource(R.drawable.default_profile_picture);
                            }
                        }
                    } else {
                        Log.w(TAG, "Student document not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading student profile", e));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up toolbar menu when leaving this fragment
        if (getActivity() != null) {
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.getMenu().clear();
            }
        }

        binding = null;
    }
}