package com.example.fitnesstracker.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fitnesstracker.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TimePickerBottomSheet extends BottomSheetDialogFragment {

    private OnTimeSelectedListener listener;

    // Интерфейс для передачи выбранного времени
    public interface OnTimeSelectedListener {
        void onTimeSelected(String selectedTime);
    }

    // Установка слушателя
    public void setOnTimeSelectedListener(OnTimeSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_scrolling_time_picker, null);
        dialog.setContentView(view);

        // Инициализация NumberPicker для часов и минут
        NumberPicker numPickerHour = view.findViewById(R.id.numPickerHour);
        NumberPicker numPickerMinute = view.findViewById(R.id.numPickerMin);

        // Установка минимальных и максимальных значений для NumberPicker
        numPickerHour.setMinValue(0);
        numPickerHour.setMaxValue(23);
        numPickerHour.setValue(18); // Начальное значение часов

        numPickerMinute.setMinValue(0);
        numPickerMinute.setMaxValue(59);
        numPickerMinute.setValue(0); // Начальное значение минут

        // Обработка нажатия на кнопку "Готово"
        Button btnDone = view.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> {
            // Получаем выбранные значения
            int hour = numPickerHour.getValue();
            int minute = numPickerMinute.getValue();

            // Форматируем время в строку (HH:mm)
            String selectedTime = String.format("%02d:%02d", hour, minute);

            // Передаем выбранное время через интерфейс
            if (listener != null) {
                listener.onTimeSelected(selectedTime);
            }

            // Закрываем BottomSheetDialog
            dismiss();
        });


        return dialog;
    }
}
