package com.example.volunhub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.volunhub.auth.LoginActivity;
import com.example.volunhub.org.OrgHomeActivity;
import com.example.volunhub.student.StudentHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class BaseRouterActivity extends AppCompatActivity {

    private static final String TAG = "BaseRouterActivity";
    public FirebaseAuth mAuth;
    public FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void routeUser(String uid) {
        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Log.d(TAG, "User data found in Firestore.");

                    String role = document.getString("role");

                    if ("Student".equals(role)) {
                        Log.d(TAG, "Role is Student. Sending to StudentHomeActivity.");
                        goToActivity(StudentHomeActivity.class);
                    } else if ("Organization".equals(role)) {
                        Log.d(TAG, "Role is Organization. Sending to OrgHomeActivity.");
                        goToActivity(OrgHomeActivity.class);
                    } else {
                        Log.w(TAG, "Role is null or unknown. Sending to LoginActivity.");
                        Toast.makeText(this, "User role not found.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        goToActivity(LoginActivity.class);
                    }
                } else {
                    Log.w(TAG, "No user document found in Firestore!");
                    Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    goToActivity(LoginActivity.class);
                }
            } else {
                Log.w(TAG, "get failed with ", task.getException());
                Toast.makeText(this, "Failed to read user data.", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                goToActivity(LoginActivity.class);
            }
        });
    }

    public void goToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
        finish();
    }
}
