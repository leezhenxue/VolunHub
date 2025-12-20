package com.example.volunhub.org.adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.volunhub.R;
import com.example.volunhub.databinding.ItemApplicantBinding;
import com.example.volunhub.models.Applicant;

import java.util.List;

/**
 * Adapter for displaying a list of student applicants.
 * Handles different views for Pending vs Accepted/Rejected tabs.
 */
public class ApplicantAdapter extends RecyclerView.Adapter<ApplicantAdapter.ApplicantViewHolder> {

    private static final String TAG = "ApplicantAdapter";
    private final List<Applicant> applicantList;
    private final String tabMode;
    private final ApplicantClickListener listener;
    private final Context context;

    /**
     * Interface for handling button clicks (Accept/Reject).
     */
    public interface ApplicantClickListener {
        /**
         * Called when the Accept button is clicked.
         * @param applicant The applicant being accepted.
         */
        void onAcceptClick(Applicant applicant);

        /**
         * Called when the Reject button is clicked.
         * @param applicant The applicant being rejected.
         */
        void onRejectClick(Applicant applicant);
    }

    /**
     * Constructor for the adapter.
     * @param context The context used for loading resources and Glide.
     * @param applicantList The list of applicants to display.
     * @param tabMode Determines if Action Buttons (Accept/Reject) should be shown.
     * @param listener The listener for button click events.
     */
    public ApplicantAdapter(Context context, List<Applicant> applicantList, String tabMode, ApplicantClickListener listener) {
        this.context = context;
        this.applicantList = applicantList;
        this.tabMode = tabMode;
        this.listener = listener;
    }

    /**
     * Creates a new ViewHolder for an applicant item.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new ApplicantViewHolder that holds the View for each applicant item.
     */
    @NonNull
    @Override
    public ApplicantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemApplicantBinding binding = ItemApplicantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ApplicantViewHolder(binding);
    }

    /**
     * Binds data to the ViewHolder at the specified position.
     * @param holder The ViewHolder to update.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ApplicantViewHolder holder, int position) {
        holder.bind(applicantList.get(position));
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items.
     */
    @Override
    public int getItemCount() {
        return applicantList.size();
    }

    /**
     * ViewHolder class for caching view references.
     */
    public class ApplicantViewHolder extends RecyclerView.ViewHolder {
        private final ItemApplicantBinding binding;

        public ApplicantViewHolder(ItemApplicantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds applicant data to the views.
         * @param applicant The applicant object containing the data.
         */
        public void bind(Applicant applicant) {
            binding.textApplicantName.setText(applicant.getStudentName());
            binding.textApplicantIntro.setText(applicant.getStudentIntroduction());

            // Load profile image
            Glide.with(context)
                    .load(applicant.getProfileImageUrl())
                    .placeholder(R.drawable.default_profile_picture)
                    .circleCrop()
                    .into(binding.imageApplicantProfile);

            // Show/Hide Accept/Reject buttons based on the tab
            if ("Pending".equals(tabMode)) {
                binding.layoutApplicantButtons.setVisibility(View.VISIBLE);
            } else {
                binding.layoutApplicantButtons.setVisibility(View.GONE);
            }

            // Button Click Listeners
            binding.buttonAccept.setOnClickListener(v -> listener.onAcceptClick(applicant));
            binding.buttonReject.setOnClickListener(v -> listener.onRejectClick(applicant));

            // Main Card Click Listener (Navigate to Student Profile)
            itemView.setOnClickListener(v -> {
                String studentId = applicant.getStudentId();
                if (studentId == null || studentId.trim().isEmpty()) {
                    Toast.makeText(context, R.string.error_student_profile_not_available, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    Bundle args = new Bundle();
                    args.putString("studentId", studentId);
                    Navigation.findNavController(v).navigate(R.id.action_manage_service_to_view_student, args);
                } catch (Exception e) {
                    Log.e(TAG, "Navigation failed: " + e.getMessage());
                    Toast.makeText(context, R.string.error_nav_profile, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}