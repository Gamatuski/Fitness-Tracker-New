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
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StepCounterService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "StepCounterChannel";
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private int initialStepCount = -1;
    private int baseSteps = 0;  // Добавляем переменную для хранения базового значения шагов

    private int currentStepCount = 0;
    private int timerInitialSteps = 0;
    private int stepsDuringTimer = 0;
    private boolean isTimerRunning = false;
    private String userId;
    private int previousDayOfWeek = -1;
    private boolean isInitialized = false;

    public class StepBinder extends Binder {
        public StepCounterService getService() {
            return StepCounterService.this;
        }
    }

    private final IBinder binder = new StepBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepCounterSensor == null) {
                Log.e("StepCounterService", "Step Counter Sensor не найден!");
                Toast.makeText(this, "Ваше устройство не поддерживает подсчёт шагов", Toast.LENGTH_LONG).show();
                stopSelf();
                return;
            }
        } else {
            Log.e("StepCounterService", "Sensor Manager не инициализирован!");
            stopSelf();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getString("userId", null);
        if (userId == null) {
            Log.e("StepCounterService", "userId не найден");
            stopSelf();
            return;
        }
        if (!isInitialized) {
            loadCurrentStepsFromDatabase(() -> {
                isInitialized = true;
            });
            return;
        }

        createNotificationChannel();
        startForeground(1, buildNotification("Служба отслеживания шагов запущена"));
    }

    private void loadCurrentStepsFromDatabase(Runnable onComplete) {
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<StepsResponse> call = api.getSteps(userId);

        call.enqueue(new Callback<StepsResponse>() {
            @Override
            public void onResponse(Call<StepsResponse> call, Response<StepsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Calendar calendar = Calendar.getInstance();
                    int dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2;
                    if (dayIndex == -1) dayIndex = 6;

                    List<Integer> steps = response.body().getSteps();
                    if (steps != null && dayIndex < steps.size()) {
                        baseSteps = steps.get(dayIndex);
                        Log.d("StepCounterService", "Загружено базовое значение шагов: " + baseSteps);
                    }
                }
                onComplete.run();
            }

            @Override
            public void onFailure(Call<StepsResponse> call, Throwable t) {
                Log.e("StepCounterService", "Ошибка загрузки шагов: " + t.getMessage());
                onComplete.run();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(1, buildNotification("Отслеживание шагов активно"));

        if (sensorManager != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (!isInitialized) {
                initialStepCount = (int) event.values[0];
                loadCurrentStepsFromDatabase(() -> {
                    isInitialized = true;
                });
                return;
            }

            int currentSteps = (int) event.values[0] - initialStepCount + baseSteps;

            Calendar calendar = Calendar.getInstance();
            int dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2;
            if (dayIndex == -1) dayIndex = 6;

            updateStepsToDatabase(currentSteps, dayIndex);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    public void startTrackingStepsForTimer() {
        timerInitialSteps = currentStepCount;
        stepsDuringTimer = 0;
        isTimerRunning = true;
    }

    public int stopTrackingStepsForTimer() {
        isTimerRunning = false;
        return stepsDuringTimer;
    }

    public int getCurrentStepCount() {
        return currentStepCount;
    }

    public void forceUpdateStepsToDatabase(int dayIndex) {
        updateStepsToDatabase(currentStepCount, dayIndex);
    }

    private void updateStepsToDatabase(int steps, int dayIndex) {
        if (!isInitialized || userId == null) return;

        Map<String, Integer> stepsData = new HashMap<>();
        stepsData.put("steps", steps);
        stepsData.put("dayIndex", dayIndex);

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<StepsResponse> call = api.updateSteps(userId, stepsData);

        call.enqueue(new Callback<StepsResponse>() {
            @Override
            public void onResponse(Call<StepsResponse> call, Response<StepsResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("StepCounterService", "Шаги обновлены: " + steps);
                }
            }

            @Override
            public void onFailure(Call<StepsResponse> call, Throwable t) {
                Log.e("StepCounterService", "Ошибка обновления шагов: " + t.getMessage());
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Служба отслеживания шагов",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Канал для отображения шагов в реальном времени");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String text) {
        Intent notificationIntent = new Intent(this, StepsFragment.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Fitness Tracker - Отслеживание шагов")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, buildNotification(text));
        }
    }
}