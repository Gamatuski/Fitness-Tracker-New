package com.example.fitnesstracker.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.fragments.StepsFragment;
import com.example.fitnesstracker.models.StepsResponse;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StepCounterService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "StepCounterChannel";
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private int stepCount = 0;
    private int previousDayOfWeek = -1; // Для отслеживания смены дня
    private String userId;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepCounterSensor == null) {
                Log.e("StepCounterService", "Step Counter Sensor не найден!");
                stopSelf(); // Остановить службу, если датчик отсутствует
                return;
            }
        } else {
            Log.e("StepCounterService", "Sensor Manager не инициализирован!");
            stopSelf(); // Остановить службу, если SensorManager не доступен
            return;
        }

        // Получение userId из SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getString("userId", null);
        if (userId == null) {
            Log.e("StepCounterService", "userId не найден. Служба остановлена.");
            stopSelf();
            return;
        }

        // Создание уведомления для Foreground Service
        createNotificationChannel();
        Notification notification = buildNotification("Служба отслеживания шагов запущена");
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        return START_STICKY; // Перезапустить службу, если она была остановлена системой
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int currentSteps = (int) event.values[0];
            if (stepCount == 0) {
                stepCount = currentSteps; // Инициализация начального значения
            }
            int stepsTaken = currentSteps - stepCount; // Шаги, сделанные с момента запуска службы
            stepCount = currentSteps; // Обновление общего количества шагов

            int currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2; // Понедельник - 0, Воскресенье - 6
            if (currentDayOfWeek == -1) {
                currentDayOfWeek = 6; // Воскресенье
            }

            if (previousDayOfWeek != currentDayOfWeek) {
                // Сменился день недели, обнуляем шаги (если нужно начинать подсчет с нуля каждый день)
                previousDayOfWeek = currentDayOfWeek;
                stepsTaken = 0; // Начать подсчет шагов за новый день с нуля
                stepCount = currentSteps; // Сбросить базовое значение для нового дня
                updateNotification("Начался новый день. Шаги за сегодня: 0");
            } else {
                updateNotification("Шаги за сегодня: " + stepsTaken);
            }

            // Обновление шагов в базе данных
            updateStepsToDatabase(stepsTaken, currentDayOfWeek);
        }
    }

    private void updateStepsToDatabase(int steps, int dayIndex) {
        if (userId == null) {
            Log.e("StepCounterService", "userId не доступен, не могу обновить базу данных.");
            return;
        }
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<StepsResponse> call = api.getSteps(userId); // Получаем текущие шаги из БД
        call.enqueue(new Callback<StepsResponse>() {
            @Override
            public void onResponse(Call<StepsResponse> call, Response<StepsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StepsResponse stepsResponse = response.body();
                    if (stepsResponse.isSuccess() && stepsResponse.getSteps() != null) {
                        java.util.List<Integer> currentStepsList = stepsResponse.getSteps();
                        if (dayIndex >= 0 && dayIndex < currentStepsList.size()) {
                            int previousSteps = currentStepsList.get(dayIndex);
                            int updatedSteps = previousSteps + steps;
                            currentStepsList.set(dayIndex, updatedSteps); // Обновляем шаги за текущий день

                            // Отправляем обновленные данные на сервер
                            Call<StepsResponse> updateCall = api.updateSteps(userId, dayIndex, updatedSteps); // Отправляем одно значение
                            updateCall.enqueue(new Callback<StepsResponse>() {
                                @Override
                                public void onResponse(Call<StepsResponse> call, Response<StepsResponse> updateResponse) {
                                    if (updateResponse.isSuccessful() && updateResponse.body() != null) {
                                        StepsResponse updateResponseBody = updateResponse.body();
                                        if (updateResponseBody.isSuccess()) {
                                            Log.d("StepCounterService", "Шаги успешно обновлены в БД, день: " + dayIndex + ", шаги: " + updatedSteps);
                                        } else {
                                            Log.e("StepCounterService", "Ошибка обновления шагов в БД: " + updateResponseBody.getMessage());
                                        }
                                    } else {
                                        Log.e("StepCounterService", "Ошибка сервера при обновлении шагов: " + updateResponse.message());
                                    }
                                }

                                @Override
                                public void onFailure(Call<StepsResponse> call, Throwable t) {
                                    Log.e("StepCounterService", "Сетевая ошибка при обновлении шагов: " + t.getMessage());
                                }
                            });


                        } else {
                            Log.e("StepCounterService", "Неверный dayIndex или данные о шагах из БД неполные.");
                        }
                    } else {
                        Log.e("StepCounterService", "Не удалось получить шаги из БД или запрос не успешен: " + stepsResponse.getMessage());
                    }
                } else {
                    Log.e("StepCounterService", "Ошибка при получении шагов из БД: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<StepsResponse> call, Throwable t) {
                Log.e("StepCounterService", "Сетевая ошибка при получении шагов из БД: " + t.getMessage());
            }
        });
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Не используется
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Служба отслеживания шагов",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification(String text) {
        Intent notificationIntent = new Intent(this, StepsFragment.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Fitness Tracker - Отслеживание шагов")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification) // Замените на свой значок уведомления
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = buildNotification(text);
        notificationManager.notify(1, notification);
    }
}