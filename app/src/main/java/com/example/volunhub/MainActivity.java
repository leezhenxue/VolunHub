package com.example.volunhub;

import android.os.Bundle;

import com.example.volunhub.auth.AuthActivity;
import com.example.volunhub.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseUser;

/**
 * The initial entry point (Launcher Activity) for the application.
 *
 * <p>This activity displays a Splash/Loading screen while it performs an asynchronous check of the user's authentication state.</p>
 *
 * <p>Its primary purpose is to route the user to the correct destination:</p>
 * <ul>
 * <li>If logged in: Routes to StudentHomeActivity or OrgHomeActivity based on the user's role.</li>
 * <li>If logged out: Routes to LoginActivity.</li>
 * </ul>
 */
public class MainActivity extends BaseRouterActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    /**
     * Called when the activity becomes visible to the user.
     *
     * <p>Checks for a currently signed-in Firebase user.</p>
     * <ul>
     * <li>If found: Routes the user based on their role (Student vs Org).</li>
     * <li>If not found: Redirects to the LoginActivity.</li>
     * </ul>
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToActivity(AuthActivity.class);
        } else {
            routeUser(currentUser.getUid());
        }
    }

}