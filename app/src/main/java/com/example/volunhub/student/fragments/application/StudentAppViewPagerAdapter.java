package com.example.volunhub.student.fragments.application;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StudentAppViewPagerAdapter extends FragmentStateAdapter {

    public StudentAppViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // This method returns the correct fragment for each tab position
        switch (position) {
            case 0:
                return new StudentMyApplicationsFragment(); // The main list
            case 1:
                return new StudentSavedListFragment();    // The saved list
            case 2:
                return new StudentHistoryFragment();      // The history list
            default:
                return new StudentMyApplicationsFragment();
        }
    }

    @Override
    public int getItemCount() {
        // We have 3 tabs in total
        return 3;
    }
}
