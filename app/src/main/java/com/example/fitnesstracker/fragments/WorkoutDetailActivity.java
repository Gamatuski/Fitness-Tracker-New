package com.example.fitnesstracker.fragments;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.models.Workout;
import com.example.fitnesstracker.receivers.WorkoutNotificationReceiver;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WorkoutDetailActivity extends AppCompatActivity {

    private Workout workout;
    private TextView  currentDate, notificationTime, preferredDays, action, difficultyText;
    private ImageView workoutImage, arrowIcon;
    private Button doneButton;

    private ConstraintLayout preferredDaysLayout;

    private static final int REQUEST_CODE_PREFERRED_DAYS = 1;

    private static final List<String> DAYS_OF_WEEK = Arrays.asList(
            "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);

        // Получаем данные Workout из Intent
        workout = (Workout) getIntent().getSerializableExtra("workout");

        // Инициализация UI
        currentDate = findViewById(R.id.currentDate);
        notificationTime = findViewById(R.id.notificationTime);
        preferredDays = findViewById(R.id.preferredDays);
        workoutImage = findViewById(R.id.workoutImage);
        arrowIcon = findViewById(R.id.arrowIcon);
        doneButton = findViewById(R.id.doneButton);
        action = findViewById(R.id.action);
        difficultyText = findViewById(R.id.difficultyText);

        preferredDaysLayout = findViewById(R.id.preferredDaysLayout);

        // Обработка нажатия на поле "Предпочтительные дни тренировки"
        findViewById(R.id.preferredDaysLayout).setOnClickListener(v -> {
            Intent intent = new Intent(this, PreferredDaysActivity.class);

            // Получаем текущие выбранные дни из TextView
            TextView preferredDaysTextView = findViewById(R.id.preferredDays);
            String selectedDaysText = preferredDaysTextView.getText().toString();
            List<String> selectedDays = Arrays.asList(selectedDaysText.split(", "));

            // Передаем выбранные дни в Intent
            intent.putStringArrayListExtra("selectedDays", new ArrayList<>(selectedDays));
            startActivityForResult(intent, REQUEST_CODE_PREFERRED_DAYS);
        });


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
            currentDate.setText(currentDateStr);


            notificationTime.setOnClickListener(v -> showTimePickerBottomSheet());

            // Устанавливаем предпочтительные дни
            preferredDays.setText("Вторник, Четверг, Суббота");

            action.setText(workout.getAction());
            difficultyText.setText(workout.getDifficulty());

            // Обработка нажатия на currentDate
            currentDate.setOnClickListener(v -> showDatePickerBottomSheet());

            // Обработка нажатия на кнопку "Готово"
            doneButton.setOnClickListener(v -> scheduleNotifications());
        }
    }

    private void showDatePickerBottomSheet() {
        // Создание экземпляра DatePickerBottomSheet
        DatePickerBottomSheet datePickerBottomSheet = new DatePickerBottomSheet();

        // Установка слушателя для получения выбранной даты
        datePickerBottomSheet.setOnDateSelectedListener(selectedDate -> {
            // Обновляем текст в currentDate
            currentDate.setText(selectedDate);
        });

        // Показываем BottomSheetDialog
        datePickerBottomSheet.show(getSupportFragmentManager(), "DatePickerBottomSheet");
    }

    private void showTimePickerBottomSheet() {
        // Создание экземпляра timePickerBottomSheet
        TimePickerBottomSheet timePickerBottomSheet = new TimePickerBottomSheet();

        // Установка слушателя для получения выбранного времени
        timePickerBottomSheet.setOnTimeSelectedListener(selectedTime -> {
            // Обновляем текст в currentDate
            notificationTime.setText(selectedTime);
        });

        // Показываем BottomSheetDialog
        timePickerBottomSheet.show(getSupportFragmentManager(), "TimePickerBottomSheet");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PREFERRED_DAYS && resultCode == RESULT_OK) {
            // Получаем выбранные дни из Intent
            List<String> selectedDays = data.getStringArrayListExtra("selectedDays");

            // Сортируем выбранные дни в порядке дней недели
            String selectedDaysText = sortDays(selectedDays);
            preferredDays.setText(selectedDaysText);
        }
    }


    private String sortDays(List<String> days) {
        if (days == null || days.isEmpty()) {
            return "Дни не выбраны";
        }


        // Сортируем дни в порядке дней недели
        days.sort((day1, day2) -> {
            int index1 = DAYS_OF_WEEK.indexOf(day1);
            int index2 = DAYS_OF_WEEK.indexOf(day2);
            return Integer.compare(index1, index2);
        });

        // Возвращаем отсортированную строку
        return TextUtils.join(", ", days);
    }

    private void scheduleNotifications() {
        // Получаем выбранные дни и время уведомления
        String[] days = preferredDays.getText().toString().split(",");
        String time = notificationTime.getText().toString() ;

        // Создаем уведомления на выбранные дни
        for (String day : days) {
            scheduleNotification(day, time);
        }

        Toast.makeText(this, "Уведомления запланированы", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void scheduleNotification(String day, String time) {
        // Преобразуем день недели в Calendar.DAY_OF_WEEK
        int dayOfWeek = getDayOfWeek(day.trim()); // Убедимся, что лишние пробелы удалены

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
        intent.putExtra("workout", workout); // Передаем объект workout
        intent.putExtra("workoutName", workout.getAction()); // Передаем название тренировки
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