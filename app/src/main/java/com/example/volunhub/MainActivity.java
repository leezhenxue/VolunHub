package com.example.volunhub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.volunhub.auth.LoginActivity;
import com.example.volunhub.organization.OrgHomeActivity;
import com.example.volunhub.student.StudentHomeActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

//        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // 1. User is NOT logged in, send to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close this (blank) activity
        } else {
            // 2. User IS logged in, check their role and send to the correct Home
            routeUser(currentUser.getUid());
            // 'routeUser' is the method we built for LoginActivity
            // You will need to copy that method into MainActivity as well.
        }
    }

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
                            Toast.makeText(MainActivity.this, "User role not found.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut(); // Sign out to be safe
                        }
                    } else {
                        // This shouldn't happen if SignupActivity is working
                        Log.w(TAG, "No user document found in Firestore!");
                        Toast.makeText(MainActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                } else {
                    Log.w(TAG, "get failed with ", task.getException());
                    Toast.makeText(MainActivity.this, "Failed to read user data.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                }
            }
        });
    }

    /**
     * Sends user to StudentHomeActivity and closes this one.
     */
    private void goToStudentHome() {
        Intent intent = new Intent(MainActivity.this, StudentHomeActivity.class);
        startActivity(intent);
        finish(); // Close LoginActivity
    }

    /**
     * Sends user to OrgHomeActivity and closes this one.
     */
    private void goToOrgHome() {
        Intent intent = new Intent(MainActivity.this, OrgHomeActivity.class);
        startActivity(intent);
        finish(); // Close LoginActivity
    }
}