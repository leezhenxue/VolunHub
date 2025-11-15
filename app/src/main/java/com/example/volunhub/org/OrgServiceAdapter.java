package com.example.volunhub.org;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.volunhub.databinding.ItemOrgServiceBinding;
import com.example.volunhub.models.Service;
import java.util.List;

public class OrgServiceAdapter extends RecyclerView.Adapter<OrgServiceAdapter.ServiceViewHolder> {

    private final List<Service> serviceList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Service service);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrgServiceBinding binding;

        public ServiceViewHolder(ItemOrgServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Service service) {
            binding.textOrgServiceTitle.setText(service.getTitle());
            binding.textOrgServiceStatus.setText(service.getStatus());
            String applicantsText = "Applicants: " + service.getVolunteersApplied() + " / " + service.getVolunteersNeeded();
            binding.textOrgServiceApplicants.setText(applicantsText);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(service);
                }
            });
        }
    }

    public OrgServiceAdapter(List<Service> serviceList) {
        this.serviceList = serviceList;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrgServiceBinding binding = ItemOrgServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ServiceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        holder.bind(serviceList.get(position));
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }
}