package com.example.fitnesstracker.fragments;

import static androidx.room.jarjarred.org.antlr.v4.runtime.misc.MurmurHash.finish;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.adapters.DaysAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PreferredDaysActivity extends AppCompatActivity {
    private DaysAdapter adapter;
    private List<String> days = Arrays.asList("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferred_days);

        // Получаем выбранные дни из Intent
        List<String> initialSelectedDays = getIntent().getStringArrayListExtra("selectedDays");

        RecyclerView daysRecyclerView = findViewById(R.id.daysRecyclerView);
        daysRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Передаем начальные выбранные дни в адаптер
        adapter = new DaysAdapter(days);
        if (initialSelectedDays != null) {
            adapter.setSelectedDays(initialSelectedDays);
        }
        daysRecyclerView.setAdapter(adapter);

        Button doneButton = findViewById(R.id.doneButton);
        doneButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra("selectedDays", new ArrayList<>(adapter.getSelectedDays()));
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

}