package com.example.volunhub.org.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volunhub.R;
import com.example.volunhub.org.models.RecentActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private final ArrayList<RecentActivity> list;

    public RecentActivityAdapter(ArrayList<RecentActivity> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentActivity item = list.get(position);
        holder.message.setText(item.getMessage());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy (hh:mm a)");
        holder.time.setText(sdf.format(item.getTimestamp().toDate()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView message, time;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.textActivityMessage);
            time = itemView.findViewById(R.id.textActivityTimestamp);
        }
    }
}
