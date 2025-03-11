package com.example.fitnesstracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.fitnesstracker.R;

import java.util.ArrayList;
import java.util.List;

public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DayViewHolder> {

    private List<String> days;
    private List<String> selectedDays;

    public DaysAdapter(List<String> days) {
        this.days = days;
        this.selectedDays = new ArrayList<>();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String day = days.get(position);
        holder.dayName.setText(day);

        if (selectedDays.contains(day)) {
            holder.checkIcon.setVisibility(View.VISIBLE);
        } else {
            holder.checkIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedDays.contains(day)) {
                selectedDays.remove(day);
            } else {
                selectedDays.add(day);
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public List<String> getSelectedDays() {
        return selectedDays;
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayName;
        ImageView checkIcon;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayName = itemView.findViewById(R.id.dayName);
            checkIcon = itemView.findViewById(R.id.checkIcon);
        }
    }
}