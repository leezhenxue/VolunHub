package com.example.volunhub.student.fragments.application;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunhub.R;
import com.example.volunhub.databinding.ItemStudentApplicationBinding;
import com.example.volunhub.models.Application;

import java.util.List;

public class StudentApplicationAdapter extends RecyclerView.Adapter<StudentApplicationAdapter.ApplicationViewHolder> {

    private final List<Application> applicationList;
    private final Context context;

    public StudentApplicationAdapter(Context context, List<Application> applicationList) {
        this.context = context;
        this.applicationList = applicationList;
    }

    public class ApplicationViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentApplicationBinding binding;

        public ApplicationViewHolder(ItemStudentApplicationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Application application) {
            binding.textAppServiceTitle.setText(application.getServiceTitle());
            binding.textAppOrgName.setText(application.getOrgName());
            binding.textAppStatus.setText(application.getStatus());

            // Set the status color
            switch (application.getStatus()) {
                case "Accepted":
                    binding.textAppStatus.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                    break;
                case "Rejected":
                    binding.textAppStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red));
                    break;
                case "Pending":
                default:
                    binding.textAppStatus.setTextColor(ContextCompat.getColor(context, R.color.volunhub_accent));
                    break;
            }
        }
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentApplicationBinding binding = ItemStudentApplicationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ApplicationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        holder.bind(applicationList.get(position));
    }

    @Override
    public int getItemCount() {
        return applicationList.size();
    }
}