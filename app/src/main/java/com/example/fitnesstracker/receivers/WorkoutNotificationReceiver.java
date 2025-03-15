package com.example.fitnesstracker.receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.fragments.WorkoutDetailActivity;
import com.example.fitnesstracker.models.Workout;

public class WorkoutNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Извлекаем объект workout из Intent
        Workout workout = (Workout) intent.getSerializableExtra("workout");
        String workoutName = intent.getStringExtra("workoutName"); // Извлекаем название тренировки

        // Создаем уведомление
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Создаем канал уведомлений (для Android 8.0 и выше)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "workout_channel",
                    "Workout Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Канал для уведомлений о тренировках");
            notificationManager.createNotificationChannel(channel);
        }

        // Создаем Intent для открытия приложения при нажатии на уведомление
        Intent notificationIntent = new Intent(context, WorkoutDetailActivity.class);
        notificationIntent.putExtra("workout", workout);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Создаем уведомление
        Notification notification = new NotificationCompat.Builder(context, "workout_channel")
                .setContentTitle("Напоминание о тренировке")
                .setContentText("Сегодня у вас тренировка: " + workoutName) // Используем название тренировки
                .setSmallIcon(R.drawable.ic_notification) // Иконка уведомления
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        // Показываем уведомление
        notificationManager.notify(workoutName.hashCode(), notification); // Используем хэш названия тренировки как ID уведомления
    }
}