package com.example.fitnesstracker.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.fitnesstracker.R;
import com.example.fitnesstracker.activities.LoginActivity;

public class LogoutDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Создаем кастомный макет для диалога
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_custom, null);

        // Находим кнопки в макете
        Button btnYes = view.findViewById(R.id.btnYes);
        Button btnNo = view.findViewById(R.id.btnNo);

        // Создаем AlertDialog с кастомным макетом
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        // Настройка кнопки "Да"
        btnYes.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish(); // Закрываем текущую активность
            dismiss(); // Закрываем диалог
        });

        // Настройка кнопки "Нет"
        btnNo.setOnClickListener(v -> dismiss()); // Закрываем диалог

        // Создаем и возвращаем диалог
        return builder.create();
    }
}