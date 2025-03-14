package com.example.fitnesstracker.fragments;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.activities.ActivityDetailsActivity;
import com.example.fitnesstracker.activities.AddTrainingActivity;
import com.example.fitnesstracker.adapters.SwipeToDeleteCallback;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.Activity;
import com.example.fitnesstracker.models.ActivityResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProgressFragment extends Fragment {

    private LinearLayout monthlyActivitiesContainer;
    private FloatingActionButton addTrainingButton;
    private Call<List<Activity>> call;
    private static final int ADD_TRAINING_REQUEST_CODE = 1;
    public static final int RESULT_OK = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        monthlyActivitiesContainer = view.findViewById(R.id.monthlyActivitiesContainer);
        addTrainingButton = view.findViewById(R.id.addTrainingButton);
        addTrainingButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTrainingActivity.class);
            startActivityForResult(intent, ADD_TRAINING_REQUEST_CODE);
        });

        // Загрузка данных
        loadActivities();

        return view;
    }

    /*@Override
    public void onResume() {
        super.onResume();
        loadActivities();
    } */


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
                    // Группируем активности по месяцам
                    Map<String, List<Activity>> activitiesByMonth = groupActivitiesByMonth(response.body());

                    // Очищаем контейнер перед добавлением новых блоков
                    monthlyActivitiesContainer.removeAllViews();

                    // Создаем блоки для каждого месяца
                    for (Map.Entry<String, List<Activity>> entry : activitiesByMonth.entrySet()) {
                        addMonthlyActivityBlock(entry.getKey(), entry.getValue());
                    }
                } else {
                    Toast.makeText(getContext(), "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Activity>> call, Throwable t) {
                if (!call.isCanceled()) {
                    showError("Ошибка сети");
                }
            }
        });
    }

    // Метод для группировки активностей по месяцам
    private Map<String, List<Activity>> groupActivitiesByMonth(List<Activity> activities) {
        Map<String, List<Activity>> activitiesByMonth = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", new Locale("ru"));

        for (Activity activity : activities) {
            String monthYear = dateFormat.format(activity.getDate());
            if (!activitiesByMonth.containsKey(monthYear)) {
                activitiesByMonth.put(monthYear, new ArrayList<>());
            }
            activitiesByMonth.get(monthYear).add(activity);
        }

        return activitiesByMonth;
    }

    // Метод для добавления блока с активностями за месяц
    private void addMonthlyActivityBlock(String monthYear, List<Activity> activities) {
        View monthlyBlock = LayoutInflater.from(getContext()).inflate(R.layout.item_monthly_activity, monthlyActivitiesContainer, false);

        // Устанавливаем заголовок месяца
        TextView monthYearTextView = monthlyBlock.findViewById(R.id.monthYearTextView);
        monthYearTextView.setText(monthYear);

        // Рассчитываем общую статистику
        double totalDuration = 0;
        double totalDistance = 0;
        int totalCalories = 0;

        for (Activity activity : activities) {
            totalDuration += activity.getDuration();
            totalDistance += activity.getDistance();
            totalCalories += activity.getCalories();
        }

        // Устанавливаем общую статистику
        TextView totalDurationTextView = monthlyBlock.findViewById(R.id.totalDurationTextView);
        TextView totalDistanceTextView = monthlyBlock.findViewById(R.id.totalDistanceTextView);
        TextView totalCaloriesTextView = monthlyBlock.findViewById(R.id.totalCaloriesTextView);

        totalDurationTextView.setText(String.format("%.2f мин", totalDuration));
        totalDistanceTextView.setText(String.format("%.2f км", totalDistance));
        totalCaloriesTextView.setText(totalCalories + " ккал");

        // Настраиваем RecyclerView для активностей
        RecyclerView activitiesRecyclerView = monthlyBlock.findViewById(R.id.activitiesRecyclerView);
        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ActivitiesAdapter activitiesAdapter = new ActivitiesAdapter(activities, getContext());
        activitiesRecyclerView.setAdapter(activitiesAdapter);

        // Добавляем свайп для удаления
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(activitiesAdapter, vibrator);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchHelper.attachToRecyclerView(activitiesRecyclerView);

        // Добавляем блок в контейнер
        monthlyActivitiesContainer.addView(monthlyBlock);
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (call != null) {
            call.cancel();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_TRAINING_REQUEST_CODE && resultCode == RESULT_OK) {
            loadActivities();
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

        public androidx.fragment.app.FragmentManager getFragmentManager() {
            if (context instanceof FragmentActivity) {
                return ((FragmentActivity) context).getSupportFragmentManager();
            }
            throw new IllegalStateException("Context is not a FragmentActivity");
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

            // Устанавливаем иконку в зависимости от названия активности
            int iconResId = getIconForActivity(activity.getAction());
            holder.iconImageView.setImageResource(iconResId);

            double duration = activity.getDuration();
            DecimalFormat durationFormat = new DecimalFormat("#.##");
            holder.durationTextView.setText(durationFormat.format(duration) + " мин");

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

            // Обработка нажатия на элемент

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ActivityDetailsActivity.class);
                intent.putExtra("activity", activity);
                intent.putExtra("isEditMode", true); // Добавляем флаг
                ((FragmentActivity) context).startActivityForResult(intent, ADD_TRAINING_REQUEST_CODE); // Используем startActivityForResult
            });

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
            public ImageView iconImageView;

            public ViewHolder(View itemView) {
                super(itemView);
                actionTextView = itemView.findViewById(R.id.actionTextView);
                durationTextView = itemView.findViewById(R.id.durationTextView);
                distanceTextView = itemView.findViewById(R.id.distanceTextView);
                caloriesTextView = itemView.findViewById(R.id.caloriesTextView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
                iconImageView = itemView.findViewById(R.id.iconImageView);
            }
        }

        private int getIconForActivity(String activityName) {
            switch (activityName) {
                case "Бег":
                    return R.drawable.ic_runnuig_man; // Иконка для бега
                case "Прогулка":
                    return R.drawable.ic_walking_man; // Иконка для прогулки
                case "Северная ходьба":
                    return R.drawable.ic_nord_walking_man; // Иконка для северной ходьбы
                case "Езда на велосипеде":
                    return R.drawable.ic_cycling_man; // Иконка для езды на велосипеде
                default:
                    return R.drawable.ic_runnuig_man; // Иконка по умолчанию
            }
        }
    }
}