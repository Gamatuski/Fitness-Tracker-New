package com.example.fitnesstracker.models;

public class RegisterRequest {
    private String email;
    private String password;
    private int height;
    private int weight;

    public RegisterRequest(String email, String password, int height, int weight) {
        this.email = email;
        this.password = password;
        this.height = height;
        this.weight = weight;
    }

    // Геттеры и сеттеры

    public String getEmail(){
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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}