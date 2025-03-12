package com.example.fitnesstracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.R;

import java.util.List;

public class ActionsAdapter extends RecyclerView.Adapter<ActionsAdapter.ActionViewHolder> {

    private List<String> actions;
    private String selectedAction;

    public ActionsAdapter(List<String> actions) {
        this.actions = actions;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_action, parent, false);
        return new ActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        String action = actions.get(position);
        holder.actionName.setText(action);

        // Show/hide checkmark based on selection
        if (action.equals(selectedAction)) {
            holder.checkIcon.setVisibility(View.VISIBLE);
        } else {
            holder.checkIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            selectedAction = action;
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    public String getSelectedAction() {
        return selectedAction;
    }

    public static class ActionViewHolder extends RecyclerView.ViewHolder {
        TextView actionName;
        ImageView checkIcon;

        public ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            actionName = itemView.findViewById(R.id.actionName);
            checkIcon = itemView.findViewById(R.id.checkIcon);
        }
    }
}