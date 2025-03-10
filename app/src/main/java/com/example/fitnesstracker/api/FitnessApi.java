package com.example.fitnesstracker.api;

import com.example.fitnesstracker.models.Activity;
import com.example.fitnesstracker.models.ActivityRequest;
import com.example.fitnesstracker.models.ActivityResponse;
import com.example.fitnesstracker.models.DistanceResponse;
import com.example.fitnesstracker.models.LoginRequest;
import com.example.fitnesstracker.models.LoginResponse;
import com.example.fitnesstracker.models.RegisterRequest;
import com.example.fitnesstracker.models.RegisterResponse;
import com.example.fitnesstracker.models.StepsResponse;
import com.example.fitnesstracker.models.User;
import com.example.fitnesstracker.models.UserRequest;
import com.example.fitnesstracker.models.UserResponse;
import com.example.fitnesstracker.models.Workout;
import com.example.fitnesstracker.models.WorkoutResponse;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FitnessApi {

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("/workouts")
    Call<List<WorkoutResponse>> getWorkouts();

    @GET("auth/users")
    Call<List<User>> getAllUsers(); // метод для получения всех пользователей

    @GET("steps/{userId}")
    Call<StepsResponse> getSteps(@Path("userId") String userId);

    @GET("distance/{userId}")
    Call<DistanceResponse> getDistance(@Path("userId") String userId);

    @POST("steps/{userId}")
    Call<StepsResponse> updateSteps(
            @Path("userId") String userId,
            @Body Map<String, Integer> stepsData
    );

    @POST("distance/{userId}")
    Call<DistanceResponse> updateDistance(
            @Path("userId") String userId,
            @Body Map<String, Double> distanceData
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
    Call<UserResponse> getUser(@Path("userId") String userId);

    @DELETE("users/{userId}/activities/{activityId}")
    Call<ActivityResponse> deleteActivity(
            @Path("userId") String userId,
            @Path("activityId") String activityId
    );

    @GET("steps/{userId}")
    Call<StepsResponse> getWeeklySteps(@Path("userId") String userId);

    @GET("distance/{userId}")
    Call<DistanceResponse> getWeeklyDistance(@Path("userId") String userId);

    @PUT("users/{userId}/goals")
    Call<UserResponse> updateGoals(
            @Path("userId") String userId,
            @Body UserRequest request
    );

}