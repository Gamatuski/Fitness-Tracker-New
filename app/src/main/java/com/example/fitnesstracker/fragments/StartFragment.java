package com.example.fitnesstracker.fragments;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.activities.CountdownActivity;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.ActivityData;
import com.example.fitnesstracker.models.ActivityRequest;
import com.example.fitnesstracker.models.DistanceResponse;
import com.example.fitnesstracker.models.TimerViewModel;
import com.example.fitnesstracker.models.User;
import com.example.fitnesstracker.models.UserResponse;
import com.example.fitnesstracker.service.StepCounterService;
import com.example.fitnesstracker.service.TimerService;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartFragment extends Fragment implements OnMapReadyCallback {
    private TimerViewModel timerViewModel; // ViewModel для таймера
    private TimerService timerService;
    private boolean bound = false;

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
    private float userHeight = 0.0f; // Рост пользователя
    private int goalSteps;
    private int goalDistance;

    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 200;
    private boolean isFragmentAttached = false;
    private double totalDistance;

    private FrameLayout mapContainer; // Контейнер для карты
    private ViewGroup.LayoutParams originalMapParams; // Исходные параметры макета карты
    private boolean isMapExpanded = false; // Флаг, указывающий, развернута ли карта
    private ImageView closeButton; // Кнопка закрытия развернутой карты

    public static final int RESULT_OK = -1;



    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TimerService.TimerBinder binder = (TimerService.TimerBinder) service;
            timerService = binder.getService();
            bound = true;

            if (timerService.isRunning()) {
                seconds = timerService.getSeconds();
                isRunning = true;
                startButton.setText("Остановить");
                updateTimerDisplay();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        isFragmentAttached = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isFragmentAttached = false;
    }

    // Add this to your existing service connections in StartFragment
    private ServiceConnection stepServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepCounterService.StepBinder binder = (StepCounterService.StepBinder) service;
            stepCounterService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            stepCounterService = null;
            isBound = false;
        }
    };

    private final BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            seconds = intent.getIntExtra("seconds", 0);
            updateTimerDisplay();
            if (seconds == 0) {
                stopTimer();
                startButton.setText("Начать");
                triggerAlarm();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_start, container, false);

        requestActivityRecognitionPermission(); // Запрос разрешения

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId != null) {
            loadUserWeightAndHeight(userId); // Загружаем вес пользователя
        } else {
            Log.e("LoadUserWeight", "User ID is null");
            Toast.makeText(requireContext(), "Ошибка: Пользователь не залогинен", Toast.LENGTH_SHORT).show();
        }

        // Инициализация карты
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Инициализация таймера
        timerTextView = view.findViewById(R.id.timerTextView);
        minusButton = view.findViewById(R.id.minusButton);
        plusButton = view.findViewById(R.id.plusButton);

        // Инициализация выпадающего списка
        activitySpinner = view.findViewById(R.id.activitySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.activities_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(adapter);


        // Инициализация кнопки "Начать"
        startButton = view.findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            if (isRunning) {
                stopTimer();
                startButton.setText("Начать");
            } else {
                //startButton.setText("Отсановить");

                // Запуск CountdownActivity
                Intent intent = new Intent(getActivity(), CountdownActivity.class);
                startActivityForResult(intent, 1); // Используем startActivityForResult
            }
        });

        // Инициализация ViewModel
        timerViewModel = new ViewModelProvider(requireActivity()).get(TimerViewModel.class);

        // Восстановление состояния таймера
        seconds = timerViewModel.getSeconds();
        isRunning = timerViewModel.isRunning();

        // Обновление UI в соответствии с состоянием таймера
        updateTimerDisplay();
        if (isRunning) {
            startButton.setText("Остановить");
        } else {
            startButton.setText("Начать");
        }


        // Обработка кнопок "+" и "-"
        minusButton.setOnClickListener(v -> adjustTimer(-300)); // Уменьшить на 5 минут
        plusButton.setOnClickListener(v -> adjustTimer(30));   // Увеличить на 5 минут

        // Инициализация FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Запрос разрешений
        requestLocationPermissions();

        mapContainer = view.findViewById(R.id.map_container); // Находим контейнер карты в макете
        closeButton = view.findViewById(R.id.close_button); // Находим кнопку закрытия в макете
        closeButton.setVisibility(View.GONE); // Initially hide the close button // Скрываем кнопку закрытия при запуске

        // Store original map layout parameters
        originalMapParams = mapContainer.getLayoutParams(); // Сохраняем исходные параметры макета

        // Set click listener for map expansion
        mapContainer.setOnClickListener(v -> { // Устанавливаем слушатель кликов на контейнер карты
            if (!isMapExpanded) { // Если карта не развернута
                Log.d("MapClick", "Map container clicked!");
                expandMap(); // Разворачиваем карту
            }
        });

        // Set click listener for close button
        closeButton.setOnClickListener(v -> { // Устанавливаем слушатель кликов на кнопку закрытия
            collapseMap(); // Сворачиваем карту
        });


        return view;
    }

     //Обработка результата из CountdownActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Запуск таймера после завершения CountdownActivity
            startTimer();
            startButton.setText("Остановить");
        }
    }


    private Location previousLocation = null; // Предыдущее местоположение

    private StepCounterService stepCounterService; // Ссылка на сервис
    private boolean isBound = false; // Флаг, указывающий, что сервис привязан
    private int stepsDuringTimer = 0; // Шаги, сделанные за время таймера

    // Список активностей с их MET
    private List<ActivityData> activities = Arrays.asList(
            new ActivityData("Бег", 8),
            new ActivityData("Прогулка", 3.5),
            new ActivityData("Скандинавская ходьба", 7),
            new ActivityData("Езда на велосипеде", 6)
    );

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap; // Сохраняем ссылку на GoogleMap



        // Настройка карты
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true); // Включение кнопки "Моё местоположение"
            startLocationUpdates(); // Начать отслеживание местоположения
        } else {
            requestLocationPermissions(); // Запросить разрешения, если они не предоставлены
        }

        // Теперь клики должны передаваться на mapContainer
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStepsInDatabase();
    }

    private void updateStepsInDatabase() {
        if (stepCounterService != null && stepCounterService.getCurrentStepCount() > 0) {
            Calendar calendar = Calendar.getInstance();
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2;
            if (currentDayOfWeek == -1) currentDayOfWeek = 6;

            stepCounterService.forceUpdateStepsToDatabase(currentDayOfWeek);
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

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(requireContext(), TimerService.class);
        requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                timerUpdateReceiver,
                new IntentFilter(TimerService.TIMER_UPDATE)
        );

    }

    @Override
    public void onStop() {
        super.onStop();
        if (bound) {
            requireContext().unbindService(connection);
            bound = false;
        }
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(timerUpdateReceiver);

        if (isBound) {
            requireContext().unbindService(stepServiceConnection);
            isBound = false;
        }
    }


    private void startTimer() {
        if (!isBound || stepCounterService == null) {
            Log.e("StartFragment", "StepCounterService is not bound or null");
            bindStepCounterService(); // Попытка привязать сервис
            handler.postDelayed(() -> {
                if (isBound && stepCounterService != null) {
                    isRunning = true;
                    timerViewModel.setRunning(true);
                    timerService.startTimer(seconds);
                    startButton.setText("Остановить");
                } else {
                    Log.e("StartFragment", "StepCounterService is still not bound");
                    Toast.makeText(getContext(), "Ошибка: Сервис не доступен", Toast.LENGTH_SHORT).show();
                }
            }, 1000); // Повторная попытка через 1 секунду
        } else {
            isRunning = true;
            timerViewModel.setRunning(true);
            timerService.startTimer(seconds);
            startButton.setText("Остановить");
        }
    }

    private void bindStepCounterService() {
        Intent stepIntent = new Intent(requireContext(), StepCounterService.class);
        requireContext().bindService(stepIntent, stepServiceConnection, Context.BIND_AUTO_CREATE);

        // Запуск сервиса, если он еще не запущен
        requireContext().startForegroundService(stepIntent);
    }

    private void stopTimer() {
        if (isBound && stepCounterService != null) {
            isRunning = false;
            timerViewModel.setRunning(false);
            timerService.stopTimer();
            startButton.setText("Начать");

            // Получаем шаги, сделанные за время таймера
            int stepsDuringTimer = stepCounterService.stopTrackingStepsForTimer();
            Log.d("StartFragment", "Шаги за таймер: " + stepsDuringTimer);

            // Получаем общую длительность активности
            long totalDuration = stepCounterService.getTotalDuration();
            Log.d("StartFragment", "Длительность активности: " + totalDuration + " секунд");

            // Получаем totalDistance из StepCounterService
            double distanceDuringTimer = stepCounterService.getDistanceDuringTimer();
            Log.d("StartFragment", "Дистанция за таймер: " + distanceDuringTimer + " км");

            // Сохраняем активность
            saveActivity(distanceDuringTimer, stepsDuringTimer, totalDuration);
        } else {
            Log.e("StartFragment", "StepCounterService is not bound or is null");
            // Можно добавить повторную попытку через некоторое время
            handler.postDelayed(() -> {
                if (isBound && stepCounterService != null) {
                    isRunning = false;
                    timerViewModel.setRunning(false);
                    timerService.stopTimer();
                    startButton.setText("Начать");

                    int stepsDuringTimer = stepCounterService.stopTrackingStepsForTimer();
                    long totalDuration = stepCounterService.getTotalDuration();
                    double distanceDuringTimer = stepCounterService.getDistanceDuringTimer();
                    saveActivity(distanceDuringTimer, stepsDuringTimer, totalDuration);
                } else {
                    Log.e("StartFragment", "StepCounterService is still not bound");
                }
            }, 1000); // Повторная попытка через 1 секунду
        }
    }

    private void adjustTimer(int delta) {
        seconds += delta;
        if (seconds < 0) seconds = 0;
        timerViewModel.setSeconds(seconds);
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        String time = String.format("%02d:%02d:%02d", hours, minutes, secs);
        timerTextView.setText(time);
    }


    private void loadUserWeightAndHeight(String userId) {
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<UserResponse> call = api.getUser(userId);
        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body().getUser();
                    userWeight = user.getWeight(); // Получаем вес пользователя
                    userHeight = user.getHeight(); // Получаем рост пользователя
                    goalSteps = user.getStepsGoal();
                    goalDistance = user.getDistanceGoal();

                    // Сохраняем userHeight в SharedPreferences
                    SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putFloat("userHeight", (float) userHeight); // Сохраняем как float
                    editor.apply();

                    Log.d("LoadUserWeight", "User weight loaded: " + userWeight);
                    Log.d("LoadUserHeight", "User height loaded: " + userHeight);

                    // Bind to StepCounterService
                    Intent stepIntent = new Intent(requireContext(), StepCounterService.class);
                    requireContext().bindService(stepIntent, stepServiceConnection, Context.BIND_AUTO_CREATE);

                    // Start the service
                    requireContext().startForegroundService(stepIntent);

                } else {
                    Log.e("LoadUserWeight", "Error loading user data: " + response.message());
                    Toast.makeText(requireContext(), "Ошибка при получении данных пользователя", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e("LoadUserWeight", "Network error: " + t.getMessage());
                Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveActivity(double distanceDuringTimer, int stepsDuringTimer, long totalDuration) {
        Log.d("SaveActivity", "Начало сохранения активности");

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);
        Log.d("SaveActivity", "userId: " + userId);

        if (userId == null) {
            Toast.makeText(requireContext(), "Ошибка: Пользователь не залогинен", Toast.LENGTH_SHORT).show();
            Log.e("SaveActivity", "Ошибка: Пользователь не залогинен");
            return;
        }

        // Получаем выбранную активность из Spinner
        String selectedActivity = activitySpinner.getSelectedItem().toString();
        Log.d("SaveActivity", "selectedActivity: " + selectedActivity);

        // Переводим секунды в часы для расчета калорий
        double durationInHours = totalDuration / 3600;
        double durationInMin = totalDuration / 60.0;
        Log.d("SaveActivity", "durationInMin: " + durationInMin);

        // Находим MET для выбранной активности
        double met = 0;
        for (ActivityData activity : activities) {
            if (activity.getActivityName().equals(selectedActivity)) {
                met = activity.getMET();
                break;
            }
        }
        Log.d("SaveActivity", "met: " + met);

        // Расчёт калорий
        double caloriesBurned = userWeight * met * durationInHours;
        Log.d("SaveActivity", "caloriesBurned: " + caloriesBurned);

        // Получаем текущую дату в формате "yyyy-MM-dd"
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        Log.d("SaveActivity", "currentDate: " + currentDate);

        Log.d("SaveActivity", "distanceDuringTimer: " + distanceDuringTimer);
        Log.d("SaveActivity", "stepsDuringTimer: " + stepsDuringTimer);
        Log.d("SaveActivity", "totalDuration: " + totalDuration);

        // Создаем запрос для сохранения активности
        ActivityRequest activityRequest = new ActivityRequest(
                selectedActivity,
                distanceDuringTimer, // Используем distanceDuringTimer
                (int) caloriesBurned,
                stepsDuringTimer, // Учитываем шаги
                durationInMin, // Общая длительность активности
                currentDate       // Текущая дата
        );

        // Отправляем запрос на сервер
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<ResponseBody> saveCall = api.addActivity(userId, activityRequest);
        saveCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d("SaveActivity", "Response code: " + response.code());
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Активность сохранена", Toast.LENGTH_SHORT).show();
                    Log.d("SaveActivity", "Активность успешно сохранена");
                } else {
                    Toast.makeText(requireContext(), "Ошибка при сохранении активности", Toast.LENGTH_SHORT).show();
                    Log.e("SaveActivity", "Ошибка при сохранении активности: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                Log.e("SaveActivity", "Ошибка сети: " + t.getMessage());
            }
        });

        Log.d("SaveActivity", "Завершение сохранения активности");
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

    private void requestActivityRecognitionPermission() {
        // Android 10 и выше
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
        }
    }

    public int getDayIndex(){
        // Получаем текущий день недели (0-6)
        Calendar calendar = Calendar.getInstance();
        int dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2;
        if (dayIndex == -1) dayIndex = 6;

        return dayIndex;
    }

    private void expandMap() {
        isMapExpanded = true; // Устанавливаем флаг, что карта развернута
        closeButton.setVisibility(View.VISIBLE); // Показываем кнопку закрытия

        // Store original values for animation
        int originalWidth = mapContainer.getWidth();
        int originalHeight = mapContainer.getHeight();

        // Calculate target values for full-screen expansion
        int targetWidth = getView().getWidth();
        int targetHeight = getView().getHeight();

        // Create width animator
        ValueAnimator widthAnimator = ValueAnimator.ofInt(originalWidth, targetWidth);
        widthAnimator.addUpdateListener(animation -> {
            int newWidth = (int) animation.getAnimatedValue();
            mapContainer.getLayoutParams().width = newWidth;
            mapContainer.requestLayout();
        });

        // Create height animator
        ValueAnimator heightAnimator = ValueAnimator.ofInt(originalHeight, targetHeight);
        heightAnimator.addUpdateListener(animation -> {
            int newHeight = (int) animation.getAnimatedValue();
            mapContainer.getLayoutParams().height = newHeight;
            mapContainer.requestLayout();
        });

        // Set animator duration
        long animationDuration = 300;
        widthAnimator.setDuration(animationDuration);
        heightAnimator.setDuration(animationDuration);

        // Start the animators
        widthAnimator.start();
        heightAnimator.start();
    }

    private void collapseMap() {
        isMapExpanded = false; // Устанавливаем флаг, что карта свернута
        closeButton.setVisibility(View.GONE); // Скрываем кнопку закрытия

        // Store current values for animation
        int currentWidth = mapContainer.getWidth();
        int currentHeight = mapContainer.getHeight();

        // Calculate target values for original size
        int targetWidth = originalMapParams.width;
        int targetHeight = originalMapParams.height;

        // Create width animator
        ValueAnimator widthAnimator = ValueAnimator.ofInt(currentWidth, targetWidth);
        widthAnimator.addUpdateListener(animation -> {
            int newWidth = (int) animation.getAnimatedValue();
            mapContainer.getLayoutParams().width = newWidth;
            mapContainer.requestLayout();
        });

        // Create height animator
        ValueAnimator heightAnimator = ValueAnimator.ofInt(currentHeight, targetHeight);
        heightAnimator.addUpdateListener(animation -> {
            int newHeight = (int) animation.getAnimatedValue();
            mapContainer.getLayoutParams().height = newHeight;
            mapContainer.requestLayout();
        });

        // Set animator duration
        long animationDuration = 300;
        widthAnimator.setDuration(animationDuration);
        heightAnimator.setDuration(animationDuration);

        // Start the animators
        widthAnimator.start();
        heightAnimator.start();
    }



}