package com.example.volunhub.student.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.volunhub.databinding.ItemServicePostingBinding;
import com.example.volunhub.models.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * RecyclerView Adapter for displaying a list of Service opportunities to students.
 */
public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private final List<Service> serviceList;
    private OnItemClickListener listener;

    /**
     * Interface for handling item clicks on the service list.
     */
    public interface OnItemClickListener {
        /**
         * Called when a service item is clicked.
         * @param service The service object that was clicked.
         */
        void onItemClick(Service service);
    }

    /**
     * Sets the click listener for the adapter.
     * @param listener The listener implementation.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Constructor for ServiceAdapter.
     * @param serviceList The list of services to display.
     */
    public ServiceAdapter(List<Service> serviceList) {
        this.serviceList = serviceList;
    }

    /**
     * Creates a new ViewHolder for a service item.
     * @param parent The parent ViewGroup.
     * @param viewType The view type integer.
     * @return A new ServiceViewHolder.
     */
    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServicePostingBinding binding = ItemServicePostingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ServiceViewHolder(binding);
    }

    /**
     * Binds the data to the ViewHolder at the specified position.
     * @param holder The ViewHolder to update.
     * @param position The index of the item in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        Service currentService = serviceList.get(position);
        holder.bind(currentService);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentService);
            }
        });
    }

    /**
     * Returns the total number of items in the list.
     * @return The item count.
     */
    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    /**
     * ViewHolder class that holds references to the UI elements for a service item.
     */
    public static class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final ItemServicePostingBinding binding;

        public ServiceViewHolder(ItemServicePostingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds service data to the UI elements.
         * @param service The service object containing the data.
         */
        public void bind(Service service) {
            binding.textItemServiceTitle.setText(service.getTitle());
            binding.textItemServiceOrgName.setText(service.getOrgName());

            if (service.getServiceDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                String dateString = sdf.format(service.getServiceDate());
                binding.textItemServiceDate.setText(dateString);
            } else {
                binding.textItemServiceDate.setText("Date TBD");
            }
        }
    }
}