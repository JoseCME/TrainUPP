package com.jossecm.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jossecm.myapplication.adapters.EjercicioRutinaAdapter;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.models.HistorialEntrenamiento;
import com.jossecm.myapplication.models.Rutina;
import com.jossecm.myapplication.models.Serie;
import com.jossecm.myapplication.repository.FitnessRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EjecutarRutinaActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView textViewCronometro;
    private TextView textViewVolumenTotal;
    private Button buttonFinalizarRutina;
    private RecyclerView recyclerViewEjercicios;
    private LinearLayout layoutLoading, layoutEmpty;
    private FloatingActionButton fabChatIA;  // NUEVO: FAB para chat de IA

    private EjercicioRutinaAdapter ejercicioAdapter;
    private FitnessRepository repository;
    private List<Exercise> exerciseList = new ArrayList<>();

    private Rutina rutina;
    private long rutinaId;
    private Handler handler;
    private Runnable cronometroRunnable;
    private long tiempoInicioMs;
    private int tiempoTranscurridoSegundos = 0;
    private double volumenTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rutinaId = getIntent().getLongExtra("rutina_id", -1);
        if (rutinaId == -1) {
            Toast.makeText(this, "Error: ID de rutina inválido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configurar barras del sistema para respetar barra de tareas
        setupSystemBars();

        setContentView(R.layout.activity_ejecutar_rutina);

        initViews();
        setupToolbar();
        setupCronometro();
        setupRecyclerView();
        loadRutina();
    }

    private void setupSystemBars() {
        // CORREGIDO para tablets: Configurar WindowInsets adecuadamente
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Para Android 11+ (API 30+)
            getWindow().setDecorFitsSystemWindows(false);

            findViewById(android.R.id.content).setOnApplyWindowInsetsListener((v, insets) -> {
                androidx.core.graphics.Insets systemInsets = androidx.core.graphics.Insets.toCompatInsets(
                        insets.getInsets(android.view.WindowInsets.Type.systemBars())
                );

                // Aplicar padding al contenido principal para evitar solapamiento
                v.setPadding(systemInsets.left, systemInsets.top, systemInsets.right, systemInsets.bottom);

                android.util.Log.d("EjecutarRutinaActivity",
                        "WindowInsets aplicados - Left: " + systemInsets.left +
                                ", Top: " + systemInsets.top +
                                ", Right: " + systemInsets.right +
                                ", Bottom: " + systemInsets.bottom);

                return insets;
            });
        } else {
            // Para versiones anteriores a Android 11 (API < 30)
            // Usar flags del sistema para un comportamiento compatible
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        textViewCronometro = findViewById(R.id.textViewCronometro);
        textViewVolumenTotal = findViewById(R.id.textViewVolumenTotal);
        buttonFinalizarRutina = findViewById(R.id.buttonFinalizarRutina);
        recyclerViewEjercicios = findViewById(R.id.recyclerViewEjercicios);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        fabChatIA = findViewById(R.id.fabChatIA);
        FloatingActionButton fabVideosYouTube = findViewById(R.id.fabVideosYouTube); // NUEVO: FAB para videos de YouTube

        repository = new FitnessRepository(this);

        // NUEVO: Configurar click listener para videos de YouTube
        fabVideosYouTube.setOnClickListener(v -> {
            Intent intent = new Intent(EjecutarRutinaActivity.this, BuscarVideosYouTubeActivity.class);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            mostrarDialogoSalir();
        });
    }

    private void setupCronometro() {
        handler = new Handler();
        tiempoInicioMs = System.currentTimeMillis();

        cronometroRunnable = new Runnable() {
            @Override
            public void run() {
                tiempoTranscurridoSegundos = (int) ((System.currentTimeMillis() - tiempoInicioMs) / 1000);
                actualizarCronometro();
                handler.postDelayed(this, 1000); // Ejecutar cada segundo
            }
        };

        // Iniciar cronómetro automáticamente
        handler.post(cronometroRunnable);

        // Inicializar volumen total
        actualizarVolumenTotal();
    }

    private void actualizarCronometro() {
        int minutos = tiempoTranscurridoSegundos / 60;
        int segundos = tiempoTranscurridoSegundos % 60;
        String tiempoFormateado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos);
        textViewCronometro.setText(tiempoFormateado);
    }

    private void actualizarVolumenTotal() {
        textViewVolumenTotal.setText(String.format(Locale.getDefault(), "%.1f lbs", volumenTotal));
    }

    private void setupRecyclerView() {
        // CORREGIDO: Pasar el repository al constructor del adapter
        ejercicioAdapter = new EjercicioRutinaAdapter(exerciseList, repository);
        ejercicioAdapter.setOnVolumenChangedListener(new EjercicioRutinaAdapter.OnVolumenChangedListener() {
            @Override
            public void onVolumenChanged(double nuevoVolumenTotal) {
                volumenTotal = nuevoVolumenTotal;
                actualizarVolumenTotal();

                android.util.Log.d("EjecutarRutinaActivity",
                        "Volumen actualizado: " + volumenTotal + " lbs");
            }
        });

        recyclerViewEjercicios.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEjercicios.setAdapter(ejercicioAdapter);

        buttonFinalizarRutina.setOnClickListener(v -> mostrarDialogoFinalizar());

        // NUEVO: Configurar FloatingActionButton del chat de IA
        fabChatIA.setOnClickListener(v -> {
            // Abrir ChatIAActivity con contexto de rutina actual
            abrirChatConContextoRutina();
        });
    }

    private void loadRutina() {
        showLoading();

        repository.getRutinaById(rutinaId, new FitnessRepository.DataCallback<Rutina>() {
            @Override
            public void onSuccess(Rutina rutinaResult) {
                runOnUiThread(() -> {
                    if (rutinaResult != null) {
                        rutina = rutinaResult;
                        toolbar.setTitle(rutina.getNombre());
                        loadEjerciciosRutina();
                    } else {
                        showEmpty();
                        android.util.Log.e("EjecutarRutinaActivity", "Rutina no encontrada");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e("EjecutarRutinaActivity", "Error cargando rutina: " + error);
                    Toast.makeText(EjecutarRutinaActivity.this,
                            "Error cargando rutina: " + error, Toast.LENGTH_LONG).show();
                    showEmpty();
                });
            }
        });
    }

    private void loadEjerciciosRutina() {
        try {
            JSONArray ejerciciosIds = new JSONArray(rutina.getEjerciciosIds());
            List<Integer> ids = new ArrayList<>();

            for (int i = 0; i < ejerciciosIds.length(); i++) {
                ids.add(ejerciciosIds.getInt(i));
            }

            repository.getAllExercises(new FitnessRepository.DataCallback<List<Exercise>>() {
                @Override
                public void onSuccess(List<Exercise> allExercises) {
                    runOnUiThread(() -> {
                        exerciseList.clear();

                        // Filtrar solo los ejercicios de la rutina
                        for (Exercise exercise : allExercises) {
                            if (ids.contains((int)exercise.getId())) {
                                exerciseList.add(exercise);
                            }
                        }

                        if (!exerciseList.isEmpty()) {
                            android.util.Log.d("EjecutarRutinaActivity",
                                "Ejercicios filtrados para la rutina: " + exerciseList.size());

                            // DEBUG: Imprimir detalles de los ejercicios
                            for (int i = 0; i < exerciseList.size(); i++) {
                                Exercise ex = exerciseList.get(i);
                                android.util.Log.d("EjecutarRutinaActivity",
                                    "Ejercicio " + i + ": ID=" + ex.getId() + ", Nombre=" + ex.getName());
                            }

                            // CORREGIDO: Usar updateExercises en lugar de notifyDataSetChanged
                            if (ejercicioAdapter != null) {
                                ejercicioAdapter.updateExercises(new ArrayList<>(exerciseList));
                            }
                            showContent();

                            android.util.Log.d("EjecutarRutinaActivity",
                                    "Ejercicios de rutina cargados: " + exerciseList.size());
                        } else {
                            showEmpty();
                            android.util.Log.w("EjecutarRutinaActivity",
                                    "No se encontraron ejercicios para la rutina");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        android.util.Log.e("EjecutarRutinaActivity",
                                "Error cargando ejercicios: " + error);
                        Toast.makeText(EjecutarRutinaActivity.this,
                                "Error cargando ejercicios: " + error, Toast.LENGTH_LONG).show();
                        showEmpty();
                    });
                }
            });

        } catch (Exception e) {
            android.util.Log.e("EjecutarRutinaActivity", "Error procesando JSON de ejercicios", e);
            showEmpty();
        }
    }

    private void mostrarDialogoSalir() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Salir del entrenamiento");
        builder.setMessage("¿Estás seguro de que quieres salir? Se perderá el progreso actual.");

        builder.setPositiveButton("Salir", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        builder.setNegativeButton("Continuar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void mostrarDialogoFinalizar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Finalizar entrenamiento");
        builder.setMessage("¿Deseas finalizar tu entrenamiento?");

        builder.setPositiveButton("Finalizar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finalizarEntrenamiento();
            }
        });

        builder.setNegativeButton("Continuar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void finalizarEntrenamiento() {
        try {
            // NUEVO: Primero guardar las series históricas de cada ejercicio
            Map<Integer, List<Serie>> seriesCompletadas = ejercicioAdapter.getSeriesCompletadas();

            if (seriesCompletadas.isEmpty()) {
                Toast.makeText(this, "No has completado ninguna serie", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar series históricas de cada ejercicio completado
            final int[] ejerciciosGuardados = {0};
            final int totalEjercicios = seriesCompletadas.size();

            for (Map.Entry<Integer, List<Serie>> entry : seriesCompletadas.entrySet()) {
                int exerciseId = entry.getKey();
                List<Serie> series = entry.getValue();

                repository.guardarSeriesEjercicio(exerciseId, series, new FitnessRepository.DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean success) {
                        ejerciciosGuardados[0]++;
                        android.util.Log.d("EjecutarRutinaActivity",
                            "Series guardadas para ejercicio " + exerciseId + ": " + series.size() + " series");

                        // Cuando se hayan guardado todos los ejercicios, guardar el historial general
                        if (ejerciciosGuardados[0] == totalEjercicios) {
                            guardarHistorialGeneral();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("EjecutarRutinaActivity",
                            "Error guardando series para ejercicio " + exerciseId + ": " + error);
                        // Continuar con el siguiente ejercicio aunque falle uno
                        ejerciciosGuardados[0]++;
                        if (ejerciciosGuardados[0] == totalEjercicios) {
                            guardarHistorialGeneral();
                        }
                    }
                });
            }

        } catch (Exception e) {
            android.util.Log.e("EjecutarRutinaActivity", "Error finalizando entrenamiento", e);
            Toast.makeText(this, "Error procesando datos del entrenamiento", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarHistorialGeneral() {
        try {
            // Obtener datos del entrenamiento para el historial general
            // CORREGIDO: Ahora getEjerciciosRealizadosJson() retorna String directamente
            String ejerciciosRealizados = ejercicioAdapter.getEjerciciosRealizadosJson();
            int duracionMinutos = tiempoTranscurridoSegundos / 60;

            android.util.Log.d("EjecutarRutinaActivity",
                "Guardando historial - JSON de ejercicios: " + ejerciciosRealizados);

            HistorialEntrenamiento historial = new HistorialEntrenamiento(
                    rutina.getId(),
                    rutina.getNombre(),
                    duracionMinutos,
                    volumenTotal,
                    ejerciciosRealizados // Ya es String, no necesita .toString()
            );

            repository.insertHistorialEntrenamiento(historial, new FitnessRepository.DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    runOnUiThread(() -> {
                        if (success) {
                            android.util.Log.d("EjecutarRutinaActivity",
                                "Entrenamiento finalizado y guardado correctamente");

                            // NUEVO: Lanzar actividad de celebración en lugar de mostrar toast
                            lanzarActividadCelebracion();
                        } else {
                            Toast.makeText(EjecutarRutinaActivity.this,
                                    "Error guardando entrenamiento", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        android.util.Log.e("EjecutarRutinaActivity",
                                "Error guardando historial: " + error);
                        Toast.makeText(EjecutarRutinaActivity.this,
                                "Error guardando entrenamiento: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (Exception e) {
            android.util.Log.e("EjecutarRutinaActivity", "Error guardando historial general", e);
            Toast.makeText(this, "Error procesando datos del entrenamiento", Toast.LENGTH_SHORT).show();
        }
    }

    // NUEVO: Método para lanzar la actividad de celebración
    private void lanzarActividadCelebracion() {
        try {
            Intent intent = new Intent(this, CelebrationActivity.class);

            // Pasar datos de la rutina completada
            intent.putExtra("rutina_nombre", rutina.getNombre());
            intent.putExtra("duracion_minutos", tiempoTranscurridoSegundos / 60);
            intent.putExtra("volumen_total", volumenTotal);

            android.util.Log.d("EjecutarRutinaActivity",
                "Lanzando actividad de celebración - Rutina: " + rutina.getNombre() +
                ", Duración: " + (tiempoTranscurridoSegundos / 60) + " min" +
                ", Volumen: " + volumenTotal + " lbs");

            startActivity(intent);

            // Animación de transición suave hacia arriba
            overridePendingTransition(R.anim.slide_up, R.anim.fade_out);

            // Finalizar esta actividad después de un pequeño delay
            new Handler().postDelayed(() -> finish(), 300);

        } catch (Exception e) {
            android.util.Log.e("EjecutarRutinaActivity", "Error lanzando actividad de celebración", e);
            // Fallback: mostrar toast y cerrar actividad
            Toast.makeText(this, "✅ Rutina completada y guardada", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        recyclerViewEjercicios.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewEjercicios.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmpty() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewEjercicios.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        // Mostrar diálogo de confirmación al presionar el botón de regreso del sistema
        mostrarDialogoSalir();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detener cronómetro
        if (handler != null && cronometroRunnable != null) {
            handler.removeCallbacks(cronometroRunnable);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        mostrarDialogoSalir();
        return true;
    }

    // NUEVO: Abrir chat con contexto completo de rutina actual
    private void abrirChatConContextoRutina() {
        if (rutina == null || exerciseList.isEmpty()) {
            android.util.Log.w("EjecutarRutinaActivity", "Rutina o ejercicios no disponibles para chat");
            Toast.makeText(this, "Cargando rutina, intenta de nuevo en un momento", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Crear JSON con información completa de ejercicios
            JSONArray ejerciciosArray = new JSONArray();

            for (Exercise exercise : exerciseList) {
                JSONObject ejercicioObj = new JSONObject();
                ejercicioObj.put("id", exercise.getId());
                ejercicioObj.put("name", exercise.getName());

                if (exercise.getDescription() != null) {
                    ejercicioObj.put("description", exercise.getDescription());
                }

                if (exercise.getMuscleNames() != null && !exercise.getMuscleNames().isEmpty()) {
                    JSONArray musclesArray = new JSONArray();
                    for (String muscle : exercise.getMuscleNames()) {
                        musclesArray.put(muscle);
                    }
                    ejercicioObj.put("muscleNames", musclesArray);
                }

                ejerciciosArray.put(ejercicioObj);
            }

            // Crear intent para abrir ChatIAActivity
            Intent intent = new Intent(this, ChatIAActivity.class);

            // Pasar datos completos de la rutina actual
            intent.putExtra("rutina_id", rutina.getId());
            intent.putExtra("rutina_nombre", rutina.getNombre());
            intent.putExtra("ejercicios_json", ejerciciosArray.toString());

            android.util.Log.d("EjecutarRutinaActivity",
                "Abriendo chat con contexto - Rutina: " + rutina.getNombre() +
                ", Ejercicios: " + exerciseList.size());

            // CAMBIADO: Usar startActivityForResult para detectar cuando se agreguen ejercicios
            startActivityForResult(intent, 1001);
            overridePendingTransition(R.anim.slide_up, R.anim.fade_out);

        } catch (Exception e) {
            android.util.Log.e("EjecutarRutinaActivity", "Error creando contexto de rutina para chat", e);
            Toast.makeText(this, "Error preparando información de rutina", Toast.LENGTH_SHORT).show();
        }
    }

    // NUEVO: Método para manejar el resultado del chat de IA
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) { // Código del chat de IA
            if (resultCode == RESULT_OK) {
                // El chat de IA agregó ejercicios, recargar la rutina actual
                android.util.Log.d("EjecutarRutinaActivity", "Chat de IA completado, recargando rutina");
                Toast.makeText(this, "Actualizando rutina con nuevos ejercicios...", Toast.LENGTH_SHORT).show();
                recargarRutinaActualizada();
            }
        }
    }

    // NUEVO: Método para recargar la rutina con los ejercicios actualizados
    private void recargarRutinaActualizada() {
        // Mostrar indicador de carga
        showLoading();

        // Recargar la rutina desde la base de datos para obtener los ejercicios actualizados
        repository.getRutinaById(rutinaId, new FitnessRepository.DataCallback<Rutina>() {
            @Override
            public void onSuccess(Rutina rutinaActualizada) {
                runOnUiThread(() -> {
                    if (rutinaActualizada != null) {
                        rutina = rutinaActualizada;
                        android.util.Log.d("EjecutarRutinaActivity",
                            "Rutina recargada - Total ejercicios: " + rutina.getCantidadEjercicios());

                        // Recargar los ejercicios
                        loadEjerciciosRutina();

                        // Mostrar mensaje de éxito
                        Toast.makeText(EjecutarRutinaActivity.this,
                            "✅ Rutina actualizada con nuevos ejercicios", Toast.LENGTH_SHORT).show();
                    } else {
                        showEmpty();
                        Toast.makeText(EjecutarRutinaActivity.this,
                            "Error actualizando rutina", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e("EjecutarRutinaActivity", "Error recargando rutina: " + error);
                    Toast.makeText(EjecutarRutinaActivity.this,
                        "Error actualizando rutina: " + error, Toast.LENGTH_SHORT).show();
                    showContent(); // Volver a mostrar la rutina anterior
                });
            }
        });
    }
}
