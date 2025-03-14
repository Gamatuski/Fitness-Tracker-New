package com.example.fitnesstracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitnesstracker.R;

public class WelcomeActivity extends AppCompatActivity {
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
            startActivity(intent);

            finish();
        });
    }
}
