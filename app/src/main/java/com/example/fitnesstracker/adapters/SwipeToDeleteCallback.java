package com.example.fitnesstracker.adapters;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesstracker.fragments.ProgressFragment;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final ProgressFragment.ActivitiesAdapter adapter;
    private final Drawable deleteIcon;
    private final ColorDrawable background;

    public SwipeToDeleteCallback(ProgressFragment.ActivitiesAdapter adapter, Drawable deleteIcon) {
        super(0, ItemTouchHelper.LEFT); // Разрешаем смахивание только влево
        this.adapter = adapter;
        this.deleteIcon = deleteIcon;
        this.background = new ColorDrawable(Color.RED); // Красный фон при смахивании
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false; // Не поддерживаем перетаскивание
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();

        // Показываем диалог подтверждения
        new AlertDialog.Builder(viewHolder.itemView.getContext())
                .setTitle("Удаление")
                .setMessage("Вы уверены, что хотите удалить этот элемент?")
                .setPositiveButton("Да", (dialog, which) -> adapter.deleteItem(position))
                .setNegativeButton("Нет", (dialog, which) -> adapter.notifyItemChanged(position)) // Отмена удаления
                .show();
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();

        // Рисуем красный фон
        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        background.draw(c);

        // Рисуем иконку удаления
        int iconMargin = (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
        int iconRight = itemView.getRight() - iconMargin;
        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        deleteIcon.draw(c);

        // Рисуем текст "Удалить"
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48); // Размер текста
        textPaint.setTextAlign(Paint.Align.CENTER);
        c.drawText("Удалить", itemView.getRight() - 300, itemView.getTop() + itemHeight / 2 + 20, textPaint);
    }
}