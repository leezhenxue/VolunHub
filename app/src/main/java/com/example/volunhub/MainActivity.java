package com.example.volunhub;

import android.os.Bundle;
import android.util.Log;

import com.example.volunhub.auth.AuthActivity;
import com.example.volunhub.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseUser;

/**
 * The initial entry point (Launcher Activity) for the application.
 * It displays a loading screen while checking the user's authentication state.
 * Based on the result, it routes the user to either the Student/Org Home screen or the Login screen.
 */
public class MainActivity extends BaseRouterActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    /**
     * Called when the activity becomes visible.
     * It checks if a user is already signed in via Firebase.
     * If signed in, it routes to the Student/Org Home Page.
     * If not signed in, it redirects to the Login Page.
     */
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToActivity(AuthActivity.class);
        } else {
            BaseRouterActivity.nfrLoginStartTime = System.currentTimeMillis();
            Log.d("NFRTest", "Auto login time start calculate at: " + BaseRouterActivity.nfrLoginStartTime);

            routeUser(currentUser.getUid());
        }
    }
}