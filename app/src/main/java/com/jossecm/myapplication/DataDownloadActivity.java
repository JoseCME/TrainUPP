package com.jossecm.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.models.Muscle;
import com.jossecm.myapplication.models.User;
import com.jossecm.myapplication.repository.FitnessRepository;
import java.util.List;

public class DataDownloadActivity extends AppCompatActivity {
    private static final String TAG = "DataDownloadActivity";

    private TextView tvDownloadStatus;
    private TextView tvProgressText;
    private TextView tvProgressDetail;
    private ProgressBar progressBar;
    private FitnessRepository repository;
    private User currentUser;

    private int totalSteps = 3; // Músculos, Ejercicios, Finalización
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar barras del sistema para evitar solapamiento
        setupSystemBars();

        setContentView(R.layout.activity_data_download);

        initViews();
        repository = new FitnessRepository(this);

        // Obtener el usuario actual
        getCurrentUser();
    }

    private void setupSystemBars() {
        // Configurar para que el contenido no se solape con las barras del sistema
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                getWindow().getDecorView().getSystemUiVisibility()
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    private void initViews() {
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus);
        tvProgressText = findViewById(R.id.tvProgressText);
        tvProgressDetail = findViewById(R.id.tvProgressDetail);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setMax(100);
    }

    private void getCurrentUser() {
        repository.getCurrentUser(new FitnessRepository.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                startDataDownload();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error obteniendo usuario: " + error);
                showError("Error obteniendo datos de usuario");
            }
        });
    }

    private void startDataDownload() {
        Log.d(TAG, "Iniciando descarga de datos");

        updateProgress(0, "Preparando descarga...", "Inicializando proceso de descarga");

        // Paso 1: Descargar músculos
        downloadMuscles();
    }

    private void downloadMuscles() {
        currentStep = 1;
        updateProgress(10, "Descargando músculos...", "Obteniendo información de músculos desde wger.de");

        repository.loadMusclesFromApi(new FitnessRepository.DataCallback<List<Muscle>>() {
            @Override
            public void onSuccess(List<Muscle> muscles) {
                Log.d(TAG, "Músculos descargados: " + muscles.size());
                runOnUiThread(() -> {
                    updateProgress(33, "Músculos descargados",
                        "Descargados " + muscles.size() + " músculos correctamente");

                    // Esperar un poco para que el usuario vea el progreso
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        downloadExercises();
                    }, 1000);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error descargando músculos: " + error);
                runOnUiThread(() -> showError("Error descargando músculos: " + error));
            }
        });
    }

    private void downloadExercises() {
        currentStep = 2;
        updateProgress(40, "Descargando ejercicios...",
            "Obteniendo ejercicios para tu equipamiento seleccionado");

        if (currentUser == null || currentUser.getSelectedEquipmentIds() == null) {
            showError("No se encontró información de equipamiento seleccionado");
            return;
        }

        Log.d(TAG, "Equipamientos seleccionados: " + currentUser.getSelectedEquipmentIds().toString());

        repository.loadExercisesFromApi(currentUser.getSelectedEquipmentIds(),
            new FitnessRepository.DataCallback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> exercises) {
                Log.d(TAG, "Ejercicios descargados: " + exercises.size());

                // Verificar que realmente se guardaron en la BD local
                verifyDataSaved(exercises.size());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error descargando ejercicios: " + error);
                runOnUiThread(() -> showError("Error descargando ejercicios: " + error));
            }
        });
    }

    private void verifyDataSaved(int expectedExercises) {
        // Verificar que los datos se guardaron correctamente en la BD local
        repository.getAllExercises(new FitnessRepository.DataCallback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> localExercises) {
                Log.d(TAG, "Ejercicios verificados en BD local: " + localExercises.size());

                runOnUiThread(() -> {
                    if (localExercises.size() > 0) {
                        updateProgress(90, "Ejercicios guardados localmente",
                            "Guardados " + localExercises.size() + " ejercicios para uso offline");

                        // Verificar nombres de músculos
                        int exercisesWithMuscleNames = 0;
                        for (Exercise ex : localExercises) {
                            if (ex.getMuscleNames() != null && !ex.getMuscleNames().isEmpty()) {
                                exercisesWithMuscleNames++;
                            }
                        }

                        Log.d(TAG, "Ejercicios con nombres de músculos: " + exercisesWithMuscleNames);

                        // Finalizar proceso
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            finishDownload(localExercises.size());
                        }, 1500);
                    } else {
                        showError("No se guardaron ejercicios en la base de datos local");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error verificando datos locales: " + error);
                runOnUiThread(() -> showError("Error verificando datos guardados: " + error));
            }
        });
    }

    private void finishDownload(int exerciseCount) {
        currentStep = 3;
        updateProgress(100, "¡Descarga completa!",
            "Todos los datos guardados localmente. " + exerciseCount + " ejercicios disponibles offline");

        // Esperar un momento y luego ir a MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(DataDownloadActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 2000);
    }

    private void updateProgress(int percentage, String status, String detail) {
        progressBar.setProgress(percentage);
        tvProgressText.setText(percentage + "%");
        tvDownloadStatus.setText(status);
        tvProgressDetail.setText(detail);

        Log.d(TAG, "Progreso: " + percentage + "% - " + status);
    }

    private void showError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        tvDownloadStatus.setText("Error en la descarga");
        tvProgressDetail.setText(error);

        // Opción para reintentar o continuar
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(DataDownloadActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 3000);
    }

    @Override
    public void onBackPressed() {
        // Deshabilitar botón de atrás durante la descarga
        // super.onBackPressed();
        Toast.makeText(this, "Espera a que termine la descarga...", Toast.LENGTH_SHORT).show();
    }
}
