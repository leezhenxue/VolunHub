package com.example.volunhub.org.fragments.service;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.volunhub.databinding.FragmentOrgServiceBinding;

public class OrgServiceFragment extends Fragment {

    private FragmentOrgServiceBinding binding;

    // constructor
    public OrgServiceFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrgServiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // write binding element function here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
