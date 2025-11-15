package com.example.volunhub.student.fragments.application;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.volunhub.databinding.FragmentStudentSavedListBinding;

public class StudentSavedListFragment extends Fragment {

    private FragmentStudentSavedListBinding binding;

    public StudentSavedListFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSavedListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO:
        // 1. Setup your RecyclerView.
        // 2. Query Firestore for the current user's document.
        // 3. Get the "savedServices" array.
        // 4. Query the "services" collection to get all services in that array.
        // 5. Update the RecyclerView.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
