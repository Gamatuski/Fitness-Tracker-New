package com.example.fitnesstracker.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.viewmodel.CreationExtras;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.DistanceResponse;
import com.example.fitnesstracker.models.StepsResponse;
import com.example.fitnesstracker.utils.StyleTitleText;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StepsFragment extends Fragment implements OnChartValueSelectedListener {
    private TextView titleTextView;
    private ProgressBar progressCircle;
    private TextView stepsCountTextView, todayTextView;
    private BarChart barChart;
    private List<Integer> stepsDataList = new ArrayList<>(); // Список для хранения данных о шагах из БД (для графика)
    private int goalSteps = 5000; // Цель шагов

    private LinearLayout stepsLayout, distanceLayout;
    private ImageView stepsIcon, distanceIcon;
    private View stepsUnderline, distanceUnderline;
    private boolean isShowingSteps = true; // Флаг для отслеживания текущего режима (шаги или расстояние)

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_steps, container, false);

        // Инициализация элементов UI
        progressCircle = view.findViewById(R.id.progressCircle);
        stepsCountTextView = view.findViewById(R.id.stepsCountTextView);
        barChart = view.findViewById(R.id.barChart);
        titleTextView = view.findViewById(R.id.titleTextView);
        todayTextView = view.findViewById(R.id.todayTextView);

        // Инициализация иконок и подчеркивания
        stepsLayout = view.findViewById(R.id.stepsLayout);
        distanceLayout = view.findViewById(R.id.distanceLayout);
        stepsIcon = view.findViewById(R.id.stepsIcon);
        distanceIcon = view.findViewById(R.id.distanceIcon);
        stepsUnderline = view.findViewById(R.id.stepsUnderline);
        distanceUnderline = view.findViewById(R.id.distanceUnderline);

        // Обработка нажатия на иконку шагов
        stepsLayout.setOnClickListener(v -> {
            if (!isShowingSteps) {
                switchToStepsMode();
            }
        });

        // Обработка нажатия на иконку расстояния
        distanceLayout.setOnClickListener(v -> {
            if (isShowingSteps) {
                switchToDistanceMode();
            }
        });

        // Стилизация заголовка
        StyleTitleText styleTitleText = new StyleTitleText();
        styleTitleText.styleTitleText(titleTextView);

        // Настройка графика
        setupBarChart();

        // Установка слушателя нажатий на график
        barChart.setOnChartValueSelectedListener(this);

        return view;
    }

    // Переключение в режим отображения шагов
    private void switchToStepsMode() {
        isShowingSteps = true;

        // Изменение цвета иконок и подчеркивания
        stepsIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purpule));
        distanceIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow));
        stepsUnderline.setVisibility(View.VISIBLE);
        distanceUnderline.setVisibility(View.INVISIBLE);

        // Обновление данных (шаги)
        loadStepsDataFromDatabase();
    }

    // Переключение в режим отображения расстояния
    private void switchToDistanceMode() {
        isShowingSteps = false;

        // Изменение цвета иконок и подчеркивания
        stepsIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purpule));
        distanceIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow));
        stepsUnderline.setVisibility(View.INVISIBLE);
        distanceUnderline.setVisibility(View.VISIBLE);

        // Обновление данных (расстояние)
        loadDistanceDataFromDatabase();
    }


    private void loadStepsDataFromDatabase() {
        // Получение userId из SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        // Проверка наличия userId
        if (userId == null) {
            Log.e("StepsFragment", "userId не найден в SharedPreferences. Пользователь не залогинен?");
            showError("Ошибка: Не удалось получить ID пользователя. Пожалуйста, войдите в систему.");
            return; // Выход из метода, если userId отсутствует
        }

        // Выполнение запроса к API для получения данных о шагах
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<StepsResponse> call = api.getSteps(userId);
        call.enqueue(new Callback<StepsResponse>() {
            @Override
            public void onResponse(Call<StepsResponse> call, Response<StepsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StepsResponse stepsResponse = response.body();
                    if (stepsResponse.isSuccess()) {
                        stepsDataList = stepsResponse.getSteps();
                        if (stepsDataList != null && stepsDataList.size() == 7) {
                            updateBarChart(stepsDataList, true); // Передаём true для шагов
                            updateUIForDay(stepsDataList.get(getCurrentDayIndex()));
                        } else {
                            showError("Неверный формат данных о шагах с сервера.");
                        }
                    } else {
                        showError(stepsResponse.getMessage() != null ? stepsResponse.getMessage() : "Ошибка при получении данных о шагах.");
                    }
                } else {
                    String message = "Ошибка сервера при загрузке данных о шагах.";
                    if (response.errorBody() != null) {
                        try {
                            message += " " + response.errorBody().string();
                            Log.e("StepsFragment", "Error Body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e("StepsFragment", "Ошибка при чтении тела ошибки", e);
                        }
                    }
                    showError(message);
                }
            }

            @Override
            public void onFailure(Call<StepsResponse> call, Throwable t) {
                showError("Ошибка сети при загрузке данных о шагах: " + t.getMessage());
                Log.e("StepsFragment", "Network Error: " + t.getMessage());
            }
        });
    }

    private void loadDistanceDataFromDatabase() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Log.e("StepsFragment", "userId не найден в SharedPreferences. Пользователь не залогинен?");
            showError("Ошибка: Не удалось получить ID пользователя. Пожалуйста, войдите в систему.");
            return;
        }

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<DistanceResponse> call = api.getDistance(userId);
        call.enqueue(new Callback<DistanceResponse>() {
            @Override
            public void onResponse(Call<DistanceResponse> call, Response<DistanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DistanceResponse distanceResponse = response.body();
                    if (distanceResponse.isSuccess()) {
                        List<Double> distanceData = distanceResponse.getDistance();
                        if (distanceData != null && distanceData.size() == 7) {
                            updateBarChart(convertDistanceToInt(distanceData), false); // Передаём false для расстояния
                            updateUIForDistance(distanceData.get(getCurrentDayIndex()));
                        } else {
                            showError("Неверный формат данных о расстоянии с сервера.");
                        }
                    } else {
                        showError(distanceResponse.getMessage() != null ? distanceResponse.getMessage() : "Ошибка при получении данных о расстоянии.");
                    }
                } else {
                    String message = "Ошибка сервера при загрузке данных о расстоянии.";
                    if (response.errorBody() != null) {
                        try {
                            message += " " + response.errorBody().string();
                            Log.e("StepsFragment", "Error Body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e("StepsFragment", "Ошибка при чтении тела ошибки", e);
                        }
                    }
                    showError(message);
                }
            }


            @Override
            public void onFailure(Call<DistanceResponse> call, Throwable t) {
                showError("Ошибка сети при загрузке данных о расстоянии: " + t.getMessage());
                Log.e("StepsFragment", "Network Error: " + t.getMessage());
            }
        });
    }

    // Преобразование List<Double> в List<Integer> для BarChart
    private List<Integer> convertDistanceToInt(List<Double> distanceData) {
        List<Integer> intData = new ArrayList<>();
        for (Double d : distanceData) {
            intData.add(d.intValue());
        }
        return intData;
    }

    // Обновление UI для отображения расстояния
    private void updateUIForDistance(double distance) {
        stepsCountTextView.setText(String.format("%.2f км", distance)); // Отображение расстояния
        int progress = (int) ((distance * 100f) / 10); // Пример: цель - 10 км
        progressCircle.setProgress(progress); // Обновление прогресс-бара
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStepsDataFromDatabase(); // Загрузка данных о шагах при каждом возобновлении фрагмента
    }


    // Настройка столбчатой диаграммы (оси, легенда, описание)
    private void setupBarChart() {
        barChart.getDescription().setEnabled(false); // Отключение описания
        barChart.setDrawGridBackground(false); // Отключение фоновой сетки (если есть фон)

        // Настройка оси X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false); // Отключение вертикальных линий сетки оси X
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7); // 7 дней
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getDaysOfWeek())); // Подписи дней недели

        // Настройка оси Y
        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setAxisMinimum(0f); // Минимальное значение
        yAxis.setAxisMaximum(10000f); // Максимальное значение (можно изменить)
        yAxis.setGranularity(1000f); // Шаг оси Y
        yAxis.setDrawLabels(false); // Отключение подписей оси Y (справа)
        yAxis.setDrawGridLines(false); // **Добавлено: Отключение горизонтальных линий сетки оси Y**
        barChart.getAxisRight().setEnabled(false); // Отключение правой оси Y

        // Настройка легенды
        barChart.getLegend().setEnabled(false); // Отключение легенды
    }

    // Обновление данных столбчатой диаграммы
    private void updateBarChart(List<Integer> data, boolean isSteps) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new BarEntry(i, data.get(i)));
        }

        BarDataSet dataSet = new BarDataSet(entries, isSteps ? "Шаги" : "Расстояние");
        dataSet.setColor(ContextCompat.getColor(requireContext(), isSteps ? R.color.purpule : R.color.yellow));
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);
        barChart.invalidate();
    }

    // Получение массива подписей дней недели для оси X графика
    private String[] getDaysOfWeek() {
        return new String[]{"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
    }

    // Обработка выбора столбца на графике
    @Override
    public void onValueSelected(Entry e, Highlight h) {
        int index = (int) e.getX();
        if (stepsDataList != null && index < stepsDataList.size()) {
            updateUIForDay(stepsDataList.get(index), index);
        } else {
            Log.e("StepsFragment", "Индекс выбранного столбца выходит за пределы данных или данные не загружены.");
            showError("Ошибка при выборе данных на графике.");
        }
    }

    // Обновление UI для отображения шагов и прогресса для выбранного дня
    private void updateUIForDay(int steps, int dayIndex) {
        stepsCountTextView.setText(String.valueOf(steps)); // Обновление текста текущих шагов
        int progress = (int) ((steps * 100f) / goalSteps); // Вычисление прогресса в процентах
        progressCircle.setProgress(progress); // Обновление прогресс-бара
        String[] daysOfWeek = getDaysOfWeek();
        todayTextView.setText(daysOfWeek[dayIndex]); // Обновление TextView с выбранным днем
    }

    // Перегрузка метода для обновления UI для текущего дня (без индекса, используется при загрузке данных)
    private void updateUIForDay(int steps) {
        updateUIForDay(steps, getCurrentDayIndex());
    }


    @Override
    public void onNothingSelected() {
        // Метод не используется
    }

    // Отображение Toast сообщения об ошибке
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @NonNull
    @Override
    public CreationExtras getDefaultViewModelCreationExtras() {
        return super.getDefaultViewModelCreationExtras();
    }

    private int getCurrentDayIndex() {
        int dayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2; // Понедельник - 0, Воскресенье - 6
        if (dayIndex == -1) {
            dayIndex = 6; // Воскресенье
        }
        return dayIndex;
    }
}