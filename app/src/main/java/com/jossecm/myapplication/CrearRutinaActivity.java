package com.jossecm.myapplication;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.jossecm.myapplication.adapters.ExerciseSelectionAdapter;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.models.Muscle;
import com.jossecm.myapplication.models.Rutina;
import com.jossecm.myapplication.repository.FitnessRepository;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class CrearRutinaActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewExercises;
    private LinearLayout layoutLoading, layoutEmpty;
    private TextInputEditText etSearchExercises;
    private AutoCompleteTextView spinnerMuscleFilter;

    private ExerciseSelectionAdapter exerciseAdapter;
    private FitnessRepository repository;
    private List<Exercise> exerciseList = new ArrayList<>();
    private List<Exercise> allExercisesList = new ArrayList<>(); // Lista completa para filtrado
    private List<Muscle> muscleList = new ArrayList<>();
    private List<Exercise> selectedExercises = new ArrayList<>();

    // Variables para filtros actuales
    private String currentSearchQuery = "";
    private int currentMuscleId = 0; // 0 = sin filtro

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar barras del sistema para respetar barra de tareas
        setupSystemBars();

        setContentView(R.layout.activity_crear_rutina);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadExercises();
        setupSearchAndFilter();
    }

    private void setupSystemBars() {
        // Configurar para que el contenido RESPETE las barras del sistema
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
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
        updateToolbarTitle();
    }

    private void setupRecyclerView() {
        exerciseAdapter = new ExerciseSelectionAdapter(exerciseList);
        exerciseAdapter.setOnSelectionChangedListener(new ExerciseSelectionAdapter.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(Exercise exercise, boolean isSelected) {
                if (isSelected) {
                    selectedExercises.add(exercise);
                } else {
                    selectedExercises.remove(exercise);
                }
                updateToolbarTitle();
            }
        });

        recyclerViewExercises.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewExercises.setAdapter(exerciseAdapter);
    }

    private void updateToolbarTitle() {
        toolbar.setTitle("Crear (" + selectedExercises.size() + " ejercicios)");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_crear_rutina, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_crear) {
            mostrarDialogoCrearRutina();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void mostrarDialogoCrearRutina() {
        if (selectedExercises.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un ejercicio", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Crear nueva rutina");

        final EditText input = new EditText(this);
        input.setHint("Nombre de la rutina");
        builder.setView(input);

        builder.setPositiveButton("Crear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nombreRutina = input.getText().toString().trim();
                if (!nombreRutina.isEmpty()) {
                    crearRutina(nombreRutina);
                } else {
                    Toast.makeText(CrearRutinaActivity.this,
                                 "Ingresa un nombre para la rutina", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void crearRutina(String nombre) {
        try {
            // Convertir lista de ejercicios seleccionados a JSON
            JSONArray jsonArray = new JSONArray();
            for (Exercise exercise : selectedExercises) {
                jsonArray.put(exercise.getId());
            }

            Rutina rutina = new Rutina(nombre, jsonArray.toString(), selectedExercises.size());

            repository.insertRutina(rutina, new FitnessRepository.DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(CrearRutinaActivity.this,
                                         "Rutina creada exitosamente", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(CrearRutinaActivity.this,
                                         "Error creando rutina", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        android.util.Log.e("CrearRutinaActivity", "Error creando rutina: " + error);
                        Toast.makeText(CrearRutinaActivity.this,
                                     "Error creando rutina: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (Exception e) {
            android.util.Log.e("CrearRutinaActivity", "Error creando JSON", e);
            Toast.makeText(this, "Error procesando ejercicios seleccionados", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadExercises() {
        showLoading();

        repository.getAllExercises(new FitnessRepository.DataCallback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> exercises) {
                runOnUiThread(() -> {
                    if (exercises != null && !exercises.isEmpty()) {
                        exerciseList.clear();
                        exerciseList.addAll(exercises);
                        allExercisesList.clear();
                        allExercisesList.addAll(exercises);
                        exerciseAdapter.notifyDataSetChanged();
                        showContent();

                        // Log para debugging
                        android.util.Log.d("CrearRutinaActivity",
                            "Ejercicios cargados: " + exercises.size());
                    } else {
                        showEmpty();
                        android.util.Log.d("CrearRutinaActivity",
                            "No hay ejercicios disponibles");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e("CrearRutinaActivity",
                        "Error cargando ejercicios: " + error);
                    Toast.makeText(CrearRutinaActivity.this,
                                 "Error cargando ejercicios: " + error, Toast.LENGTH_LONG).show();
                    showEmpty();
                });
            }
        });
    }

    private void setupSearchAndFilter() {
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

        // Cargar músculos para el filtro
        loadMuscles();
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

                        android.util.Log.d("CrearRutinaActivity",
                            "Músculos cargados para filtro: " + muscles.size());
                    } else {
                        android.util.Log.d("CrearRutinaActivity",
                            "No hay músculos disponibles para filtro");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e("CrearRutinaActivity",
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
        List<Exercise> filteredList = new ArrayList<>();

        for (Exercise exercise : allExercisesList) {
            boolean matchesName = true;
            boolean matchesMuscle = true;

            // Filtro por nombre
            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                matchesName = exercise.getName() != null &&
                             exercise.getName().toLowerCase().contains(currentSearchQuery.toLowerCase());
            }

            // Filtro por músculo
            if (currentMuscleId > 0) {
                matchesMuscle = false;
                if (exercise.getMuscleIds() != null) {
                    for (Integer exMuscleId : exercise.getMuscleIds()) {
                        if (exMuscleId.equals(currentMuscleId)) {
                            matchesMuscle = true;
                            break;
                        }
                    }
                }
            }

            if (matchesName && matchesMuscle) {
                filteredList.add(exercise);
            }
        }

        exerciseList.clear();
        exerciseList.addAll(filteredList);
        exerciseAdapter.notifyDataSetChanged();

        // Actualizar título con resultados
        updateToolbarWithResults();

        // Mostrar/ocultar estados según resultados
        if (exerciseList.isEmpty() && !allExercisesList.isEmpty()) {
            showEmptyResults();
        } else if (!exerciseList.isEmpty()) {
            showContent();
        }

        android.util.Log.d("CrearRutinaActivity", "Filtros aplicados: '" + currentSearchQuery +
                          "' + músculo " + currentMuscleId + " - Mostrando " + filteredList.size() +
                          " de " + allExercisesList.size());
    }

    private void updateToolbarWithResults() {
        int selectedCount = selectedExercises.size();
        int filteredCount = exerciseList.size();
        int totalCount = allExercisesList.size();

        String titleText;
        if (filteredCount == totalCount) {
            titleText = "Crear (" + selectedCount + " ejercicios)";
        } else {
            titleText = "Crear (" + selectedCount + " de " + filteredCount + "/" + totalCount + ")";
        }

        toolbar.setTitle(titleText);
    }

    private void showEmptyResults() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewExercises.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);

        Toast.makeText(this, "No se encontraron ejercicios con los filtros aplicados",
                      Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
