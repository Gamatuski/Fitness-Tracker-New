package com.example.fitnesstracker.models;

public class Workout {
    private String action;
    private int duration;
    private String difficulty;
    private int workoutsPerWeek;
    private WorkoutImage image;

    public static class WorkoutImage {
        private String imageUrl;
        private String contentType;

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getWorkoutsPerWeek() { return workoutsPerWeek; }
    public void setWorkoutsPerWeek(int workoutsPerWeek) { this.workoutsPerWeek = workoutsPerWeek; }

    public WorkoutImage getImage() { return image; }
    public void setImage(WorkoutImage image) { this.image = image; }
}