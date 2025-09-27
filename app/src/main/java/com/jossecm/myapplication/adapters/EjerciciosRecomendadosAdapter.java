package com.jossecm.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.Exercise;
import java.util.List;

public class EjerciciosRecomendadosAdapter extends RecyclerView.Adapter<EjerciciosRecomendadosAdapter.EjercicioViewHolder> {

    private List<Exercise> ejercicios;
    private OnImplementarEjercicioListener implementarListener;

    public interface OnImplementarEjercicioListener {
        void onImplementarEjercicio(Exercise ejercicio);
    }

    public EjerciciosRecomendadosAdapter(List<Exercise> ejercicios) {
        this.ejercicios = ejercicios;
    }

    public void setOnImplementarEjercicioListener(OnImplementarEjercicioListener listener) {
        this.implementarListener = listener;
    }

    @NonNull
    @Override
    public EjercicioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ejercicio_recomendado, parent, false);
        return new EjercicioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EjercicioViewHolder holder, int position) {
        Exercise ejercicio = ejercicios.get(position);
        holder.bind(ejercicio, implementarListener);
    }

    @Override
    public int getItemCount() {
        return ejercicios != null ? ejercicios.size() : 0;
    }

    static class EjercicioViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombreEjercicio;
        private TextView tvDescripcionEjercicio;
        private TextView tvMusculosEjercicio;
        private Button btnAgregarEjercicio;

        public EjercicioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEjercicio = itemView.findViewById(R.id.tvNombreEjercicio);
            tvDescripcionEjercicio = itemView.findViewById(R.id.tvDescripcionEjercicio);
            tvMusculosEjercicio = itemView.findViewById(R.id.tvMusculosEjercicio);
            btnAgregarEjercicio = itemView.findViewById(R.id.btnAgregarEjercicio);
        }

        public void bind(Exercise ejercicio, OnImplementarEjercicioListener listener) {
            tvNombreEjercicio.setText(ejercicio.getName());
            tvDescripcionEjercicio.setText(ejercicio.getDescription());

            // Mostrar músculos objetivo
            if (ejercicio.getMuscleNames() != null && !ejercicio.getMuscleNames().isEmpty()) {
                String musculos = "Músculos: " + String.join(", ", ejercicio.getMuscleNames());
                tvMusculosEjercicio.setText(musculos);
                tvMusculosEjercicio.setVisibility(View.VISIBLE);
            } else {
                tvMusculosEjercicio.setVisibility(View.GONE);
            }

            // Configurar botón de agregar
            btnAgregarEjercicio.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImplementarEjercicio(ejercicio);
                }
            });
        }
    }
}
