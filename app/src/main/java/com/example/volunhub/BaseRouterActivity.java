package com.example.volunhub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.volunhub.auth.AuthActivity;
import com.example.volunhub.org.OrgHomeActivity;
import com.example.volunhub.student.StudentHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * An abstract base class that provides common authentication and navigation logic.
 * Used by MainActivity and AuthActivity.
 */
public abstract class BaseRouterActivity extends AppCompatActivity {

    private static final String TAG = "BaseRouterActivity";
    public static long nfrLoginStartTime = 0;
    public FirebaseAuth mAuth;
    public FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetches the user's document from Firestore and routes them based on their role.
     * Routes Students to StudentHomeActivity and Organizations to OrgHomeActivity.
     * @param uid The unique user ID from Firebase Authentication.
     */
    public void routeUser(String uid) {
        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Log.d(TAG, "User data found in Firestore.");
                    String role = document.getString("role");

                    if ("Student".equals(role)) {
                        Log.d(TAG, "Role is Student. Sending to StudentHomeActivity.");
                        goToActivity(StudentHomeActivity.class);
                    } else if ("Organization".equals(role)) {
                        Log.d(TAG, "Role is Organization. Sending to OrgHomeActivity.");
                        goToActivity(OrgHomeActivity.class);
                    } else {
                        handleRoutingError("Role is null or unknown.");
                    }
                } else {
                    handleRoutingError("No user document found in Firestore.");
                }
            } else {
                handleRoutingError("Failed to read user data: " + task.getException());
            }
        });
    }

    /**
     * Handles routing failures by logging the error, showing a toast, signing out, and redirecting to Login.
     * @param errorMessage The error description for logging.
     */
    private void handleRoutingError(String errorMessage) {
        Log.w(TAG, errorMessage);
        Toast.makeText(this, R.string.error_login_failed, Toast.LENGTH_SHORT).show(); // Ensure this string exists or use literal
        mAuth.signOut();
        goToActivity(AuthActivity.class);
    }

    /**
     * Helper method to start a new Activity and clear the previous one from the back stack.
     * This prevents the user from returning to the Login or Splash screen by pressing Back.
     * @param activityClass The class of the Activity to start.
     */
    public void goToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}