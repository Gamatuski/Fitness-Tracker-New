package com.example.fitnesstracker.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TimerService extends Service {
    private final IBinder binder = new TimerBinder();
    private final Handler handler = new Handler();
    private int seconds = 0;
    private boolean isRunning = false;
    public static final String TIMER_UPDATE = "timer_update";

    private StepCounterService stepCounterService;
    private boolean isStepCounterServiceBound = false;


    private final ServiceConnection stepCounterServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepCounterService.StepBinder binder = (StepCounterService.StepBinder) service;
            stepCounterService = binder.getService();
            isStepCounterServiceBound = true;
            Log.d("TimerService", "StepCounterService connected");
            if (stepCounterService != null && isRunning) {
                stepCounterService.startTrackingStepsForTimer();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isStepCounterServiceBound = false;
            stepCounterService = null;
            Log.d("TimerService", "StepCounterService disconnected");
        }
    };


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
                updateTotalDuration(1);
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

    private void bindStepCounterService() {
        Intent intent = new Intent(this, StepCounterService.class);
        bindService(intent, stepCounterServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindStepCounterService() {
        if (isStepCounterServiceBound) {
            if (stepCounterService != null) {
                stepCounterService.stopTrackingStepsForTimer();
            }
            unbindService(stepCounterServiceConnection);
            isStepCounterServiceBound = false;
            stepCounterService = null;
            Log.d("TimerService", "StepCounterService unbound");
        }
    }

    public void startTimer(int initialSeconds) {
        seconds = initialSeconds;
        isRunning = true;

        // Привязываем StepCounterService, если он еще не привязан
        if (!isStepCounterServiceBound) {
            bindStepCounterService();
        }

        // Проверяем, что StepCounterService привязан и не равен null
        if (stepCounterService != null) {
            stepCounterService.startTrackingStepsForTimer();
        } else {
            Log.e("TimerService", "StepCounterService is not bound yet");
            // Можно добавить повторную попытку через некоторое время
            handler.postDelayed(() -> {
                if (stepCounterService != null) {
                    stepCounterService.startTrackingStepsForTimer();
                } else {
                    Log.e("TimerService", "StepCounterService is still not bound");
                }
            }, 1000); // Повторная попытка через 1 секунду
        }

        handler.post(timerRunnable);
    }

    public void stopTimer() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);

        // Проверяем, что StepCounterService привязан и не равен null
        if (isStepCounterServiceBound && stepCounterService != null) {
            stepCounterService.stopTrackingStepsForTimer();
        } else {
            Log.e("TimerService", "StepCounterService is not bound or is null");
        }

        // Отвязываем сервис, если он был привязан
        unbindStepCounterService();
    }

    public int getSeconds() {
        return seconds;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void updateTotalDuration(long duration) {
        if (isStepCounterServiceBound && stepCounterService != null) {
            stepCounterService.updateTotalDuration(duration);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindStepCounterService();
    }
}