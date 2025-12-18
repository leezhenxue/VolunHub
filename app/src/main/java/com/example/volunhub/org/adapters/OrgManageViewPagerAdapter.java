package com.example.volunhub.org.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.volunhub.org.service.OrgAcceptedApplicantsFragment;
import com.example.volunhub.org.service.OrgPendingApplicantsFragment;
import com.example.volunhub.org.service.OrgRejectedApplicantsFragment;

public class OrgManageViewPagerAdapter extends FragmentStateAdapter {

    // --- 1. Add a field to hold the serviceId ---
    private final String serviceId;

    // --- 2. Modify the constructor to accept the serviceId ---
    public OrgManageViewPagerAdapter(@NonNull Fragment fragment, String serviceId) {
        super(fragment);
        this.serviceId = serviceId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // --- 3. Pass the serviceId to each fragment ---
        switch (position) {
            case 1:
                return OrgAcceptedApplicantsFragment.newInstance(serviceId);
            case 2:
                return OrgRejectedApplicantsFragment.newInstance(serviceId);
            default:
                return OrgPendingApplicantsFragment.newInstance(serviceId);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}