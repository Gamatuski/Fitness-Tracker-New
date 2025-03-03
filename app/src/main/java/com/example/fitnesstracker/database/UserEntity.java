package com.example.fitnesstracker.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String email;
    public String password;
    public int height;
    public int weight;
}