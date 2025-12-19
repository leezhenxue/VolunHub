package com.example.volunhub.student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.MainActivity;
import com.example.volunhub.R;
import com.example.volunhub.databinding.ActivityStudentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class StudentHomeActivity extends AppCompatActivity {

    private static final String TAG = "StudentHomeActivity";
    private FirebaseAuth mAuth;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStudentHomeBinding binding = ActivityStudentHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ---------------------------------------------------------
        // [NFR 3 TEST] STOP TIMER: Login/Routing Latency
        // ---------------------------------------------------------
        // This checks if we came from Login or MainActivity with a running timer.
        if (BaseRouterActivity.nfrLoginStartTime > 0) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - BaseRouterActivity.nfrLoginStartTime;

            Log.d("NFRTest", "NFR 3 - Login/Routing Complete (Activity Created). Duration: " + duration + "ms");

            if (duration < 5000) {
                Log.d("NFRTest", "NFR 3 - TEST PASSED (Success < 5.0s)");
            } else {
                Log.d("NFRTest", "NFR 3 - TEST FAILED (Too Slow)");
            }

            // Reset to 0 so navigation inside the app doesn't trigger false positives
            BaseRouterActivity.nfrLoginStartTime = 0;
        }

        BottomNavigationView studentBottomNav = binding.studentBottomNav;
        Toolbar toolbar = binding.toolbar;
        mAuth = FirebaseAuth.getInstance();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.student_nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            setSupportActionBar(toolbar);

            Set<Integer> topLevelDestinations = new HashSet<>();
            topLevelDestinations.add(R.id.student_nav_home);
            topLevelDestinations.add(R.id.student_nav_application);
            topLevelDestinations.add(R.id.student_nav_profile);

            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(studentBottomNav, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                Menu menu = studentBottomNav.getMenu();

                // --- Case A: Edit Profile ---
                if (id == R.id.student_edit_profile) {
                    menu.findItem(R.id.student_nav_profile).setChecked(true);
                }

                // --- Case B: Deep Navigation (Service Detail OR Org Profile) ---
                else if (id == R.id.student_service_detail || id == R.id.student_view_org_profile) {

                    try {
                        // Check if "My Applications" is ANYWHERE in the history stack.
                        // If this succeeds, it means we started from the Applications tab.
                        navController.getBackStackEntry(R.id.student_nav_application);

                        // If no error was thrown, highlight Applications
                        menu.findItem(R.id.student_nav_application).setChecked(true);

                    } catch (IllegalArgumentException e) {
                        // If "My Applications" is NOT in the stack, an exception is thrown.
                        // This means we must have come from Home.
                        menu.findItem(R.id.student_nav_home).setChecked(true);
                    }
                }
            });
        } else {
            Log.e(TAG, "ERROR: NavHostFragment not found in layout!");
        }
    }

    public void returnToMain() {
        if (mAuth != null) {
            mAuth.signOut();
        }
        Intent intent = new Intent(StudentHomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
