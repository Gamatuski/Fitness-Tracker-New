package com.example.fitnesstracker.viewmodels;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.Workout;
import com.example.fitnesstracker.models.WorkoutResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlanViewModel extends AndroidViewModel {

    private MutableLiveData<List<Workout>> workouts = new MutableLiveData<>();
    private Application application;

    public PlanViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        loadWorkouts();
    }

    public LiveData<List<Workout>> getWorkouts() {
        return workouts;
    }

    private void loadWorkouts() {
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<List<WorkoutResponse>> call = api.getWorkouts();

        call.enqueue(new Callback<List<WorkoutResponse>>() {
            @Override
            public void onResponse(Call<List<WorkoutResponse>> call, Response<List<WorkoutResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WorkoutResponse> workoutResponses = response.body();
                    List<Workout> allWorkouts = new ArrayList<>();
                    for (WorkoutResponse workoutResponse : workoutResponses) {
                        if (workoutResponse != null && workoutResponse.getWorkouts() != null) {
                            allWorkouts.addAll(workoutResponse.getWorkouts());
                        }
                    }
                    workouts.setValue(allWorkouts);
                    preloadImages(allWorkouts); // Предварительная загрузка изображений
                } else {
                    Log.e("PlanViewModel", "Failed to load workouts: " + response.message());
                    workouts.setValue(null); // Или пустой список, в зависимости от логики
                }
            }

            @Override
            public void onFailure(Call<List<WorkoutResponse>> call, Throwable t) {
                Log.e("PlanViewModel", "Error loading workouts: " + t.getMessage());
                workouts.setValue(null); // Или пустой список, в зависимости от логики
            }
        });
    }

    private void preloadImages(List<Workout> workoutList) {
        ExecutorService executor = Executors.newFixedThreadPool(5); // Настройте количество потоков по необходимости

        for (Workout workout : workoutList) {
            if (workout.getImage() != null && workout.getImage().getImageUrl() != null) {
                String imageUrl = workout.getImage().getImageUrl();
                executor.execute(() -> {
                    try {
                        FutureTarget<Bitmap> futureTarget = Glide.with(application)
                                .asBitmap()
                                .load(imageUrl)
                                .submit();
                        Bitmap bitmap = futureTarget.get(); // Блокирует поток, пока изображение не загрузится
                        Glide.with(application).clear(futureTarget); // Очистка ресурсов
                        Log.d("PlanViewModel", "Preloaded image: " + imageUrl);
                    } catch (Exception e) {
                        Log.e("PlanViewModel", "Failed to preload image: " + imageUrl, e);
                    }
                });
            }
        }

        executor.shutdown();
    }
}