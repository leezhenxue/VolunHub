package com.example.volunhub.auth;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentSignUpBinding;
import com.example.volunhub.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles new user registration for both Students and Organizations.
 *
 * <p>This activity manages a complex flow:
 * <ol>
 * <li><b>Validation:</b> checks for empty fields, password strength, and role-specific requirements.</li>
 * <li><b>Authentication:</b> Creates a new user in Firebase Auth.</li>
 * <li><b>Media Upload:</b> Uploads the profile picture to Cloudinary (if selected).</li>
 * <li><b>Database:</b> Creates a new document in the 'users' Firestore collection.</li>
 * </ol>
 * </p>
 */
public class SignUpFragment extends Fragment {

    private static final String TAG = "SignUpFragment";
    private FragmentSignUpBinding binding;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        clearErrorOnType(binding.textInputLayoutEmail);
        clearErrorOnType(binding.textInputLayoutPassword);
        clearErrorOnType(binding.textInputLayoutRetypePassword);
        clearErrorOnType(binding.textInputLayoutStudentName);
        clearErrorOnType(binding.textInputLayoutStudentAge);
        clearErrorOnType(binding.textInputLayoutStudentIntroduction);
        clearErrorOnType(binding.textInputLayoutOrgCompanyName);
        clearErrorOnType(binding.textInputLayoutOrgField);
        clearErrorOnType(binding.textInputLayoutOrgDescription);


        binding.editTextStudentName.setFilters(new InputFilter[] {
            new InputFilter.AllCaps()
        });

