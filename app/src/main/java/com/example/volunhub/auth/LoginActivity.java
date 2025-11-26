package com.example.volunhub.auth;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.databinding.ActivityLoginBinding;
import com.example.volunhub.databinding.DialogForgotPasswordBinding;

import com.google.firebase.auth.FirebaseUser;

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
public class LoginActivity extends BaseRouterActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonLogin.setOnClickListener(view -> {
            String email = binding.editTextLoginEmail.getText().toString().trim();
            String password = binding.editTextLoginPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            signIn(email, password);
        });

        binding.buttonGoToSignUp.setOnClickListener(view ->
            goToActivity(SignUpActivity.class)
        );

        binding.textViewForgotPassword.setOnClickListener(view ->
            showForgotPasswordDialog()
        );
    }

    /**
     * Displays a custom AlertDialog to capture the user's email.
     * If the input is valid and exist in Firebase Auth, it triggers the password reset email.
     */
    private void showForgotPasswordDialog() {
        DialogForgotPasswordBinding dialogBinding = DialogForgotPasswordBinding.inflate(LayoutInflater.from(this));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your email to receive a password reset link.");
        builder.setView(dialogBinding.getRoot());

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = dialogBinding.edittextDialogForgotEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            sendPasswordResetEmail(email);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Sends a password reset email via Firebase Auth.
     * Displays a success or error Toast based on the result.
     *
     * @param email The email address entered in the dialog.
     */
    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Password reset email sent.");
                Toast.makeText(LoginActivity.this, "Reset link sent to your email.", Toast.LENGTH_LONG).show();
            } else {
                Log.w(TAG, "Error sending reset email", task.getException());
                String errorMessage = "Failed to send reset email. Please try again.";
                if (task.getException() != null && task.getException().getMessage() != null) {
                    errorMessage = task.getException().getMessage();
                }
                Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Authenticates the user with Firebase using email and password.
     * On success: Retrieves the User ID and routes to the correct home screen.
     * On failure: Displays an error message.
     *
     * @param email    The user's email.
     * @param password The user's password.
     */
    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "signInWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    routeUser(user.getUid());
                } else {
                    Log.w(TAG, "signIn:success, but currentUser is null.");
                    Toast.makeText(LoginActivity.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "signInWithEmail:failure", task.getException());
                Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}