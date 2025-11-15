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

        setupLogoutMenu(); // Setup the logout button
        loadProfileData(); // Load the student's info

        // Set the click listener for the "Edit" FAB
        binding.fabStudentEditProfile.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            // This action must be added to your student_nav_graph.xml
            navController.navigate(R.id.action_student_profile_to_edit_profile);
        });
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        String studentId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate the views
                        binding.textStudentProfileName.setText(documentSnapshot.getString("studentName"));
                        binding.textStudentProfileEmail.setText(documentSnapshot.getString("email"));
                        binding.textStudentProfileIntro.setText(documentSnapshot.getString("studentIntroduction"));

                        // Load profile image
                        if (getContext() != null) {
                            Glide.with(getContext())
                                    .load(documentSnapshot.getString("profileImageUrl"))
                                    .placeholder(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(binding.imageStudentProfilePicture);
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