package com.example.fitnesstracker.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WorkoutDao {
    @Query("SELECT * FROM workouts")
    List<WorkoutEntity> getAll();

    @Insert
    void insert(WorkoutEntity workout);
}