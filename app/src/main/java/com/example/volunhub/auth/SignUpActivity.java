package com.example.volunhub.auth;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.R;
import com.example.volunhub.org.OrgHomeActivity;
import com.example.volunhub.student.StudentHomeActivity;
import com.example.volunhub.Constants;
import com.google.firebase.auth.FirebaseUser;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import com.example.volunhub.databinding.ActivitySignUpBinding;

public class SignUpActivity extends BaseRouterActivity {

    private static final String TAG = "SignUpActivity";
    private ActivitySignUpBinding binding;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.editTextStudentName.setFilters(new InputFilter[] {
            new InputFilter.AllCaps()
        });

        binding.buttonBackToLogin.setOnClickListener(v ->
            goToActivity(LoginActivity.class)
        );

        binding.buttonSignUp.setOnClickListener(View ->
            registerUser()
        );

        binding.autoCompleteOrgField.setOnClickListener(v ->
            binding.autoCompleteOrgField.showDropDown()
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, Constants.ORG_FIELDS);
        binding.autoCompleteOrgField.setAdapter(adapter);

        binding.radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_button_student) {
                binding.linearLayoutSignUpStudent.setVisibility(View.VISIBLE);
                binding.linearLayoutSignUpOrg.setVisibility(View.GONE);

            } else if (checkedId == R.id.radio_button_organization) {
                binding.linearLayoutSignUpStudent.setVisibility(View.GONE);
                binding.linearLayoutSignUpOrg.setVisibility(View.VISIBLE);
            }
        });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Toast.makeText(SignUpActivity.this, "Image selected", Toast.LENGTH_SHORT).show();
                    binding.imageViewProfilePicture.setImageURI(uri);
                } else {
                    Toast.makeText(SignUpActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
                }
        });

        binding.buttonUploadProfilePicture.setOnClickListener(v ->
            imagePickerLauncher.launch("image/*")
        );

    }

    /**
     * Controls the sign-up flow.
     * 1. Gathers and validates input.
     * 2. Creates the Firebase Auth user.
     * 3. Triggers image upload (if selected) or saves data directly.
     */
    private void registerUser() {
        String email = binding.editTextSignUpEmail.getText().toString().trim();
        String password = binding.editTextSignUpPassword.getText().toString().trim();
        String retypePassword = binding.editTextSignUpRetypePassword.getText().toString().trim();

        int selectedId = binding.radioGroupRole.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Please select a role (Student or Organization)", Toast.LENGTH_SHORT).show();
            return;
        }

        final String role;
        if (selectedId == binding.radioButtonStudent.getId()) {
            role = binding.radioButtonStudent.getText().toString();
        } else {
            role = binding.radioButtonOrganization.getText().toString();
        }

        if (!validateForm(email, password, retypePassword, role)) {
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(SignUpActivity.this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(SignUpActivity.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "createUserWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(SignUpActivity.this, "Sign up failed, user is null.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String uid = user.getUid();

                Map<String, Object> userData = buildUserData(email, role);

                if (selectedImageUri != null) {
                    uploadImageToCloudinary(uid, selectedImageUri, userData, role);
                } else {
                    saveMapToFirestore(uid, userData, role);
                }

            } else {
                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                Toast.makeText(SignUpActivity.this, "Sign up failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Validates all input fields based on the selected role.
     * Performs checks for empty fields, email format, password strength, and role-specific requirements.
     *
     * @param email          The email entered by the user.
     * @param password       The password entered by the user.
     * @param retypePassword The confirmation password.
     * @param role           The selected role ("Student" or "Organization").
     * @return true if all fields are valid; false otherwise (sets error on the invalid view).
     */
    private boolean validateForm(String email, String password, String retypePassword, String role) {
        if (!checkEditTextNotEmpty(binding.editTextSignUpEmail, "Email is required")) return false;
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextSignUpEmail.setError("Invalid email format");
            binding.editTextSignUpEmail.requestFocus();
            return false;
        }

        if (!checkEditTextNotEmpty(binding.editTextSignUpPassword, "Password is required")) return false;
        if (password.length() < 6 || password.length() > 20) {
            binding.editTextSignUpPassword.setError("Password length must between 6 to 20 characters");
            binding.editTextSignUpPassword.requestFocus();
            return false;
        }
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,20}$";

        if (!password.matches(passwordPattern)) {
            binding.editTextSignUpPassword.setError("Password must contain 1 uppercase, 1 lowercase, 1 number, and 1 symbol");
            binding.editTextSignUpPassword.requestFocus();
            return false;
        }
        if (!password.equals(retypePassword)) {
            binding.editTextSignUpRetypePassword.setError("Passwords do not match");
            binding.editTextSignUpRetypePassword.requestFocus();
            return false;
        }

        if (role.equals("Student")) {
            if (!checkEditTextNotEmpty(binding.editTextStudentName, "Full Name is required")) return false;

            if (binding.editTextStudentName.getText().toString().matches(".*\\d.*")) {
                binding.editTextStudentName.setError("Name cannot contain numbers");
                binding.editTextStudentName.requestFocus();
                return false;
            }

            String ageText = binding.editTextStudentAge.getText().toString().trim();
            if (TextUtils.isEmpty(ageText)) {
                binding.editTextStudentAge.setError("Age is required");
                binding.editTextStudentAge.requestFocus();
                return false;
            }

            int age = Integer.parseInt(ageText);
            if (age < 1 || age > 200) {
                binding.editTextStudentAge.setError("Age must be between 1 and 200");
                binding.editTextStudentAge.requestFocus();
                return false;
            }

            if (binding.radioGroupStudentGender.getCheckedRadioButtonId() == -1) {
                Toast.makeText(SignUpActivity.this, "Please select a gender", Toast.LENGTH_SHORT).show();
                return false;
            }

            return checkEditTextNotEmpty(binding.editTextStudentIntroduction, "Introduction is required");
        } else if (role.equals("Organization")) {
            if (!checkEditTextNotEmpty(binding.editTextOrgCompanyName, "Company Name is required")) return false;
            if (!checkEditTextNotEmpty(binding.editTextOrgDescription, "Description is required")) return false;
            return checkEditTextNotEmpty(binding.autoCompleteOrgField, "Field is required");
        } else {
            Log.e(TAG, "Unknown role: " + role);
            return false;
        }

    }

    /**
     * Constructs the User Data Map to be saved in Firestore.
     * Filters and adds only the fields relevant to the selected role.
     *
     * @param email The validated email address.
     * @param role  The selected role.
     * @return A Map containing the key-value pairs for the user document.
     */
    @NonNull
    private Map<String, Object> buildUserData(String email, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("role", role);

        if (role.equals("Student")) {
            userData.put("studentName", binding.editTextStudentName.getText().toString().trim());
            userData.put("studentAge", binding.editTextStudentAge.getText().toString().trim());
            String studentGender = binding.radioGroupStudentGender.getCheckedRadioButtonId() == R.id.radio_button_student_male ? "Male" : "Female";
            userData.put("studentGender", studentGender);
            String studentExperience = binding.editTextStudentExperience.getText().toString().trim();
            if (!studentExperience.isEmpty()) {
                userData.put("studentExperience", studentExperience);
            }
            userData.put("studentIntroduction", binding.editTextStudentIntroduction.getText().toString().trim());
        } else if (role.equals("Organization")) {
            userData.put("orgCompanyName", binding.editTextOrgCompanyName.getText().toString().trim());
            userData.put("orgDescription", binding.editTextOrgDescription.getText().toString().trim());
            userData.put("orgField", binding.autoCompleteOrgField.getText().toString().trim());
        }
        return userData;
    }

    /**
     * Uploads the profile image to Cloudinary.
     * Upon success, it retrieves the image URL and then triggers the Firestore save.
     * Upon failure, it defaults to a local image and triggers the Firestore save.
     *
     * @param uid       The user's Firebase Auth UID.
     * @param imageUri  The URI of the selected image.
     * @param userData  The map of user data to be updated with the image URL.
     * @param role      The user's role for navigation.
     */
    private void uploadImageToCloudinary(String uid, Uri imageUri, Map<String, Object> userData, final String role) {
        Toast.makeText(SignUpActivity.this, "Uploading image...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(imageUri)
            .option("folder", "profileImages")
            .unsigned("volunhub")
            .callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    Log.d(TAG, "Cloudinary upload started...");
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {}

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String imageUrl = (String) resultData.get("secure_url");
                    Log.d(TAG, "Image uploaded to Cloudinary: " + imageUrl);
                    userData.put("profileImageUrl", imageUrl);
                    saveMapToFirestore(uid, userData, role);
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    Log.w(TAG, "Error uploading to Cloudinary: " + error.getDescription());
                    Toast.makeText(SignUpActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    saveMapToFirestore(uid, userData, role);
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {}

            })
            .dispatch(); // This starts the upload
    }

    /**
     * This is the FINAL step.
     * Saves the completed User Data Map to Firestore and navigates to the correct home.
     *
     * @param uid      The user's unique ID from Firebase Authentication.
     * @param userData The Map containing all the user's data (email, role, name, etc.).
     * @param role     The user's role ("Student" or "Organization") to decide navigation.
     */
    private void saveMapToFirestore(String uid, Map<String, Object> userData, final String role) {
        db.collection("users").document(uid).set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User document successfully created in Firestore!");
                Toast.makeText(SignUpActivity.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                if (role.equals("Student")) {
                    goToActivity(StudentHomeActivity.class);
                } else if (role.equals("Organization")) {
                    goToActivity(OrgHomeActivity.class);
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error creating user document in Firestore", e);
                Toast.makeText(SignUpActivity.this, "Error: Could not save user data.", Toast.LENGTH_LONG).show();
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    user.delete();
                }
            });
    }

    /**
     * A helper method to check if an EditText is empty.
     * If it is empty, it sets an error and returns false.
     *
     * @param field        The EditText to check.
     * @param errorMessage The error message to display.
     * @return true if the field is not empty, false if it is.
     */
    private boolean checkEditTextNotEmpty(@NonNull EditText field, String errorMessage) {
        String text = field.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            field.setError(errorMessage);
            field.requestFocus();
            return false;
        }
        return true;
    }

}