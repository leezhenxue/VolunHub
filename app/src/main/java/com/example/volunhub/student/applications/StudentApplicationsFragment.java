package com.example.volunhub.student.applications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.volunhub.databinding.FragmentStudentApplicationsBinding;
import com.example.volunhub.student.adapters.StudentAppViewPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;

public class StudentApplicationsFragment extends Fragment {

    private FragmentStudentApplicationsBinding binding;

    // constructor
    public StudentApplicationsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentApplicationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize the adapter
        StudentAppViewPagerAdapter viewPagerAdapter = new StudentAppViewPagerAdapter(requireActivity());

        // 2. Set the adapter on the ViewPager
        binding.viewPager.setAdapter(viewPagerAdapter);

        // 3. Link the TabLayout to the ViewPager
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    // This sets the text for each tab
                    switch (position) {
                        case 0:
                            tab.setText("My Applications");
                            break;
                        case 1:
                            tab.setText("Saved");
                            break;
                        case 2:
                            tab.setText("History");
                            break;
                    }
                }
        ).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}