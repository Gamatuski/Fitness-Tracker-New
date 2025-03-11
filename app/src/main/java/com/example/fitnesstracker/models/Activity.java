package com.example.fitnesstracker.models;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Activity {
    @SerializedName("_id")
    private String id;
    private String action;
    private double distance;
    private int calories;
    private int steps;
    private double duration;
    private Date date;

    // Геттеры и сеттеры

    public String getId() { // Геттер для ID
        return id;
    }

    public void setId(String id) { // Сеттер для ID
        this.id = id;
    }


    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}