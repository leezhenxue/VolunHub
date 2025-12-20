package com.example.volunhub.auth;

import android.os.Bundle;
import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.databinding.ActivityAuthBinding;

/**
 * The host Activity for the user authentication flow.
 * It acts as a container for the Login and Sign Up fragments using the Navigation Component.
 * This activity handles the layout styling but delegates logic to the fragments.
 */
public class AuthActivity extends BaseRouterActivity {

    /**
     * Initializes the activity layout and the Navigation Component.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAuthBinding binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}