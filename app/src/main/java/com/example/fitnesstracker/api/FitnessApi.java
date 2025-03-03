package com.example.fitnesstracker.api;

import com.example.fitnesstracker.models.LoginRequest;
import com.example.fitnesstracker.models.LoginResponse;
import com.example.fitnesstracker.models.RegisterRequest;
import com.example.fitnesstracker.models.RegisterResponse;
import com.example.fitnesstracker.models.User;
import com.example.fitnesstracker.models.Workout;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface FitnessApi {

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("workouts")
    Call<List<Workout>> getWorkouts(@Header("Authorization") String token);

    @GET("auth/users")
    Call<List<User>> getAllUsers(); // метод для получения всех пользователей

}