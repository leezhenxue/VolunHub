package com.example.volunhub.student.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.volunhub.R;
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
            return;
        }

        studentDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        setupGenderDropdown();
        loadCurrentProfileData();
        binding.buttonSaveStudentProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void setupGenderDropdown() {
        String[] genderOptions = getResources().getStringArray(R.array.gender_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                genderOptions
        );
        binding.autoCompleteEditStudentGender.setAdapter(adapter);
        binding.autoCompleteEditStudentGender.setKeyListener(null);
        binding.autoCompleteEditStudentGender.setOnClickListener(v -> {
            binding.autoCompleteEditStudentGender.showDropDown();
        });
    }

    private void loadCurrentProfileData() {
        studentDocRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("studentName");
                if (name != null) binding.editTextEditStudentName.setText(name);

                Object ageObj = doc.get("studentAge");
                if (ageObj != null) binding.editTextEditStudentAge.setText(ageObj.toString());

                String gender = doc.getString("gender");
                if (gender != null) binding.autoCompleteEditStudentGender.setText(gender, false);

                String contact = doc.getString("contactNumber");
                if (contact != null) {
                    if (contact.startsWith("+60")) {
                        binding.editTextEditStudentContact.setText(contact.substring(3));
                    } else {
                        binding.editTextEditStudentContact.setText(contact);
                    }
                }

                String intro = doc.getString("studentIntroduction");
                if (intro != null) binding.editTextEditStudentIntro.setText(intro);

                String experience = doc.getString("volunteerExperience");
                if (experience != null) binding.editTextEditStudentExperience.setText(experience);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading profile data", e);
            Toast.makeText(getContext(), "Error loading profile data", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProfileChanges() {
        String name = binding.editTextEditStudentName.getText().toString().trim();
        String ageStr = binding.editTextEditStudentAge.getText().toString().trim();
        String gender = binding.autoCompleteEditStudentGender.getText().toString().trim();
        String rawContact = binding.editTextEditStudentContact.getText().toString().trim();
        String intro = binding.editTextEditStudentIntro.getText().toString().trim();
        String experience = binding.editTextEditStudentExperience.getText().toString().trim();

        if (name.isEmpty()) {
            binding.inputLayoutEditStudentName.setError("Name is required");
            return;
        } else {
            binding.inputLayoutEditStudentName.setError(null);
        }

        if (ageStr.isEmpty()) {
            binding.inputLayoutEditStudentAge.setError("Age is required");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age <= 0 || age > 120) {
                binding.inputLayoutEditStudentAge.setError("Please enter a valid age (1-120)");
                return;
            }
            binding.inputLayoutEditStudentAge.setError(null);
        } catch (NumberFormatException e) {
            binding.inputLayoutEditStudentAge.setError("Invalid age format");
            return;
        }

        if (gender.isEmpty()) {
            binding.inputLayoutEditStudentGender.setError("Gender is required");
            return;
        } else {
            binding.inputLayoutEditStudentGender.setError(null);
        }

        if (rawContact.isEmpty()) {
            binding.inputLayoutEditStudentContact.setError("Contact number is required");
            return;
        } else {
            binding.inputLayoutEditStudentContact.setError(null);
        }

        if (intro.isEmpty()) {
            binding.inputLayoutEditStudentIntro.setError("Introduction is required");
            return;
        } else {
            binding.inputLayoutEditStudentIntro.setError(null);
        }

        if (experience.isEmpty()) {
            binding.inputLayoutEditStudentExperience.setError("Volunteer experience is required");
            return;
        } else {
            binding.inputLayoutEditStudentExperience.setError(null);
        }

        String finalContact;
        if (rawContact.startsWith("0")) {
            finalContact = "+60" + rawContact.substring(1);
        } else {
            finalContact = "+60" + rawContact;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("studentName", name);
        updates.put("studentAge", age);
        updates.put("gender", gender);
        updates.put("contactNumber", finalContact);
        updates.put("studentIntroduction", intro);
        updates.put("volunteerExperience", experience);

        studentDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating document", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}