package com.example.fitnesstracker.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.ActivityRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTrainingActivity extends AppCompatActivity {

    private TextView cancelTextView, saveTextView;
    private EditText  distanceEditText, caloriesEditText, stepsEditText, durationEditText, startDateEditText;
    private Spinner actionSpinner;
    private boolean isFormatting;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_training);

        // Инициализация элементов UI
        cancelTextView = findViewById(R.id.cancelTextView);
        saveTextView = findViewById(R.id.saveTextView);
        actionSpinner = findViewById(R.id.actionSpinner);

        // Создание адаптера для Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.activities_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionSpinner.setAdapter(adapter);

        distanceEditText = findViewById(R.id.distanceEditText);
        caloriesEditText = findViewById(R.id.caloriesEditText);
        stepsEditText = findViewById(R.id.stepsEditText);
        durationEditText = findViewById(R.id.durationEditText);

        // Получаем текущую дату
        LocalDate date = LocalDate.now();

        // Форматируем дату в строку
        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        startDateEditText = findViewById(R.id.startDateEditText);
        // Устанавливаем текст в поле редактирования
        startDateEditText.setText(formattedDate);


        // Добавление TextWatcher для автоматического форматирования даты
        startDateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


                if (isFormatting) return;

                String input = s.toString();
                StringBuilder formatted = new StringBuilder();

                // Удаляем все символы, кроме цифр
                String digitsOnly = input.replaceAll("[^\\d]", "");

                for (int i = 0; i < digitsOnly.length(); i++) {
                    if (i == 2 || i == 4) {
                        formatted.append("."); // Добавляем точку после дня и месяца
                    }
                    formatted.append(digitsOnly.charAt(i));
                }

                // Обрезаем лишние символы, если длина больше 10 (ДД.ММ.ГГГГ)
                if (formatted.length() > 10) {
                    formatted.setLength(10);
                }

                isFormatting = true;
                startDateEditText.setText(formatted.toString());
                startDateEditText.setSelection(formatted.length()); // Устанавливаем курсор в конец
                isFormatting = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String date = s.toString();
                if (!isValidDate(date)) {
                    startDateEditText.setError("Некорректная дата");
                } else {
                    startDateEditText.setError(null);
                }
            }

            private boolean isValidDate(String date) {
                if (date.length() != 10) return false; // Проверяем длину строки (ДД.ММ.ГГГГ)

                String[] parts = date.split("\\.");
                if (parts.length != 3) return false; // Должно быть 3 части: день, месяц, год

                try {
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);

                    // Проверяем корректность дня и месяца
                    if (month < 1 || month > 12) return false;
                    if (day < 1 || day > 31) return false;

                    // Дополнительные проверки для месяцев с 30 днями и февраля
                    if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) return false;
                    if (month == 2) {
                        // Проверка для февраля (високосный год)
                        boolean isLeapYear = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
                        if (day > 29 || (!isLeapYear && day > 28)) return false;
                    }

                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
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
                saveTrainingData();
            }
        });
    }

    private void saveTrainingData() {
        // Получение выбранного действия из Spinner
        String action = actionSpinner.getSelectedItem().toString();
        double distance = Double.parseDouble(distanceEditText.getText().toString());
        int calories = Integer.parseInt(caloriesEditText.getText().toString());
        int steps = Integer.parseInt(stepsEditText.getText().toString());
        int duration = Integer.parseInt(durationEditText.getText().toString());
        String date = startDateEditText.getText().toString();

        // Проверка обязательных полей
        if (action.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        // Получаем userId из SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "Ошибка: Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        ActivityRequest activityRequest = new ActivityRequest(action, distance, calories, steps, duration, date);

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<ResponseBody> call = api.addActivity(userId, activityRequest);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddTrainingActivity.this, "Тренировка сохранена", Toast.LENGTH_SHORT).show();
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