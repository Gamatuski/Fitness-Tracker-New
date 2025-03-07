package com.example.fitnesstracker.utils;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

public class StyleTitleText {
    public void styleTitleText(TextView titleTextView) {
        String title = "Fitness Tracker";
        SpannableString spannableString = new SpannableString(title);

        // Зелёный цвет для буквы "F"
        spannableString.setSpan(
                new ForegroundColorSpan(Color.MAGENTA), // Зелёный цвет
                0, // Начальная позиция (F)
                1, // Конечная позиция (F)
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Зелёный цвет для буквы "T"
        spannableString.setSpan(
                new ForegroundColorSpan(Color.MAGENTA), // Зелёный цвет
                8, // Начальная позиция (T)
                9, // Конечная позиция (T)
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Установка стилизованного текста
        titleTextView.setText(spannableString);
    }
}
