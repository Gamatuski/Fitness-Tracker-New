package com.example.fitnesstracker.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

import com.example.fitnesstracker.R;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Обработка нажатия на "Еженедельный отчёт"
        view.findViewById(R.id.weeklyReportLayout).setOnClickListener(v -> {
            // Открываем новую активность для отчёта
            Intent intent = new Intent(getActivity(), WeeklyReportActivity.class);
            startActivity(intent);
        });

        // Обработка нажатия на "Google Fit"
        view.findViewById(R.id.googleFitLayout).setOnClickListener(v -> {
            // Открываем Google Fit
            openGoogleFit();
        });

        // Обработка нажатия на "Изменить цели"
        view.findViewById(R.id.editGoalsLayout).setOnClickListener(v -> {
            // Открываем новую активность для отчёта
            Intent intent = new Intent(getActivity(), EditGoalsActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void openGoogleFit() {
        try {
            // Пытаемся открыть Google Fit
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage("com.google.android.apps.fitness");
            startActivity(intent);
        } catch (Exception e) {
            // Если Google Fit не установлен, открываем страницу в Play Store
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=com.google.android.apps.fitness"));
            startActivity(intent);
        }
    }
}