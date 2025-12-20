package com.example.volunhub.student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.MainActivity;
import com.example.volunhub.R;
import com.example.volunhub.databinding.ActivityStudentHomeBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.Set;

/**
 * The main container Activity for the Student side of the application.
 * Hosts the navigation graph and handles bottom navigation logic.
 */
public class StudentHomeActivity extends AppCompatActivity {

    private static final String TAG = "StudentHomeActivity";
    private FirebaseAuth mAuth;
    private NavController navController;

    /**
     * Initializes the activity, sets up the navigation controller, and handles NFR performance logging.
     *
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStudentHomeBinding binding = ActivityStudentHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (BaseRouterActivity.nfrLoginStartTime > 0) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - BaseRouterActivity.nfrLoginStartTime;

            Log.d("NFRTest", "Login/Routing Complete (Activity Created). Duration: " + duration + "ms");

            if (duration < 5000) {
                Log.d("NFRTest", "Login/Routing Test Passed (Success < 5.0s)");
            } else {
                Log.d("NFRTest", "Login/Routing Test Failed (Too Slow)");
            }

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

            // Handle Bottom Navigation highlighting for nested screens
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                Menu menu = studentBottomNav.getMenu();

                if (id == R.id.student_edit_profile) {
                    menu.findItem(R.id.student_nav_profile).setChecked(true);
                } else if (id == R.id.student_service_detail || id == R.id.student_view_org_profile) {
                    try {
                        // Check if we came from Applications tab
                        navController.getBackStackEntry(R.id.student_nav_application);
                        menu.findItem(R.id.student_nav_application).setChecked(true);
                    } catch (IllegalArgumentException e) {
                        // Otherwise, assume we came from Home
                        menu.findItem(R.id.student_nav_home).setChecked(true);
                    }
                }
            });
        } else {
            Log.e(TAG, "ERROR: NavHostFragment not found in layout!");
        }
    }

    /**
     * Signs the user out and returns to the main landing page.
     */
    public void returnToMain() {
        if (mAuth != null) {
            mAuth.signOut();
        }
        Intent intent = new Intent(StudentHomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Handles the Up button navigation in the toolbar.
     *
     * @return True if navigation was successful, false otherwise.
     */
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}