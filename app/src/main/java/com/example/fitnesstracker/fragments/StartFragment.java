package com.example.fitnesstracker.fragments;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.ActivityData;
import com.example.fitnesstracker.models.ActivityRequest;
import com.example.fitnesstracker.models.User;
import com.example.fitnesstracker.service.StepCounterService;
import com.example.fitnesstracker.utils.StyleTitleText;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private TextView timerTextView, titleTextView;
    private ImageButton minusButton, plusButton;
    Button startButton;
    private Spinner activitySpinner;
    private Handler handler = new Handler();
    private int seconds = 0;
    private boolean isRunning = false;

    // Константы для запроса разрешений
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // Для работы с местоположением
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Marker userMarker;

    private double userWeight = 0.0; // Вес пользователя

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_start, container, false);

        // Получаем userId из SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId != null) {
            loadUserWeight(userId); // Загружаем вес пользователя
        }

        // Инициализация карты
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Инициализация таймера
        timerTextView = view.findViewById(R.id.timerTextView);
        minusButton = view.findViewById(R.id.minusButton);
        plusButton = view.findViewById(R.id.plusButton);
        titleTextView = view.findViewById(R.id.titleTextView);

        // Инициализация выпадающего списка
        activitySpinner = view.findViewById(R.id.activitySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.activities_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(adapter);

        // Стилизация заголовка
        StyleTitleText styleTitleText = new StyleTitleText();
        styleTitleText.styleTitleText(titleTextView);

        // Инициализация кнопки "Начать"
        startButton = view.findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            if (isRunning) {
                stopTimer();
                startButton.setText("Начать");
            } else {
                startTimer();
                startButton.setText("Остановить");
            }
        });

        // Обработка кнопок "+" и "-"
        minusButton.setOnClickListener(v -> adjustTimer(-300)); // Уменьшить на 5 минут
        plusButton.setOnClickListener(v -> adjustTimer(30));   // Увеличить на 5 минут

        // Инициализация FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Запрос разрешений
        requestLocationPermissions();

        return view;
    }

    private double totalDistance = 0.0; // Общее пройденное расстояние
    private Location previousLocation = null; // Предыдущее местоположение

    private StepCounterService stepCounterService; // Ссылка на сервис
    private boolean isBound = false; // Флаг, указывающий, что сервис привязан
    private int stepsDuringTimer = 0; // Шаги, сделанные за время таймера

    // Список активностей с их MET
    private List<ActivityData> activities = Arrays.asList(
            new ActivityData("Бег", 8),
            new ActivityData("Прогулка", 3.5),
            new ActivityData("Северная ходьба", 7),
            new ActivityData("Езда на велосипеде", 6)
    );

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Настройка карты
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true); // Включение кнопки "Моё местоположение"
            startLocationUpdates(); // Начать отслеживание местоположения
        } else {
            requestLocationPermissions(); // Запросить разрешения, если они не предоставлены
        }
    }

    // Запуск обновлений местоположения
    private void startLocationUpdates() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // Обновление каждые 5 секунд
        locationRequest.setFastestInterval(2000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // Обновление маркера на карте
                    if (userMarker == null) {
                        userMarker = googleMap.addMarker(new MarkerOptions()
                                .position(userLocation)
                                .title("Ваше местоположение"));
                    } else {
                        userMarker.setPosition(userLocation);
                    }

                    // Перемещение камеры к текущему местоположению
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

                    // Расчёт пройденного расстояния
                    if (previousLocation != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                previousLocation.getLatitude(),
                                previousLocation.getLongitude(),
                                location.getLatitude(),
                                location.getLongitude(),
                                results
                        );
                        totalDistance += results[0] / 1000; // Переводим в километры
                    }
                    previousLocation = location;
                }
            }
        };

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    // Остановка обновлений местоположения
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // Запрос разрешений на доступ к местоположению
    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates(); // Если разрешения уже предоставлены, начать отслеживание
        }
    }

    // Обработка результата запроса разрешений
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates(); // Начать отслеживание, если разрешения предоставлены
            } else {
                Toast.makeText(requireContext(), "Разрешения на доступ к местоположению не предоставлены", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLocationUpdates(); // Остановить отслеживание при уничтожении фрагмента
    }


    private void startTimer() {
        isRunning = true;
        handler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
    }

    private void adjustTimer(int delta) {
        seconds += delta;
        if (seconds < 0) seconds = 0;
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        String time = String.format("%02d:%02d:%02d", hours, minutes, secs);
        timerTextView.setText(time);
    }

    // Логика работы таймера
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                if (seconds > 0) {
                    seconds--; // Уменьшаем таймер на 1 секунду
                    updateTimerDisplay();
                } else {
                    stopTimer(); // Останавливаем таймер, когда он достигает 0
                    startButton.setText("Начать");
                    triggerAlarm(); // Запуск сигнала

                    // Получаем шаги, сделанные за время таймера
                    if (stepCounterService != null) {
                        stepsDuringTimer = stepCounterService.stopTrackingStepsForTimer();
                    }

                    // Сохранение активности и расчёт калорий
                    saveActivity();
                }
                handler.postDelayed(this, 1000); // Повторяем каждую секунду
            }
        }
    };

    private void loadUserWeight(String userId) {
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<User> call = api.getUser(userId);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    userWeight = user.getWeight(); // Получаем вес пользователя
                } else {
                    Toast.makeText(requireContext(), "Ошибка при получении данных пользователя", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveActivity() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);
        if (userId == null) {
            Toast.makeText(requireContext(), "Ошибка: Пользователь не залогинен", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedActivity = activitySpinner.getSelectedItem().toString();
        double durationInHours = seconds / 3600.0; // Переводим секунды в часы

        // Находим MET для выбранной активности
        double met = 0;
        for (ActivityData activity : activities) {
            if (activity.getActivityName().equals(selectedActivity)) {
                met = activity.getMET();
                break;
            }
        }

        // Расчёт калорий
        double caloriesBurned = userWeight * met * durationInHours;

        // Сохранение активности
        ActivityRequest activityRequest = new ActivityRequest(
                selectedActivity,
                totalDistance,
                (int) caloriesBurned,
                stepsDuringTimer, // Учитываем шаги
                seconds,
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())
        );

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<ResponseBody> saveCall = api.addActivity(userId, activityRequest);
        saveCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Активность сохранена", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Ошибка при сохранении активности", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Запуск сигнала (Alarm)
    private void triggerAlarm() {
        // Воспроизведение звука
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        RingtoneManager.getRingtone(requireContext(), alarmSound).play();

        // Вибрация
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(1000);
            }
        }

        // Уведомление
        createNotificationChannel();
        showNotification();
    }

    // Создание канала уведомлений (для Android 8.0 и выше)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "fitness_tracker_channel",
                    "Fitness Tracker",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Канал для уведомлений Fitness Tracker");
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Показ уведомления
    private void showNotification() {
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "fitness_tracker_channel",
                    "Fitness Tracker",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        android.app.Notification notification = new android.app.Notification.Builder(requireContext(), "fitness_tracker_channel")
                .setContentTitle("Таймер завершён")
                .setContentText("Ваша тренировка завершена!")
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(1, notification);
    }





}