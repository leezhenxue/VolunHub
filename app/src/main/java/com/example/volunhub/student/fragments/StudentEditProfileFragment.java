package com.example.volunhub.student.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.volunhub.databinding.FragmentStudentEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StudentEditProfileFragment extends Fragment {

    private static final String TAG = "StudentEditProfileFrag";
    private FragmentStudentEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DocumentReference studentDocRef;

    public StudentEditProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "No user logged in.");
            return;
        }

        studentDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        loadCurrentProfileData();
        binding.buttonSaveStudentProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadCurrentProfileData() {
        studentDocRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                binding.editTextEditStudentName.setText(doc.getString("studentName"));
                binding.editTextEditStudentAge.setText(String.valueOf(doc.getLong("studentAge")));
                binding.editTextEditStudentIntro.setText(doc.getString("studentIntroduction"));
                // TODO: Load profile image
            }
        });
    }

    private void saveProfileChanges() {
        String name = binding.editTextEditStudentName.getText().toString().trim();
        String ageStr = binding.editTextEditStudentAge.getText().toString().trim();
        String intro = binding.editTextEditStudentIntro.getText().toString().trim();

        if (name.isEmpty() || ageStr.isEmpty() || intro.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = 0;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            binding.inputLayoutEditStudentAge.setError("Invalid age");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("studentName", name);
        updates.put("studentAge", age);
        updates.put("studentIntroduction", intro);

        studentDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    // Navigate back to the profile page
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating profile.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating document", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}