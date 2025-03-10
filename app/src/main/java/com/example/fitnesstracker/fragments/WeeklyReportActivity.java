package com.example.fitnesstracker.fragments;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.DistanceResponse;
import com.example.fitnesstracker.models.StepsResponse;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeeklyReportActivity extends AppCompatActivity {

    private BarChart stepsBarChart, distanceBarChart;
    private TextView averageStepsTextView, averageDistanceTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_report);

        // Инициализация элементов
        stepsBarChart = findViewById(R.id.stepsBarChart);
        distanceBarChart = findViewById(R.id.distanceBarChart);
        averageStepsTextView = findViewById(R.id.averageStepsTextView);
        averageDistanceTextView = findViewById(R.id.averageDistanceTextView);

        // Получение userId из SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId != null) {
            // Загрузка данных из базы данных
            loadStepsData(userId);
            loadDistanceData(userId);
        } else {
            Toast.makeText(this, "Ошибка: Пользователь не авторизован", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadStepsData(String userId) {
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<StepsResponse> call = api.getWeeklySteps(userId);
        call.enqueue(new Callback<StepsResponse>() {
            @Override
            public void onResponse(Call<StepsResponse> call, Response<StepsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Integer> stepsData = response.body().getSteps();
                    if (stepsData != null && stepsData.size() == 7) {
                        // Рассчитаем среднее значение шагов
                        int averageSteps = (int) calculateAverage(stepsData);
                        averageStepsTextView.setText(String.valueOf(averageSteps));

                        setupBarChart(stepsBarChart, convertToBarEntries(stepsData), "Шаги", averageSteps, 5000, ContextCompat.getColor(WeeklyReportActivity.this, R.color.purpule));
                    } else {
                        Toast.makeText(WeeklyReportActivity.this, "Неверный формат данных о шагах", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(WeeklyReportActivity.this, "Ошибка при загрузке данных о шагах", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<StepsResponse> call, Throwable t) {
                Toast.makeText(WeeklyReportActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDistanceData(String userId) {
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<DistanceResponse> call = api.getWeeklyDistance(userId);
        call.enqueue(new Callback<DistanceResponse>() {
            @Override
            public void onResponse(Call<DistanceResponse> call, Response<DistanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Double> distanceData = response.body().getDistance();
                    if (distanceData != null && distanceData.size() == 7) {
                        // Рассчитаем среднее значение расстояния
                        double averageDistance = calculateAverage(distanceData);
                        averageDistanceTextView.setText(String.format("%.2f км", averageDistance));

                        // Настройка BarChart для расстояния
                        setupBarChart(distanceBarChart, convertToBarEntries(distanceData), "Расстояние", averageDistance, 10.0, ContextCompat.getColor(WeeklyReportActivity.this, R.color.yellow));
                    } else {
                        Toast.makeText(WeeklyReportActivity.this, "Неверный формат данных о расстоянии", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(WeeklyReportActivity.this, "Ошибка при загрузке данных о расстоянии", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DistanceResponse> call, Throwable t) {
                Toast.makeText(WeeklyReportActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private <T extends Number> List<BarEntry> convertToBarEntries(List<T> data) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new BarEntry(i, data.get(i).floatValue()));
        }
        return entries;
    }

    private <T extends Number> double calculateAverage(List<T> data) {
        double sum = 0;
        for (T value : data) {
            sum += value.doubleValue();
        }
        return sum / data.size();
    }

    private void setupBarChart(BarChart barChart, List<BarEntry> entries, String label, double averageValue, double goalValue, int color) {
        // Настройка данных
        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setColor(color); // Устанавливаем цвет графика
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // Настройка оси X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false); // Отключаем сетку на оси X
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"}));

        // Настройка оси Y
        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum((float) (goalValue * 1.2)); // Добавляем 20% отступа
        yAxis.setGranularity(1f);
        yAxis.setDrawGridLines(false); // Отключаем сетку на оси Y

        // Отключаем правую ось Y
        barChart.getAxisRight().setEnabled(false);

        // Добавляем пунктирные линии
        LimitLine averageLine = new LimitLine((float) averageValue, "Среднее");
        averageLine.setLineColor(Color.GRAY);
        averageLine.setLineWidth(1f);
        averageLine.enableDashedLine(10f, 10f, 0f); // Пунктирная линия
        yAxis.addLimitLine(averageLine);

        LimitLine goalLine = new LimitLine((float) goalValue, "Цель");
        goalLine.setLineColor(android.graphics.Color.RED);
        goalLine.setLineWidth(1f);
        goalLine.enableDashedLine(10f, 10f, 0f); // Пунктирная линия
        yAxis.addLimitLine(goalLine);

        // Обновление графика
        barChart.invalidate();
    }
}