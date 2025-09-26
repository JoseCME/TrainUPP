package com.jossecm.myapplication.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.Exercise;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    private List<Exercise> exerciseList;
    private Set<Integer> selectedExerciseIds = new HashSet<>();

    public ExerciseAdapter(List<Exercise> exerciseList) {
        this.exerciseList = exerciseList;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exerciseList.get(position);
        holder.bind(exercise, selectedExerciseIds.contains(exercise.getId()));
    }

    @Override
    public int getItemCount() {
        return exerciseList.size();
    }

    public Set<Integer> getSelectedExerciseIds() {
        return new HashSet<>(selectedExerciseIds);
    }

    public void clearSelection() {
        selectedExerciseIds.clear();
        notifyDataSetChanged();
    }

    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardExercise;
        private ImageView ivExerciseImage;
        private TextView tvExerciseName;
        private TextView tvPrimaryMuscle;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            cardExercise = itemView.findViewById(R.id.cardExercise);
            ivExerciseImage = itemView.findViewById(R.id.ivExerciseImage);
            tvExerciseName = itemView.findViewById(R.id.tvExerciseName);
            tvPrimaryMuscle = itemView.findViewById(R.id.tvPrimaryMuscle);
        }

        public void bind(Exercise exercise, boolean isSelected) {
            tvExerciseName.setText(exercise.getName());

            // Mejorar el manejo de nombres de músculos con logging
            String primaryMuscle = exercise.getPrimaryMuscle();
            if (primaryMuscle != null && !primaryMuscle.equals("Múltiples músculos")) {
                tvPrimaryMuscle.setText(primaryMuscle);
                android.util.Log.d("ExerciseAdapter", "Ejercicio: " + exercise.getName() +
                    " - Músculo mostrado: " + primaryMuscle);
            } else {
                // Fallback: mostrar información de debugging
                if (exercise.getMuscleNames() != null && !exercise.getMuscleNames().isEmpty()) {
                    tvPrimaryMuscle.setText(exercise.getMuscleNames().get(0));
                    android.util.Log.d("ExerciseAdapter", "Usando muscleNames[0]: " + exercise.getMuscleNames().get(0));
                } else if (exercise.getMuscleIds() != null && !exercise.getMuscleIds().isEmpty()) {
                    tvPrimaryMuscle.setText("Músculo ID: " + exercise.getMuscleIds().get(0));
                    android.util.Log.w("ExerciseAdapter", "Sin nombres de músculos, mostrando ID: " + exercise.getMuscleIds().get(0));
                } else {
                    tvPrimaryMuscle.setText("Sin información de músculo");
                    android.util.Log.w("ExerciseAdapter", "Ejercicio sin información de músculos: " + exercise.getName());
                }
            }

            // Cargar imagen con Glide - Corregir problema con WebP y AnimatedImageDrawable
            Glide.with(itemView.getContext())
                    .load(exercise.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .skipMemoryCache(false)
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(ivExerciseImage);

            // Actualizar apariencia según selección
            updateSelectionAppearance(isSelected);

            // Manejar clic directo en el card
            cardExercise.setOnClickListener(v -> {
                int exerciseId = exercise.getId();
                boolean wasSelected = selectedExerciseIds.contains(exerciseId);

                if (wasSelected) {
                    selectedExerciseIds.remove(exerciseId);
                } else {
                    selectedExerciseIds.add(exerciseId);
                }

                updateSelectionAppearance(!wasSelected);
            });
        }

        private void updateSelectionAppearance(boolean isSelected) {
            if (isSelected) {
                // Ejercicio seleccionado - mostrar borde colorido
                cardExercise.setStrokeColor(Color.BLUE);
                cardExercise.setStrokeWidth(4);
                cardExercise.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
            } else {
                // Ejercicio no seleccionado - apariencia normal
                cardExercise.setStrokeWidth(0);
                cardExercise.setCardBackgroundColor(Color.WHITE);
            }
        }
    }
}
