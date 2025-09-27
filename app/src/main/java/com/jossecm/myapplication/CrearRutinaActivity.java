package com.jossecm.myapplication;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.jossecm.myapplication.adapters.ExerciseSelectionAdapter;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.models.Rutina;
import com.jossecm.myapplication.repository.FitnessRepository;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class CrearRutinaActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewExercises;
    private LinearLayout layoutLoading, layoutEmpty;
    private ExerciseSelectionAdapter exerciseAdapter;
    private FitnessRepository repository;
    private List<Exercise> exerciseList = new ArrayList<>();
    private List<Exercise> selectedExercises = new ArrayList<>();

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
