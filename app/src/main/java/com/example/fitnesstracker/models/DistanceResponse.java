package com.example.fitnesstracker.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DistanceResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("distance") // Убедитесь, что имя поля совпадает с JSON
    private List<Double> distance;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public List<Double> getDistance() {
        return distance;
    }

    public String getMessage() {
        return message;
    }
}