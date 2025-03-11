package com.example.fitnesstracker.utils;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

public class DistanceValueFormatter extends ValueFormatter {
    private final DecimalFormat mFormat;

    public DistanceValueFormatter() {
        mFormat = new DecimalFormat("0.00"); // Формат с двумя знаками после запятой
    }

    @Override
    public String getFormattedValue(float value) {
        return mFormat.format(value);
    }
}