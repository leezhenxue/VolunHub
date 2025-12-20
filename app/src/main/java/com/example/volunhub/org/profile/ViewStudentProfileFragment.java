package com.example.volunhub.org.profile;

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

/**
 * Allows the Organization to view the detailed profile of a Student applicant.
 */
public class ViewStudentProfileFragment extends Fragment {

    private static final String TAG = "ViewStudentProfile";
    private FragmentViewStudentProfileBinding binding;
    private FirebaseFirestore db;
    private String studentId;

    public ViewStudentProfileFragment() {}

    /**
     * Retrieves the student ID passed in the arguments.
     *
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
        }
    }

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater The LayoutInflater object.
     * @param container The parent view.
     * @param savedInstanceState Saved state bundle.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentViewStudentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes the view and loads the student's profile data.
     *
     * @param view The created view.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (studentId != null && !studentId.isEmpty()) {
            loadStudentProfileData(studentId);
        } else {
            binding.textViewStudentName.setText(R.string.error_profile_not_found);
        }
    }

    /**
     * Fetches and displays the student's data from Firestore.
     *
     * @param studentId The UID of the student to load.
     */
    private void loadStudentProfileData(String studentId) {
        db.collection("users").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        binding.textViewStudentName.setText(documentSnapshot.getString("studentName"));

                        // Safe Age Handling
                        String ageText;
                        try {
                            Object ageObj = documentSnapshot.get("studentAge");
                            ageText = (ageObj != null) ? String.valueOf(ageObj) : "N/A";
                        } catch (Exception e) {
                            ageText = "N/A";
                        }

                        String gender = documentSnapshot.getString("studentGender");
                        String genderText = (gender != null) ? gender : "N/A";
                        binding.textViewStudentAgeGender.setText(ageText + " Years Old • " + genderText);

                        String email = documentSnapshot.getString("email");
                        binding.textViewStudentEmail.setText(email != null ? email : getString(R.string.not_available));

                        String phone = documentSnapshot.getString("contactNumber");
                        binding.textViewStudentPhone.setText(phone != null ? phone : getString(R.string.not_available));

                        String intro = documentSnapshot.getString("studentIntroduction");
                        binding.textViewStudentIntro.setText(intro != null ? intro : "No introduction provided.");

                        String exp = documentSnapshot.getString("studentExperience");
                        binding.textViewStudentExp.setText(exp != null ? exp : "No experience listed.");

                        if (getContext() != null) {
                            Glide.with(getContext())
                                    .load(documentSnapshot.getString("profileImageUrl"))
                                    .placeholder(R.drawable.default_profile_picture)
                                    .centerCrop()
                                    .into(binding.imageViewStudentPhoto);
                        }
                    } else {
                        binding.textViewStudentName.setText(R.string.error_student_profile_not_found);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading student profile", e);
                    if (binding != null) binding.textViewStudentName.setText(R.string.error_loading_profile);
                });
    }

    /**
     * Cleans up the binding when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}