package com.example.fitnesstracker.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.fitnesstracker.R;

public class SensorPermissionActivity extends AppCompatActivity {
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1002;
    Button sensorPermissionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_fitness_permision);

        sensorPermissionButton = findViewById(R.id.SensorPermissionButton);

        sensorPermissionButton.setOnClickListener(v -> {
            requestActivityRecognitionPermission();
        });
    }

    private void requestActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    ACTIVITY_RECOGNITION_REQUEST_CODE);
        } else {
            Intent intent = new Intent(SensorPermissionActivity.this, MessagesPermissionActivity.class);
            startActivity(intent);

            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(SensorPermissionActivity.this, MessagesPermissionActivity.class);
                startActivity(intent);

                finish();
            } else {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            }
        }
    }
}