package com.example.volunhub.auth;

import android.os.Bundle;
import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.databinding.ActivityAuthBinding;

/**
 * The host Activity for the user authentication flow.
 *
 * <p>This Activity acts as a container using the Single-Activity Architecture pattern.
 * It does not handle logic itself but hosts the {@link androidx.navigation.fragment.NavHostFragment}
 * which manages navigation between the {@link LoginFragment} and {@link SignUpFragment}.</p>
 *
 * <p>Layout behavior:
 * <ul>
 * <li>Displays a centered CardView styling.</li>
 * <li>Provides the primary color background.</li>
 * <li>Handles fragment transitions.</li>
 * </ul>
 * </p>
 */
public class AuthActivity extends BaseRouterActivity {

    /**
     * Initializes the activity layout and the Navigation Component.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     * being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAuthBinding binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}