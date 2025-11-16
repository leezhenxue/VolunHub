package com.example.volunhub;

import android.os.Bundle;

import com.example.volunhub.auth.LoginActivity;
import com.example.volunhub.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseRouterActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToActivity(LoginActivity.class);
        } else {
            routeUser(currentUser.getUid());
        }
    }

}