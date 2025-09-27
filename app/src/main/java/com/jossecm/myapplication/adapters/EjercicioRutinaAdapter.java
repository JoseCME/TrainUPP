package com.jossecm.myapplication.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.models.Serie;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EjercicioRutinaAdapter extends RecyclerView.Adapter<EjercicioRutinaAdapter.ViewHolder> {

    private List<Exercise> exercises;
    private Map<Integer, List<Serie>> seriesPorEjercicio = new HashMap<>();
    private OnVolumenChangedListener volumenListener;

    public interface OnVolumenChangedListener {
        void onVolumenChanged(double nuevoVolumenTotal);
    }

    public EjercicioRutinaAdapter(List<Exercise> exercises) {
        this.exercises = exercises;

        android.util.Log.d("EjercicioRutinaAdapter", "=== INICIALIZANDO ADAPTADOR ===");
        android.util.Log.d("EjercicioRutinaAdapter", "Número de ejercicios recibidos: " + (exercises != null ? exercises.size() : 0));

        if (exercises == null || exercises.isEmpty()) {
            android.util.Log.w("EjercicioRutinaAdapter", "Lista de ejercicios está vacía o es null");
            return;
        }

        // CORREGIDO: Inicializar con 3 series por defecto para cada ejercicio
        for (Exercise exercise : exercises) {
            if (exercise == null) {
                android.util.Log.w("EjercicioRutinaAdapter", "Ejercicio null encontrado en la lista");
                continue;
            }

            android.util.Log.d("EjercicioRutinaAdapter",
                "Procesando ejercicio - ID: " + exercise.getId() + ", Nombre: " + exercise.getName());

            List<Serie> series = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                series.add(new Serie());
            }

            // IMPORTANTE: Usar directamente exercise.getId() como int
            Integer exerciseId = exercise.getId();
            seriesPorEjercicio.put(exerciseId, series);

            android.util.Log.d("EjercicioRutinaAdapter",
                "✓ Inicializadas 3 series para ejercicio ID: " + exerciseId + " - " + exercise.getName());
        }

        android.util.Log.d("EjercicioRutinaAdapter",
            "=== INICIALIZACIÓN COMPLETADA - Total ejercicios con series: " + seriesPorEjercicio.size() + " ===");

        // Imprimir todas las claves del mapa para debugging
        for (Integer key : seriesPorEjercicio.keySet()) {
            android.util.Log.d("EjercicioRutinaAdapter", "Clave en mapa: " + key);
        }
    }

    public void setOnVolumenChangedListener(OnVolumenChangedListener listener) {
        this.volumenListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio_rutina, parent, false);
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

    private void calcularVolumenTotal() {
        double volumenTotal = 0.0;

        for (List<Serie> series : seriesPorEjercicio.values()) {
            for (Serie serie : series) {
                volumenTotal += serie.getVolumen(); // Solo suma si está completada
            }
        }

        if (volumenListener != null) {
            volumenListener.onVolumenChanged(volumenTotal);
        }
    }

    public JSONObject getEjerciciosRealizadosJson() {
        try {
            JSONObject resultado = new JSONObject();
            JSONArray ejerciciosArray = new JSONArray();

            for (Exercise exercise : exercises) {
                List<Serie> series = seriesPorEjercicio.get(exercise.getId());
                if (series != null) {
                    JSONObject ejercicioObj = new JSONObject();
                    ejercicioObj.put("ejercicio_id", exercise.getId());
                    ejercicioObj.put("nombre", exercise.getName());

                    JSONArray seriesArray = new JSONArray();
                    for (Serie serie : series) {
                        if (serie.isCompletada()) {
                            JSONObject serieObj = new JSONObject();
                            serieObj.put("peso", serie.getPeso());
                            serieObj.put("repeticiones", serie.getRepeticiones());
                            serieObj.put("volumen", serie.getVolumen());
                            seriesArray.put(serieObj);
                        }
                    }

                    if (seriesArray.length() > 0) {
                        ejercicioObj.put("series", seriesArray);
                        ejerciciosArray.put(ejercicioObj);
                    }
                }
            }

            resultado.put("ejercicios", ejerciciosArray);
            return resultado;

        } catch (Exception e) {
            android.util.Log.e("EjercicioRutinaAdapter", "Error creando JSON", e);
            return new JSONObject();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewNombre;
        private TextView textViewMusculos;
        private ImageView imageViewEjercicio;
        private LinearLayout layoutSeries;
        private Button buttonAgregarSerie;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNombre = itemView.findViewById(R.id.textViewNombreEjercicio);
            textViewMusculos = itemView.findViewById(R.id.textViewMusculos);
            imageViewEjercicio = itemView.findViewById(R.id.imageViewEjercicio);
            layoutSeries = itemView.findViewById(R.id.layoutSeries);
            buttonAgregarSerie = itemView.findViewById(R.id.buttonAgregarSerie);
        }

        public void bind(Exercise exercise) {
            android.util.Log.d("EjercicioRutinaAdapter",
                ">>> BIND llamado para ejercicio ID: " + exercise.getId() + " - " + exercise.getName());

            // SOLUCIÓN DE RESPALDO: Inicializar series si no existen
            Integer exerciseId = exercise.getId();
            if (!seriesPorEjercicio.containsKey(exerciseId) || seriesPorEjercicio.get(exerciseId) == null) {
                android.util.Log.w("EjercicioRutinaAdapter",
                    "Series no encontradas para ejercicio " + exerciseId + ", inicializando ahora...");

                List<Serie> series = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    series.add(new Serie());
                }
                seriesPorEjercicio.put(exerciseId, series);

                android.util.Log.d("EjercicioRutinaAdapter",
                    "✓ Inicializadas 3 series de respaldo para ejercicio ID: " + exerciseId);
            }

            textViewNombre.setText(exercise.getName());

            if (exercise.getMuscleNames() != null && !exercise.getMuscleNames().isEmpty()) {
                textViewMusculos.setText(String.join(", ", exercise.getMuscleNames()));
            } else {
                textViewMusculos.setText("Músculos no especificados");
            }

            // Cargar imagen
            if (exercise.getImageUrl() != null && !exercise.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(exercise.getImageUrl())
                    .placeholder(R.drawable.ic_exercise_placeholder)
                    .error(R.drawable.ic_exercise_placeholder)
                    .into(imageViewEjercicio);
            } else {
                imageViewEjercicio.setImageResource(R.drawable.ic_exercise_placeholder);
            }

            // IMPORTANTE: Asegurar que las series se muestren desde el inicio
            setupSeries(exercise);

            // Configurar botón agregar serie con logging para debugging
            buttonAgregarSerie.setOnClickListener(v -> {
                android.util.Log.d("EjercicioRutinaAdapter",
                    "Botón agregar serie presionado para ejercicio ID: " + exerciseId);

                // SOLUCIÓN DE RESPALDO: Verificar e inicializar si es necesario
                if (!seriesPorEjercicio.containsKey(exerciseId) || seriesPorEjercicio.get(exerciseId) == null) {
                    android.util.Log.w("EjercicioRutinaAdapter",
                        "Inicializando series dinámicamente para ejercicio " + exerciseId);

                    List<Serie> series = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        series.add(new Serie());
                    }
                    seriesPorEjercicio.put(exerciseId, series);
                }

                List<Serie> series = seriesPorEjercicio.get(exerciseId);
                if (series != null) {
                    series.add(new Serie());
                    android.util.Log.d("EjercicioRutinaAdapter",
                        "Agregada nueva serie a " + exercise.getName() + ". Total series: " + series.size());
                    setupSeries(exercise); // Refrescar vista
                } else {
                    android.util.Log.e("EjercicioRutinaAdapter",
                        "ERROR CRÍTICO: No se pudo inicializar series para ejercicio: " + exercise.getName());
                    // Debug adicional
                    android.util.Log.e("EjercicioRutinaAdapter",
                        "Claves disponibles en mapa: " + seriesPorEjercicio.keySet().toString());
                }
            });
        }

        private void setupSeries(Exercise exercise) {
            android.util.Log.d("EjercicioRutinaAdapter",
                ">>> SETUP SERIES llamado para ejercicio ID: " + exercise.getId() + " - " + exercise.getName());

            layoutSeries.removeAllViews();
            Integer exerciseId = exercise.getId();

            android.util.Log.d("EjercicioRutinaAdapter",
                "Buscando series para ID: " + exerciseId + " en mapa con " + seriesPorEjercicio.size() + " entradas");

            List<Serie> series = seriesPorEjercicio.get(exerciseId);

            if (series != null && !series.isEmpty()) {
                android.util.Log.d("EjercicioRutinaAdapter",
                    "✓ Configurando " + series.size() + " series para " + exercise.getName());

                for (int i = 0; i < series.size(); i++) {
                    Serie serie = series.get(i);
                    View serieView = createSerieView(serie, i + 1);
                    layoutSeries.addView(serieView);
                }
            } else {
                android.util.Log.w("EjercicioRutinaAdapter",
                    "❌ No hay series para mostrar en ejercicio: " + exercise.getName());
                android.util.Log.w("EjercicioRutinaAdapter",
                    "❌ Claves disponibles en mapa: " + seriesPorEjercicio.keySet().toString());
                android.util.Log.w("EjercicioRutinaAdapter",
                    "❌ Buscando clave: " + exerciseId + " (tipo: " + exerciseId.getClass().getSimpleName() + ")");
            }
        }

        private View createSerieView(Serie serie, int numeroSerie) {
            View view = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_serie, layoutSeries, false);

            TextView textViewNumero = view.findViewById(R.id.textViewNumeroSerie);
            EditText editTextPeso = view.findViewById(R.id.editTextPeso);
            EditText editTextReps = view.findViewById(R.id.editTextRepeticiones);
            CheckBox checkBoxCompletada = view.findViewById(R.id.checkBoxCompletada);

            textViewNumero.setText(String.valueOf(numeroSerie));

            // Configurar valores actuales
            if (serie.getPeso() > 0) {
                editTextPeso.setText(String.format(Locale.getDefault(), "%.1f", serie.getPeso()));
            }
            if (serie.getRepeticiones() > 0) {
                editTextReps.setText(String.valueOf(serie.getRepeticiones()));
            }
            checkBoxCompletada.setChecked(serie.isCompletada());

            // TextWatcher para peso
            editTextPeso.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        String text = s.toString().trim();
                        if (!text.isEmpty()) {
                            double peso = Double.parseDouble(text);
                            serie.setPeso(peso);
                        } else {
                            serie.setPeso(0.0);
                        }

                        // Si estaba completada, recalcular volumen
                        if (serie.isCompletada()) {
                            calcularVolumenTotal();
                        }
                    } catch (NumberFormatException e) {
                        serie.setPeso(0.0);
                    }
                }
            });

            // TextWatcher para repeticiones
            editTextReps.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        String text = s.toString().trim();
                        if (!text.isEmpty()) {
                            int reps = Integer.parseInt(text);
                            serie.setRepeticiones(reps);
                        } else {
                            serie.setRepeticiones(0);
                        }

                        // Si estaba completada, recalcular volumen
                        if (serie.isCompletada()) {
                            calcularVolumenTotal();
                        }
                    } catch (NumberFormatException e) {
                        serie.setRepeticiones(0);
                    }
                }
            });

            // Listener para checkbox con validación
            checkBoxCompletada.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Validar que peso > 0 y reps > 0
                    if (serie.getPeso() > 0 && serie.getRepeticiones() > 0) {
                        serie.setCompletada(true);
                        calcularVolumenTotal();

                        android.util.Log.d("EjercicioRutinaAdapter",
                            "Serie completada - Peso: " + serie.getPeso() +
                            ", Reps: " + serie.getRepeticiones() +
                            ", Volumen: " + serie.getVolumen());
                    } else {
                        // No permitir marcar si no hay peso o reps válidos
                        checkBoxCompletada.setChecked(false);
                        android.widget.Toast.makeText(itemView.getContext(),
                            "Ingresa peso y repeticiones válidos",
                            android.widget.Toast.LENGTH_SHORT).show();
                    }
                } else {
                    serie.setCompletada(false);
                    calcularVolumenTotal();

                    android.util.Log.d("EjercicioRutinaAdapter",
                        "Serie desmarcada - Volumen removido: " + (serie.getPeso() * serie.getRepeticiones()));
                }
            });

            return view;
        }
    }
}
