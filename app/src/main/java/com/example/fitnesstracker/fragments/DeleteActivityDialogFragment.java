package com.example.fitnesstracker.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.fitnesstracker.R;

public class DeleteActivityDialogFragment extends DialogFragment {

    private int position; // Позиция элемента для удаления
    private DeleteListener deleteListener; // Колбэк для удаления
    private CancelListener cancelListener; // Колбэк для отмены удаления


    // Интерфейс для обработки удаления
    public interface DeleteListener {
        void onDeleteConfirmed(int position);
    }

    // Интерфейс для обработки отмены удаления
    public interface CancelListener {
        void onDeleteCanceled(int position);
    }

    // Метод для создания нового экземпляра диалога
    public static DeleteActivityDialogFragment newInstance(int position, DeleteListener deleteListener, CancelListener cancelListener) {
        DeleteActivityDialogFragment fragment = new DeleteActivityDialogFragment();
        fragment.position = position;
        fragment.deleteListener = deleteListener;
        fragment.cancelListener = cancelListener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Создаем кастомный макет для диалога
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_delete_custom, null);

        // Находим кнопки в макете
        Button btnYes = view.findViewById(R.id.btnYes);
        Button btnNo = view.findViewById(R.id.btnNo);

        // Создаем AlertDialog с кастомным макетом
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        // Создаем диалог
        AlertDialog dialog = builder.create();

        // Устанавливаем прозрачный фон для диалога
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Настройка кнопки "Да"
        btnYes.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteConfirmed(position); // Вызываем колбэк для удаления
            }
            dismiss(); // Закрываем диалог
        });

        // Настройка кнопки "Нет"
        btnNo.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onDeleteCanceled(position); // Вызываем колбэк для удаления
            }
            dismiss(); // Закрываем диалог
        });// Закрываем диалог

        // Возвращаем диалог
        return dialog;
    }
}