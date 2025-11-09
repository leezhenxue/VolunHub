package com.example.volunhub.student;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.volunhub.R;
import com.example.volunhub.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class StudentHomeActivity extends AppCompatActivity {

    // Declare Firebase Auth
    private FirebaseAuth mAuth;

    // Declare UI elements
    private Button buttonStudentHomeLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);
        buttonStudentHomeLogout = findViewById(R.id.button_student_home_logout);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        buttonStudentHomeLogout.setOnClickListener(v -> {
            // Sign the user out of Firebase
            mAuth.signOut();

            // Send the user back to the LoginActivity
            goToLogin();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(StudentHomeActivity.this, LoginActivity.class);
        // These flags are important for a clean logout
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Destroy this activity
    }
}
