package com.example.volunhub.student.profile;

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

/**
 * Fragment responsible for editing the student's profile details.
 * It handles data validation, image selection, Cloudinary upload, and Firestore updates.
 */
public class StudentEditProfileFragment extends Fragment {

    private static final String TAG = "StudentEditProfile";
    private FragmentStudentEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DocumentReference studentDocRef;

    private Uri selectedImageUri = null;
    private String currentImageUrl = null;
    private boolean isImageRemoved = false;
    private ActivityResultLauncher<String> imagePickerLauncher;

    public StudentEditProfileFragment() {}

    /**
     * Inflates the layout for this fragment using ViewBinding.
     * @param inflater The LayoutInflater object.
     * @param container If non-null, this is the parent view.
     * @param savedInstanceState Bundle containing saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes Firebase components, setup listeners, and loads initial profile data.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState Bundle containing saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) return;
        studentDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        setupValidationListeners();
        setupImagePicker();
        loadCurrentProfileData();

        binding.btnChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnRemovePhoto.setOnClickListener(v -> removeProfilePhoto());
        binding.buttonSaveStudentProfile.setOnClickListener(v -> validateAndSave());
    }

    /**
     * Configures the image picker result launcher and handles file size validation.
     */
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                long fileSizeInBytes = getFileSize(uri);
                long sizeInMB = fileSizeInBytes / (1024 * 1024);
                if (sizeInMB > 5) {
                    showToast(R.string.error_image_too_large);
                    return;
                }
                selectedImageUri = uri;
                isImageRemoved = false;
                binding.imageEditStudentPicture.setImageURI(uri);
            }
        });
    }

    /**
     * Resets the profile picture to the default placeholder and marks the image for removal.
     */
    private void removeProfilePhoto() {
        selectedImageUri = null;
        isImageRemoved = true;
        binding.imageEditStudentPicture.setImageResource(R.drawable.default_profile_picture);
        showToast(R.string.msg_image_removed);
    }

    /**
     * Fetches current student data from Firestore and populates the UI fields.
     */
    private void loadCurrentProfileData() {
        studentDocRef.get().addOnSuccessListener(doc -> {
            if (binding == null || !doc.exists()) return;

            binding.editTextEditStudentName.setText(doc.getString("studentName"));

            Object ageObj = doc.get("studentAge");
            if (ageObj != null) binding.editTextEditStudentAge.setText(ageObj.toString());

            String gender = doc.getString("studentGender");
            if (gender == null) gender = doc.getString("gender");

            if ("Male".equalsIgnoreCase(gender)) {
                binding.radioBtnMale.setChecked(true);
            } else if ("Female".equalsIgnoreCase(gender)) {
                binding.radioBtnFemale.setChecked(true);
            }

            String contact = doc.getString("contactNumber");
            if (contact == null) contact = doc.getString("contact");

            if (contact != null) {
                if (contact.startsWith("+60")) {
                    binding.editTextEditStudentContact.setText(contact.substring(3));
                } else {
                    binding.editTextEditStudentContact.setText(contact);
                }
            }

            binding.editTextEditStudentIntro.setText(doc.getString("studentIntroduction"));

            String experience = doc.getString("studentExperience");
            if (experience == null) experience = doc.getString("volunteerExperience");
            binding.editTextEditStudentExperience.setText(experience);

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
                binding.imageEditStudentPicture.setImageResource(R.drawable.default_profile_picture);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading profile data", e));
    }

    /**
     * Validates input forms and prepares data map for Firestore update.
     */
    private void validateAndSave() {
        String name = getSafeText(binding.editTextEditStudentName.getText());
        if (checkEditTextIsEmpty(binding.inputLayoutEditStudentName, binding.editTextEditStudentName, R.string.error_name_required)) return;
        if (name.matches(".*\\d.*")) {
            binding.inputLayoutEditStudentName.setError(getString(R.string.error_name_no_numbers));
            return;
        }

        String ageStr = getSafeText(binding.editTextEditStudentAge.getText());
        if (checkEditTextIsEmpty(binding.inputLayoutEditStudentAge, binding.editTextEditStudentAge, R.string.error_age_required)) return;
        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 13 || age > 100) {
                binding.inputLayoutEditStudentAge.setError(getString(R.string.error_age_range));
                return;
            }
        } catch (NumberFormatException e) {
            binding.inputLayoutEditStudentAge.setError(getString(R.string.not_available));
            return;
        }

        int selectedGenderId = binding.radioGroupEditGender.getCheckedRadioButtonId();
        if (selectedGenderId == -1) {
            showToast(R.string.error_gender_required);
            return;
        }
        String gender = (selectedGenderId == R.id.radio_btn_male) ? "Male" : "Female";

        String rawContact = getSafeText(binding.editTextEditStudentContact.getText());
        if (checkEditTextIsEmpty(binding.inputLayoutEditStudentContact, binding.editTextEditStudentContact, R.string.error_contact_required)) return;
        if (rawContact.length() < 8 || rawContact.length() > 11) {
            binding.inputLayoutEditStudentContact.setError(getString(R.string.error_contact_length));
            return;
        }

        if (checkEditTextIsEmpty(binding.inputLayoutEditStudentIntro, binding.editTextEditStudentIntro, R.string.error_intro_required)) return;

        setLoading(true);
        String finalContact = rawContact.startsWith("0") ? "+60" + rawContact.substring(1) : "+60" + rawContact;

        Map<String, Object> updates = new HashMap<>();
        updates.put("studentName", name);
        updates.put("studentAge", age);
        updates.put("studentGender", gender);
        updates.put("gender", gender);
        updates.put("contactNumber", finalContact);
        updates.put("studentIntroduction", getSafeText(binding.editTextEditStudentIntro.getText()));

        String studentExperience = getSafeText(binding.editTextEditStudentExperience.getText());
        updates.put("studentExperience", studentExperience);

        if (selectedImageUri != null) {
            uploadImageToCloudinary(updates);
        } else if (isImageRemoved) {
            updates.put("profileImageUrl", "");
            updateFirestore(updates);
        } else {
            updateFirestore(updates);
        }
    }

    /**
     * Uploads selected profile image to Cloudinary storage.
     * @param updates The data map to be updated with the new image URL.
     */
    private void uploadImageToCloudinary(Map<String, Object> updates) {
        MediaManager.get().upload(selectedImageUri)
                .option("folder", "profileImages")
                .unsigned("volunhub")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String newUrl = (String) resultData.get("secure_url");
                        updates.put("profileImageUrl", newUrl);
                        if (getActivity() != null) getActivity().runOnUiThread(() -> updateFirestore(updates));
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> {
                            setLoading(false);
                            showToast(R.string.error_upload_failed);
                        });
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    /**
     * Performs the final update to the student's document in Firestore.
     * @param updates The map containing fields to be updated.
     */
    private void updateFirestore(Map<String, Object> updates) {
        studentDocRef.update(updates).addOnSuccessListener(aVoid -> {
            setLoading(false);
            showToast(R.string.msg_signup_success);
            Navigation.findNavController(requireView()).popBackStack();
        }).addOnFailureListener(e -> {
            setLoading(false);
            showToast(R.string.error_save_user_data);
        });
    }

    /**
     * Updates UI visibility to reflect a loading state during save operations.
     * @param isLoading True to show progress bar, false to show save button.
     */
    private void setLoading(boolean isLoading) {
        binding.buttonSaveStudentProfile.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.progressBarSave.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    /**
     * Checks if an EditText is empty and displays an error message on the TextInputLayout.
     * @param inputLayout The layout containing the EditText.
     * @param field The EditText field to check.
     * @param errorResId The string resource ID for the error message.
     * @return True if the field is empty, false otherwise.
     */
    private boolean checkEditTextIsEmpty(TextInputLayout inputLayout, EditText field, int errorResId) {
        if (TextUtils.isEmpty(getSafeText(field.getText()))) {
            inputLayout.setError(getString(errorResId));
            return true;
        }
        inputLayout.setError(null);
        return false;
    }

    /**
     * Attaches TextWatchers to fields to clear validation errors as the user types.
     */
    private void setupValidationListeners() {
        clearErrorOnType(binding.inputLayoutEditStudentName);
        clearErrorOnType(binding.inputLayoutEditStudentAge);
        clearErrorOnType(binding.inputLayoutEditStudentContact);
        clearErrorOnType(binding.inputLayoutEditStudentIntro);
    }

    /**
     * Removes error messages from a TextInputLayout when the user enters text.
     * @param textInputLayout The layout to monitor.
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
     * Retrieves trimmed text from an Editable object safely.
     * @param editable The text source.
     * @return The trimmed string or an empty string if null.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }

    /**
     * Determines the file size of a provided URI.
     * @param uri The URI of the file.
     * @return Size in bytes, or -1 if size could not be determined.
     */
    private long getFileSize(Uri uri) {
        try (android.database.Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                return cursor.getLong(sizeIndex);
            }
        }
        return -1;
    }

    /**
     * Displays a short Toast message using a string resource ID.
     * @param resId The string resource ID.
     */
    private void showToast(int resId) {
        if (getContext() != null) Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }

    /**
     * Cleans up the binding object when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}