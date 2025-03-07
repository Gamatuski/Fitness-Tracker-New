package com.example.fitnesstracker.api;

import com.example.fitnesstracker.models.Activity;
import com.example.fitnesstracker.models.ActivityRequest;
import com.example.fitnesstracker.models.DistanceResponse;
import com.example.fitnesstracker.models.LoginRequest;
import com.example.fitnesstracker.models.LoginResponse;
import com.example.fitnesstracker.models.RegisterRequest;
import com.example.fitnesstracker.models.RegisterResponse;
import com.example.fitnesstracker.models.StepsResponse;
import com.example.fitnesstracker.models.User;
import com.example.fitnesstracker.models.Workout;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FitnessApi {

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("workouts")
    Call<List<Workout>> getWorkouts(@Header("Authorization") String token);

    @GET("auth/users")
    Call<List<User>> getAllUsers(); // метод для получения всех пользователей

    @GET("steps/{userId}")
    Call<StepsResponse> getSteps(@Path("userId") String userId);

    @GET("distance/{userId}")
    Call<DistanceResponse> getDistance(@Path("userId") String userId);

    @POST("steps/{userId}") // Используем POST для обновления шагов
    Call<StepsResponse> updateSteps(
            @Path("userId") String userId,
            @Query("dayIndex") int dayIndex,
            @Query("steps") int steps // Отправляем только одно значение шагов для обновления в нужный день
    );

    @POST("users/{userId}/activities")
    Call<ResponseBody> addActivity(
            @Path("userId") String userId,
            @Body ActivityRequest activityRequest
    );

    @GET("users/{userId}/activities")
    Call<List<Activity>> getActivities(@Path("userId") String userId);

    @GET("activities/{action}")
    Call<Activity> getActivityByName(@Path("activityName") String activityName);

    @GET("users/{userId}")
    Call<User> getUser(@Path("userId") String userId);

}