        binding.buttonBackToLogin.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_sign_up_to_login)
        );

        binding.buttonSignUp.setOnClickListener(v ->
            registerUser()
        );

        binding.autoCompleteOrgField.setOnClickListener(v ->
            binding.autoCompleteOrgField.showDropDown()
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, Constants.ORG_FIELDS);
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
                    long fileSizeInBytes = getFileSize(uri);
                    long sizeInMB = fileSizeInBytes / (1024 * 1024);
                    if (sizeInMB > 5) {
                        Toast.makeText(getContext(), "Image is too large! Please choose an image under 5MB.", Toast.LENGTH_LONG).show();
                        selectedImageUri = null;
                        return;
                    }
                    selectedImageUri = uri;
                    Toast.makeText(getContext(), "Image selected", Toast.LENGTH_SHORT).show();
                    binding.imageViewProfilePicture.setImageURI(uri);
                } else {
                    Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
                }
        });

        binding.buttonUploadProfilePicture.setOnClickListener(v ->
            imagePickerLauncher.launch("image/*")
        );

    }

    /**
     * Handles the user login process.
     *
     * <p>This activity is responsible for:
     * <ul>
     * <li>Validating email and password input.</li>
     * <li>Authenticating against Firebase Auth.</li>
     * <li>Fetching the user's role (Student/Org) from Firestore upon success.</li>
     * <li>Routing the user to the correct dashboard via {@link com.example.volunhub.BaseRouterActivity}.</li>
     * </ul>
     * </p>
     */
    private void registerUser() {
        String email = binding.editTextSignUpEmail.getText().toString().trim();
        String password = binding.editTextSignUpPassword.getText().toString().trim();
        String retypePassword = binding.editTextSignUpRetypePassword.getText().toString().trim();

        int selectedId = binding.radioGroupRole.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(getContext(), "Please select a role (Student or Organization)", Toast.LENGTH_SHORT).show();
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

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Sign up successful", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "createUserWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(getContext(), "Sign up failed, user is null.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String uid = user.getUid();

                Map<String, Object> userData = buildUserData(email, role);

                if (selectedImageUri != null) {
                    uploadImageToCloudinary(uid, selectedImageUri, userData);
                } else {
                    saveMapToFirestore(uid, userData);
                }

            } else {
                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                Toast.makeText(getContext(), "Sign up failed", Toast.LENGTH_SHORT).show();
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
        boolean isValid = true;
        if (!checkEditTextNotEmpty(binding.textInputLayoutEmail, binding.editTextSignUpEmail, "Email is required")) {
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayoutEmail.setError("Invalid email format");
            isValid = false;
        }

        if (!checkEditTextNotEmpty(binding.textInputLayoutPassword, binding.editTextSignUpPassword, "Password is required")) {
            isValid = false;
        } else if (password.length() < 6 || password.length() > 20) {
            binding.textInputLayoutPassword.setError("Password length must between 6 to 20 characters");
            isValid = false;
        } else {
            String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,20}$";
            if (!password.matches(passwordPattern)) {
                binding.textInputLayoutPassword.setError("Password must contain 1 uppercase, 1 lowercase, 1 number, and 1 symbol");
                isValid = false;
            }
        }

        if (!password.equals(retypePassword)) {
            binding.textInputLayoutRetypePassword.setError("Passwords do not match");
            isValid = false;
        }

        if (role.equals("Student")) {
            if (!checkEditTextNotEmpty(binding.textInputLayoutStudentName, binding.editTextStudentName, "Full Name is required")) {
                isValid = false;
            } else if (binding.editTextStudentName.getText().toString().matches(".*\\d.*")) {
                binding.textInputLayoutStudentName.setError("Name cannot contain numbers");
                isValid = false;
            }

            String ageText = binding.editTextStudentAge.getText().toString().trim();
            if (TextUtils.isEmpty(ageText)) {
                binding.textInputLayoutStudentAge.setError("Age is required");
                isValid = false;
            } else {
                int age = Integer.parseInt(ageText);
                if (age < 1 || age > 200) {
                    binding.textInputLayoutStudentAge.setError("Age must be between 1 and 200");
                    isValid = false;
                }
            }

            if (binding.radioGroupStudentGender.getCheckedRadioButtonId() == -1) {
                Toast.makeText(getContext(), "Please select a gender", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
            if (!checkEditTextNotEmpty(binding.textInputLayoutStudentIntroduction, binding.editTextStudentIntroduction, "Introduction is required")) {
                isValid = false;
            }
        } else if (role.equals("Organization")) {
            if (!checkEditTextNotEmpty(binding.textInputLayoutOrgCompanyName, binding.editTextOrgCompanyName, "Company Name is required")) {
                isValid = false;
            }
            if (!checkEditTextNotEmpty(binding.textInputLayoutOrgDescription, binding.editTextOrgDescription, "Description is required")) {
                isValid = false;
            }
            if (!checkEditTextNotEmpty(binding.textInputLayoutOrgField, binding.autoCompleteOrgField, "Field is required")) {
                isValid = false;
            }
        } else {
            Log.e(TAG, "Unknown role: " + role);
            isValid = false;
        }

        return isValid;

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
     */
    private void uploadImageToCloudinary(String uid, Uri imageUri, Map<String, Object> userData) {
        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();

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
                    saveMapToFirestore(uid, userData);
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    Log.w(TAG, "Error uploading to Cloudinary: " + error.getDescription());
                    Toast.makeText(getContext(), "Image upload failed", Toast.LENGTH_SHORT).show();
                    saveMapToFirestore(uid, userData);
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
     */
    private void saveMapToFirestore(String uid, Map<String, Object> userData) {
        db.collection("users").document(uid).set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User document successfully created in Firestore!");
                Toast.makeText(getContext(), "Sign up successful", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof BaseRouterActivity) {
                    ((BaseRouterActivity) getActivity()).routeUser(uid);
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error creating user document in Firestore", e);
                Toast.makeText(getContext(), "Error: Could not save user data.", Toast.LENGTH_LONG).show();
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
    private boolean checkEditTextNotEmpty(com.google.android.material.textfield.TextInputLayout inputLayout, @NonNull EditText field, String errorMessage) {
        String text = field.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            inputLayout.setError(errorMessage);
            return false;
        } else {
            inputLayout.setError(null);
            return true;
        }
    }

    /**
     * Helper method to get the size of a file from its URI.
     *
     * @param uri The URI of the selected file.
     * @return The size of the file in bytes, or -1 if it cannot be determined.
     */
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

    /**
     * Automatically clears the error message and the error state from the TextInputLayout
     * as soon as the user starts typing in its child EditText.
     *
     * <p>This improves user experience by removing the "red" error feedback immediately
     * when the user attempts to fix the input, rather than waiting for the next
     * validation check.</p>
     *
     * @param textInputLayout The TextInputLayout wrapper that is displaying the error.
     */
    private void clearErrorOnType(com.google.android.material.textfield.TextInputLayout textInputLayout) {
        if (textInputLayout.getEditText() == null) return;

        textInputLayout.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // The moment they type ANYTHING, hide the error.
                // We will re-check validity when they click "Sign Up" again.
                if (textInputLayout.getError() != null) {
                    textInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

}