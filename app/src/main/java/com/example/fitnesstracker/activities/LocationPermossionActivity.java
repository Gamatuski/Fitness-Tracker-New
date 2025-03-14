package com.example.fitnesstracker.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.fitnesstracker.R;

public class LocationPermossionActivity extends AppCompatActivity {
    private static final String TAG = "LocationPermission";
    Button locactionPermissionButton;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_location_permition);

        locactionPermissionButton = findViewById(R.id.locactionPermissionButton);

        locactionPermissionButton.setOnClickListener(v -> {
            Log.d(TAG, "Button clicked");
            requestLocationPermission();
        });
    }

    private void requestLocationPermission() {
        Log.d(TAG, "Requesting permission");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Permission not granted, requesting");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Permission already granted");
            // Add handling for already granted permission
            Intent intent = new Intent(LocationPermossionActivity.this, SensorPermissionActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(LocationPermossionActivity.this, SensorPermissionActivity.class);
                startActivity(intent);

                finish();
            } else {

                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }


}
