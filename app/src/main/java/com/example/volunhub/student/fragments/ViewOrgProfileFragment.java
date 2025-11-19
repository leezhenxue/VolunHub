package com.example.volunhub.student.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.example.volunhub.databinding.FragmentViewOrgProfileBinding;

public class ViewOrgProfileFragment extends Fragment {
    private FragmentViewOrgProfileBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentViewOrgProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


}
