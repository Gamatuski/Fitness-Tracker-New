package com.example.fitnesstracker.fragments;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.models.Workout;
import com.example.fitnesstracker.receivers.WorkoutNotificationReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WorkoutDetailActivity extends AppCompatActivity {

    private Workout workout;
    private TextView startDate, currentDate, notificationTime, preferredDays;
    private ImageView workoutImage, arrowIcon;
    private Button doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);

        // Получаем данные Workout из Intent
        workout = (Workout) getIntent().getSerializableExtra("workout");

        // Инициализация UI
        startDate = findViewById(R.id.startDate);
        currentDate = findViewById(R.id.currentDate);
        notificationTime = findViewById(R.id.notificationTime);
        preferredDays = findViewById(R.id.preferredDays);
        workoutImage = findViewById(R.id.workoutImage);
        arrowIcon = findViewById(R.id.arrowIcon);
        doneButton = findViewById(R.id.doneButton);

        // Устанавливаем данные Workout
        if (workout != null) {
            if (workout.getImage() != null && workout.getImage().getImageUrl() != null) {
                Glide.with(this)
                        .load(workout.getImage().getImageUrl())
                        .into(workoutImage);
            }

            // Устанавливаем даты
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            String currentDateStr = dateFormat.format(Calendar.getInstance().getTime());
            startDate.setText("Дата начала: " + currentDateStr);
            currentDate.setText("Сегодня: " + currentDateStr);

            // Устанавливаем время уведомления по умолчанию
            notificationTime.setText("Время уведомления: 18:00");

            // Устанавливаем предпочтительные дни
            preferredDays.setText("Предпочтительные дни: Вторник, Четверг, Суббота");

            // Обработка нажатия на кнопку "Готово"
            doneButton.setOnClickListener(v -> scheduleNotifications());
        }
    }

    private void scheduleNotifications() {
        // Получаем выбранные дни и время уведомления
        String[] days = {"Вторник", "Четверг", "Суббота"};
        String time = "18:00";

        // Создаем уведомления на выбранные дни
        for (String day : days) {
            scheduleNotification(day, time);
        }

        Toast.makeText(this, "Уведомления запланированы", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void scheduleNotification(String day, String time) {
        // Преобразуем день недели в Calendar.DAY_OF_WEEK
        int dayOfWeek = getDayOfWeek(day);

        // Преобразуем время в часы и минуты
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // Создаем Calendar для установки времени уведомления
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Создаем Intent для BroadcastReceiver
        Intent intent = new Intent(this, WorkoutNotificationReceiver.class);
        intent.putExtra("workout", workout);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, dayOfWeek, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Устанавливаем уведомление через AlarmManager
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY * 7, pendingIntent);
        }
    }

    private int getDayOfWeek(String day) {
        switch (day) {
            case "Понедельник":
                return Calendar.MONDAY;
            case "Вторник":
                return Calendar.TUESDAY;
            case "Среда":
                return Calendar.WEDNESDAY;
            case "Четверг":
                return Calendar.THURSDAY;
            case "Пятница":
                return Calendar.FRIDAY;
            case "Суббота":
                return Calendar.SATURDAY;
            case "Воскресенье":
                return Calendar.SUNDAY;
            default:
                return Calendar.MONDAY;
        }
    }
}