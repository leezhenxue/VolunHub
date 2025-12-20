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

/**
 * RecyclerView Adapter for displaying a student's applications (My Applications).
 * Handles status color coding and removed services.
 */
public class StudentApplicationAdapter extends RecyclerView.Adapter<StudentApplicationAdapter.ApplicationViewHolder> {

    private final List<Application> applicationList;
    private final Context context;
    private OnItemClickListener listener;

    /**
     * Interface for handling clicks on application items.
     */
    public interface OnItemClickListener {
        /**
         * Called when an application item is clicked.
         * @param application The application object clicked.
         */
        void onItemClick(Application application);
    }

    /**
     * Sets the click listener.
     * @param listener The listener implementation.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Constructor for the adapter.
     * @param context Context for resource access.
     * @param applicationList List of applications.
     */
    public StudentApplicationAdapter(Context context, List<Application> applicationList) {
        this.context = context;
        this.applicationList = applicationList;
    }

    /**
     * Creates a new ViewHolder.
     * @param parent Parent view group.
     * @param viewType View type.
     * @return New ApplicationViewHolder.
     */
    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentApplicationBinding binding = ItemStudentApplicationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ApplicationViewHolder(binding);
    }

    /**
     * Binds data to the ViewHolder.
     * @param holder The ViewHolder.
     * @param position Position in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        Application application = applicationList.get(position);
        holder.bind(application, context);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(application);
            }
        });
    }

    /**
     * Returns total item count.
     * @return List size.
     */
    @Override
    public int getItemCount() {
        return applicationList.size();
    }

    /**
     * ViewHolder for application items.
     */
    public static class ApplicationViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentApplicationBinding binding;

        public ApplicationViewHolder(ItemStudentApplicationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds application data to views and sets text colors based on status.
         * @param application The application data.
         * @param context Context for retrieving colors.
         */
        public void bind(Application application, Context context) {
            String serviceTitle = application.getServiceTitle();
            if (serviceTitle == null || serviceTitle.isEmpty()) {
                serviceTitle = "Service Details Unavailable";
            }
            binding.textAppServiceTitle.setText(serviceTitle);

            if (application.isServiceRemoved()) {
                binding.textAppOrgName.setText("Service Removed by Org");
                binding.textAppStatus.setText("REMOVED");
                binding.textAppStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red));
            } else {
                String orgName = application.getOrgName();
                if (orgName == null || orgName.isEmpty()) {
                    orgName = "Organization Name Unavailable";
                }
                binding.textAppOrgName.setText(orgName);

                String status = normalizeStatus(application.getStatus());
                binding.textAppStatus.setText(status);

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
         * Normalizes the status string to handle case variations (e.g., "pending" -> "Pending").
         * @param status The raw status string from the database.
         * @return Normalized status string with proper capitalization.
         */
        private String normalizeStatus(String status) {
            if (status == null || status.isEmpty()) {
                return "Unknown";
            }
            String lower = status.toLowerCase();
            if (lower.equals("pending")) return "Pending";
            if (lower.equals("accepted")) return "Accepted";
            if (lower.equals("rejected")) return "Rejected";

            return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        }
    }
}