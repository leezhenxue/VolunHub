package com.example.volunhub.org.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.volunhub.org.service.OrgAcceptedApplicantsFragment;
import com.example.volunhub.org.service.OrgPendingApplicantsFragment;
import com.example.volunhub.org.service.OrgRejectedApplicantsFragment;

/**
 * Adapter for the ViewPager in the "Manage Service" screen.
 * It manages the tabs: Pending, Accepted, and Rejected.
 */
public class OrgManageViewPagerAdapter extends FragmentStateAdapter {

    private final String serviceId;

    public OrgManageViewPagerAdapter(@NonNull Fragment fragment, String serviceId) {
        super(fragment);
        this.serviceId = serviceId;
    }

    /**
     * Creates the correct fragment based on the tab position.
     * Passes the serviceId to each fragment so they can load data.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return OrgAcceptedApplicantsFragment.newInstance(serviceId);
            case 2:
                return OrgRejectedApplicantsFragment.newInstance(serviceId);
            default:
                return OrgPendingApplicantsFragment.newInstance(serviceId);
        }
    }

    /**
     * Returns the total number of tabs.
     */
    @Override
    public int getItemCount() {
        return 3;
    }
}