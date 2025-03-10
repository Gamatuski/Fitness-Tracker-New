package com.example.fitnesstracker.models;

public class UserRequest {
    private int stepsGoal;
    private int distanceGoal;

    public UserRequest(int stepsGoal, int distanceGoal) {
        this.stepsGoal = stepsGoal;
        this.distanceGoal = distanceGoal;
    }

    public int getStepsGoal() {
        return stepsGoal;
    }

    public int getDistanceGoal() {
        return distanceGoal;
    }
}