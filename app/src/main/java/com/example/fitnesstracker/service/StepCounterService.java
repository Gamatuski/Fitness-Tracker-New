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
import com.example.fitnesstracker.models.DistanceResponse;
import com.example.fitnesstracker.models.StepsResponse;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StepCounterService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "StepCounterChannel";
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private int initialStepCount = -1;
    private int baseSteps = 0;  // Базовое значение шагов, загруженное из базы данных
    private int currentStepCount = 0;
    private int timerInitialSteps = 0;
    private int stepsDuringTimer = 0;  // Шаги, сделанные за время таймера
    private boolean isTimerRunning = false;
    private String userId;
    private float userHeight;
    private boolean isInitialized = false;
    private long totalDuration = 0;  // Общая длительность активности в секундах
    private double totalDistance = 0.0;

    private double distanceDuringTimer = 0.0; // Дистанция, пройденная за время таймера
    private double previousTotalDistance = 0.0; // Предыдущее значение totalDistance

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
        userHeight = sharedPreferences.getFloat("userHeight", 180.0f); // Получаем рост пользователя
        Log.e("StepCounterService","userHeight" +userHeight );
        if (userId == null) {
            Log.e("StepCounterService", "userId не найден");
            stopSelf();
            return;
        }

        // Загружаем начальное значение шагов из SharedPreferences
        initialStepCount = sharedPreferences.getInt("initialStepCount", -1);

        // Загружаем базовое значение шагов из базы данных
        loadCurrentStepsFromDatabase(() -> {
            isInitialized = true;
        });

        createNotificationChannel();
        startForeground(1, buildNotification("Служба отслеживания шагов запущена"));
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (!isInitialized) {
                // Инициализация начального значения шагов
                initialStepCount = (int) event.values[0];
                SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("initialStepCount", initialStepCount);
                editor.apply();
                return;
            }

            // Рассчитываем текущее количество шагов
            int currentSteps = (int) event.values[0] - initialStepCount + baseSteps;

            // Обновляем текущее количество шагов
            if (currentSteps != currentStepCount) {
                currentStepCount = currentSteps;

                // Если таймер активен, обновляем шаги, сделанные за время таймера
                if (isTimerRunning) {
                    if(timerInitialSteps == 0) timerInitialSteps = currentStepCount;
                    stepsDuringTimer = currentStepCount - timerInitialSteps;
                    // Рассчитываем дистанцию за время таймера
                    updateDistanceDuringTimer(stepsDuringTimer);

                    // Логирование значений
                    Log.d("StepCounterService", "Timer is running:");
                    Log.d("StepCounterService", "  currentStepCount: " + currentStepCount);
                    Log.d("StepCounterService", "  timerInitialSteps: " + timerInitialSteps);
                    Log.d("StepCounterService", "  stepsDuringTimer: " + stepsDuringTimer);
                }

                // Обновляем шаги в базе данных
                Calendar calendar = Calendar.getInstance();
                int dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2;
                if (dayIndex == -1) dayIndex = 6;

                updateStepsToDatabase(currentStepCount, dayIndex);
                updateDistance(currentStepCount);

                // Обновляем уведомление с текущим количеством шагов
                updateNotification("Шагов: " + currentStepCount);
            }
        }
    }

    private void updateDistance(int steps) {

        // Рассчитываем длину шага по формуле: ДШ = (Рост / 4) + 0.37
        double stepLength = ((userHeight / 100) / 4) + 0.37;

        // Рассчитываем пройденное расстояние в метрах
        double distanceInMeters = steps * stepLength;

        // Переводим расстояние в километры
        double distanceInKilometers = distanceInMeters / 1000;

        // Обновляем общее расстояние
        totalDistance = distanceInKilometers; // Обновляем totalDistance

        Log.d("StepCounterService", "Шаги: " + steps + ", Длина шага: " + stepLength + " м, Расстояние: " + totalDistance + " км");

        // Обновляем расстояние в базе данных
        updateDistanceToDatabase(totalDistance, getDayIndex());

    }

    private void updateDistanceDuringTimer(int steps) {
        // Рассчитываем длину шага по формуле: ДШ = (Рост / 4) + 0.37
        double stepLength = ((userHeight / 100) / 4) + 0.37;

        // Рассчитываем пройденное расстояние в метрах
        double distanceInMeters = steps * stepLength;

        // Переводим расстояние в километры
        double distanceInKilometers = distanceInMeters / 1000;

        distanceDuringTimer = distanceInKilometers;

        Log.d("StepCounterService", "Шаги за таймер: " + steps + ", Длина шага: " + stepLength + " м, Расстояние за таймер: " + distanceDuringTimer + " км");
    }

    private void updateDistanceToDatabase(double distance, int dayIndex) {
        if (!isInitialized || userId == null) return;

        Map<String, Double> distanceData = new HashMap<>();
        distanceData.put("distance", distance);
        distanceData.put("dayIndex", (double) dayIndex);

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<DistanceResponse> call = api.updateDistance(userId, distanceData);

        call.enqueue(new Callback<DistanceResponse>() {
            @Override
            public void onResponse(Call<DistanceResponse> call, Response<DistanceResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("StepCounterService", "Расстояние обновлено: " + distance);
                }
            }

            @Override
            public void onFailure(Call<DistanceResponse> call, Throwable t) {
                Log.e("StepCounterService", "Ошибка обновления расстояния: " + t.getMessage());
            }
        });
    }

    public int getDayIndex(){
        // Получаем текущий день недели (0-6)
        Calendar calendar = Calendar.getInstance();
        int dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2;
        if (dayIndex == -1) dayIndex = 6;

        return dayIndex;
    }

    // Метод для сброса дистанции таймера
    public void resetDistanceDuringTimer() {
        distanceDuringTimer = 0.0;
        previousTotalDistance = totalDistance;
    }

    // Метод для получения дистанции, пройденной за время таймера
    public double getDistanceDuringTimer() {
        return distanceDuringTimer;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Не используется
    }

    public void startTrackingStepsForTimer() {
        stepsDuringTimer = 0;
        isTimerRunning = true;
        totalDuration = 0;  // Сбрасываем общую длительность активности при старте таймера
        resetDistanceDuringTimer(); // Сбрасываем дистанцию таймера
        previousTotalDistance = totalDistance;
    }

    public int stopTrackingStepsForTimer() {
        isTimerRunning = false;
        return stepsDuringTimer;
    }


    public int getCurrentStepCount() {
        return currentStepCount;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void updateTotalDuration(long duration) {
        totalDuration += duration;
    }

    public void forceUpdateStepsToDatabase(int dayIndex) {
        updateStepsToDatabase(currentStepCount, dayIndex);
        updateDistance(currentStepCount);
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

    public double getTotalDistance() {
        return totalDistance;
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