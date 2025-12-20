package com.example.volunhub.student.applications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentStudentApplicationsBinding;
import com.example.volunhub.student.adapters.StudentAppViewPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Fragment that hosts a ViewPager2 to manage different student application views.
 * It provides tabs for My Applications, Saved services, and Application History.
 */
public class StudentApplicationsFragment extends Fragment {

    private FragmentStudentApplicationsBinding binding;

    public StudentApplicationsFragment() {}

    /**
     * Inflates the fragment layout using ViewBinding.
     * @param inflater The LayoutInflater object to inflate views.
     * @param container The parent view the fragment is attached to.
     * @param savedInstanceState Saved state bundle.
     * @return The root View of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentApplicationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Sets up the ViewPager2 adapter and connects it with the TabLayout.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize the ViewPager adapter
        StudentAppViewPagerAdapter viewPagerAdapter = new StudentAppViewPagerAdapter(requireActivity());

        // 2. Attach the adapter to the ViewPager
        binding.viewPager.setAdapter(viewPagerAdapter);

        // 3. Link TabLayout and ViewPager using TabLayoutMediator
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    // Configures tab labels based on position using string resources
                    switch (position) {
                        case 0:
                            tab.setText(getString(R.string.text_my_applications)); // Using "Pending" as "My Applications"
                            break;
                        case 1:
                            tab.setText(getString(R.string.btn_saved));
                            break;
                        case 2:
                            tab.setText(getString(R.string.tab_accepted)); // Or a specific "History" string if added
                            break;
                    }
                }
        ).attach();
    }

    /**
     * Cleans up the ViewBinding reference when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}