package com.example.volunhub.student.fragments.application;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.volunhub.databinding.FragmentStudentHistoryBinding;

public class StudentHistoryFragment extends Fragment {

    private FragmentStudentHistoryBinding binding;

    public StudentHistoryFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO:
        // 1. Setup your RecyclerView.
        // 2. Get today's date (Timestamp.now()).
        // 3. Query Firestore for applications where "studentId" == myId AND
        //    "status" == "Accepted" AND "serviceDate" < today's date.
        // 4. Update the RecyclerView.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
