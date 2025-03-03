package com.example.fitnesstracker.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {WorkoutEntity.class, UserEntity.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract WorkoutDao workoutDao(); // Метод для доступа к WorkoutDao
    public abstract UserDao userDao(); // Метод для доступа к UserDao

    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "fitness_database")
                            .fallbackToDestructiveMigration() // Удаляет старую базу данных при обновлении версии
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}