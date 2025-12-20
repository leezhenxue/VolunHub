package com.example.volunhub.student.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentStudentProfileBinding;
import com.example.volunhub.student.StudentHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Displays the Student's profile details.
 * Also handles manual logout via the toolbar menu.
 */
public class StudentProfileFragment extends Fragment {

    private static final String TAG = "StudentProfileFragment";
    private FragmentStudentProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public StudentProfileFragment() {}

    /**
     * Inflates the layout for this fragment.
     * @param inflater LayoutInflater object.
     * @param container Parent view.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes the view, sets up the toolbar menu, and loads profile data.
     * @param view The created view.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupToolbarMenu();
        loadProfileData();

        binding.fabStudentEditProfile.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_student_profile_to_edit_profile);
        });
    }

    /**
     * Manually attaches the logout menu to the Activity's toolbar.
     * Required because this fragment does not rely on setHasOptionsMenu.
     */
    private void setupToolbarMenu() {
        if (getActivity() == null) return;

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);

        if (toolbar != null) {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.toolbar_menu);
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.toolbar_logout) {
                    if (getActivity() instanceof StudentHomeActivity) {
                        ((StudentHomeActivity) getActivity()).returnToMain();
                    }
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Fetches and displays the student's profile data from Firestore.
     */
    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;
        String studentId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {

                        // Name
                        String studentName = documentSnapshot.getString("studentName");
                        binding.textStudentProfileName.setText(
                                (studentName != null) ? studentName : "Student Name"
                        );

                        // Subtitle: Age & Gender
                        Object ageObj = documentSnapshot.get("studentAge");
                        String ageText = (ageObj != null) ? String.valueOf(ageObj) : "N/A";

                        String gender = documentSnapshot.getString("studentGender");
                        if (gender == null) gender = documentSnapshot.getString("gender");
                        String genderText = (gender != null) ? gender : "N/A";

                        binding.textStudentProfileSubtitle.setText(ageText + " Years Old • " + genderText);

                        // Contact Info
                        String email = documentSnapshot.getString("email");
                        binding.textStudentProfileEmail.setText(
                                (email != null) ? email : "No email"
                        );

                        String contact = documentSnapshot.getString("contact");
                        if (contact == null) contact = documentSnapshot.getString("contactNumber");
                        binding.textStudentProfileContact.setText(
                                (contact != null) ? contact : "No contact number"
                        );

                        // About Me
                        String intro = documentSnapshot.getString("studentIntroduction");
                        binding.textStudentProfileIntro.setText(
                                (intro != null && !intro.isEmpty()) ? intro : "No introduction provided."
                        );

                        // Experience
                        String experience = documentSnapshot.getString("studentExperience");
                        if (experience == null) experience = documentSnapshot.getString("volunteerExperience");

                        binding.textStudentProfileExperience.setText(
                                (experience != null && !experience.isEmpty()) ? experience : "No experience listed."
                        );

                        // Profile Picture
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

    /**
     * Cleans up the binding and clears the toolbar menu when leaving the fragment.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (getActivity() != null) {
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.getMenu().clear();
            }
        }
        binding = null;
    }
}