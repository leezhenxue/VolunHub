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
import com.example.volunhub.Constants;
import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgEditProfileBinding;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class OrgEditProfileFragment extends Fragment {

    private static final String TAG = "OrgEditProfile";
    private FragmentOrgEditProfileBinding binding;
    private DocumentReference orgDocRef;

    // Image Handling
    private Uri selectedImageUri = null;
    private String currentImageUrl = null;
    private boolean isImageRemoved = false;
    private ActivityResultLauncher<String> imagePickerLauncher;

    public OrgEditProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) return;
        orgDocRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

        // 1. Setup Validation Logic (Clear error when typing)
        clearErrorOnType(binding.inputLayoutEditOrgName);
        clearErrorOnType(binding.inputLayoutEditOrgField);
        clearErrorOnType(binding.inputLayoutEditOrgDesc);
        clearErrorOnType(binding.inputLayoutEditOrgContact);

        setupOrgFieldSpinner();

        // 2. Setup Image Picker
        setupImagePicker();

        // 3. Load Data
        loadCurrentProfileData();

        // 4. Button Listeners
        binding.btnChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.btnRemovePhoto.setOnClickListener(v -> removeProfilePhoto());

        binding.buttonSaveOrgProfile.setOnClickListener(v -> validateAndSave());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                // Check size (5MB limit)
                long fileSizeInBytes = getFileSize(uri);
                long sizeInMB = fileSizeInBytes / (1024 * 1024);
                if (sizeInMB > 5) {
                    Toast.makeText(getContext(), "Image is too large! Please choose an image under 5MB.", Toast.LENGTH_LONG).show();
                    return;
                }

                selectedImageUri = uri;
                isImageRemoved = false;

                binding.imageEditOrgLogo.setImageURI(uri);
                Toast.makeText(getContext(), "Photo selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeProfilePhoto() {
        selectedImageUri = null;
        isImageRemoved = true;
        binding.imageEditOrgLogo.setImageResource(R.drawable.default_profile_picture); // Default placeholder
        Toast.makeText(getContext(), "Photo removed (Save to apply)", Toast.LENGTH_SHORT).show();
    }

    private void loadCurrentProfileData() {
        orgDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                binding.editTextEditOrgName.setText(documentSnapshot.getString("orgCompanyName"));
                binding.editTextEditOrgDesc.setText(documentSnapshot.getString("orgDescription"));
                binding.autoCompleteEditOrgField.setText(documentSnapshot.getString("orgField"), false);

                // Note: Email logic removed

                // Handle Contact (+60 removal)
                String contact = documentSnapshot.getString("contact");
                if (contact == null) contact = documentSnapshot.getString("contactNumber");

                if (contact != null) {
                    if (contact.startsWith("+60")) {
                        binding.editTextEditOrgContact.setText(contact.substring(3));
                    } else {
                        binding.editTextEditOrgContact.setText(contact);
                    }
                }

                // Handle Image
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
                    // Show default if nothing in DB
                    binding.imageEditOrgLogo.setImageResource(R.drawable.default_profile_picture);
                }
            }
        });
    }

    private void validateAndSave() {
        // Validation Checks
        if (checkEditTextIsEmpty(binding.inputLayoutEditOrgName, binding.editTextEditOrgName, "Name is required")) return;
        if (checkEditTextIsEmpty(binding.inputLayoutEditOrgField, binding.autoCompleteEditOrgField, "Field is required")) return;
        if (checkEditTextIsEmpty(binding.inputLayoutEditOrgDesc, binding.editTextEditOrgDesc, "Description is required")) return;

        String rawContact = getSafeText(binding.editTextEditOrgContact.getText());
        if (TextUtils.isEmpty(rawContact)) {
            binding.inputLayoutEditOrgContact.setError("Contact number is required");
            return;
        }
        if (rawContact.length() < 8 || rawContact.length() > 10) {
            binding.inputLayoutEditOrgContact.setError("Enter 8-10 digits");
            return;
        }

        // Prepare Data
        setLoading(true);

        String finalContact = rawContact.startsWith("0") ? "+60" + rawContact.substring(1) : "+60" + rawContact;

        Map<String, Object> updates = new HashMap<>();
        updates.put("orgCompanyName", getSafeText(binding.editTextEditOrgName.getText()));
        updates.put("orgField", getSafeText(binding.autoCompleteEditOrgField.getText()));
        updates.put("orgDescription", getSafeText(binding.editTextEditOrgDesc.getText()));
        updates.put("contactNumber", finalContact);

        // Handle Image Logic
        if (selectedImageUri != null) {
            uploadImageToCloudinary(updates);
        } else if (isImageRemoved) {
            deleteOldImageIfExist();
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
                        String newImageUrl = (String) resultData.get("secure_url");

                        deleteOldImageIfExist();

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
                                Toast.makeText(getContext(), "Image upload failed", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void deleteOldImageIfExist() {
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Log.d(TAG, "Old image would be deleted here: " + currentImageUrl);
        }
    }

    private void updateFirestore(Map<String, Object> updates) {
        orgDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Error updating profile.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating document", e);
                });
    }

    // --- Helper Methods ---

    private void setLoading(boolean isLoading) {
        binding.buttonSaveOrgProfile.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.progressBarSave.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.editTextEditOrgName.setEnabled(!isLoading);
        binding.btnChangePhoto.setEnabled(!isLoading);
    }

    private void setupOrgFieldSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, Constants.ORG_FIELDS);
        binding.autoCompleteEditOrgField.setAdapter(adapter);
    }

    private boolean checkEditTextIsEmpty(TextInputLayout inputLayout, EditText field, String errorMessage) {
        String text = getSafeText(field.getText());
        if (TextUtils.isEmpty(text)) {
            inputLayout.setError(errorMessage);
            return true;
        } else {
            inputLayout.setError(null);
            return false;
        }
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