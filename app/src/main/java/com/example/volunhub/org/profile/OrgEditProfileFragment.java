package com.example.volunhub.org.profile;

import android.net.Uri;
import android.os.Bundle;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgEditProfileBinding;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles editing of the Organization's profile.
 * Supports updating name, description, field, contact number, and profile image.
 * If the name changes, it performs a cascading update to related documents.
 */
public class OrgEditProfileFragment extends Fragment {

    private static final String TAG = "OrgEditProfile";
    private FragmentOrgEditProfileBinding binding;
    private DocumentReference orgDocRef;

    private Uri selectedImageUri = null;
    private String currentImageUrl = null;
    private boolean isImageRemoved = false;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public OrgEditProfileFragment() {}

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
        binding = FragmentOrgEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes UI, listeners, and loads current profile data.
     *
     * @param view The created view.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) return;
        orgDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        setupValidationListeners();
        setupOrgFieldSpinner();
        setupImagePicker();
        loadCurrentProfileData();

        binding.btnChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnRemovePhoto.setOnClickListener(v -> removeProfilePhoto());
        binding.buttonSaveOrgProfile.setOnClickListener(v -> validateAndSave());
    }

    /**
     * Registers the activity result launcher for selecting an image from the gallery.
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

                binding.imageEditOrgLogo.setImageURI(uri);
                showToast(R.string.msg_image_selected);
            }
        });
    }

    /**
     * Removes the currently selected profile photo and sets the default placeholder.
     */
    private void removeProfilePhoto() {
        selectedImageUri = null;
        isImageRemoved = true;
        binding.imageEditOrgLogo.setImageResource(R.drawable.default_profile_picture);
        showToast(R.string.msg_image_removed);
    }

    /**
     * Fetches current organization data from Firestore and populates the fields.
     */
    private void loadCurrentProfileData() {
        orgDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (binding == null) return;
            if (documentSnapshot.exists()) {
                binding.editTextEditOrgName.setText(documentSnapshot.getString("orgCompanyName"));
                binding.editTextEditOrgDesc.setText(documentSnapshot.getString("orgDescription"));
                binding.autoCompleteEditOrgField.setText(documentSnapshot.getString("orgField"), false);

                String contact = documentSnapshot.getString("contactNumber");
                if (contact != null) {
                    if (contact.startsWith("+60")) {
                        binding.editTextEditOrgContact.setText(contact.substring(3));
                    } else {
                        binding.editTextEditOrgContact.setText(contact);
                    }
                }

                currentImageUrl = documentSnapshot.getString("profileImageUrl");
                if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                    if (getContext() != null) {
                        Glide.with(getContext())
                                .load(currentImageUrl)
                                .placeholder(R.drawable.ic_org_dashboard)
                                .centerCrop()
                                .into(binding.imageEditOrgLogo);
                    }
                } else {
                    binding.imageEditOrgLogo.setImageResource(R.drawable.default_profile_picture);
                }
            }
        });
    }

    /**
     * Validates input fields and initiates the save process if valid.
     */
    private void validateAndSave() {
        if (checkEditTextIsEmpty(binding.inputLayoutEditOrgName, binding.editTextEditOrgName, R.string.error_company_required)) return;
        if (checkEditTextIsEmpty(binding.inputLayoutEditOrgField, binding.autoCompleteEditOrgField, R.string.error_field_required)) return;
        if (checkEditTextIsEmpty(binding.inputLayoutEditOrgDesc, binding.editTextEditOrgDesc, R.string.error_desc_required)) return;

        String rawContact = getSafeText(binding.editTextEditOrgContact.getText());
        if (TextUtils.isEmpty(rawContact)) {
            binding.inputLayoutEditOrgContact.setError(getString(R.string.error_contact_required));
            return;
        }
        if (rawContact.length() < 8 || rawContact.length() > 10) {
            binding.inputLayoutEditOrgContact.setError(getString(R.string.error_contact_length));
            return;
        }

        setLoading(true);

        String finalContact = rawContact.startsWith("0") ? "+60" + rawContact.substring(1) : "+60" + rawContact;

        Map<String, Object> updates = new HashMap<>();
        updates.put("orgCompanyName", getSafeText(binding.editTextEditOrgName.getText()));
        updates.put("orgField", getSafeText(binding.autoCompleteEditOrgField.getText()));
        updates.put("orgDescription", getSafeText(binding.editTextEditOrgDesc.getText()));
        updates.put("contactNumber", finalContact);

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
     * Uploads the new profile image to Cloudinary and updates the Firestore map on success.
     *
     * @param updates The map of user data to be updated.
     */
    private void uploadImageToCloudinary(Map<String, Object> updates) {
        showToast(R.string.msg_uploading_image);
        MediaManager.get().upload(selectedImageUri)
                .option("folder", "profileImages")
                .unsigned("volunhub")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String newImageUrl = (String) resultData.get("secure_url");
                        updates.put("profileImageUrl", newImageUrl);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> updateFirestore(updates));
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                setLoading(false);
                                showToast(R.string.error_upload_failed);
                            });
                        }
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    /**
     * Updates the Firestore document with the new data.
     * Performs a batch update for cascading changes if the organization name has changed.
     *
     * @param updates The map containing the updated fields.
     */
    private void updateFirestore(Map<String, Object> updates) {
        String newName = (String) updates.get("orgCompanyName");

        // If name unchanged, standard update
        if (newName == null) {
            orgDocRef.update(updates)
                    .addOnSuccessListener(aVoid -> finishUpdate(true))
                    .addOnFailureListener(e -> finishUpdate(false));
            return;
        }

        // Cascading Update if name changed
        String orgId = mAuth.getCurrentUser().getUid();

        var servicesTask = db.collection("services").whereEqualTo("orgId", orgId).get();
        var appsTask = db.collection("applications").whereEqualTo("orgId", orgId).get();

        com.google.android.gms.tasks.Tasks.whenAllSuccess(servicesTask, appsTask)
                .addOnSuccessListener(results -> {
                    if (binding == null) return;

                    var serviceSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(0);
                    var appSnapshots = (com.google.firebase.firestore.QuerySnapshot) results.get(1);

                    var batch = db.batch();

                    batch.update(orgDocRef, updates);

                    for (var doc : serviceSnapshots) {
                        batch.update(doc.getReference(), "orgName", newName);
                    }
                    for (var doc : appSnapshots) {
                        batch.update(doc.getReference(), "orgName", newName);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> finishUpdate(true))
                            .addOnFailureListener(e -> finishUpdate(false));
                })
                .addOnFailureListener(e -> finishUpdate(false));
    }

    /**
     * Finalizes the update process, shows a status message, and navigates back on success.
     *
     * @param success True if the update was successful, false otherwise.
     */
    private void finishUpdate(boolean success) {
        if (binding == null) return;
        setLoading(false);
        if (success) {
            showToast(R.string.msg_signup_success);
            Navigation.findNavController(requireView()).popBackStack();
        } else {
            showToast(R.string.error_save_user_data);
        }
    }

    /**
     * Toggles the loading state of the UI.
     *
     * @param isLoading True to show progress bar and disable inputs, false otherwise.
     */
    private void setLoading(boolean isLoading) {
        binding.buttonSaveOrgProfile.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.progressBarSave.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.editTextEditOrgName.setEnabled(!isLoading);
        binding.btnChangePhoto.setEnabled(!isLoading);
    }

    /**
     * Sets up the dropdown adapter for organization fields.
     */
    private void setupOrgFieldSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.org_field_options));
        binding.autoCompleteEditOrgField.setAdapter(adapter);
    }

    /**
     * Checks if an EditText field is empty and sets an error on its layout.
     *
     * @param inputLayout The TextInputLayout to display the error.
     * @param field The EditText to check.
     * @param errorId The resource ID of the error message string.
     * @return True if the field is empty, false otherwise.
     */
    private boolean checkEditTextIsEmpty(TextInputLayout inputLayout, EditText field, int errorId) {
        String text = getSafeText(field.getText());
        if (TextUtils.isEmpty(text)) {
            inputLayout.setError(getString(errorId));
            return true;
        } else {
            inputLayout.setError(null);
            return false;
        }
    }

    /**
     * Sets up listeners to clear validation errors when the user starts typing.
     */
    private void setupValidationListeners() {
        clearErrorOnType(binding.inputLayoutEditOrgName);
        clearErrorOnType(binding.inputLayoutEditOrgField);
        clearErrorOnType(binding.inputLayoutEditOrgDesc);
        clearErrorOnType(binding.inputLayoutEditOrgContact);
    }

    /**
     * Helper to clear errors from a TextInputLayout on text change.
     *
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
     * Safely retrieves text from an Editable, handling nulls.
     *
     * @param editable The editable object.
     * @return The trimmed string or empty string.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }

    /**
     * Gets the file size of a URI in bytes.
     *
     * @param uri The URI of the file.
     * @return The size in bytes, or -1 if failed.
     */
    private long getFileSize(Uri uri) {
        try (android.database.Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                long size = cursor.getLong(sizeIndex);
                return size;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking file size", e);
        }
        return -1;
    }

    /**
     * Shows a toast message safely.
     *
     * @param msgId The resource ID of the message.
     */
    private void showToast(int msgId) {
        if (getContext() != null) Toast.makeText(getContext(), msgId, Toast.LENGTH_SHORT).show();
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