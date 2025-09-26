package com.jossecm.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.jossecm.myapplication.adapters.ExerciseAdapter;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.repository.FitnessRepository;
import java.util.ArrayList;
import java.util.List;

public class ExerciseListActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewExercises;
    private LinearLayout layoutLoading, layoutEmpty;
    private ExerciseAdapter exerciseAdapter;
    private FitnessRepository repository;
    private List<Exercise> exerciseList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadExercises();
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
    }

    private void setupRecyclerView() {
        exerciseAdapter = new ExerciseAdapter(exerciseList);
        recyclerViewExercises.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewExercises.setAdapter(exerciseAdapter);
    }

    private void loadExercises() {
        showLoading();

        // Primero intentar cargar desde la base de datos local (OFFLINE)
        repository.getAllExercises(new FitnessRepository.DataCallback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> exercises) {
                runOnUiThread(() -> {
                    if (exercises != null && !exercises.isEmpty()) {
                        exerciseList.clear();
                        exerciseList.addAll(exercises);
                        exerciseAdapter.notifyDataSetChanged();
                        showContent();

                        // Actualizar título con número de ejercicios
                        toolbar.setTitle("Ejercicios (" + exercises.size() + ")");

                        // Log para debugging
                        android.util.Log.d("ExerciseListActivity",
                            "Ejercicios cargados desde BD local: " + exercises.size());

                        // Mostrar información del primer ejercicio para debugging
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
