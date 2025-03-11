package com.example.fitnesstracker.utils;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;

// Custom ValueFormatter to draw rounded bars
public class RoundedBarValueFormatter extends ValueFormatter {
    private final float mRadius = 15f; // Adjust for corner radius
    private final Paint mPaint;
    private final BarChart mChart;

    public RoundedBarValueFormatter(BarChart chart) {
        mChart = chart;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    public String getFormattedValue(float value) {
        return String.valueOf(value); // Return the actual value as a string
    }

    // This is where the magic happens.  We draw the rounded rectangle *before* the default bar.
    public void drawValue(Canvas c, IValueFormatter formatter, float value, BarEntry entry, int dataSetIndex, float x, float y, int color) {
        BarData barData = mChart.getBarData();
        BarDataSet dataSet = (BarDataSet) barData.getDataSetByIndex(dataSetIndex);
        float barWidth = barData.getBarWidth();

        float left = x - barWidth / 2f;
        float right = x + barWidth / 2f;
        float top = y;
        float bottom = mChart.getHeight();

        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);

        RectF rectF = new RectF(left, top, right, bottom);
        c.drawRoundRect(rectF, mRadius, mRadius, mPaint);
    }
}