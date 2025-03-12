package com.example.fitnesstracker.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.LoginRequest;
import com.example.fitnesstracker.models.LoginResponse;
import com.example.fitnesstracker.service.StepCounterService;
import com.example.fitnesstracker.utils.StyleTitleText;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.SharedPreferences;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout passwordInputLayout, emailInputLayout;
    private TextInputEditText passwordEditText, emailEditText;
    private Button loginButton;
    private TextView registerTextView, titleTextView, errorTextView;;
    private CardView loginCard;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailEditText = findViewById(R.id.emailEditText);

        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        passwordEditText = findViewById(R.id.passwordEditText);

        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        loginCard = findViewById(R.id.loginCard);
        titleTextView = findViewById(R.id.titleTextView);
        errorTextView = findViewById(R.id.errorTextView);

        progressBar = findViewById(R.id.progressBar);

        // Стилизация заголовка
        StyleTitleText styleTitleText = new StyleTitleText();
        styleTitleText.styleTitleText(titleTextView);

        // Обработчик фокуса для полей ввода
        setupFocusListeners();

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            progressBar.setVisibility(View.VISIBLE); // Показываем ProgressBar
            loginButton.setEnabled(false); // Отключаем кнопку
            loginButton.setBackgroundColor(Color.GRAY);

            login(email, password);
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                resetErrorUI(); // Сброс ошибок при изменении текста
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });


    }

    private void setupFocusListeners() {
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus) {
                // Поднимаем карточку при фокусе
                loginCard.animate().translationY(-100).setDuration(200).start();
            } else {
                // Возвращаем карточку на место при потере фокуса
                loginCard.animate().translationY(0).setDuration(200).start();
            }
        };

        emailEditText.setOnFocusChangeListener(focusListener);
        passwordEditText.setOnFocusChangeListener(focusListener);
    }


    private void login(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);

        // Логирование тела запроса
        Log.d("LoginActivity", "Отправка запроса: " + loginRequest.toString());

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<LoginResponse> call = api.login(loginRequest);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse != null && loginResponse.isSuccess()) {

                        // После получения ответа от сервера:
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE); // Скрываем ProgressBar
                            loginButton.setEnabled(true); // Включаем кнопку
                            loginButton.setBackgroundColor(Color.MAGENTA);
                        });

                        // Успешный вход
                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);

                        String userId = loginResponse.getUser().getId(); //получение user_id

                        // **Сохраняем userId в SharedPreferences**
                        SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE); // Имя файла настроек "fitness_prefs"
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("userId", userId); // Ключ "userId", значение - полученный userId
                        editor.apply(); // или editor.commit(); - 'apply' работает асинхронно, 'commit' синхронно

                        startActivity(intent);
                        Toast.makeText(LoginActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                        resetErrorUI(); // Сброс ошибок

                        // **Проверка наличия датчика перед запуском службы**
                        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                        Sensor stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

                        if (stepCounterSensor != null) {
                            // Датчик шагомера найден, запускаем службу
                            Intent serviceIntent = new Intent(LoginActivity.this, StepCounterService.class);
                            ContextCompat.startForegroundService(LoginActivity.this, serviceIntent);
                        } else {
                            // Датчик шагомера не найден, не запускаем службу и сообщаем пользователю
                            Toast.makeText(LoginActivity.this, "Отслеживание шагов не поддерживается на этом устройстве.", Toast.LENGTH_LONG).show();
                            Log.w("LoginActivity", "Step Counter Sensor не найден на устройстве. Служба не запущена.");
                        }
                    } else {
                        // Ошибка входа
                        showError("Неверный пароль");
                        // После получения ответа от сервера:

                    }
                } else {
                    // Ошибка сервера (например, 400 Bad Request)
                    try {
                        // Извлекаем тело ошибки
                        String errorBody = response.errorBody().string();
                        Log.e("LoginActivity", "Тело ошибки: " + errorBody);

                        // Парсим JSON-ответ
                        JsonObject errorJson = JsonParser.parseString(errorBody).getAsJsonObject();
                        String errorMessage = errorJson.get("message").getAsString();

                        // Показываем сообщение об ошибке
                        showError(errorMessage);

                    } catch (IOException e) {
                        e.printStackTrace();
                        showError("Ошибка при обработке ответа сервера");
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // Ошибка сети
                Log.e("LoginActivity", "Ошибка сети: " + t.getMessage());

                // После получения ответа от сервера:
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE); // Скрываем ProgressBar
                    loginButton.setEnabled(true); // Включаем кнопку
                    loginButton.setBackgroundResource(R.drawable.rounded_button);
                });

                showError("Ошибка сети");
            }
        });
    }

    private void showError(String errorMessage) {
        // Показываем текст ошибки
        errorTextView.setText(errorMessage);
        errorTextView.setVisibility(View.VISIBLE);

        // Красная обводка для поля пароля
        passwordEditText.setBackgroundResource(R.drawable.edittext_error_background);

        // Красная обводка для карточки
        loginCard.setCardBackgroundColor(Color.parseColor("#FFF0F0")); // Светло-красный фон

        // После получения ответа от сервера:
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE); // Скрываем ProgressBar
            loginButton.setEnabled(true); // Включаем кнопку
            loginButton.setBackgroundColor(Color.MAGENTA);
        });
    }

    private void resetErrorUI() {
        // Скрываем текст ошибки
        errorTextView.setVisibility(View.GONE);

        // Возвращаем стандартный фон для поля пароля
        passwordEditText.setBackgroundResource(R.drawable.edittext_background);

        // Возвращаем стандартный фон для карточки
        loginCard.setCardBackgroundColor(Color.WHITE);
    }
}