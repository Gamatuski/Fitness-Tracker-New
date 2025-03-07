package com.example.fitnesstracker.models;

public class ActivityData {
    private String activityName;
    private double MET;

    public ActivityData(String activityName, double MET) {
        this.activityName = activityName;
        this.MET = MET;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public double getMET() {
        return MET;
    }

    public void setMET(double MET) {
        this.MET = MET;
    }
}