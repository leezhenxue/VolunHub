package com.example.volunhub.org;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.volunhub.org.fragments.service.OrgAcceptedApplicantsFragment;
import com.example.volunhub.org.fragments.service.OrgPendingApplicantsFragment;
import com.example.volunhub.org.fragments.service.OrgRejectedApplicantsFragment;

public class OrgManageViewPagerAdapter extends FragmentStateAdapter {

    // --- 1. Add a field to hold the serviceId ---
    private final String serviceId;

    // --- 2. Modify the constructor to accept the serviceId ---
    public OrgManageViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, String serviceId) {
        super(fragmentActivity);
        this.serviceId = serviceId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // --- 3. Pass the serviceId to each fragment ---
        switch (position) {
            case 0:
                return OrgPendingApplicantsFragment.newInstance(serviceId);
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