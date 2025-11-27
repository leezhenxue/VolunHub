package com.example.volunhub.auth;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.R;
import com.example.volunhub.databinding.DialogForgotPasswordBinding;

import com.example.volunhub.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
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
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;

    public LoginFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        binding.buttonLogin.setOnClickListener(v -> {
            String email = getSafeText(binding.editTextLoginEmail.getText());
            String password = getSafeText(binding.editTextLoginPassword.getText());
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Email and password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            signIn(email, password);
        });

        binding.buttonGoToSignUp.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_login_to_sign_up)
        );

        binding.textViewForgotPassword.setOnClickListener(v ->
            showForgotPasswordDialog()
        );
    }

    /**
     * Displays a custom AlertDialog to capture the user's email.
     * If the input is valid and exist in Firebase Auth, it triggers the password reset email.
     */
    private void showForgotPasswordDialog() {
        if (getContext() == null) return;
        DialogForgotPasswordBinding dialogBinding = DialogForgotPasswordBinding.inflate(LayoutInflater.from(getContext()));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your email to receive a password reset link.");
        builder.setView(dialogBinding.getRoot());

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = getSafeText(dialogBinding.edittextDialogForgotEmail.getText());
            if (email.isEmpty()) {
                Toast.makeText(getContext(), "Please enter your email", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "Reset link sent to your email.", Toast.LENGTH_LONG).show();
            } else {
                Log.w(TAG, "Error sending reset email", task.getException());
                String errorMessage = "Failed to send reset email. Please try again.";
                if (task.getException() != null && task.getException().getMessage() != null) {
                    errorMessage = task.getException().getMessage();
                }
                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();
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
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "signInWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();
                androidx.fragment.app.FragmentActivity activity = getActivity();
                if (user != null && activity instanceof BaseRouterActivity) {
                    ((BaseRouterActivity) getActivity()).routeUser(user.getUid());
                } else {
                    Log.w(TAG, "signIn:success, but currentUser is null.");
                    Toast.makeText(getContext(), "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "signInWithEmail:failure", task.getException());
                Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Safely retrieves text from an {@link android.text.Editable} object, handling potential null values.
     *
     * <p>This helper method prevents {@link NullPointerException} when accessing text from an EditText,
     * as {@code getText()} can theoretically return null. It also automatically trims leading and
     * trailing whitespace from the result.</p>
     *
     * @param editable The Editable object returned by {@code EditText.getText()}.
     * @return A trimmed String containing the text, or an empty String ("") if the input was null.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }

}