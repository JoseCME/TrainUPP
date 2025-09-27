package com.jossecm.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.HistorialEntrenamiento;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {

    private List<HistorialEntrenamiento> historialList;

    public HistorialAdapter(List<HistorialEntrenamiento> historialList) {
        this.historialList = historialList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistorialEntrenamiento historial = historialList.get(position);
        holder.bind(historial);
    }

    @Override
    public int getItemCount() {
        return historialList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewNombreRutina;
        private TextView textViewFecha;
        private TextView textViewDuracion;
        private TextView textViewVolumen;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNombreRutina = itemView.findViewById(R.id.textViewNombreRutina);
            textViewFecha = itemView.findViewById(R.id.textViewFecha);
            textViewDuracion = itemView.findViewById(R.id.textViewDuracion);
            textViewVolumen = itemView.findViewById(R.id.textViewVolumen);
        }

        public void bind(HistorialEntrenamiento historial) {
            textViewNombreRutina.setText(historial.getNombreRutina());

            // Formatear fecha
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date fecha = new Date(historial.getFecha());
            textViewFecha.setText(dateFormat.format(fecha));

            // Mostrar duración
            if (historial.getDuracionMinutos() > 0) {
                textViewDuracion.setText(historial.getDuracionMinutos() + " min");
            } else {
                textViewDuracion.setText("< 1 min");
            }

            // Mostrar volumen total
            textViewVolumen.setText(String.format(Locale.getDefault(), "%.1f lbs", historial.getVolumenTotal()));
        }
    }
}
