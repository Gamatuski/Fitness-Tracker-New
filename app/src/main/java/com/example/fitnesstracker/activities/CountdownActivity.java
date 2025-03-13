package com.example.fitnesstracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fitnesstracker.R;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CountdownActivity extends AppCompatActivity {

    private TextView countdownText;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        countdownText = findViewById(R.id.countdownText);
        progressBar = findViewById(R.id.progressBar);

        startCountdown();
    }

    private void startCountdown() {
        // Устанавливаем максимальное значение ProgressBar
        progressBar.setMax(5); // 5 секунд
        progressBar.setProgress(5); // Начальное значение

        // Отсчет времени
        countDownTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                countdownText.setText(String.valueOf(secondsRemaining + 1));
                progressBar.setProgress(secondsRemaining); // Обновляем ProgressBar
            }

            @Override
            public void onFinish() {
                countdownText.setText("0");
                progressBar.setProgress(0);

                // Задержка перед закрытием активности
                new Handler().postDelayed(() -> {
                    setResult(RESULT_OK); // Устанавливаем результат
                    finish(); // Закрываем активность
                }, 500); // Задержка 500 мс перед закрытием
            }
        };
        countDownTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}