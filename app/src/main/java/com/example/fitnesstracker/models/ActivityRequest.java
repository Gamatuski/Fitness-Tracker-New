package com.example.fitnesstracker.models;

public class ActivityRequest {
    private String id;
    private String action;
    private double distance;
    private int calories;
    private int steps;
    private double duration;
    private String date;

    public ActivityRequest(String action, double distance, int calories, int steps, double duration, String date) {
        this.action = action;
        this.distance = distance;
        this.calories = calories;
        this.steps = steps;
        this.duration = duration;
        this.date = date;
    }

    // Геттеры и сеттеры
}