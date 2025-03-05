package com.example.fitnesstracker.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.activities.AddTrainingActivity;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.Activity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProgressFragment extends Fragment {

    private RecyclerView activitiesRecyclerView;
    private ActivitiesAdapter activitiesAdapter;
    private FloatingActionButton addTrainingButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        activitiesRecyclerView = view.findViewById(R.id.activitiesRecyclerView);
        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        // Инициализация адаптера с пустым списком
        activitiesAdapter = new ActivitiesAdapter(new ArrayList<>());
        activitiesRecyclerView.setAdapter(activitiesAdapter); // Устанавл

        addTrainingButton = view.findViewById(R.id.addTrainingButton);
        addTrainingButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTrainingActivity.class);
            startActivity(intent);
        });

        // Загрузка данных
        loadActivities();

        return view;
    }

    private void loadActivities() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            return;
        }

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<List<Activity>> call = api.getActivities(userId);
        call.enqueue(new Callback<List<Activity>>() {
            @Override
            public void onResponse(Call<List<Activity>> call, Response<List<Activity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Обновляем адаптер новыми данными
                    activitiesAdapter.updateData(response.body());
                } else {
                    Toast.makeText(getContext(), "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Activity>> call, Throwable t) {
                Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class ActivitiesAdapter extends RecyclerView.Adapter<ActivitiesAdapter.ViewHolder> {
        private List<Activity> activities;

        public ActivitiesAdapter(List<Activity> activities) {
            this.activities = activities;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Activity activity = activities.get(position);
            holder.actionTextView.setText(activity.getAction());
            holder.durationTextView.setText(activity.getDuration() + " мин");
            holder.distanceTextView.setText(activity.getDistance() + " км");
            holder.caloriesTextView.setText(activity.getCalories() + " ккал");
            holder.dateTextView.setText(activity.getDate().toString()); // Форматируйте дату по необходимости
        }

        @Override
        public int getItemCount() {
            return activities.size();
        }

        public void updateData(List<Activity> newActivities) {
            this.activities.clear();
            this.activities.addAll(newActivities);
            notifyDataSetChanged(); // Уведомляем RecyclerView об изменении данных
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView actionTextView, durationTextView, distanceTextView, caloriesTextView, dateTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                actionTextView = itemView.findViewById(R.id.actionTextView);
                durationTextView = itemView.findViewById(R.id.durationTextView);
                distanceTextView = itemView.findViewById(R.id.distanceTextView);
                caloriesTextView = itemView.findViewById(R.id.caloriesTextView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
            }
        }
    }
}