package com.example.fitnesstracker.fragments;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.fitnesstracker.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class StartFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private TextView timerTextView;
    private ImageButton minusButton, plusButton;
    Button startButton;
    private Spinner activitySpinner;
    private Handler handler = new Handler();
    private int seconds = 0;
    private boolean isRunning = false;

    // Константы для запроса разрешений
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_start, container, false);

        // Инициализация карты
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Инициализация таймера
        timerTextView = view.findViewById(R.id.timerTextView);
        minusButton = view.findViewById(R.id.minusButton);
        plusButton = view.findViewById(R.id.plusButton);
        minusButton.setVisibility(View.VISIBLE); // Убедитесь, что кнопка видима
        plusButton.setVisibility(View.VISIBLE);  // Убедитесь, что кнопка видима
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
                startTimer();
                startButton.setText("Остановить");
            }
        });

        // Обработка кнопок "+" и "-"
        minusButton.setOnClickListener(v -> adjustTimer(-300)); // Уменьшить на 5 минут
        plusButton.setOnClickListener(v -> adjustTimer(30));   // Увеличить на 5 минут

        return view;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Настройка карты
        LatLng defaultLocation = new LatLng(55.7558, 37.6176); // Москва
        googleMap.addMarker(new MarkerOptions()
                .position(defaultLocation)
                .title("Старт")); // Установка заголовка маркера
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
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
                }
                handler.postDelayed(this, 1000); // Повторяем каждую секунду
            }
        }
    };

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

    // Запрос разрешений
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }


}