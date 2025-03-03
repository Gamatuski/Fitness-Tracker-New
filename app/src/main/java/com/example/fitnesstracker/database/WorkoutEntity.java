package com.example.fitnesstracker.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workouts")
public class WorkoutEntity {
    @PrimaryKey
    public int id;

    public int userId;
    public String date;
    public String notes;
}