package com.example.fitnesstracker.adapters;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Vibrator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.fragments.ProgressFragment;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final ProgressFragment.ActivitiesAdapter adapter;
    private final ColorDrawable background;
    private final Vibrator vibrator;
    private boolean isSwipedBeyondThreshold = false;

    public SwipeToDeleteCallback(ProgressFragment.ActivitiesAdapter adapter, Vibrator vibrator) {
        super(0, ItemTouchHelper.LEFT ); // Разрешаем смахивание влево
        this.adapter = adapter;
        this.background = new ColorDrawable(Color.RED); // Красный фон при смахивании
        this.vibrator = vibrator;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false; // Не поддерживаем перетаскивание
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();

        if (direction == ItemTouchHelper.LEFT) {
            // Показываем диалог подтверждения
            new AlertDialog.Builder(viewHolder.itemView.getContext())
                    .setTitle("Удаление")
                    .setMessage("Вы уверены, что хотите удалить этот элемент?")
                    .setPositiveButton("Да", (dialog, which) -> adapter.deleteItem(position))
                    .setNegativeButton("Нет", (dialog, which) -> adapter.notifyItemChanged(position)) // Отмена удаления
                    .show();
        } else if (direction == ItemTouchHelper.RIGHT) {
            // Возвращаем элемент на место
            adapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();
        int itemWidth = itemView.getWidth();

        // Рисуем красный фон
        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        background.draw(c);

        // Рисуем текст "Удалить"
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48); // Размер текста
        textPaint.setTextAlign(Paint.Align.CENTER);

        float threshold = itemWidth * 0.8f; // Порог в 80% ширины элемента

        if (Math.abs(dX) > threshold) {
            if (!isSwipedBeyondThreshold) {
                // Вибрация при достижении порога
                vibrator.vibrate(50);
                isSwipedBeyondThreshold = true;
            }
            c.drawText("Удалить", itemView.getRight() - itemWidth / 2, itemView.getTop() + itemHeight / 2 + 20, textPaint);
        } else {
            isSwipedBeyondThreshold = false;
        }

        // Зависание элемента при появлении надписи "Удалить"
        if (Math.abs(dX) > itemWidth * 0.5f && Math.abs(dX) < threshold) {
            if (dX < 0) {
                dX = -itemWidth * 0.5f;
            } else {
                dX = itemWidth * 0.5f;
            }
        }

        // Обновляем позицию элемента
        itemView.setTranslationX(dX);
    }
}