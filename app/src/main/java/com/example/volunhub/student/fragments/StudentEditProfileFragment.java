package com.example.volunhub.student.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentStudentEditProfileBinding;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StudentEditProfileFragment extends Fragment {

    private static final String TAG = "StudentEditProfile";
    private FragmentStudentEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DocumentReference studentDocRef;

    // Image Handling
    private Uri selectedImageUri = null;
    private String currentImageUrl = null;
    private boolean isImageRemoved = false;
    private ActivityResultLauncher<String> imagePickerLauncher;

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

        if (mAuth.getCurrentUser() == null) return;
        studentDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        // 1. Validation Helpers (Clear error on type)
        clearErrorOnType(binding.inputLayoutEditStudentName);
        clearErrorOnType(binding.inputLayoutEditStudentAge);
        clearErrorOnType(binding.inputLayoutEditStudentContact);
        clearErrorOnType(binding.inputLayoutEditStudentIntro);
        // Note: Experience is optional, so no error clearing needed usually, but good practice if you add checks later

        // 2. Setup Image Picker
        setupImagePicker();

        // 3. Load Data
        loadCurrentProfileData();

        // 4. Buttons
        binding.btnChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnRemovePhoto.setOnClickListener(v -> removeProfilePhoto());
        binding.buttonSaveStudentProfile.setOnClickListener(v -> validateAndSave());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                // Check 5MB limit
                long fileSizeInBytes = getFileSize(uri);
                long sizeInMB = fileSizeInBytes / (1024 * 1024);
                if (sizeInMB > 5) {
                    Toast.makeText(getContext(), "Image is too large! Please choose under 5MB.", Toast.LENGTH_LONG).show();
                    return;
                }
                selectedImageUri = uri;
                isImageRemoved = false;
                binding.imageEditStudentPicture.setImageURI(uri);
            }
        });
    }

    private void removeProfilePhoto() {
        selectedImageUri = null;
        isImageRemoved = true;
        binding.imageEditStudentPicture.setImageResource(R.drawable.default_profile_picture);
        Toast.makeText(getContext(), "Photo removed (Save to apply)", Toast.LENGTH_SHORT).show();
    }

    private void loadCurrentProfileData() {
        studentDocRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // Name
                String name = doc.getString("studentName");
                if (name != null) binding.editTextEditStudentName.setText(name);

                // Age
                Object ageObj = doc.get("studentAge");
                if (ageObj != null) binding.editTextEditStudentAge.setText(ageObj.toString());

                // Gender (Radio Group)
                String gender = doc.getString("gender");
                // Check fallback key
                if (gender == null) gender = doc.getString("studentGender");

                if ("Male".equalsIgnoreCase(gender)) {
                    binding.radioBtnMale.setChecked(true);
                } else if ("Female".equalsIgnoreCase(gender)) {
                    binding.radioBtnFemale.setChecked(true);
                }

                // Contact (+60 stripping)
                String contact = doc.getString("contact");
                if (contact == null) contact = doc.getString("contactNumber");

                if (contact != null) {
                    if (contact.startsWith("+60")) {
                        binding.editTextEditStudentContact.setText(contact.substring(3));
                    } else {
                        binding.editTextEditStudentContact.setText(contact);
                    }
                }

                // Intro
                String intro = doc.getString("studentIntroduction");
                if (intro != null) binding.editTextEditStudentIntro.setText(intro);

                // Experience
                String experience = doc.getString("volunteerExperience");
                // Check fallback key
                if (experience == null) experience = doc.getString("studentExperience");
                if (experience != null) binding.editTextEditStudentExperience.setText(experience);

                // Image
                currentImageUrl = doc.getString("profileImageUrl");
                if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                    if (getContext() != null) {
                        Glide.with(getContext())
                                .load(currentImageUrl)
                                .placeholder(R.drawable.default_profile_picture)
                                .centerCrop()
                                .into(binding.imageEditStudentPicture);
                    }
                } else {
                    // Show default if nothing in DB
                    binding.imageEditStudentPicture.setImageResource(R.drawable.default_profile_picture);
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading profile data", e);
            Toast.makeText(getContext(), "Error loading profile data", Toast.LENGTH_SHORT).show();
        });
    }

    private void validateAndSave() {
        // --- VALIDATION START ---

        // 1. Name
        String name = getSafeText(binding.editTextEditStudentName.getText());
        if (checkEditTextIsEmpty(binding.inputLayoutEditStudentName, binding.editTextEditStudentName, "Name is required")) return;
        if (name.matches(".*\\d.*")) {
            binding.inputLayoutEditStudentName.setError("Name cannot contain numbers");
            return;
        }

        // 2. Age
        String ageStr = getSafeText(binding.editTextEditStudentAge.getText());
        if (checkEditTextIsEmpty(binding.inputLayoutEditStudentAge, binding.editTextEditStudentAge, "Age is required")) return;
        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 13 || age > 100) {
                binding.inputLayoutEditStudentAge.setError("Valid age: 13-100");
                return;
            }
        } catch (NumberFormatException e) {
            binding.inputLayoutEditStudentAge.setError("Invalid number");
            return;
        }

        // 3. Gender (Radio)
        int selectedGenderId = binding.radioGroupEditGender.getCheckedRadioButtonId();
        if (selectedGenderId == -1) {
            Toast.makeText(getContext(), "Please select a gender", Toast.LENGTH_SHORT).show();
            return;
        }
        String gender = (selectedGenderId == R.id.radio_btn_male) ? "Male" : "Female";

        // 4. Contact
        String rawContact = getSafeText(binding.editTextEditStudentContact.getText());
        if (checkEditTextIsEmpty(binding.inputLayoutEditStudentContact, binding.editTextEditStudentContact, "Contact is required")) return;
        if (rawContact.length() < 8 || rawContact.length() > 10) {
            binding.inputLayoutEditStudentContact.setError("Enter 8-10 digits");
            return;
        }

        // 5. Intro
        if (checkEditTextIsEmpty(binding.inputLayoutEditStudentIntro, binding.editTextEditStudentIntro, "Introduction is required")) return;

        // 6. Experience (OPTIONAL - No Validation check needed)
        // If empty, we just save an empty string.

        // --- VALIDATION END ---

        setLoading(true);

        String finalContact = rawContact.startsWith("0") ? "+60" + rawContact.substring(1) : "+60" + rawContact;

        Map<String, Object> updates = new HashMap<>();
        updates.put("studentName", name);
        updates.put("studentAge", age);
        updates.put("gender", gender);
        updates.put("studentGender", gender); // Sync both keys to be safe
        updates.put("contactNumber", finalContact);
        updates.put("studentIntroduction", getSafeText(binding.editTextEditStudentIntro.getText()));
        // Optional Experience
        String studentExperience = getSafeText(binding.editTextEditStudentExperience.getText());
        if (!studentExperience.isEmpty()) {
            updates.put("studentExperience", studentExperience);
        }

        // Handle Image
        if (selectedImageUri != null) {
            uploadImageToCloudinary(updates);
        } else if (isImageRemoved) {
            updates.put("profileImageUrl", "");
            updateFirestore(updates);
        } else {
            updateFirestore(updates);
        }
    }

    private void uploadImageToCloudinary(Map<String, Object> updates) {

        MediaManager.get().upload(selectedImageUri)
                .option("folder", "profileImages")
                .unsigned("volunhub")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String newUrl = (String) resultData.get("secure_url");
                        updates.put("profileImageUrl", newUrl);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> updateFirestore(updates));
                        }
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(getContext(), "Image upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void updateFirestore(Map<String, Object> updates) {
        studentDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Error updating profile.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Firestore error", e);
                });
    }

    // --- Helpers ---

    private void setLoading(boolean isLoading) {
        binding.buttonSaveStudentProfile.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.progressBarSave.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // Can optionally disable inputs here
    }

    private boolean checkEditTextIsEmpty(TextInputLayout inputLayout, EditText field, String errorMessage) {
        if (TextUtils.isEmpty(getSafeText(field.getText()))) {
            inputLayout.setError(errorMessage);
            return true;
        }
        inputLayout.setError(null);
        return false;
    }

    private void clearErrorOnType(TextInputLayout textInputLayout) {
        if (textInputLayout.getEditText() == null) return;
        textInputLayout.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textInputLayout.getError() != null) textInputLayout.setError(null);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }

    private long getFileSize(Uri uri) {
        android.database.Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
            cursor.moveToFirst();
            long size = cursor.getLong(sizeIndex);
            cursor.close();
            return size;
        }
        return -1;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}