package com.example.fitnesstracker.models;

import java.util.List;

public class WorkoutResponse {
    private String difficulty;
    private int workoutsPerWeek;
    private String _id;
    private List<Workout> workouts;

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getWorkoutsPerWeek() {
        return workoutsPerWeek;
    }

    public void setWorkoutsPerWeek(int workoutsPerWeek) {
        this.workoutsPerWeek = workoutsPerWeek;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public List<Workout> getWorkouts() {
        return workouts;
    }

    public void setWorkouts(List<Workout> workouts) {
        this.workouts = workouts;
    }
}