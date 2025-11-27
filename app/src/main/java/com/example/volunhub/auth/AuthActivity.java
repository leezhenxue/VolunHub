package com.example.volunhub.auth;

import android.os.Bundle;
import com.example.volunhub.BaseRouterActivity;
import com.example.volunhub.databinding.ActivityAuthBinding;

public class AuthActivity extends BaseRouterActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAuthBinding binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}