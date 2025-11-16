package com.example.volunhub.student.fragments;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.volunhub.databinding.ItemServicePostingBinding;
import com.example.volunhub.models.Service;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private final List<Service> serviceList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Service service);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // The ViewHolder is now just a simple "holder"
    // It no longer knows about the click listener
    public static class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final ItemServicePostingBinding binding;

        public ServiceViewHolder(ItemServicePostingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Service service) {
            binding.textItemServiceTitle.setText(service.getTitle());
            binding.textItemServiceOrgName.setText(service.getOrgName());
            // TODO: Format and set the date
        }
    }

    public ServiceAdapter(List<Service> serviceList) {
        this.serviceList = serviceList;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServicePostingBinding binding = ItemServicePostingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ServiceViewHolder(binding);
    }

    // --- THIS IS THE FIX ---
    // The click listener logic is moved here.
    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        Service currentService = serviceList.get(position);

        // 1. Bind the data
        holder.bind(currentService);

        // 2. Set the click listener here
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentService);
            }
        });
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }
}