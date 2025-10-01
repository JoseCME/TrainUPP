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
import com.jossecm.myapplication.models.SerieHistorial;
import com.jossecm.myapplication.repository.FitnessRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EjercicioRutinaAdapter extends RecyclerView.Adapter<EjercicioRutinaAdapter.ViewHolder> {

    private List<Exercise> exercises;
    private Map<Integer, List<Serie>> seriesPorEjercicio = new HashMap<>();
    private Map<Integer, List<SerieHistorial>> historialPorEjercicio = new HashMap<>();
    // NUEVO: Mapa para mantener el número de series configuradas por ejercicio
    private Map<Integer, Integer> numSeriesConfiguradas = new HashMap<>();
    private OnVolumenChangedListener volumenListener;
    private FitnessRepository repository;

    public interface OnVolumenChangedListener {
        void onVolumenChanged(double nuevoVolumenTotal);
    }

    public EjercicioRutinaAdapter(List<Exercise> exercises, FitnessRepository repository) {
        this.exercises = exercises != null ? exercises : new ArrayList<>();
        this.repository = repository;

        android.util.Log.d("EjercicioRutinaAdapter", "=== INICIALIZANDO ADAPTADOR ===");
        android.util.Log.d("EjercicioRutinaAdapter", "Número de ejercicios recibidos: " + this.exercises.size());

        if (this.exercises.isEmpty()) {
            android.util.Log.w("EjercicioRutinaAdapter", "Lista de ejercicios está vacía - se inicializará cuando se carguen");
            return;
        }

        inicializarEjercicios();
    }

    // NUEVO: Método para actualizar la lista de ejercicios después de la carga inicial
    public void updateExercises(List<Exercise> newExercises) {
        android.util.Log.d("EjercicioRutinaAdapter", "Actualizando ejercicios - recibidos: " +
            (newExercises != null ? newExercises.size() : 0));

        if (newExercises != null && !newExercises.isEmpty()) {
            // CORREGIDO: No limpiar la lista hasta verificar que hay nuevos ejercicios válidos
            android.util.Log.d("EjercicioRutinaAdapter", "Ejercicios válidos recibidos: " + newExercises.size());

            this.exercises.clear();
            this.exercises.addAll(newExercises);

            // Limpiar datos anteriores
            seriesPorEjercicio.clear();
            historialPorEjercicio.clear();

            // Inicializar con los nuevos ejercicios
            inicializarEjercicios();

            // Notificar cambios
            notifyDataSetChanged();

            android.util.Log.d("EjercicioRutinaAdapter", "Adapter actualizado correctamente con " + this.exercises.size() + " ejercicios");
        } else {
            android.util.Log.w("EjercicioRutinaAdapter", "No se recibieron ejercicios válidos para actualizar");
        }
    }

    private void inicializarEjercicios() {
        android.util.Log.d("EjercicioRutinaAdapter", "Inicializando " + exercises.size() + " ejercicios");

        for (Exercise exercise : exercises) {
            if (exercise == null) {
                android.util.Log.w("EjercicioRutinaAdapter", "Ejercicio null encontrado en la lista");
                continue;
            }

            android.util.Log.d("EjercicioRutinaAdapter",
                "Procesando ejercicio - ID: " + exercise.getId() + ", Nombre: " + exercise.getName());

            Integer exerciseId = exercise.getId();

            // Cargar historial PRIMERO para saber cuántas series crear
            cargarHistorialEjercicio(exerciseId);
            // NUEVO: Cargar configuración de series guardada
            cargarConfiguracionSeries(exerciseId);

            android.util.Log.d("EjercicioRutinaAdapter",
                "✓ Configuración iniciada para ejercicio ID: " + exerciseId + " - " + exercise.getName());
        }

        android.util.Log.d("EjercicioRutinaAdapter",
            "=== INICIALIZACIÓN COMPLETADA ===");
    }

    // NUEVO: Método para cargar la configuración de series guardada
    private void cargarConfiguracionSeries(int exerciseId) {
        repository.getConfiguracionSeries(exerciseId, new FitnessRepository.DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer numSeries) {
                if (numSeries != null && numSeries > 0) {
                    numSeriesConfiguradas.put(exerciseId, numSeries);
                    android.util.Log.d("EjercicioRutinaAdapter",
                        "Configuración cargada para ejercicio " + exerciseId + ": " + numSeries + " series");

                    // CRÍTICO: Actualizar las series ahora que tenemos la configuración real
                    actualizarSeriesConConfiguracion(exerciseId, numSeries);
                } else {
                    // Si no hay configuración guardada, usar valor por defecto
                    numSeriesConfiguradas.put(exerciseId, 3);
                    android.util.Log.d("EjercicioRutinaAdapter",
                        "Sin configuración previa para ejercicio " + exerciseId + ", usando 3 series por defecto");

                    actualizarSeriesConConfiguracion(exerciseId, 3);
                }
            }

            @Override
            public void onError(String error) {
                // En caso de error, usar valor por defecto
                numSeriesConfiguradas.put(exerciseId, 3);
                android.util.Log.w("EjercicioRutinaAdapter",
                    "Error cargando configuración para ejercicio " + exerciseId + ", usando 3 series por defecto");

                actualizarSeriesConConfiguracion(exerciseId, 3);
            }
        });
    }

    // NUEVO: Método para actualizar series cuando la configuración se carga
    private void actualizarSeriesConConfiguracion(int exerciseId, int numSeriesConfiguradas) {
        List<SerieHistorial> historial = historialPorEjercicio.get(exerciseId);
        if (historial == null) {
            historial = new ArrayList<>();
        }

        // Crear las series definitivas con la configuración real
        crearSeriesConConfiguracion(exerciseId, numSeriesConfiguradas, historial);

        // Actualizar vista en el hilo principal
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.postDelayed(() -> {
            try {
                if (exercises != null && !exercises.isEmpty()) {
                    notifyDataSetChanged();
                    android.util.Log.d("EjercicioRutinaAdapter",
                        "Vista actualizada después de cargar configuración para ejercicio " + exerciseId);
                }
            } catch (Exception e) {
                android.util.Log.e("EjercicioRutinaAdapter",
                    "Error actualizando vista para ejercicio " + exerciseId, e);
            }
        }, 150); // Delay ligeramente mayor para asegurar que todo esté listo
    }

    private void cargarHistorialEjercicio(int exerciseId) {
        repository.getUltimasSeriesEjercicio(exerciseId, new FitnessRepository.DataCallback<List<SerieHistorial>>() {
            @Override
            public void onSuccess(List<SerieHistorial> historial) {
                historialPorEjercicio.put(exerciseId, historial);
                android.util.Log.d("EjercicioRutinaAdapter",
                    "Historial cargado para ejercicio " + exerciseId + ": " + historial.size() + " series");

                // MODIFICADO: Crear series basándose en la configuración guardada y el historial
                inicializarSeriesEjercicio(exerciseId, historial);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("EjercicioRutinaAdapter",
                    "Error cargando historial para ejercicio " + exerciseId + ": " + error);
                historialPorEjercicio.put(exerciseId, new ArrayList<>());

                // Si no hay historial, usar configuración guardada o por defecto
                inicializarSeriesEjercicio(exerciseId, new ArrayList<>());
            }
        });
    }

    // NUEVO: Método para inicializar series basándose en configuración y historial
    private void inicializarSeriesEjercicio(int exerciseId, List<SerieHistorial> historial) {
        // CRÍTICO: NO crear las series aquí si la configuración aún no se ha cargado
        // Este método se llama después de cargar el historial, pero la configuración se carga en paralelo

        // Solo crear series de respaldo si no hay configuración pendiente
        Integer numConfiguracion = numSeriesConfiguradas.get(exerciseId);
        if (numConfiguracion == null) {
            // Configuración aún no cargada, usar valor temporal
            List<Serie> series = new ArrayList<>();
            for (int i = 0; i < 3; i++) { // Temporal: 3 series por defecto
                series.add(new Serie());
            }
            seriesPorEjercicio.put(exerciseId, series);

            android.util.Log.d("EjercicioRutinaAdapter",
                "⏳ Series temporales inicializadas para ejercicio ID: " + exerciseId +
                " (esperando configuración de BD)");
        } else {
            // Configuración ya cargada, crear series definitivas
            crearSeriesConConfiguracion(exerciseId, numConfiguracion, historial);
        }

        // Actualizar vista en el hilo principal
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.postDelayed(() -> {
            try {
                if (exercises != null && !exercises.isEmpty()) {
                    notifyDataSetChanged();
                    android.util.Log.d("EjercicioRutinaAdapter",
                        "Vista actualizada después de cargar historial para ejercicio " + exerciseId);
                }
            } catch (Exception e) {
                android.util.Log.e("EjercicioRutinaAdapter",
                    "Error actualizando vista para ejercicio " + exerciseId, e);
            }
        }, 100);
    }

    // NUEVO: Método para crear series con la configuración real
    private void crearSeriesConConfiguracion(int exerciseId, int numSeriesConfiguradas, List<SerieHistorial> historial) {
        List<Serie> series = new ArrayList<>();

        // Asegurar que siempre hay al menos 3 series
        int numSeriesACrear = Math.max(numSeriesConfiguradas, 3);

        // CRÍTICO: Crear exactamente el número de series configuradas
        for (int i = 0; i < numSeriesACrear; i++) {
            series.add(new Serie());
        }

        seriesPorEjercicio.put(exerciseId, series);

        android.util.Log.d("EjercicioRutinaAdapter",
            "✓ Series definitivas creadas para ejercicio ID: " + exerciseId +
            " (" + numSeriesACrear + " series configuradas, " + historial.size() + " entradas de historial para referencia)");
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

                    // NUEVO: Guardar configuración de series
                    Integer numConfiguracion = numSeriesConfiguradas.get(exercise.getId());
                    int numSeriesConfiguracion = (numConfiguracion != null) ? numConfiguracion : 3;
                    ejercicioObj.put("num_series_configuradas", numSeriesConfiguracion);

                    JSONArray seriesArray = new JSONArray();

                    // Solo tomar las series configuradas (no las de historial) para el JSON
                    int maxSeries = Math.min(numSeriesConfiguracion, series.size());

                    for (int i = 0; i < maxSeries; i++) {
                        Serie serie = series.get(i);
                        if (serie.isCompletada()) {
                            JSONObject serieObj = new JSONObject();
                            serieObj.put("peso", serie.getPeso());
                            serieObj.put("repeticiones", serie.getRepeticiones());
                            serieObj.put("volumen", serie.getVolumen());
                            seriesArray.put(serieObj);

                            android.util.Log.d("EjercicioRutinaAdapter",
                                "JSON - Serie nueva añadida para ejercicio " + exercise.getId() +
                                " - Serie " + (i+1) + ": " + serie.getPeso() + " lbs, " +
                                serie.getRepeticiones() + " reps");
                        }
                    }

                    if (seriesArray.length() > 0 || numSeriesConfiguracion > 3) {
                        ejercicioObj.put("series", seriesArray);
                        ejerciciosArray.put(ejercicioObj);

                        android.util.Log.d("EjercicioRutinaAdapter",
                            "JSON - ✓ Agregado ejercicio " + exercise.getId() + " con " +
                            seriesArray.length() + " series completadas y " + numSeriesConfiguracion + " series configuradas");
                    }
                }
            }

            resultado.put("ejercicios", ejerciciosArray);

            android.util.Log.d("EjercicioRutinaAdapter",
                "JSON final - Total ejercicios en JSON: " + ejerciciosArray.length());

            return resultado;

        } catch (Exception e) {
            android.util.Log.e("EjercicioRutinaAdapter", "Error creando JSON", e);
            return new JSONObject();
        }
    }

    public Map<Integer, List<Serie>> getSeriesCompletadas() {
        Map<Integer, List<Serie>> seriesCompletadas = new HashMap<>();

        for (Map.Entry<Integer, List<Serie>> entry : seriesPorEjercicio.entrySet()) {
            List<Serie> seriesCompletas = new ArrayList<>();
            List<Serie> todasLasSeries = entry.getValue();
            Integer exerciseId = entry.getKey();

            // CORREGIDO: Tomar TODAS las series configuradas, no limitarse por historial
            Integer numConfiguracion = numSeriesConfiguradas.get(exerciseId);
            int numSeriesConfiguradas = (numConfiguracion != null) ? numConfiguracion : 3;

            // Procesar TODAS las series configuradas para este ejercicio
            int maxSeries = Math.min(numSeriesConfiguradas, todasLasSeries.size());

            android.util.Log.d("EjercicioRutinaAdapter",
                "Procesando ejercicio " + exerciseId + ": " + numSeriesConfiguradas + " series configuradas, " +
                todasLasSeries.size() + " series totales, procesando " + maxSeries + " series");

            // CRÍTICO: Crear objetos Serie con posición preservada
            for (int i = 0; i < maxSeries; i++) {
                Serie serie = todasLasSeries.get(i);
                if (serie.isCompletada()) {
                    // NUEVO: Crear una copia que preserve el número de serie correcto
                    Serie serieCopia = new Serie(serie.getPeso(), serie.getRepeticiones(), true);
                    serieCopia.setNumeroSerie(i + 1); // Preservar la posición real

                    seriesCompletas.add(serieCopia);
                    android.util.Log.d("EjercicioRutinaAdapter",
                        "Serie completada añadida para ejercicio " + exerciseId +
                        " - Serie " + (i+1) + ": " + serie.getPeso() + " lbs, " +
                        serie.getRepeticiones() + " reps, Volumen: " + serie.getVolumen());
                }
            }

            if (!seriesCompletas.isEmpty()) {
                seriesCompletadas.put(exerciseId, seriesCompletas);
                android.util.Log.d("EjercicioRutinaAdapter",
                    "✓ Guardando " + seriesCompletas.size() + " series completadas para ejercicio " + exerciseId);
            }
        }

        android.util.Log.d("EjercicioRutinaAdapter",
            "Total ejercicios con series completadas: " + seriesCompletadas.size());
        return seriesCompletadas;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewNombre;
        private final TextView textViewMusculos;
        private final ImageView imageViewEjercicio;
        private final LinearLayout layoutSeries;
        private final Button buttonAgregarSerie;

        // NUEVO: Variables para el recuadro de eliminación
        private final View layoutEliminarSerie;
        private final TextView textViewMensajeEliminar;
        private final Button buttonCancelarEliminar;
        private final Button buttonConfirmarEliminar;
        private Serie serieAEliminar = null;
        private int numeroSerieAEliminar = -1;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNombre = itemView.findViewById(R.id.textViewNombreEjercicio);
            textViewMusculos = itemView.findViewById(R.id.textViewMusculos);
            imageViewEjercicio = itemView.findViewById(R.id.imageViewEjercicio);
            layoutSeries = itemView.findViewById(R.id.layoutSeries);
            buttonAgregarSerie = itemView.findViewById(R.id.buttonAgregarSerie);

            // NUEVO: Inicializar componentes del recuadro de eliminación
            layoutEliminarSerie = itemView.findViewById(R.id.layoutEliminarSerie);
            textViewMensajeEliminar = layoutEliminarSerie.findViewById(R.id.textViewMensajeEliminar);
            buttonCancelarEliminar = layoutEliminarSerie.findViewById(R.id.buttonCancelarEliminar);
            buttonConfirmarEliminar = layoutEliminarSerie.findViewById(R.id.buttonConfirmarEliminar);

            // Configurar listeners para el recuadro de eliminación
            configurarRecuadroEliminacion();
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
                textViewMusculos.setText(R.string.musculos_no_especificados);
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

                agregarSerie(exerciseId, exercise);
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
                    "❌ Claves disponibles en mapa: " + seriesPorEjercicio.keySet());
                android.util.Log.w("EjercicioRutinaAdapter",
                    "❌ Buscando clave: " + exerciseId + " (tipo: " + exerciseId.getClass().getSimpleName() + ")");
            }
        }

        private View createSerieView(Serie serie, int numeroSerie) {
            // Obtener el ID del ejercicio para buscar historial
            Integer exerciseId = null;
            for (Exercise ex : exercises) {
                List<Serie> seriesEjercicio = seriesPorEjercicio.get(ex.getId());
                if (seriesEjercicio != null && seriesEjercicio.contains(serie)) {
                    exerciseId = ex.getId();
                    break;
                }
            }

            final Integer finalExerciseId = exerciseId;

            View view = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_serie_con_historial, layoutSeries, false);

            TextView textViewNumero = view.findViewById(R.id.textViewNumeroSerie);
            TextView textViewPesoHistorico = view.findViewById(R.id.textViewPesoHistorico);
            TextView textViewRepsHistorico = view.findViewById(R.id.textViewRepsHistorico);
            EditText editTextPeso = view.findViewById(R.id.editTextPeso);
            EditText editTextReps = view.findViewById(R.id.editTextRepeticiones);
            CheckBox checkBoxCompletada = view.findViewById(R.id.checkBoxCompletada);

            textViewNumero.setText(String.valueOf(numeroSerie));

            // NUEVO: Configurar click en el número de serie para eliminar
            textViewNumero.setOnClickListener(v -> {
                android.util.Log.d("EjercicioRutinaAdapter",
                    "Click en número de serie " + numeroSerie + " para eliminar");

                if (finalExerciseId != null) {
                    List<Serie> series = seriesPorEjercicio.get(finalExerciseId);
                    if (series != null && series.size() > 3) { // Siempre permitir mínimo 3 series
                        mostrarRecuadroEliminarSerie(serie, numeroSerie, finalExerciseId);
                    } else {
                        android.widget.Toast.makeText(itemView.getContext(),
                            R.string.no_puedes_eliminar_series,
                            android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Hacer el número de serie más visible para click
            textViewNumero.setBackgroundResource(android.R.drawable.btn_default);
            textViewNumero.setClickable(true);
            textViewNumero.setFocusable(true);

            // CORREGIDO: Lógica mejorada para mostrar historial como referencia
            if (finalExerciseId != null && historialPorEjercicio.containsKey(finalExerciseId)) {
                List<SerieHistorial> historial = historialPorEjercicio.get(finalExerciseId);

                // NUEVA LÓGICA: Buscar específicamente el historial para este número de serie
                SerieHistorial serieHistoricaEspecifica = null;
                if (historial != null && !historial.isEmpty()) {
                    for (SerieHistorial serieHist : historial) {
                        if (serieHist.getNumeroSerie() == numeroSerie) {
                            serieHistoricaEspecifica = serieHist;
                            break;
                        }
                    }
                }

                if (serieHistoricaEspecifica != null) {
                    textViewPesoHistorico.setText(String.format(Locale.getDefault(), "%.1f lbs", serieHistoricaEspecifica.getPeso()));
                    textViewRepsHistorico.setText(String.format(Locale.getDefault(), "%d reps", serieHistoricaEspecifica.getRepeticiones()));
                    textViewPesoHistorico.setVisibility(View.VISIBLE);
                    textViewRepsHistorico.setVisibility(View.VISIBLE);

                    android.util.Log.d("EjercicioRutinaAdapter",
                        "Serie " + numeroSerie + " - Mostrando historial específico: " + serieHistoricaEspecifica.getPeso() + " lbs, " + serieHistoricaEspecifica.getRepeticiones() + " reps (numero_serie=" + serieHistoricaEspecifica.getNumeroSerie() + ")");
                } else {
                    textViewPesoHistorico.setText(R.string.peso_placeholder);
                    textViewRepsHistorico.setText(R.string.reps_placeholder);
                    textViewPesoHistorico.setVisibility(View.VISIBLE);
                    textViewRepsHistorico.setVisibility(View.VISIBLE);

                    android.util.Log.d("EjercicioRutinaAdapter",
                        "Serie " + numeroSerie + " - Sin historial específico para esta serie (disponibles: " +
                        (historial != null ? historial.size() : 0) + " entradas)");
                }
            } else {
                textViewPesoHistorico.setText(R.string.peso_placeholder);
                textViewRepsHistorico.setText(R.string.reps_placeholder);
                textViewPesoHistorico.setVisibility(View.VISIBLE);
                textViewRepsHistorico.setVisibility(View.VISIBLE);
            }

            // CRÍTICO: Configurar valores actuales (editables) - mantener los datos ya ingresados
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
                            R.string.ingresa_peso_repeticiones_validos,
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

        // MODIFICADO: Actualizar configuración cuando se agrega una serie
        private void agregarSerie(Integer exerciseId, Exercise exercise) {
            List<Serie> series = seriesPorEjercicio.get(exerciseId);
            if (series != null) {
                // CRÍTICO: Simplemente agregar una nueva serie vacía
                series.add(new Serie());

                // NUEVO: Actualizar configuración de series
                int numNuevasSeries = series.size();
                numSeriesConfiguradas.put(exerciseId, numNuevasSeries);

                // Guardar configuración en base de datos
                repository.guardarConfiguracionSeries(exerciseId, numNuevasSeries);

                android.util.Log.d("EjercicioRutinaAdapter",
                    "Serie agregada a " + exercise.getName() + ". Total series: " + numNuevasSeries +
                    ". Configuración actualizada y guardada.");

                // Refrescar la vista para mostrar la nueva serie
                setupSeries(exercise);
            }
        }

        // NUEVO: Método para mostrar el recuadro de eliminación
        private void mostrarRecuadroEliminarSerie(Serie serie, int numeroSerie, Integer exerciseId) {
            android.util.Log.d("EjercicioRutinaAdapter",
                "Mostrando recuadro para eliminar serie " + numeroSerie);

            // Guardar la serie y número a eliminar
            serieAEliminar = serie;
            numeroSerieAEliminar = numeroSerie;

            // Actualizar mensaje del recuadro
            textViewMensajeEliminar.setText(itemView.getContext().getString(R.string.deseas_eliminar_serie, numeroSerie));

            // Mostrar el recuadro
            layoutEliminarSerie.setVisibility(View.VISIBLE);

            android.util.Log.d("EjercicioRutinaAdapter",
                "Recuadro de eliminación mostrado para serie " + numeroSerie);
        }

        // NUEVO: Método para configurar el recuadro de eliminación
        private void configurarRecuadroEliminacion() {
            // Ocultar por defecto
            layoutEliminarSerie.setVisibility(View.GONE);

            buttonCancelarEliminar.setOnClickListener(v -> {
                android.util.Log.d("EjercicioRutinaAdapter", "Eliminación cancelada por el usuario");
                layoutEliminarSerie.setVisibility(View.GONE);
                serieAEliminar = null;
                numeroSerieAEliminar = -1;
            });

            buttonConfirmarEliminar.setOnClickListener(v -> {
                android.util.Log.d("EjercicioRutinaAdapter", "Confirmando eliminación de serie " + numeroSerieAEliminar);

                if (serieAEliminar != null && numeroSerieAEliminar != -1) {
                    // Encontrar el ejercicio y eliminar la serie
                    Integer exerciseId = null;
                    Exercise exercise = null;

                    // Buscar el ejercicio que contiene esta serie
                    for (Exercise ex : exercises) {
                        List<Serie> series = seriesPorEjercicio.get(ex.getId());
                        if (series != null && series.contains(serieAEliminar)) {
                            exerciseId = ex.getId();
                            exercise = ex;
                            break;
                        }
                    }

                    if (exerciseId != null && exercise != null) {
                        eliminarSerie(exerciseId, serieAEliminar, exercise);
                    } else {
                        android.util.Log.e("EjercicioRutinaAdapter", "No se pudo encontrar el ejercicio para la serie");
                    }

                    // Ocultar recuadro y limpiar variables
                    layoutEliminarSerie.setVisibility(View.GONE);
                    serieAEliminar = null;
                    numeroSerieAEliminar = -1;
                } else {
                    android.util.Log.w("EjercicioRutinaAdapter", "No hay serie seleccionada para eliminar");
                }
            });
        }

        // NUEVO: Método para eliminar una serie
        private void eliminarSerie(int exerciseId, Serie serie, Exercise exercise) {
            List<Serie> series = seriesPorEjercicio.get(exerciseId);
            if (series == null) {
                android.util.Log.e("EjercicioRutinaAdapter", "No se encontraron series para el ejercicio " + exerciseId);
                return;
            }

            int indiceSerie = series.indexOf(serie);
            if (indiceSerie == -1) {
                android.util.Log.e("EjercicioRutinaAdapter", "Serie no encontrada en la lista");
                return;
            }

            // Verificar que podemos eliminar (mínimo 3 series)
            if (series.size() <= 3) {
                android.widget.Toast.makeText(itemView.getContext(),
                    R.string.no_se_puede_eliminar_minimo_series,
                    android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // Recalcular volumen si la serie estaba completada
            if (serie.isCompletada()) {
                android.util.Log.d("EjercicioRutinaAdapter",
                    "Eliminando serie completada - Volumen a restar: " + serie.getVolumen());
            }

            // Eliminar la serie
            series.remove(indiceSerie);

            // NUEVO: Actualizar configuración de series
            int numNuevasSeries = series.size();
            numSeriesConfiguradas.put(exerciseId, numNuevasSeries);
            repository.guardarConfiguracionSeries(exerciseId, numNuevasSeries);

            android.util.Log.d("EjercicioRutinaAdapter",
                "✓ Serie eliminada. Total series restantes: " + series.size() +
                ". Configuración actualizada.");

            // Refrescar vista para que se reorganicen los números
            setupSeries(exercise);

            // Recalcular volumen después de eliminar
            calcularVolumenTotal();

            android.widget.Toast.makeText(itemView.getContext(),
                R.string.serie_eliminada_reorganizadas, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
