package com.example.fitnesstracker.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.fitnesstracker.R;

public class PlanFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Загрузка макета для фрагмента
        return inflater.inflate(R.layout.fragment_plan, container, false);
    }
}