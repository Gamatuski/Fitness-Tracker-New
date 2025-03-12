package com.example.fitnesstracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.adapters.ActionsAdapter;

import java.util.Arrays;
import java.util.List;

public class SelectActionActivity extends AppCompatActivity {

    private ActionsAdapter adapter;
    private List<String> actions = Arrays.asList("Бег", "Прогулка", "Северная ходьба", "Езда на велосипеде");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_action);

        // Получаем выбранную активность из Intent
        String initialSelectedAction = getIntent().getStringExtra("selectedAction");

        RecyclerView actionsRecyclerView = findViewById(R.id.actionsRecyclerView);
        actionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Передаем начальную выбранную активность в адаптер
        adapter = new ActionsAdapter(actions);
        actionsRecyclerView.setAdapter(adapter);

        Button doneButton = findViewById(R.id.doneButton);
        doneButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedAction", adapter.getSelectedAction());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}