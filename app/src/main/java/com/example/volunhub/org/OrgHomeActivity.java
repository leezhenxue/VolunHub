package com.example.volunhub.org;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.MainActivity;
import com.example.volunhub.R;
import com.example.volunhub.databinding.ActivityOrgHomeBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.Set;

/**
 * The main container Activity for the Organization side of the app.
 * It hosts the Navigation Graph and handles the bottom navigation bar.
 */
public class OrgHomeActivity extends AppCompatActivity {

    private static final String TAG = "OrgHomeActivity";
    private FirebaseAuth mAuth;
    private NavController navController;

    /**
     * Initializes the activity, sets up navigation, and checks NFR performance metrics.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOrgHomeBinding binding = ActivityOrgHomeBinding.inflate(getLayoutInflater());
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

            // Reset to 0 so navigation inside the app doesn't trigger false positives
            BaseRouterActivity.nfrLoginStartTime = 0;
        }

        BottomNavigationView orgBottomNav = binding.orgBottomNav;
        Toolbar toolbar = binding.toolbar;
        mAuth = FirebaseAuth.getInstance();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.org_nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            Set<Integer> topLevelDestinations = new HashSet<>();
            topLevelDestinations.add(R.id.org_nav_dashboard);
            topLevelDestinations.add(R.id.org_nav_service);
            topLevelDestinations.add(R.id.org_nav_profile);

            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(orgBottomNav, navController);

        } else {
            Log.e(TAG, "ERROR: NavHostFragment not found in layout!");
        }
    }

    /**
     * Signs the user out and returns them to the main entry point (Login/Splash).
     */
    public void returnToMain() {
        if (mAuth != null) {
            mAuth.signOut();
        }
        Intent intent = new Intent(OrgHomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Handles the Up button navigation in the toolbar.
     * @return True if navigation was successful, false otherwise.
     */
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}