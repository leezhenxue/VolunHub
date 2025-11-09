package com.example.volunhub.auth;

// ... (Your other imports)
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// --- 1. IMPORT FIRESTORE ---
// We need this to read the user's role from the database
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
// ---

import com.example.volunhub.R;
// You will create these two new Activities (StudentHomeActivity, OrgHomeActivity)
import com.example.volunhub.student.StudentHomeActivity;
import com.example.volunhub.organization.OrgHomeActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;

    // --- 2. DECLARE FIRESTORE ---
    private FirebaseFirestore db; // The database object

    // UI elements
    private EditText editTextLoginEmail;
    private EditText editTextLoginPassword;
    private Button buttonLogin;
    private Button buttonGoToSignUp;
    private TextView textViewForgotPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // --- 3. INITIALIZE AUTH & FIRESTORE ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        // Find your UI elements (replace with your XML IDs)
        editTextLoginEmail = findViewById(R.id.edit_text_login_email);
        editTextLoginPassword = findViewById(R.id.edit_text_login_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonGoToSignUp = findViewById(R.id.button_go_to_sign_up);
        textViewForgotPassword = findViewById(R.id.text_view_forgot_password);

        // --- Click Listeners ---

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextLoginEmail.getText().toString().trim();
                String password = editTextLoginPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Call our signin method
                signIn(email, password);
            }
        });

        buttonGoToSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
                finish();
            }
        });

        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call a method to show the "Forgot Password" dialog
                showForgotPasswordDialog();
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, we just need to find their role
            Log.d(TAG, "User already logged in. Checking role...");
            routeUser(currentUser.getUid()); // Send to the correct home page
        }
    }

    private void showForgotPasswordDialog() {
        // Create the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your email to receive a password reset link.");

        // Set up the input field (an EditText) for the user to type their email
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        // Set up the "Send" button
        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = input.getText().toString().trim();

                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Call the Firebase method to send the reset email
                sendPasswordResetEmail(email);
            }
        });

        // Set up the "Cancel" button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Show the dialog
        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password reset email sent.");
                            Toast.makeText(LoginActivity.this, "Reset link sent to your email.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Log.w(TAG, "Error sending reset email", task.getException());
                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Tries to sign the user in with Firebase Authentication.
     */
    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in was a success!
                            Log.d(TAG, "signInWithEmail:success");

                            // --- 4. GET USER ID AND CHECK ROLE ---
                            // Now that login is successful, get the user's unique ID...
                            String uid = mAuth.getCurrentUser().getUid();
                            // ...and use it to find their role in the database.
                            routeUser(uid);

                        } else {
                            // If sign in fails, show a message
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * --- 5. NEW METHOD TO ROUTE THE USER ---
     * This method reads the user's document from Firestore to find their "role"
     * and sends them to the correct Home Activity.
     * @param uid The user's unique ID from FirebaseAuth
     */
    private void routeUser(String uid) {
        // Get the document reference for this user from the "users" collection
        DocumentReference docRef = db.collection("users").document(uid);

        // Read the document
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "User data found in Firestore.");

                        // Get the "role" field from the document
                        String role = document.getString("role");

                        if ("Student".equals(role)) {
                            // User is a Student, go to Student Home
                            Log.d(TAG, "Role is Student. Sending to StudentHomeActivity.");
                            goToStudentHome();
                        } else if ("Organization".equals(role)) {
                            // User is an Organization, go to Org Home
                            Log.d(TAG, "Role is Organization. Sending to OrgHomeActivity.");
                            goToOrgHome();
                        } else {
                            // Role is null or invalid
                            Log.w(TAG, "Role is null or unknown. Sending to Login.");
                            Toast.makeText(LoginActivity.this, "User role not found.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut(); // Sign out to be safe
                        }
                    } else {
                        // This shouldn't happen if SignupActivity is working
                        Log.w(TAG, "No user document found in Firestore!");
                        Toast.makeText(LoginActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                } else {
                    Log.w(TAG, "get failed with ", task.getException());
                    Toast.makeText(LoginActivity.this, "Failed to read user data.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                }
            }
        });
    }

    /**
     * Sends user to StudentHomeActivity and closes this one.
     */
    private void goToStudentHome() {
        Intent intent = new Intent(LoginActivity.this, StudentHomeActivity.class);
        startActivity(intent);
        finish(); // Close LoginActivity
    }

    /**
     * Sends user to OrgHomeActivity and closes this one.
     */
    private void goToOrgHome() {
        Intent intent = new Intent(LoginActivity.this, OrgHomeActivity.class);
        startActivity(intent);
        finish(); // Close LoginActivity
    }
}