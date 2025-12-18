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

import com.example.volunhub.MainActivity;
import com.example.volunhub.R;
import com.example.volunhub.databinding.ActivityOrgHomeBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.Set;

public class OrgHomeActivity extends AppCompatActivity {

    private static final String TAG = "OrgHomeActivity";
    private FirebaseAuth mAuth;
//    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOrgHomeBinding binding = ActivityOrgHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView orgBottomNav = binding.orgBottomNav;
        Toolbar toolbar = binding.toolbar;
        mAuth = FirebaseAuth.getInstance();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.org_nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            Set<Integer> topLevelDestinations = new HashSet<>();
            topLevelDestinations.add(R.id.org_nav_dashboard);
            topLevelDestinations.add(R.id.org_nav_service);
            topLevelDestinations.add(R.id.org_nav_profile);

            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(orgBottomNav, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                // Check if we are on a "sub-screen" of My Services
                if (destination.getId() == R.id.org_manage_service ||
                        destination.getId() == R.id.org_view_student_profile) {

                    // Force the "My Services" tab to be checked
                    orgBottomNav.getMenu().findItem(R.id.org_nav_service).setChecked(true);
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
        Intent intent = new Intent(OrgHomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

//    @Override
//    public boolean onSupportNavigateUp() {
//        return navController.navigateUp() || super.onSupportNavigateUp();
//    }
}
