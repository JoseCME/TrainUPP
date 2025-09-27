package com.jossecm.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.Rutina;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RutinaAdapter extends RecyclerView.Adapter<RutinaAdapter.ViewHolder> {

    private List<Rutina> rutinas;
    private OnRutinaClickListener listener;

    public interface OnRutinaClickListener {
        void onComenzarRutina(Rutina rutina);
    }

    public RutinaAdapter(List<Rutina> rutinas) {
        this.rutinas = rutinas;
    }

    public void setOnRutinaClickListener(OnRutinaClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rutina, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Rutina rutina = rutinas.get(position);
        holder.bind(rutina);
    }

    @Override
    public int getItemCount() {
        return rutinas.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewNombre;
        private TextView textViewCantidadEjercicios;
        private TextView textViewFecha;
        private Button buttonComenzar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNombre = itemView.findViewById(R.id.textViewNombreRutina);
            textViewCantidadEjercicios = itemView.findViewById(R.id.textViewCantidadEjercicios);
            textViewFecha = itemView.findViewById(R.id.textViewFechaCreacion);
            buttonComenzar = itemView.findViewById(R.id.buttonComenzarRutina);
        }

        public void bind(Rutina rutina) {
            textViewNombre.setText(rutina.getNombre());
            textViewCantidadEjercicios.setText(rutina.getCantidadEjercicios() + " ejercicios");

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fecha = new Date(rutina.getFechaCreacion()); // Convertir timestamp a Date
            textViewFecha.setText("Creada: " + dateFormat.format(fecha));

            buttonComenzar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onComenzarRutina(rutina);
                }
            });
        }
    }
}
