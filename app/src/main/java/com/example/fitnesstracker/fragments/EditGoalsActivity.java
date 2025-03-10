package com.example.fitnesstracker.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.User;
import com.example.fitnesstracker.models.UserRequest;
import com.example.fitnesstracker.models.UserResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditGoalsActivity extends AppCompatActivity {

    private TextView stepsGoalTextView, distanceGoalTextView;
    private Button decreaseStepsButton, increaseStepsButton;
    private Button decreaseDistanceButton, increaseDistanceButton;
    private Button doneButton;
    private TextView cancelTextView;

    private int stepsGoal = 5000; // Начальное значение цели шагов
    private int distanceGoal = 10; // Начальное значение цели расстояния

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_goals);

        stepsGoalTextView = findViewById(R.id.stepsGoalTextView);
        distanceGoalTextView = findViewById(R.id.distanceGoalTextView);
        decreaseStepsButton = findViewById(R.id.decreaseStepsButton);
        increaseStepsButton = findViewById(R.id.increaseStepsButton);
        decreaseDistanceButton = findViewById(R.id.decreaseDistanceButton);
        increaseDistanceButton = findViewById(R.id.increaseDistanceButton);
        doneButton = findViewById(R.id.doneButton);
        cancelTextView = findViewById(R.id.cancelTextView);

        // Загрузка текущих целей
        loadCurrentGoals();

        // Обработка нажатий на кнопки для шагов
        decreaseStepsButton.setOnClickListener(v -> {
            stepsGoal = Math.max(0, stepsGoal - 250); // Уменьшаем на 250, но не меньше 0
            stepsGoalTextView.setText(String.valueOf(stepsGoal));
        });

        increaseStepsButton.setOnClickListener(v -> {
            stepsGoal += 250; // Увеличиваем на 250
            stepsGoalTextView.setText(String.valueOf(stepsGoal));
        });

        // Обработка нажатий на кнопки для расстояния
        decreaseDistanceButton.setOnClickListener(v -> {
            distanceGoal = Math.max(0, distanceGoal - 1); // Уменьшаем на 1 км, но не меньше 0
            distanceGoalTextView.setText(String.valueOf(distanceGoal));
        });

        increaseDistanceButton.setOnClickListener(v -> {
            distanceGoal += 1; // Увеличиваем на 1 км
            distanceGoalTextView.setText(String.valueOf(distanceGoal));
        });

        // Обработка нажатия на "Готово"
        doneButton.setOnClickListener(v -> saveGoals());

        // Обработка нажатия на "Отменить"
        cancelTextView.setOnClickListener(v -> finish());
    }

    private void loadCurrentGoals() {
        SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "Ошибка: Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<UserResponse> call = api.getUser(userId);

        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body().getUser(); // Добавить .getUser()
                    stepsGoal = user.getStepsGoal();
                    distanceGoal = user.getDistanceGoal();

                    // Логирование для проверки значений
                    System.out.println("Steps Goal: " + stepsGoal);
                    System.out.println("Distance Goal: " + distanceGoal);

                    runOnUiThread(() -> {
                        stepsGoalTextView.setText(String.valueOf(stepsGoal));
                        distanceGoalTextView.setText(String.valueOf(distanceGoal));
                    });
                } else {
                    Toast.makeText(EditGoalsActivity.this,
                            "Ошибка загрузки данных: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toast.makeText(EditGoalsActivity.this,
                        "Ошибка сети: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGoals() {
        SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "Ошибка: Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        UserRequest request = new UserRequest(stepsGoal, distanceGoal);
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<UserResponse> call = api.updateGoals(userId, request);

        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(EditGoalsActivity.this,
                            "Цели успешно обновлены",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMessage = response.body() != null ? response.body().getMessage() : "Ошибка обновления целей";
                    Toast.makeText(EditGoalsActivity.this,
                            errorMessage,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toast.makeText(EditGoalsActivity.this,
                        "Ошибка сети: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}