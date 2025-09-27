package com.jossecm.myapplication;

import android.content.DialogInterface;
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
import com.jossecm.myapplication.repository.FitnessRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
            // Para versiones anteriores
            getWindow().setDecorFitsSystemWindows(true);
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
        fabChatIA = findViewById(R.id.fabChatIA); // Inicializar FAB de chat de IA

        repository = new FitnessRepository(this);
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
        ejercicioAdapter = new EjercicioRutinaAdapter(exerciseList);
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
            // Modo de prueba para el chat de IA
            mostrarDialogoChatIA();
        });
    }

    // NUEVO: Método para mostrar diálogo de chat de IA (modo prueba)
    private void mostrarDialogoChatIA() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🤖 Asistente de IA");
        builder.setMessage("¡Funcionalidad de chat de IA en desarrollo!\n\n" +
                "Próximamente podrás:\n" +
                "• Pedir consejos sobre ejercicios\n" +
                "• Sugerencias de peso y repeticiones\n" +
                "• Recomendaciones de descanso\n" +
                "• Análisis de tu progreso\n\n" +
                "Esta función estará disponible pronto.");

        builder.setPositiveButton("¡Entendido!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Log para debugging
                android.util.Log.d("EjecutarRutinaActivity",
                        "Usuario interactuó con el chat de IA");
            }
        });

        builder.setNeutralButton("Feedback", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(EjecutarRutinaActivity.this,
                        "Gracias por tu interés. ¡Implementaremos esta función pronto!",
                        Toast.LENGTH_LONG).show();
            }
        });

        builder.show();
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
                            ejercicioAdapter.notifyDataSetChanged();
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
            // Obtener datos del entrenamiento
            JSONObject ejerciciosRealizados = ejercicioAdapter.getEjerciciosRealizadosJson();
            int duracionMinutos = tiempoTranscurridoSegundos / 60;

            HistorialEntrenamiento historial = new HistorialEntrenamiento(
                    rutina.getId(),
                    rutina.getNombre(),
                    duracionMinutos,
                    volumenTotal,
                    ejerciciosRealizados.toString()
            );

            repository.insertHistorialEntrenamiento(historial, new FitnessRepository.DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(EjecutarRutinaActivity.this,
                                    "Rutina completada", Toast.LENGTH_SHORT).show();
                            finish();
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
            android.util.Log.e("EjecutarRutinaActivity", "Error finalizando entrenamiento", e);
            Toast.makeText(this, "Error procesando datos del entrenamiento", Toast.LENGTH_SHORT).show();
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
}
