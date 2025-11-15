package com.example.volunhub.org.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.example.volunhub.R;
import com.example.volunhub.databinding.FragmentOrgProfileBinding;
import com.example.volunhub.org.OrgHomeActivity;

public class OrgProfileFragment extends Fragment {

    private FragmentOrgProfileBinding binding;

    // constructor
    public OrgProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrgProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenu();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupMenu() {
        MenuHost menuHost = requireActivity();
        LifecycleOwner lifecycleOwner = getViewLifecycleOwner();

        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.toolbar_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.toolbar_logout) {
                    if (getActivity() != null) {
                        ((OrgHomeActivity) getActivity()).returnToMain();
                    }
                    return true;
                }
                return false;
            }

        }, lifecycleOwner, Lifecycle.State.RESUMED); // The menu is only visible when this fragment is resumed
    }
}
