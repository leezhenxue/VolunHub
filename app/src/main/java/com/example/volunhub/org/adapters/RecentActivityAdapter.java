package com.example.volunhub.org.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunhub.R;
import com.example.volunhub.models.RecentActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Adapter to display the "Recent Activity" log on the Organization Dashboard.
 */
public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private final ArrayList<RecentActivity> list;

    /**
     * Constructor for the adapter.
     * @param list The list of recent activities to display.
     */
    public RecentActivityAdapter(ArrayList<RecentActivity> list) {
        this.list = list;
    }

    /**
     * Creates a new ViewHolder for a recent activity item.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds the View for each activity item.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds data to the ViewHolder at the specified position.
     * @param holder The ViewHolder to update.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentActivity item = list.get(position);
        holder.message.setText(item.getMessage());

        // Format the timestamp (e.g., 20 Oct 2025 (10:30 AM))
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy (hh:mm a)", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));

        if (item.getTimestamp() != null) {
            holder.time.setText(sdf.format(item.getTimestamp().toDate()));
        } else {
            holder.time.setText("");
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items.
     */
    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * ViewHolder class for caching view references.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView message, time;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.textActivityMessage);
            time = itemView.findViewById(R.id.textActivityTimestamp);
        }
    }
}