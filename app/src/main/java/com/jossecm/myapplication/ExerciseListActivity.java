package com.jossecm.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.jossecm.myapplication.adapters.ExerciseAdapter;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.models.Muscle;
import com.jossecm.myapplication.repository.FitnessRepository;
import java.util.ArrayList;
import java.util.List;

public class ExerciseListActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewExercises;
    private LinearLayout layoutLoading, layoutEmpty;
    private TextInputEditText etSearchExercises;
    private AutoCompleteTextView spinnerMuscleFilter;

    private ExerciseAdapter exerciseAdapter;
    private FitnessRepository repository;
    private List<Exercise> exerciseList = new ArrayList<>();
    private List<Muscle> muscleList = new ArrayList<>();

    // Variables para filtros actuales
    private String currentSearchQuery = "";
    private int currentMuscleId = 0; // 0 = sin filtro

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchAndFilters();
        loadExercises();
        loadMuscles();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewExercises = findViewById(R.id.recyclerViewExercises);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        etSearchExercises = findViewById(R.id.etSearchExercises);
        spinnerMuscleFilter = findViewById(R.id.spinnerMuscleFilter);

        repository = new FitnessRepository(this);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        exerciseAdapter = new ExerciseAdapter(exerciseList);
        recyclerViewExercises.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewExercises.setAdapter(exerciseAdapter);
    }

    private void setupSearchAndFilters() {
        // Configurar búsqueda en tiempo real
        etSearchExercises.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim();
                applyFilters();
            }
        });

        // Configurar filtro por músculo
        spinnerMuscleFilter.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                // Primera opción = "Todos los músculos"
                currentMuscleId = 0;
            } else {
                // Obtener el músculo seleccionado (position - 1 porque la primera opción es "Todos")
                if (muscleList.size() > (position - 1)) {
                    currentMuscleId = muscleList.get(position - 1).getId();
                }
            }
            applyFilters();
        });
    }

    private void loadExercises() {
        showLoading();

        // Cargar ejercicios desde la base de datos local
        repository.getAllExercises(new FitnessRepository.DataCallback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> exercises) {
                runOnUiThread(() -> {
                    if (exercises != null && !exercises.isEmpty()) {
                        exerciseList.clear();
                        exerciseList.addAll(exercises);

                        // Actualizar el adaptador con la nueva lista
                        exerciseAdapter.updateExercises(exerciseList);
                        showContent();

                        // Actualizar título con número de ejercicios
                        updateTitle();

                        android.util.Log.d("ExerciseListActivity",
                            "Ejercicios cargados desde BD local: " + exercises.size());

                        // Log del primer ejercicio para debugging
                        if (!exercises.isEmpty()) {
                            Exercise firstExercise = exercises.get(0);
                            android.util.Log.d("ExerciseListActivity",
                                "Primer ejercicio - Nombre: " + firstExercise.getName() +
                                ", Músculos: " + firstExercise.getMuscleNames() +
                                ", Imagen: " + firstExercise.getImageUrl());
                        }
                    } else {
                        showEmpty();
                        android.util.Log.d("ExerciseListActivity",
                            "No hay ejercicios en la base de datos local");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e("ExerciseListActivity",
                        "Error cargando ejercicios: " + error);
                    Toast.makeText(ExerciseListActivity.this,
                                 "Error cargando ejercicios: " + error, Toast.LENGTH_LONG).show();
                    showEmpty();
                });
            }
        });
    }

    private void loadMuscles() {
        // Cargar músculos disponibles para el filtro
        repository.getMusclesFromExercises(new FitnessRepository.DataCallback<List<Muscle>>() {
            @Override
            public void onSuccess(List<Muscle> muscles) {
                runOnUiThread(() -> {
                    if (muscles != null && !muscles.isEmpty()) {
                        muscleList.clear();
                        muscleList.addAll(muscles);
                        setupMuscleSpinner();

                        android.util.Log.d("ExerciseListActivity",
                            "Músculos cargados para filtro: " + muscles.size());
                    } else {
                        android.util.Log.d("ExerciseListActivity",
                            "No hay músculos disponibles para filtro");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e("ExerciseListActivity",
                        "Error cargando músculos: " + error);
                    // No mostrar error al usuario, solo log
                });
            }
        });
    }

    private void setupMuscleSpinner() {
        List<String> muscleNames = new ArrayList<>();
        muscleNames.add("Todos los músculos"); // Opción por defecto

        for (Muscle muscle : muscleList) {
            // Usar el nombre en inglés si está disponible, sino el nombre normal
            String muscleName = (muscle.getNameEn() != null && !muscle.getNameEn().isEmpty())
                ? muscle.getNameEn() : muscle.getName();
            muscleNames.add(muscleName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            muscleNames
        );

        spinnerMuscleFilter.setAdapter(adapter);
        spinnerMuscleFilter.setText(muscleNames.get(0), false); // Seleccionar "Todos" por defecto
    }

    private void applyFilters() {
        if (exerciseAdapter != null) {
            // Aplicar filtros combinados usando el método del adaptador
            exerciseAdapter.applyFilters(currentSearchQuery, currentMuscleId);

            // Actualizar título con resultados
            updateTitle();

            // Mostrar/ocultar estados según resultados
            if (exerciseAdapter.getFilteredCount() == 0 && exerciseAdapter.getTotalCount() > 0) {
                showEmptyResults();
            } else if (exerciseAdapter.getFilteredCount() > 0) {
                showContent();
            }
        }
    }

    private void updateTitle() {
        if (exerciseAdapter != null) {
            int filteredCount = exerciseAdapter.getFilteredCount();
            int totalCount = exerciseAdapter.getTotalCount();

            String titleText;
            if (filteredCount == totalCount) {
                titleText = "Ejercicios (" + totalCount + ")";
            } else {
                titleText = "Ejercicios (" + filteredCount + "/" + totalCount + ")";
            }

            toolbar.setTitle(titleText);
        }
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        recyclerViewExercises.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewExercises.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmpty() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewExercises.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void showEmptyResults() {
        // Crear un layout temporal para mostrar "Sin resultados"
        layoutLoading.setVisibility(View.GONE);
        recyclerViewExercises.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);

        // Podrías personalizar el mensaje aquí si quisieras
        Toast.makeText(this, "No se encontraron ejercicios con los filtros aplicados",
                      Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
