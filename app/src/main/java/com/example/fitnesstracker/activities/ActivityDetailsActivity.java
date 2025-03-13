package com.example.fitnesstracker.activities;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.Activity;
import com.example.fitnesstracker.models.ActivityResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityDetailsActivity extends AppCompatActivity {

    private Activity activity;
    private ImageView shareButton, menuButton;

    private TextView dateTextView, activityNameTextView, stepsTextView, durationTextView, distanceTextView, caloriesTextView, speedTextView;
    private LinearLayout backLayout;

    private static final int REQUEST_EDIT_ACTIVITY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_details);

        // Получаем данные активности из Intent
        activity = (Activity) getIntent().getSerializableExtra("activity");

        // Инициализация UI элементов
         dateTextView = findViewById(R.id.dateTextView);
         activityNameTextView = findViewById(R.id.activityNameTextView);
         stepsTextView = findViewById(R.id.stepsTextView);
         durationTextView = findViewById(R.id.durationTextView);
         distanceTextView = findViewById(R.id.distanceTextView);
         caloriesTextView = findViewById(R.id.caloriesTextView);
         speedTextView = findViewById(R.id.speedTextView);
        shareButton = findViewById(R.id.shareButton);
        menuButton = findViewById(R.id.menuButton);
        backLayout = findViewById(R.id.BackLayout);

        // Устанавливаем данные активности
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        dateTextView.setText(dateFormat.format(activity.getDate()));
        activityNameTextView.setText(activity.getAction());
        stepsTextView.setText(String.valueOf(activity.getSteps()));
        durationTextView.setText(String.format("%.2f мин", activity.getDuration()));
        distanceTextView.setText(String.format("%.2f км", activity.getDistance()));
        caloriesTextView.setText(String.valueOf(activity.getCalories()));
        speedTextView.setText(String.format("%.2f км/ч", calculateSpeed(activity.getDuration(), activity.getDistance())));

        // Обработка нажатия на кнопку "Поделиться"
        shareButton.setOnClickListener(v -> shareScreenshot());

        // Обработка нажатия на кнопку меню
        menuButton.setOnClickListener(v -> showMenu(v));

        backLayout.setOnClickListener(v -> finish());

    }

    // Метод для обновления UI
    // В ActivityDetailsActivity, в методе updateUI:
    private void updateUI() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        if (activity.getDate() != null) {
            dateTextView.setText(dateFormat.format(activity.getDate()));
        } else {
            dateTextView.setText(""); // Или какое-то другое значение по умолчанию
        }

        activityNameTextView.setText(activity.getAction());
        stepsTextView.setText(String.valueOf(activity.getSteps()));
        durationTextView.setText(String.format("%.2f мин", activity.getDuration()));
        distanceTextView.setText(String.format("%.2f км", activity.getDistance()));
        caloriesTextView.setText(String.valueOf(activity.getCalories()));
        speedTextView.setText(String.format("%.2f км/ч", calculateSpeed(activity.getDuration(), activity.getDistance())));
    }

    // Метод для отображения меню
    private void showMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.activity_menu, popupMenu.getMenu());

        // Обработка нажатий на пункты меню
        popupMenu.setOnMenuItemClickListener(item -> {

            int itemId = item.getItemId();

            if(itemId == R.id.menu_delete){
                deleteActivity();
            }else if(itemId == R.id.menu_edit){
                editActivity();
            }else if(itemId == R.id.menu_cancel){
                return true; // Просто закрываем мен
            }

            return false;
        });

        popupMenu.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Проверяем, что результат пришел от редактирования активности
        if (requestCode == REQUEST_EDIT_ACTIVITY && resultCode == RESULT_OK) {
            // Проверяем, что data не null
            if (data != null) {
                // Получаем обновленную активность из Intent
                Activity updatedActivity = (Activity) data.getSerializableExtra("activity");
                if (updatedActivity != null) {
                    // Обновляем текущую активность
                    activity = updatedActivity;

                    // Обновляем UI с новыми данными
                    updateUI();
                } else {
                    // Обрабатываем случай, когда updatedActivity == null
                    Toast.makeText(this, "Ошибка: Обновленная активность не была получена", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Обрабатываем случай, когда data == null
                Toast.makeText(this, "Ошибка: Данные не были возвращены", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteActivity() {
        SharedPreferences sharedPreferences = getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);
        String activityId = activity.getId();

        if (userId == null || activityId == null) {
            Toast.makeText(this, "Ошибка: Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<ActivityResponse> call = api.deleteActivity(userId, activityId);
        call.enqueue(new Callback<ActivityResponse>() {
            @Override
            public void onResponse(Call<ActivityResponse> call, Response<ActivityResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ActivityDetailsActivity.this, "Активность удалена", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Указываем, что активность удалена
                    finish(); // Закрываем ActivityDetailsActivity
                } else {
                    Toast.makeText(ActivityDetailsActivity.this, "Ошибка при удалении активности", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ActivityResponse> call, Throwable t) {
                Toast.makeText(ActivityDetailsActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editActivity() {
        Intent intent = new Intent(this, AddTrainingActivity.class);
        intent.putExtra("activity", activity); // Передаем данные активности
        intent.putExtra("isEditMode", true); // Указываем, что это режим редактирования
        intent.putExtra("activityId", activity.getId()); // Передаем ID активности
        startActivityForResult(intent, REQUEST_EDIT_ACTIVITY);
    }

    // Метод для расчета скорости
    private double calculateSpeed(double durationInMinutes, double distanceInKilometers) {
        if (durationInMinutes == 0) return 0; // Чтобы избежать деления на ноль
        return (distanceInKilometers * 60) / durationInMinutes; // Скорость в км/ч
    }

    // Метод для создания скриншота и его публикации
    private void shareScreenshot() {
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);

        // Создаем Bitmap с размерами view
        Bitmap bitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        rootView.draw(canvas);
        rootView.setBackgroundColor(Color.WHITE);

        if (bitmap == null) {
            Toast.makeText(this, "Не удалось создать скриншот", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "activity_progress_" + System.currentTimeMillis() + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        try {
            Uri uri = getContentResolver().insert(contentUri, values);
            if (uri == null) {
                throw new IOException("Failed to insert MediaStore record");
            }

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Failed to open output stream.");
                }
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Поделиться прогрессом"));

        } catch (IOException e) {
            Toast.makeText(this, "Не удалось сохранить скриншот", Toast.LENGTH_SHORT).show();
        }
    }
}