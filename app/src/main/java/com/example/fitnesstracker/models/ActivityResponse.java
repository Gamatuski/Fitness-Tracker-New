package com.example.fitnesstracker.models;

public class ActivityResponse {
    private boolean success;
    private String message;
    private Activity activity;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Activity getActivity() {
        return activity;
    }
}