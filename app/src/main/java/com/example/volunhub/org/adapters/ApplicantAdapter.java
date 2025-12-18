package com.example.volunhub.org.adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // You'll need to add this dependency
import com.example.volunhub.R;
import com.example.volunhub.databinding.ItemApplicantBinding;
import com.example.volunhub.models.Applicant;

import java.util.List;

public class ApplicantAdapter extends RecyclerView.Adapter<ApplicantAdapter.ApplicantViewHolder> {

    private final String TAG = "ApplicantAdapter";
    private final List<Applicant> applicantList;
    private final String tabMode;
    private final ApplicantClickListener listener;
    final private Context context;

    // 1. Click listener interface for Accept, Reject, and viewing the profile
    public interface ApplicantClickListener {
        void onAcceptClick(Applicant applicant);
        void onRejectClick(Applicant applicant);
        void onProfileClick(Applicant applicant);
    }

    // 2. ViewHolder
    public class ApplicantViewHolder extends RecyclerView.ViewHolder {
        private final ItemApplicantBinding binding;

        public ApplicantViewHolder(ItemApplicantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Applicant applicant) {
            binding.textApplicantName.setText(applicant.getStudentName());
            binding.textApplicantIntro.setText(applicant.getStudentIntroduction());

            // Use Glide (or another library) to load the profile image
            Glide.with(context)
                    .load(applicant.getProfileImageUrl())
                    .placeholder(R.drawable.default_profile_picture) // Default icon
                    .circleCrop()
                    .into(binding.imageApplicantProfile);

            // 3. Show/Hide buttons based on the tab
            if (tabMode.equals("Pending")) {
                binding.layoutApplicantButtons.setVisibility(View.VISIBLE);
            } else {
                binding.layoutApplicantButtons.setVisibility(View.GONE);
            }

            // 4. Set click listeners
            binding.buttonAccept.setOnClickListener(v -> listener.onAcceptClick(applicant));
            binding.buttonReject.setOnClickListener(v -> listener.onRejectClick(applicant));

            // Allow clicking the main card (not the buttons) to view the profile
            itemView.setOnClickListener(v -> {
                String studentId = applicant.getStudentId();
                if (studentId == null || studentId.trim().isEmpty()) {
                    Toast.makeText(v.getContext(), v.getContext().getString(R.string.error_student_profile_not_available), Toast.LENGTH_SHORT).show();
                    return;
                }

                Bundle args = new Bundle();
                args.putString("studentId", studentId);

                // Use NavController to navigate, ensuring the graph remains consistent
                try {
                    androidx.navigation.Navigation.findNavController(v).navigate(R.id.action_manage_service_to_view_student, args);
                } catch (Exception e) {
                    Log.e(TAG, "Navigation failed: " + e.getMessage());
                    Toast.makeText(v.getContext(), "Navigation failed. Ensure you are on the correct screen.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // 5. Adapter constructor
    public ApplicantAdapter(Context context, List<Applicant> applicantList, String tabMode, ApplicantClickListener listener) {
        this.context = context;
        this.applicantList = applicantList;
        this.tabMode = tabMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ApplicantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemApplicantBinding binding = ItemApplicantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ApplicantViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicantViewHolder holder, int position) {
        holder.bind(applicantList.get(position));
    }

    @Override
    public int getItemCount() {
        return applicantList.size();
    }
}