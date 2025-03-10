// WorkoutNotificationReceiver.java
package com.example.fitnesstracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.fitnesstracker.models.Workout;

public class WorkoutNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Workout workout = (Workout) intent.getSerializableExtra("workout");
        if (workout != null) {
            // Отправляем уведомление
            Log.d("WorkoutNotification", "Уведомление для тренировки: " + workout.getAction());
            // Здесь можно добавить код для отображения уведомления
        }
    }
}