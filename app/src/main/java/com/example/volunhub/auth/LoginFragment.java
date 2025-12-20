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

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;

    public LoginFragment() {}

    /**
     * Inflates the layout for this fragment using ViewBinding.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes Firebase and sets up click listeners for the login button and navigation links.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        binding.buttonLogin.setOnClickListener(v -> {
            String email = getSafeText(binding.editTextLoginEmail.getText());
            String password = getSafeText(binding.editTextLoginPassword.getText());
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), R.string.error_login_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            BaseRouterActivity.nfrLoginStartTime = System.currentTimeMillis();
            Log.d("NFRTest", "Manual login time start calculate at: " + BaseRouterActivity.nfrLoginStartTime);
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
     * Shows a popup dialog for the user to enter their email for password recovery.
     */
    private void showForgotPasswordDialog() {
        if (getContext() == null) return;

        DialogForgotPasswordBinding dialogBinding = DialogForgotPasswordBinding.inflate(LayoutInflater.from(getContext()));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(R.string.dialog_title_reset_password);
        builder.setMessage(R.string.dialog_msg_reset_password);
        builder.setView(dialogBinding.getRoot());

        builder.setPositiveButton(R.string.btn_send, (dialog, which) -> {
            String email = getSafeText(dialogBinding.edittextDialogForgotEmail.getText());
            if (email.isEmpty()) {
                Toast.makeText(getContext(), R.string.error_email_required, Toast.LENGTH_SHORT).show();
                return;
            }
            sendPasswordResetEmail(email);
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Sends a password reset email using Firebase Auth.
     * @param email The email address entered in the dialog.
     */
    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Password reset email sent.");
                Toast.makeText(getContext(), R.string.toast_reset_email_sent, Toast.LENGTH_LONG).show();
            } else {
                Log.w(TAG, "Error sending reset email", task.getException());
                Toast.makeText(getContext(), R.string.toast_reset_email_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Authenticates the user with Firebase and routes them to the correct dashboard.
     * @param email The user's email.
     * @param password The user's password.
     */
    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "signInWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null && getActivity() instanceof BaseRouterActivity) {
                    ((BaseRouterActivity) getActivity()).routeUser(user.getUid());
                } else {
                    Toast.makeText(getContext(), R.string.toast_login_failed, Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "signInWithEmail:failure", task.getException());
                Toast.makeText(getContext(), R.string.toast_auth_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Helper to get trimmed text from an EditText and handle null values.
     * @param editable The raw text object from the view.
     * @return A trimmed String, or an empty string if input was null.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
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