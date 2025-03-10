package com.example.fitnesstracker.models;

import androidx.lifecycle.ViewModel;

public class TimerViewModel extends ViewModel {
    private int seconds = 0; // Текущее значение таймера
    private boolean isRunning = false; // Состояние таймера (запущен/остановлен)

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}