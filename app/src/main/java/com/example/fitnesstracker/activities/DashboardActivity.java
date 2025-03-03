package com.example.fitnesstracker.activities;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.fragments.PlanFragment;
import com.example.fitnesstracker.fragments.ProgressFragment;
import com.example.fitnesstracker.fragments.StartFragment;
import com.example.fitnesstracker.fragments.StepsFragment;
import com.example.fitnesstracker.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        checkPermission();

        // Инициализация BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Установка начального фрагмента (StartFragment)
        loadFragment(new StartFragment());

        // Установка начального выбранного элемента меню (Start)
        bottomNavigationView.setSelectedItemId(R.id.nav_start);

        // Обработка нажатий на элементы BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_plan) {
                selectedFragment = new PlanFragment();
            } else if (itemId == R.id.nav_progress) {
                selectedFragment = new ProgressFragment();
            } else if (itemId == R.id.nav_start) {
                selectedFragment = new StartFragment();
            } else if (itemId == R.id.nav_steps) {
                selectedFragment = new StepsFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            return true;
        });
    }

    // Метод для загрузки фрагмента
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
            }
        }
    }
}