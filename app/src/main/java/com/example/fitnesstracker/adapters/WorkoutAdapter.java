// WorkoutAdapter.java
package com.example.fitnesstracker.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.fitnesstracker.R;
import com.example.fitnesstracker.fragments.WorkoutDetailActivity;
import com.example.fitnesstracker.models.Workout;
import java.util.List;
import com.bumptech.glide.Glide;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    public List<Workout> workoutList;

    public WorkoutAdapter(List<Workout> workoutList) {
        this.workoutList = workoutList;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Workout workout = workoutList.get(position);
        holder.workoutName.setText(workout.getAction());
        holder.difficultyText.setText(workout.getDifficulty());

        // Устанавливаем цвет текста и иконки в зависимости от уровня сложности
        switch (workout.getDifficulty().toLowerCase()) {
            case "легкий":
                holder.difficultyText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green));
                holder.distanceIcon.setBackgroundResource(R.drawable.distance_ic_green);
                break;
            case "умеренный":
                holder.difficultyText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.blue));
                holder.distanceIcon.setBackgroundResource(R.drawable.distance_ic_blue);
                break;
            case "сложный":
                holder.difficultyText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red));
                holder.distanceIcon.setBackgroundResource(R.drawable.distance_ic_red);
                break;
        }

        holder.workoutsPerWeekText.setText(workout.getWorkoutsPerWeek() + " тренировки/неделя");

        if (workout.getImage() != null && workout.getImage().getImageUrl() != null) {
            String imageUrl = workout.getImage().getImageUrl();
            Log.d("WorkoutAdapter", "Loading image from URL: " + imageUrl);

            Context context = holder.itemView.getContext();
            Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.workout_placeholder)
                    .error(R.drawable.workout_placeholder)
                    .into(holder.workoutImage);
        } else {
            Log.d("WorkoutAdapter", "No image URL for workout: " + workout.getAction());
            holder.workoutImage.setImageResource(R.drawable.workout_placeholder);
        }

        // Обработка нажатия на элемент списка
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), WorkoutDetailActivity.class);
            intent.putExtra("workout", workout);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return workoutList.size();
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        ImageView workoutImage;
        TextView workoutName;
        ImageView distanceIcon;
        TextView difficultyText;
        TextView workoutsPerWeekText;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            workoutImage = itemView.findViewById(R.id.workoutImage);
            workoutName = itemView.findViewById(R.id.workoutName);
            distanceIcon = itemView.findViewById(R.id.distanceIcon);
            difficultyText = itemView.findViewById(R.id.difficultyText);
            workoutsPerWeekText = itemView.findViewById(R.id.workoutsPerWeekText);
        }
    }
}