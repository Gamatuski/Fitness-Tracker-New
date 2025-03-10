package com.example.fitnesstracker.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.activities.AddTrainingActivity;
import com.example.fitnesstracker.adapters.SwipeToDeleteCallback;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.Activity;
import com.example.fitnesstracker.models.ActivityResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProgressFragment extends Fragment {

    private RecyclerView activitiesRecyclerView;
    private ActivitiesAdapter activitiesAdapter;
    private FloatingActionButton addTrainingButton;
    private Call<List<Activity>> call; // Объе

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        activitiesRecyclerView = view.findViewById(R.id.activitiesRecyclerView);
        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        // Инициализация адаптера с пустым списком
        activitiesAdapter = new ActivitiesAdapter(new ArrayList<>(),requireContext());
        activitiesRecyclerView.setAdapter(activitiesAdapter); // Устанавл

        addTrainingButton = view.findViewById(R.id.addTrainingButton);
        addTrainingButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTrainingActivity.class);
            startActivity(intent);
        });

        // Иконка для удаления (если нужно)
        Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);

        // Привязываем ItemTouchHelper
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(activitiesAdapter, deleteIcon));
        itemTouchHelper.attachToRecyclerView(activitiesRecyclerView);

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
        call = api.getActivities(userId);
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
                if (!call.isCanceled()) { // Проверяем, что запрос не был отменён
                    showError("Ошибка сети");
                }
            }


        });


    }
    private void showError(String message) {
        if (getContext() != null) { // Проверяем, что контекст не null
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Отменяем все запросы к API
        if (call != null) {
            call.cancel();
        }
    }



    public static class ActivitiesAdapter extends RecyclerView.Adapter<ActivitiesAdapter.ViewHolder> {
        private List<Activity> activities;
        private Context context; // Добавляем контекст

        public ActivitiesAdapter(List<Activity> activities,Context context) {
            this.activities = activities;
            this.context = context;
        }

        private String getUserIdFromSharedPreferences() {
            SharedPreferences sharedPreferences = context.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
            return sharedPreferences.getString("userId", null);
        }


        public void deleteItem(int position) {
            Activity activity = activities.get(position);
            String userId = getUserIdFromSharedPreferences();
            String activityId = activity.getId();

            Log.d("DeleteActivity", "User ID: " + userId);
            Log.d("DeleteActivity", "Activity ID: " + activityId);

            FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
            Call<ActivityResponse> call = api.deleteActivity(userId, activityId);
            call.enqueue(new Callback<ActivityResponse>() {
                @Override
                public void onResponse(Call<ActivityResponse> call, Response<ActivityResponse> response) {
                    if (response.isSuccessful()) {
                        Log.d("DeleteActivity", "Activity deleted successfully");
                        activities.remove(position);
                        notifyItemRemoved(position);
                    } else {
                        Log.e("DeleteActivity", "Error deleting activity: " + response.message());
                        Toast.makeText(context, "Ошибка при удалении активности", Toast.LENGTH_SHORT).show();
                        notifyItemChanged(position);
                    }
                }

                @Override
                public void onFailure(Call<ActivityResponse> call, Throwable t) {
                    Log.e("DeleteActivity", "Network error: " + t.getMessage());
                    Toast.makeText(context, "Ошибка сети", Toast.LENGTH_SHORT).show();
                    notifyItemChanged(position);
                }
            });
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

            // Форматирование отображения расстояния
            double distance = activity.getDistance();
            if (distance < 1.0) {
                // Переводим в метры и округляем до целых
                int distanceInMeters = (int) (distance * 1000);
                holder.distanceTextView.setText(distanceInMeters + " м");
            } else {
                // Округляем до двух знаков после запятой для километров
                String formattedDistance = String.format("%.2f км", distance);
                holder.distanceTextView.setText(formattedDistance);
            }

            holder.caloriesTextView.setText(activity.getCalories() + " ккал");

            // Получаем текущую дату
            Date date = activity.getDate();

            // Создаем форматтер для даты
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

            // Преобразуем дату в строку в нужном формате
            String formattedDate = dateFormat.format(date);

            holder.dateTextView.setText(formattedDate);
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