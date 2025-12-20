package com.example.volunhub.auth;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentSignUpBinding;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles new user registration for both Students and Organizations.
 * It validates inputs, creates a Firebase Auth account, uploads profile images,
 * and saves the user profile data to Firestore.
 */
public class SignUpFragment extends Fragment {

    private static final String TAG = "SignUpFragment";
    private FragmentSignUpBinding binding;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /**
     * Inflates the layout for the Sign Up screen.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes UI components, listeners, and external services (Firebase).
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        setupValidationListeners();

        // Force uppercase input for Student Name
        binding.editTextStudentName.setFilters(new InputFilter[] {
                new InputFilter.AllCaps()
        });

        // Navigation Back Button
        binding.buttonBackToLogin.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_sign_up_to_login)
        );

        // Sign Up Button
        binding.buttonSignUp.setOnClickListener(v ->
                registerUser()
        );

        // Dropdown setup for Organization Field
        binding.autoCompleteOrgField.setOnClickListener(v ->
                binding.autoCompleteOrgField.showDropDown()
        );
        String[] orgFields = getResources().getStringArray(R.array.org_field_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, orgFields);
        binding.autoCompleteOrgField.setAdapter(adapter);

        // Role Selection Logic
        binding.radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_button_student) {
                binding.linearLayoutSignUpStudent.setVisibility(View.VISIBLE);
                binding.linearLayoutSignUpOrg.setVisibility(View.GONE);
            } else if (checkedId == R.id.radio_button_organization) {
                binding.linearLayoutSignUpStudent.setVisibility(View.GONE);
                binding.linearLayoutSignUpOrg.setVisibility(View.VISIBLE);
            }
        });

        // Image Picker Logic
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                if (getFileSizeInMB(uri) > 5) {
                    showToast(R.string.error_image_too_large);
                    selectedImageUri = null;
                    return;
                }
                selectedImageUri = uri;
                showToast(R.string.msg_image_selected);
                binding.imageViewProfilePicture.setImageURI(uri);
                binding.buttonRemoveProfilePicture.setVisibility(View.VISIBLE);
            } else {
                showToast(R.string.error_image_too_large); // Generic error or create new string "No image selected"
            }
        });

        binding.buttonRemoveProfilePicture.setOnClickListener(v -> {
            selectedImageUri = null;
            binding.imageViewProfilePicture.setImageResource(R.drawable.default_profile_picture);
            binding.buttonRemoveProfilePicture.setVisibility(View.GONE);
            showToast(R.string.msg_image_removed);
        });

        binding.buttonUploadProfilePicture.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*")
        );
    }

    /**
     * Orchestrates the user registration process.
     * It validates the form, creates the auth account, and then saves user data.
     */
    private void registerUser() {
        String email = getSafeText(binding.editTextSignUpEmail.getText());
        String password = getSafeText(binding.editTextSignUpPassword.getText());
        String retypePassword = getSafeText(binding.editTextSignUpRetypePassword.getText());

        int selectedId = binding.radioGroupRole.getCheckedRadioButtonId();

        if (selectedId == -1) {
            showToast(R.string.error_role_required);
            return;
        }

        final String role;
        if (selectedId == binding.radioButtonStudent.getId()) {
            role = "Student";
        } else {
            role = "Organization";
        }

        if (!validateForm(email, password, retypePassword, role)) {
            return;
        }

        binding.progressBarSignup.setVisibility(View.VISIBLE);
        binding.buttonSignUp.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task -> {
            binding.progressBarSignup.setVisibility(View.GONE);
            binding.buttonSignUp.setEnabled(true);
            if (task.isSuccessful()) {
                Log.d(TAG, "createUserWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    showToast(R.string.error_signup_failed);
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
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    binding.textInputLayoutEmail.setError(getString(R.string.error_email_in_use));
                    binding.textInputLayoutEmail.requestFocus();
                } else {
                    showToast(R.string.error_signup_failed);
                }
            }
        });
    }

    /**
     * Validates all input fields based on the selected role using regex and length checks.
     * @param email User email
     * @param password User password
     * @param retypePassword Password confirmation
     * @param role Selected role
     * @return True if form is valid, false otherwise.
     */
    private boolean validateForm(String email, String password, String retypePassword, String role) {
        boolean isValid = true;

        if (checkEditTextIsEmpty(binding.textInputLayoutEmail, binding.editTextSignUpEmail, R.string.error_email_required)) {
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayoutEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        if (checkEditTextIsEmpty(binding.textInputLayoutPassword, binding.editTextSignUpPassword, R.string.error_password_required)) {
            isValid = false;
        } else if (password.length() < 6 || password.length() > 20) {
            binding.textInputLayoutPassword.setError(getString(R.string.error_password_length));
            isValid = false;
        } else {
            String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,20}$";
            if (!password.matches(passwordPattern)) {
                binding.textInputLayoutPassword.setError(getString(R.string.error_password_complexity));
                isValid = false;
            }
        }

        String contactNumberInput = getSafeText(binding.editTextSignUpContactNumber.getText());
        if (TextUtils.isEmpty(contactNumberInput)) {
            binding.textInputLayoutContactNumber.setError(getString(R.string.error_contact_required));
            isValid = false;
        } else if (contactNumberInput.length() < 8 || contactNumberInput.length() > 11) {
            binding.textInputLayoutContactNumber.setError(getString(R.string.error_contact_length));
            isValid = false;
        }

        if (!password.equals(retypePassword)) {
            binding.textInputLayoutRetypePassword.setError(getString(R.string.error_password_mismatch));
            isValid = false;
        }

        if (role.equals("Student")) {
            if (checkEditTextIsEmpty(binding.textInputLayoutStudentName, binding.editTextStudentName, R.string.error_name_required)) {
                isValid = false;
            } else if (getSafeText(binding.editTextStudentName.getText()).matches(".*\\d.*")) {
                binding.textInputLayoutStudentName.setError(getString(R.string.error_name_no_numbers));
                isValid = false;
            }

            String ageText = getSafeText(binding.editTextStudentAge.getText());
            if (TextUtils.isEmpty(ageText)) {
                binding.textInputLayoutStudentAge.setError(getString(R.string.error_age_required));
                isValid = false;
            } else {
                int age = Integer.parseInt(ageText);
                if (age < 13 || age > 100) {
                    binding.textInputLayoutStudentAge.setError(getString(R.string.error_age_range));
                    isValid = false;
                }
            }

            if (binding.radioGroupStudentGender.getCheckedRadioButtonId() == -1) {
                showToast(R.string.error_gender_required);
                isValid = false;
            }

            if (checkEditTextIsEmpty(binding.textInputLayoutStudentIntroduction, binding.editTextStudentIntroduction, R.string.error_intro_required)) {
                isValid = false;
            }

        } else if (role.equals("Organization")) {
            if (checkEditTextIsEmpty(binding.textInputLayoutOrgCompanyName, binding.editTextOrgCompanyName, R.string.error_company_required)) isValid = false;
            if (checkEditTextIsEmpty(binding.textInputLayoutOrgDescription, binding.editTextOrgDescription, R.string.error_desc_required)) isValid = false;
            if (checkEditTextIsEmpty(binding.textInputLayoutOrgField, binding.autoCompleteOrgField, R.string.error_field_required)) isValid = false;
        }

        return isValid;
    }

    /**
     * Helper to check if an EditText is empty and set an error on its layout if so.
     * @param inputLayout The layout wrapper to display the error.
     * @param field The EditText to check.
     * @param errorStringId The Resource ID of the error message string.
     * @return True if empty, false otherwise.
     */
    private boolean checkEditTextIsEmpty(TextInputLayout inputLayout, @NonNull EditText field, int errorStringId) {
        String text = field.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            inputLayout.setError(getString(errorStringId));
            return true;
        } else {
            inputLayout.setError(null);
            return false;
        }
    }

    /**
     * Constructs a Map object containing all relevant user data to be saved in Firestore.
     * @param email Validated email.
     * @param role Selected role.
     * @return A Map of key-value pairs representing the user profile.
     */
    @NonNull
    private Map<String, Object> buildUserData(String email, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("role", role);

        String rawContact = getSafeText(binding.editTextSignUpContactNumber.getText());
        String finalContact = rawContact.startsWith("0") ? "+60" + rawContact.substring(1) : "+60" + rawContact;
        userData.put("contactNumber", finalContact);

        if (role.equals("Student")) {
            userData.put("studentName", getSafeText(binding.editTextStudentName.getText()));
            userData.put("studentAge", getSafeText(binding.editTextStudentAge.getText()));
            String studentGender = binding.radioGroupStudentGender.getCheckedRadioButtonId() == R.id.radio_button_student_male ? "Male" : "Female";
            userData.put("studentGender", studentGender);
            String studentExperience = getSafeText(binding.editTextStudentExperience.getText());
            if (!studentExperience.isEmpty()) {
                userData.put("studentExperience", studentExperience);
            }
            userData.put("studentIntroduction", getSafeText(binding.editTextStudentIntroduction.getText()));
        } else if (role.equals("Organization")) {
            userData.put("orgCompanyName", getSafeText(binding.editTextOrgCompanyName.getText()));
            userData.put("orgDescription", getSafeText(binding.editTextOrgDescription.getText()));
            userData.put("orgField", binding.autoCompleteOrgField.getText().toString().trim());
        }
        return userData;
    }

    /**
     * Uploads the selected profile image to Cloudinary and saves the resulting URL.
     * @param uid User ID.
     * @param imageUri Image file URI.
     * @param userData User data map to append the image URL to.
     */
    private void uploadImageToCloudinary(String uid, Uri imageUri, Map<String, Object> userData) {
        showToast(R.string.msg_uploading_image);

        MediaManager.get().upload(imageUri)
                .option("folder", "profileImages")
                .unsigned("volunhub")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        userData.put("profileImageUrl", imageUrl);
                        saveMapToFirestore(uid, userData);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Error uploading to Cloudinary: " + error.getDescription());
                        showToast(R.string.error_upload_failed);
                        saveMapToFirestore(uid, userData); // Save without image on failure
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    /**
     * Saves the final user data map to the 'users' collection in Firestore.
     * Routes the user to their home screen upon success.
     */
    private void saveMapToFirestore(String uid, Map<String, Object> userData) {
        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(aVoid -> {
                    showToast(R.string.msg_signup_success);
                    if (getActivity() instanceof BaseRouterActivity) {
                        ((BaseRouterActivity) getActivity()).routeUser(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user document", e);
                    showToast(R.string.error_save_user_data);
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) user.delete();
                });
    }

    /**
     * Attaches a TextWatcher to clear the error message on a layout as soon as the user types.
     */
    private void setupValidationListeners() {
        clearErrorOnType(binding.textInputLayoutEmail);
        clearErrorOnType(binding.textInputLayoutPassword);
        clearErrorOnType(binding.textInputLayoutRetypePassword);
        clearErrorOnType(binding.textInputLayoutStudentName);
        clearErrorOnType(binding.textInputLayoutStudentAge);
        clearErrorOnType(binding.textInputLayoutStudentIntroduction);
        clearErrorOnType(binding.textInputLayoutOrgCompanyName);
        clearErrorOnType(binding.textInputLayoutOrgField);
        clearErrorOnType(binding.textInputLayoutOrgDescription);
        clearErrorOnType(binding.textInputLayoutContactNumber);
    }

    /**
     * Attaches a TextWatcher to clear the error message on a layout as soon as the user types.
     * @param textInputLayout The layout to attach the listener to.
     */
    private void clearErrorOnType(TextInputLayout textInputLayout) {
        if (textInputLayout.getEditText() == null) return;
        textInputLayout.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textInputLayout.getError() != null) textInputLayout.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    /**
     * Checks the size of the selected image file.
     * @param uri Image file URI.
     * @return file size or 0 if has error.
     */
    private long getFileSizeInMB(Uri uri) {
        try (android.database.Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                long size = cursor.getLong(sizeIndex);
                return size / (1024 * 1024);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking file size", e);
        }
        return 0;
    }

    /**
     * Helper to get a String from an Editable.
     * @param editable Editable to get text from.
     * @return String or empty if null.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }

    /**
     * Displays a Toast message.
     * @param stringId
     */
    private void showToast(int stringId) {
        if (getContext() != null) Toast.makeText(getContext(), stringId, Toast.LENGTH_SHORT).show();
    }

    /**
     * Cleans up the binding reference to prevent memory leaks when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}