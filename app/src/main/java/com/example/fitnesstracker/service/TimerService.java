package com.example.fitnesstracker.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TimerService extends Service {
    private final IBinder binder = new TimerBinder();
    private final Handler handler = new Handler();
    private int seconds = 0;
    private boolean isRunning = false;
    public static final String TIMER_UPDATE = "timer_update";

    public class TimerBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning && seconds > 0) {
                seconds--;
                broadcastTimerUpdate();
                handler.postDelayed(this, 1000);
            } else if (seconds == 0) {
                stopTimer();
            }
        }
    };

    private void broadcastTimerUpdate() {
        Intent intent = new Intent(TIMER_UPDATE);
        intent.putExtra("seconds", seconds);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void startTimer(int initialSeconds) {
        seconds = initialSeconds;
        isRunning = true;
        handler.post(timerRunnable);
    }

    public void stopTimer() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
    }

    public int getSeconds() {
        return seconds;
    }

    public boolean isRunning() {
        return isRunning;
    }
}