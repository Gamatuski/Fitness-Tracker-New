package com.example.fitnesstracker.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
import com.example.fitnesstracker.viewmodels.PlanViewModel;

import java.util.List;

public class PlanFragment extends Fragment {

    private RecyclerView workoutRecyclerView;
    private WorkoutAdapter workoutAdapter;
    private PlanViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plan, container, false);

        workoutRecyclerView = view.findViewById(R.id.workoutRecyclerView);
        workoutRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getActivity().getApplication())).get(PlanViewModel.class);

        viewModel.getWorkouts().observe(getViewLifecycleOwner(), workouts -> {
            if (workouts != null) {
                workoutAdapter = new WorkoutAdapter(workouts);
                workoutRecyclerView.setAdapter(workoutAdapter);
            } else {
                Toast.makeText(requireContext(), "Не удалось загрузить данные.", Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }
}