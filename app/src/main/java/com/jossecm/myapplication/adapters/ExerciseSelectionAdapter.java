package com.jossecm.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.Exercise;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExerciseSelectionAdapter extends RecyclerView.Adapter<ExerciseSelectionAdapter.ViewHolder> {

    private List<Exercise> exercises;
    private Set<Exercise> selectedExercises = new HashSet<>();
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(Exercise exercise, boolean isSelected);
    }

    public ExerciseSelectionAdapter(List<Exercise> exercises) {
        this.exercises = exercises;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.bind(exercise);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewName;
        private TextView textViewMuscles;
        private ImageView imageViewExercise;
        private CheckBox checkBoxSelected;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewExerciseName);
            textViewMuscles = itemView.findViewById(R.id.textViewMuscles);
            imageViewExercise = itemView.findViewById(R.id.imageViewExercise);
            checkBoxSelected = itemView.findViewById(R.id.checkBoxSelected);
        }

        public void bind(Exercise exercise) {
            textViewName.setText(exercise.getName());

            if (exercise.getMuscleNames() != null && !exercise.getMuscleNames().isEmpty()) {
                textViewMuscles.setText(String.join(", ", exercise.getMuscleNames()));
            } else {
                textViewMuscles.setText("Músculos no especificados");
            }

            // Cargar imagen con Glide
            if (exercise.getImageUrl() != null && !exercise.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(exercise.getImageUrl())
                    .placeholder(R.drawable.ic_exercise_placeholder)
                    .error(R.drawable.ic_exercise_placeholder)
                    .into(imageViewExercise);
            } else {
                imageViewExercise.setImageResource(R.drawable.ic_exercise_placeholder);
            }

            // Configurar checkbox
            checkBoxSelected.setChecked(selectedExercises.contains(exercise));

            // Listener para checkbox
            checkBoxSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedExercises.add(exercise);
                } else {
                    selectedExercises.remove(exercise);
                }

                if (listener != null) {
                    listener.onSelectionChanged(exercise, isChecked);
                }
            });

            // También permitir selección tocando toda la fila
            itemView.setOnClickListener(v -> {
                checkBoxSelected.setChecked(!checkBoxSelected.isChecked());
            });
        }
    }
}
