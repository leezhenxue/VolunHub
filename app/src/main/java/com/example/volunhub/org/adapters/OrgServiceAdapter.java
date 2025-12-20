package com.example.volunhub.org.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunhub.R;
import com.example.volunhub.databinding.ItemOrgServiceBinding;
import com.example.volunhub.models.Service;
import java.util.List;

/**
 * Adapter to display the list of services created by the Organization.
 */
public class OrgServiceAdapter extends RecyclerView.Adapter<OrgServiceAdapter.ServiceViewHolder> {

    private final List<Service> serviceList;
    private OnItemClickListener listener;

    /**
     * Interface for handling item clicks.
     */
    public interface OnItemClickListener {
        /**
         * Called when a service item is clicked.
         * @param service The service that was clicked.
         */
        void onItemClick(Service service);
    }

    /**
     * Sets the listener for item click events.
     * @param listener The listener to set.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Constructor for the adapter.
     * @param serviceList The list of services to display.
     */
    public OrgServiceAdapter(List<Service> serviceList) {
        this.serviceList = serviceList;
    }

    /**
     * Creates a new ViewHolder for a service item.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new ServiceViewHolder that holds the View for each service item.
     */
    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrgServiceBinding binding = ItemOrgServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ServiceViewHolder(binding);
    }

    /**
     * Binds data to the ViewHolder at the specified position.
     * @param holder The ViewHolder to update.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        holder.bind(serviceList.get(position));
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items.
     */
    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    /**
     * ViewHolder class for caching view references.
     */
    public class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrgServiceBinding binding;

        public ServiceViewHolder(ItemOrgServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds service data to the views.
         * @param service The service object containing the data.
         */
        public void bind(Service service) {
            Context context = itemView.getContext();

            binding.textOrgServiceTitle.setText(service.getTitle());
            binding.textOrgServiceStatus.setText(service.getStatus());

            // Format: "Applicants: 5 / 10"
            String applicantsText = context.getString(
                    R.string.label_applicants,
                    service.getVolunteersApplied(),
                    service.getVolunteersNeeded()
            );
            binding.textOrgServiceApplicants.setText(applicantsText);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(service);
                }
            });
        }
    }
}