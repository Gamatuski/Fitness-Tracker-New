package com.example.fitnesstracker.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class User {
    @SerializedName("_id") // Указываем соответствие JSON полю "_id"
    private String id; // Поле для хранения ID пользователя

    private String email;
    private String password; // Внимание: пароль не должен передаваться обратно с сервера в production
    private int height;
    private int weight;
    private List<Double> distance;
    private int stepsGoal; // Цель шагов
    private int distanceGoal; // Цель расстояния


    // Конструктор по умолчанию (необходим для Gson)
    public User() {
    }


    // Конструктор
    public User(String email, String password, int height, int weight, int stepsGoal, int distanceGoal) {
        this.email = email;
        this.password = password;
        this.height = height;
        this.weight = weight;
        this.stepsGoal = stepsGoal;
        this.distanceGoal = distanceGoal;
    }

    // Геттеры и сеттеры

    public String getId() { // Геттер для ID
        return id;
    }

    public void setId(String id) { // Сеттер для ID
        this.id = id;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getStepsGoal() {
        return stepsGoal;
    }

    public void setStepsGoal(int stepsGoal) {
        this.stepsGoal = stepsGoal;
    }

    public int getDistanceGoal() {
        return distanceGoal;
    }

    public void setDistanceGoal(int distanceGoal) {
        this.distanceGoal = distanceGoal;
    }


}