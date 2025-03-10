
package com.example.fitnesstracker.fragments;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.adapters.WorkoutAdapter;
import com.example.fitnesstracker.api.FitnessApi;
import com.example.fitnesstracker.api.RetrofitClient;
import com.example.fitnesstracker.models.Workout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlanFragment extends Fragment {

    private RecyclerView workoutRecyclerView;
    private WorkoutAdapter workoutAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plan, container, false);

        workoutRecyclerView = view.findViewById(R.id.workoutRecyclerView);
        workoutRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Загрузка данных из базы данных
        loadWorkouts();

        return view;
    }

    private void loadWorkouts() {
        FitnessApi api = RetrofitClient.getClient().create(FitnessApi.class);
        Call<List<Workout>> call = api.getWorkouts();

        call.enqueue(new Callback<List<Workout>>() {
            @Override
            public void onResponse(Call<List<Workout>> call, Response<List<Workout>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Workout> workouts = response.body();
                    workoutAdapter = new WorkoutAdapter(workouts);
                    workoutRecyclerView.setAdapter(workoutAdapter);
                } else {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String responseBody = null;
                    try {
                        responseBody = response.errorBody().string();
                        Log.e("API Error", "Code: " + response.code() + ", Message: " + response.message() + ", Body: " + responseBody);
                    } catch (IOException e) {
                        Log.e("API Error", "Code: " + response.code() + ", Message: " + response.message() + ", Body: Could not parse error body");
                    }
                    Toast.makeText(requireContext(), "Не удалось загрузить данные. Ошибка: " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Workout>> call, Throwable t) {
                Log.e("API Failure", "Error: " + t.getMessage(), t);
                Toast.makeText(requireContext(), "Не удалось загрузить данные. Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
