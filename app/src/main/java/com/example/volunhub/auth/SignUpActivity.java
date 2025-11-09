package com.example.volunhub.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.volunhub.R;
import com.example.volunhub.organization.OrgHomeActivity;
import com.example.volunhub.student.StudentHomeActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    //declare auth
    private FirebaseAuth mAuth;
    private Button buttonBackToLogin, buttonSignUp, buttonUploadProfilePicture;
    private EditText editTextEmail, editTextPassword, editTextRetypePassword, editTextStudentName, editTextStudentAge, editTextStudentExperience, editTextStudentIntroduction, editTextOrgCompanyName, editTextOrgDescription;
    private RadioGroup radioGroupRole, radioGroupGender;
    private AutoCompleteTextView autoCompleteOrgField;
    private LinearLayout linearLayoutStudent, linearLayoutOrganization;
    private static final String DEFAULT_PROFILE_PIC_URL = "https://res.cloudinary.com/dw1ccoqrq/image/upload/v1762680274/default_profile_picture_bnprgw.png";
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ImageView imageViewProfilePicture;



    // In SignUpActivity.java

    private static final String[] ORG_FIELDS = new String[] {
            // Health & Wellness
            "Health Services",
            "Mental Health Support",
            "Public Health & Awareness",
            "Disability Support",

            // Education
            "Education & Literacy",
            "Tutoring & Mentoring",
            "STEM Education",
            "Arts Education",

            // Social Services
            "Poverty Alleviation",
            "Homelessness Support",
            "Food Bank & Hunger Relief",
            "Youth Development",
            "Elderly Care",
            "Refugee & Immigrant Services",

            // Environment & Animals
            "Animal Welfare & Rescue",
            "Environmental Conservation",
            "Wildlife Protection",
            "Climate Action",

            // Community
            "Community Development",
            "Arts & Culture",
            "Sports & Recreation",
            "Public Safety",
            "Disaster Relief",

            // Advocacy & Rights
            "Human Rights",
            "Legal Aid & Justice",
            "Women's Rights",
            "LGBTQ+ Advocacy",

            // Professional
            "Technology & IT Support",
            "Event Management",
            "Career Services"
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        buttonBackToLogin = findViewById(R.id.button_back_to_sign_up);
        buttonSignUp = findViewById(R.id.button_sign_up);
        buttonUploadProfilePicture = findViewById(R.id.button_upload_profile_picture);
        editTextEmail = findViewById(R.id.edit_text_sign_up_email);
        editTextPassword = findViewById(R.id.edit_text_sign_up_password);
        editTextRetypePassword = findViewById(R.id.edit_text_sign_up_retype_password);
        autoCompleteOrgField = findViewById(R.id.auto_complete_org_field);
        radioGroupRole = findViewById(R.id.radio_group_role);
        radioGroupGender = findViewById(R.id.radio_group_student_gender);
        editTextStudentName = findViewById(R.id.edit_text_student_name);
        editTextStudentAge = findViewById(R.id.edit_text_student_age);
        editTextStudentExperience = findViewById(R.id.edit_text_student_experience);
        editTextStudentIntroduction = findViewById(R.id.edit_text_student_introduction);
        editTextOrgCompanyName = findViewById(R.id.edit_text_org_company_name);
        editTextOrgDescription = findViewById(R.id.edit_text_org_description);
        autoCompleteOrgField = findViewById(R.id.auto_complete_org_field);
        linearLayoutStudent = findViewById(R.id.linear_layout_sign_up_student);
        linearLayoutOrganization = findViewById(R.id.linear_layout_sign_up_org);
        imageViewProfilePicture = findViewById(R.id.image_view_profile_picture);



        buttonBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        buttonSignUp.setOnClickListener(View -> {
            registerUser();
        });

        // Create the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ORG_FIELDS);

        // Attach the adapter to the AutoCompleteTextView
        autoCompleteOrgField.setAdapter(adapter);

        radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_button_student) {
                linearLayoutStudent.setVisibility(View.VISIBLE);
                linearLayoutOrganization.setVisibility(View.GONE);

            } else if (checkedId == R.id.radio_button_organization) {
                linearLayoutStudent.setVisibility(View.GONE);
                linearLayoutOrganization.setVisibility(View.VISIBLE);
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            selectedImageUri = uri;
                            Toast.makeText(SignUpActivity.this, "Image selected", Toast.LENGTH_SHORT).show();
                            imageViewProfilePicture.setImageURI(uri);
                        } else {
                            Toast.makeText(SignUpActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        buttonUploadProfilePicture.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String retypePassword = editTextRetypePassword.getText().toString().trim();


        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return; // Stop the code
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return; // Stop the code
        }

        if (!password.equals(retypePassword)) {
            editTextRetypePassword.setError("Passwords do not match");
            editTextRetypePassword.requestFocus();
            return; // Stop the code
        }

        RadioGroup radioGroupRole = findViewById(R.id.radio_group_role);
        int selectedId = radioGroupRole.getCheckedRadioButtonId();

        if (selectedId == -1) {
            // No button was selected
            Toast.makeText(this, "Please select a role (Student or Organization)", Toast.LENGTH_SHORT).show();
            return; // Stop the signup process
        }

// THIS IS THE FIX:
// Get the role ONE TIME and store it in a FINAL variable.
// The compiler now knows this variable will never change.
        final String role = ((RadioButton) findViewById(selectedId)).getText().toString();
        Log.d(TAG, "User selected role: " + role);

        // Now you can safely use the 'role' variable
        Log.d(TAG, "User selected role: " + role);


        if (role.equals("Student")) {
            String studentName = editTextStudentName.getText().toString().trim();
            int studentAge = Integer.parseInt(editTextStudentAge.getText().toString().trim());
            String studentExperience = editTextStudentExperience.getText().toString().trim();
            String studentIntroduction = editTextStudentIntroduction.getText().toString().trim();

            if (studentName.isEmpty()) {
                editTextStudentName.setError("Full Name is required");
                editTextStudentName.requestFocus();
                return; // Stop the code
            }

            if (studentAge == 0) {
                editTextStudentAge.setError("Age is required");
                editTextStudentAge.requestFocus();
                return; // Stop the code
            }

            if (studentExperience.isEmpty()) {
                editTextStudentExperience.setError("Experience is required");
                editTextStudentExperience.requestFocus();
                return; // Stop the code
            }

            if (studentIntroduction.isEmpty()) {
                editTextStudentIntroduction.setError("Introduction is required");
                editTextStudentIntroduction.requestFocus();
                return;
            }

        } else if (role.equals("Organization")) {
            String orgCompanyName = editTextOrgCompanyName.getText().toString().trim();
            String orgDescription = editTextOrgDescription.getText().toString().trim();
            String orgField = autoCompleteOrgField.getText().toString().trim();

            if (orgCompanyName.isEmpty()) {
                editTextOrgCompanyName.setError("Company Name is required");
                editTextOrgCompanyName.requestFocus();
                return; // Stop the code
            }

            if (orgDescription.isEmpty()) {
                editTextOrgDescription.setError("Description is required");
                editTextOrgDescription.requestFocus();
                return; // Stop the code
            }

            if (orgField.isEmpty()) {
                autoCompleteOrgField.setError("Field is required");
                autoCompleteOrgField.requestFocus();
                return; // Stop the code
            }
        } else {
            Toast.makeText(this, "Please select a role (Student or Organization)", Toast.LENGTH_SHORT).show();
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(SignUpActivity.this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(SignUpActivity.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "createUserWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();
                String uid = user.getUid();



                Map<String, Object> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("role", role);

                // Get the common fields (if any)

                // NOW, add role-specific fields
                if (role.equals("Student")) {
                    // Only get and put Student data if the role is Student
                    String studentName = editTextStudentName.getText().toString().trim();
                    int studentAge = 0; // Default to 0
                    try {
                        studentAge = Integer.parseInt(editTextStudentAge.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Student age was empty, defaulting to 0.");
                    }
                    String studentGender = radioGroupGender.getCheckedRadioButtonId() == R.id.radio_button_student_male ? "Male" : "Female";
                    String studentExperience = editTextStudentExperience.getText().toString().trim();
                    String studentIntroduction = editTextStudentIntroduction.getText().toString().trim();

                    userData.put("studentName", studentName);
                    userData.put("studentAge", studentAge);
                    userData.put("studentGender", studentGender);
                    userData.put("studentExperience", studentExperience);
                    userData.put("studentIntroduction", studentIntroduction);

                } else if (role.equals("Organization")) {
                    // Only get and put Organization data if the role is Organization
                    String orgCompanyName = editTextOrgCompanyName.getText().toString().trim();
                    String orgDescription = editTextOrgDescription.getText().toString().trim();
                    String orgField = autoCompleteOrgField.getText().toString().trim();

                    userData.put("orgCompanyName", orgCompanyName);
                    userData.put("orgDescription", orgDescription);
                    userData.put("orgField", orgField);
                }

                if (selectedImageUri != null) {
                    // Path 1: User selected an image. Upload it.
                    uploadImageToCloudinary(uid, selectedImageUri, userData, role);
                } else {
                    // Path 2: User did NOT select an image. Use the default.
                    userData.put("profileImageUrl", DEFAULT_PROFILE_PIC_URL);
                    saveMapToFirestore(uid, userData, role);
                }

            } else {
                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                Toast.makeText(SignUpActivity.this, "Sign up failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sends user to StudentHomeActivity and closes this one.
     */
    private void goToStudentHome() {
        Intent intent = new Intent(SignUpActivity.this, StudentHomeActivity.class);
        startActivity(intent);
        finish(); // Close LoginActivity
    }

    /**
     * Sends user to OrgHomeActivity and closes this one.
     */
    private void goToOrgHome() {
        Intent intent = new Intent(SignUpActivity.this, OrgHomeActivity.class);
        startActivity(intent);
        finish(); // Close LoginActivity
    }

    /**
     * This is Path 1 - The NEW Cloudinary Upload Method
     * Uploads the image to Cloudinary first, then saves the user.
     */
    private void uploadImageToCloudinary(String uid, Uri imageUri, Map<String, Object> userData, final String role) {

        Toast.makeText(SignUpActivity.this, "Uploading image...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(imageUri)
                // We use the user's UID as the "public_id" (the filename)
                // Put it in a specific folder
                .option("folder", "profileImages")
                .unsigned("volunhub")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Cloudinary upload started...");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // You could show a progress bar here
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Upload was successful!
                        // Get the public URL from the resultData map.
                        String imageUrl = (String) resultData.get("secure_url");

                        Log.d(TAG, "Image uploaded to Cloudinary: " + imageUrl);

                        // Add this URL to our user data map
                        userData.put("profileImageUrl", imageUrl);

                        // Now, save the user data (with the new URL) to Firestore.
                        saveMapToFirestore(uid, userData, role);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Error uploading to Cloudinary: " + error.getDescription());
                        Toast.makeText(SignUpActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();

                        // Fallback: Even if image upload fails, save the user data
                        // with the default profile picture URL.
                        userData.put("profileImageUrl", DEFAULT_PROFILE_PIC_URL);
                        saveMapToFirestore(uid, userData, role);
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // (Can ignore this for your project)
                    }
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
        // 1. Get an instance of the Firestore database
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 2. Specify the path: "users" collection -> [user's_uid] document
        db.collection("users").document(uid)
                .set(userData)  // 3. Set the data (this will create or overwrite the document)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // 4. Handle Success
                        Log.d(TAG, "User document successfully created in Firestore!");
                        Toast.makeText(SignUpActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();

                        // 5. Navigate to the correct home page
                        if (role.equals("Student")) {
                            goToStudentHome();
                        } else if (role.equals("Organization")) {
                            goToOrgHome();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // 6. Handle Failure
                        Log.w(TAG, "Error creating user document in Firestore", e);
                        Toast.makeText(SignUpActivity.this, "Error: Could not save user data.", Toast.LENGTH_LONG).show();

                        // Optional: If saving fails, you might want to delete the
                        // half-created Auth user so they can try signing up again.
                        // mAuth.getCurrentUser().delete();
                    }
                });
    }



}