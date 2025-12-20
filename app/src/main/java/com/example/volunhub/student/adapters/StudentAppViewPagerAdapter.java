package com.example.volunhub.student.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.volunhub.student.applications.StudentHistoryFragment;
import com.example.volunhub.student.applications.StudentMyApplicationsFragment;
import com.example.volunhub.student.applications.StudentSavedListFragment;

/**
 * ViewPager Adapter for handling tabs in the Student Applications section.
 * Manages switching between My Applications, Saved List, and History.
 */
public class StudentAppViewPagerAdapter extends FragmentStateAdapter {

    /**
     * Constructor for the adapter.
     * @param fragmentActivity The parent activity hosting the ViewPager.
     */
    public StudentAppViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * Creates the fragment corresponding to the selected tab position.
     * @param position The index of the tab (0: My Apps, 1: Saved, 2: History).
     * @return The fragment to display.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new StudentMyApplicationsFragment();
            case 1:
                return new StudentSavedListFragment();
            case 2:
                return new StudentHistoryFragment();
            default:
                return new StudentMyApplicationsFragment();
        }
    }

    /**
     * Returns the total number of tabs.
     * @return The count of tabs (3).
     */
    @Override
    public int getItemCount() {
        return 3;
    }
}