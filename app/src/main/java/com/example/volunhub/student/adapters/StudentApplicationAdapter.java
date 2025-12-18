package com.example.volunhub.student.adapters;

import android.content.Context;
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

    // --- 1. ADD LISTENER VARIABLE ---
    private OnItemClickListener listener;

    // --- 2. ADD INTERFACE ---
    public interface OnItemClickListener {
        void onItemClick(Application application);
    }

    // --- 3. ADD SETTER METHOD ---
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public StudentApplicationAdapter(Context context, List<Application> applicationList) {
        this.context = context;
        this.applicationList = applicationList;
    }

    public static class ApplicationViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentApplicationBinding binding;

        public ApplicationViewHolder(ItemStudentApplicationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Application application, Context context) {
            // Handle service title - show placeholder if null or empty
            String serviceTitle = application.getServiceTitle();
            if (serviceTitle == null || serviceTitle.isEmpty()) {
                serviceTitle = "Service Details Unavailable";
            }
            binding.textAppServiceTitle.setText(serviceTitle);
            
            // Check if service has been removed
            if (application.isServiceRemoved()) {
                binding.textAppOrgName.setText("Service Removed by Org");
                binding.textAppStatus.setText("REMOVED");
                binding.textAppStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red));
            } else {
                // Handle org name - show placeholder if null
                String orgName = application.getOrgName();
                if (orgName == null || orgName.isEmpty()) {
                    orgName = "Organization Name Unavailable";
                }
                binding.textAppOrgName.setText(orgName);
                
                // Handle status - normalize case and show placeholder if null
                String status = application.getStatus();
                if (status == null || status.isEmpty()) {
                    status = "Unknown";
                } else {
                    // Normalize status to handle case variations (e.g., "pending" -> "Pending")
                    status = normalizeStatus(status);
                }
                binding.textAppStatus.setText(status);

                // Set color based on normalized status
                switch (status) {
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
        
        /**
         * Normalizes status string to handle case variations.
         * Converts "pending" -> "Pending", "accepted" -> "Accepted", etc.
         */
        private String normalizeStatus(String status) {
            if (status == null || status.isEmpty()) {
                return "Unknown";
            }
            // Convert to lowercase for comparison, then capitalize first letter
            String lower = status.toLowerCase();
            if (lower.equals("pending")) {
                return "Pending";
            } else if (lower.equals("accepted")) {
                return "Accepted";
            } else if (lower.equals("rejected")) {
                return "Rejected";
            }
            // If it doesn't match known statuses, capitalize first letter and return
            return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
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
        Application application = applicationList.get(position);

        holder.bind(application, context);

        // --- 4. ATTACH CLICK LISTENER ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(application);
            }
        });
    }

    @Override
    public int getItemCount() {
        return applicationList.size();
    }
}