package com.example.fitnesstracker.activities;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.fragments.DatePickerBottomSheet;
import com.example.fitnesstracker.fragments.TimePickerBottomSheet;
import com.example.fitnesstracker.models.Activity;
import com.example.fitnesstracker.models.ActivityRequest;
import com.example.fitnesstracker.models.ActivityResponse;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTrainingActivity extends AppCompatActivity {

    private TextView cancelTextView, saveTextView, actionTextView;
    private EditText  distanceEditText, caloriesEditText, stepsEditText, durationEditText, startDateEditText;
    private boolean isFormatting;
    private static final int REQUEST_ACTIVITY_CODE = 1;

    private LinearLayout actionLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_training);

        // Инициализация элементов UI
        cancelTextView = findViewById(R.id.cancelTextView);
        saveTextView = findViewById(R.id.saveTextView);
        actionLayout = findViewById(R.id.actionLayout);
        actionTextView = findViewById(R.id.actionTextView);


        distanceEditText = findViewById(R.id.distanceEditText);
        caloriesEditText = findViewById(R.id.caloriesEditText);
        stepsEditText = findViewById(R.id.stepsEditText);
        durationEditText = findViewById(R.id.durationEditText);
        startDateEditText = findViewById(R.id.startDateEditText);


        boolean isEditMode = getIntent().getBooleanExtra("isEditMode", false);

        if(isEditMode){
            // Получаем данные активности из Intent, если они есть
            Activity activity = (Activity) getIntent().getSerializableExtra("activity");
            if (activity != null) {
                // Заполняем поля данными активности
                actionTextView.setText(activity.getAction());
                distanceEditText.setText(String.valueOf(activity.getDistance()));
                caloriesEditText.setText(String.valueOf(activity.getCalories()));
                stepsEditText.setText(String.valueOf(activity.getSteps()));
                durationEditText.setText(String.valueOf(activity.getDuration()));

                // Format the date
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                startDateEditText.setText(dateFormat.format(activity.getDate()));

                // Меняем текст кнопки "Сохранить"
                saveTextView.setText("Обновить");
            } else {
                // Получаем текущую дату
                LocalDate date = LocalDate.now();

                // Форматируем дату в строку
                String formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

                // Устанавливаем текст в поле редактирования
                startDateEditText.setText(formattedDate);
            }
        }
        // Получаем текущую дату
        LocalDate date = LocalDate.now();

        // Форматируем дату в строку
        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        // Устанавливаем текст в поле редактирования
        startDateEditText.setText(formattedDate);


        // Обработка нажатия на currentDate
        startDateEditText.setOnClickListener(v -> showDatePickerBottomSheet());



        findViewById(R.id.actionLayout).setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectActionActivity.class);

            // Получаем текущие выбранные дни из TextView
            TextView actionTextView = findViewById(R.id.actionTextView);
            String selectedActionText = actionTextView.getText().toString();


            // Передаем выбранные дни в Intent
            intent.putExtra("selectedAction",selectedActionText);
            startActivityForResult(intent, REQUEST_ACTIVITY_CODE);
        });

        // Обработчик нажатия на "Отменить"
        cancelTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Закрываем текущую активность
            }
        });

        // Обработчик нажатия на "Сохранить"
        saveTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    saveTrainingData();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void showDatePickerBottomSheet() {
        // Создание экземпляра DatePickerBottomSheet
        DatePickerBottomSheet datePickerBottomSheet = new DatePickerBottomSheet();

        // Установка слушателя для получения выбранной даты
        datePickerBottomSheet.setOnDateSelectedListener(selectedDate -> {
            // Обновляем текст в currentDate
            startDateEditText.setText(selectedDate);
        });

        // Показываем BottomSheetDialog
        datePickerBottomSheet.show(getSupportFragmentManager(), "DatePickerBottomSheet");
    }



    private void saveTrainingData() throws ParseException {
        boolean isEditMode = getIntent().getBooleanExtra("isEditMode", false);
        String action = actionTextView.getText().toString();
        String date = startDateEditText.getText().toString();

        if (action.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        double distance;
        try {
            distance = Double.parseDouble(distanceEditText.getText().toString());
        } catch (NumberFormatException e) {
            distance = 0.0;
        }

        int calories;
        try {
            calories = Integer.parseInt(caloriesEditText.getText().toString());
        } catch (NumberFormatException e) {
            calories = 0;
        }

        int steps;
        try {
            steps = Integer.parseInt(stepsEditText.getText().toString());
        } catch (NumberFormatException e) {
            steps = 0;
        }

        Double duration;
        try {
            duration = Double.parseDouble(durationEditText.getText().toString());
        } catch (NumberFormatException e) {
            duration = 0.0;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "Ошибка: Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        ActivityRequest activityRequest = new ActivityRequest(action, distance, calories, steps, duration, date);

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);

        if (isEditMode) {
            // Режим редактирования: обновляем существующую активность
            String activityId = getIntent().getStringExtra("activityId");
            Call<ActivityResponse> call = api.updateActivity(userId, activityId, activityRequest);
            double finalDistance = distance;
            int finalCalories = calories;
            int finalSteps = steps;
            Double finalDuration = duration;

            DateFormat formatter = new SimpleDateFormat("dd.MM.YYYY");

            Date finalDate =  formatter.parse(date);
            call.enqueue(new Callback<ActivityResponse>() {
                @Override
                public void onResponse(Call<ActivityResponse> call, Response<ActivityResponse> response) {
                    if (response.isSuccessful()) {
                        // Проверяем, что response.body() не null
                        if (response.body() != null) {

                            Activity updatedActivity = new Activity(action, finalDistance, finalCalories, finalSteps, finalDuration, finalDate);

                            // Создаем Intent для возврата данных
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("activity", updatedActivity);
                            Log.d("EditActivity", String.valueOf(updatedActivity));
                            Log.d("EditActivity", String.valueOf(response.body()));

                            // Устанавливаем результат и закрываем активность
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            // Обрабатываем случай, когда response.body() == null
                            Toast.makeText(AddTrainingActivity.this, "Ошибка: Пустой ответ от сервера", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_CANCELED); // Устанавливаем RESULT_CANCELED, чтобы ActivityDetailsActivity знала, что произошла ошибка
                            finish();
                        }
                    } else {
                        Toast.makeText(AddTrainingActivity.this, "Ошибка при обновлении активности", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED); // Устанавливаем RESULT_CANCELED, чтобы ActivityDetailsActivity знала, что произошла ошибка
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<ActivityResponse> call, Throwable t) {
                    Toast.makeText(AddTrainingActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED); // Устанавливаем RESULT_CANCELED, чтобы ActivityDetailsActivity знала, что произошла ошибка
                    finish();
                }
            });
        } else {
            // Режим добавления: создаем новую активность
            Call<ResponseBody> call = api.addActivity(userId, activityRequest);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddTrainingActivity.this, "Тренировка сохранена", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(AddTrainingActivity.this, "Ошибка при сохранении тренировки", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(AddTrainingActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ACTIVITY_CODE && resultCode == RESULT_OK) {
            // Получаем выбранную активность из Intent
            String selectedAction = data.getStringExtra("selectedAction");

            // Обновляем поле actionTextView
            if (selectedAction != null) {
                actionTextView.setText(selectedAction);
            }
        }
    }
}