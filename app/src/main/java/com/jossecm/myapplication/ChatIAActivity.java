package com.jossecm.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.jossecm.myapplication.adapters.MensajeAdapter;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.models.Mensaje;
import com.jossecm.myapplication.models.Rutina;
import com.jossecm.myapplication.models.User;
import com.jossecm.myapplication.repository.FitnessRepository;
import com.jossecm.myapplication.services.AIService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ChatIAActivity extends AppCompatActivity {

    private static final String TAG = "ChatIAActivity";
    private static final String PREFS_NAME = "ChatIAPrefs";
    private static final String KEY_CONTEXTO_PERSONALIZADO = "contexto_personalizado";
    private static final int MAX_MENSAJES = 50;

    // UI Components
    private RecyclerView recyclerViewMensajes;
    private EditText etMensaje;
    private ImageButton btnEnviar;
    private ImageButton btnCerrarChat;
    private TextView tvNombreIA;
    private TextView tvStatusIA;
    private TextView tvMensajeBienvenida;
    private LinearLayout layoutTypingIndicator;
    private LinearLayout layoutEmptyState;

    // Data
    private List<Mensaje> mensajes;
    private MensajeAdapter mensajeAdapter;
    private FitnessRepository repository;
    private AIService aiService;
    private String contextoPersonalizado;
    private SharedPreferences sharedPreferences;

    // NUEVO: Datos de la rutina actual
    private String contextoRutina;
    private Rutina rutinaActual;
    private List<Exercise> ejerciciosRutina;
    private List<Exercise> todosLosEjercicios; // Base de datos completa de ejercicios

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar ventana flotante
        configurarVentanaFlotante();

        setContentView(R.layout.activity_chat_ia);

        initializeViews();
        initializeData();
        setupRecyclerView();
        setupListeners();
        cargarContextoPersonalizado();
        mostrarMensajeBienvenida();

        android.util.Log.d(TAG, "ChatIAActivity iniciada");
    }

    private void configurarVentanaFlotante() {
        // Permitir que la ventana sea completamente flexible en orientación
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // Configurar como ventana flotante adaptable
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        );

        // Configurar tamaño de ventana que se adapte a la orientación
        WindowManager.LayoutParams params = getWindow().getAttributes();
        android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // Detectar orientación y ajustar tamaño dinámicamente
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // Modo horizontal: más ancho, menos alto
            params.width = (int) (displayMetrics.widthPixels * 0.85);
            params.height = (int) (displayMetrics.heightPixels * 0.80);
        } else {
            // Modo vertical: menos ancho, más alto
            params.width = (int) (displayMetrics.widthPixels * 0.90);
            params.height = (int) (displayMetrics.heightPixels * 0.75);
        }

        // Configurar gravedad para centrar en cualquier orientación
        params.gravity = android.view.Gravity.CENTER;

        // Permitir que la ventana se ajuste al contenido y orientación
        params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        getWindow().setAttributes(params);

        // Configurar el layout para que sea flexible con el teclado
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void initializeViews() {
        recyclerViewMensajes = findViewById(R.id.recyclerViewMensajes);
        etMensaje = findViewById(R.id.etMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnCerrarChat = findViewById(R.id.btnCerrarChat);
        tvNombreIA = findViewById(R.id.tvNombreIA);
        tvStatusIA = findViewById(R.id.tvStatusIA);
        tvMensajeBienvenida = findViewById(R.id.tvMensajeBienvenida);
        layoutTypingIndicator = findViewById(R.id.layoutTypingIndicator);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
    }

    private void initializeData() {
        mensajes = new ArrayList<>();
        repository = new FitnessRepository(this);
        aiService = new AIService();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Cargar contexto personalizado guardado si existe
        contextoPersonalizado = sharedPreferences.getString(KEY_CONTEXTO_PERSONALIZADO, "");

        // NUEVO: Cargar datos de rutina desde Intent
        cargarDatosRutina();

        // NUEVO: Cargar todos los ejercicios para detección
        cargarTodosLosEjercicios();
    }

    // NUEVO: Cargar todos los ejercicios de la base de datos para detección
    private void cargarTodosLosEjercicios() {
        repository.getAllExercises(new FitnessRepository.DataCallback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> exercises) {
                runOnUiThread(() -> {
                    todosLosEjercicios = exercises;
                    android.util.Log.d(TAG, "Ejercicios cargados para detección: " + (exercises != null ? exercises.size() : 0));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e(TAG, "Error cargando ejercicios para detección: " + error);
                    todosLosEjercicios = new ArrayList<>();
                });
            }
        });
    }

    // NUEVO: Método para cargar datos de rutina desde Intent
    private void cargarDatosRutina() {
        long rutinaId = getIntent().getLongExtra("rutina_id", -1);
        String rutinaNombre = getIntent().getStringExtra("rutina_nombre");
        String ejerciciosJson = getIntent().getStringExtra("ejercicios_json");

        android.util.Log.d(TAG, "Cargando contexto de rutina - ID: " + rutinaId + ", Nombre: " + rutinaNombre);

        if (rutinaId != -1 && rutinaNombre != null && ejerciciosJson != null) {
            // Crear rutina temporal con datos recibidos
            rutinaActual = new Rutina();
            rutinaActual.setId(rutinaId);
            rutinaActual.setNombre(rutinaNombre);

            // Parsear ejercicios desde JSON
            try {
                ejerciciosRutina = parseEjerciciosFromJson(ejerciciosJson);
                construirContextoRutina();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error parseando ejercicios JSON", e);
                contextoRutina = "";
            }
        } else {
            android.util.Log.w(TAG, "No se recibieron datos de rutina completos");
            contextoRutina = "";
        }
    }

    // NUEVO: Parsear ejercicios desde JSON
    private List<Exercise> parseEjerciciosFromJson(String ejerciciosJson) throws Exception {
        List<Exercise> ejercicios = new ArrayList<>();
        org.json.JSONArray jsonArray = new org.json.JSONArray(ejerciciosJson);

        for (int i = 0; i < jsonArray.length(); i++) {
            org.json.JSONObject ejercicioObj = jsonArray.getJSONObject(i);
            Exercise ejercicio = new Exercise();

            ejercicio.setId(ejercicioObj.getInt("id"));
            ejercicio.setName(ejercicioObj.getString("name"));

            if (ejercicioObj.has("description")) {
                ejercicio.setDescription(ejercicioObj.getString("description"));
            }

            if (ejercicioObj.has("muscleNames")) {
                org.json.JSONArray musclesArray = ejercicioObj.getJSONArray("muscleNames");
                List<String> muscleNames = new ArrayList<>();
                for (int j = 0; j < musclesArray.length(); j++) {
                    muscleNames.add(musclesArray.getString(j));
                }
                ejercicio.setMuscleNames(muscleNames);
            }

            ejercicios.add(ejercicio);
        }

        return ejercicios;
    }

    // NUEVO: Construir contexto específico de la rutina
    private void construirContextoRutina() {
        if (rutinaActual == null || ejerciciosRutina == null || ejerciciosRutina.isEmpty()) {
            contextoRutina = "";
            return;
        }

        StringBuilder contexto = new StringBuilder();
        contexto.append("\n=== RUTINA ACTUAL ===\n");
        contexto.append("Rutina: ").append(rutinaActual.getNombre()).append("\n");
        contexto.append("Ejercicios en esta rutina:\n");

        for (int i = 0; i < ejerciciosRutina.size(); i++) {
            Exercise ejercicio = ejerciciosRutina.get(i);
            contexto.append(i + 1).append(". ").append(ejercicio.getName());

            if (ejercicio.getMuscleNames() != null && !ejercicio.getMuscleNames().isEmpty()) {
                contexto.append(" (Músculos: ").append(String.join(", ", ejercicio.getMuscleNames())).append(")");
            }

            if (ejercicio.getDescription() != null && !ejercicio.getDescription().isEmpty()) {
                contexto.append(" - ").append(ejercicio.getDescription());
            }

            contexto.append("\n");
        }

        contexto.append("\nINSTRUCCIONES ESPECÍFICAS DE RUTINA:\n");
        contexto.append("1. Cuando el usuario pregunte sobre 'mis ejercicios' o 'esta rutina', refiere específicamente a los ejercicios listados arriba\n");
        contexto.append("2. Analiza la combinación de músculos y ejercicios para dar feedback sobre balance y efectividad\n");
        contexto.append("3. Sugiere mejoras específicas basadas en los ejercicios actuales\n");
        contexto.append("4. Si pregunta sobre progresión, considera los ejercicios específicos que está haciendo\n");
        contexto.append("5. Evalúa si la rutina es apropiada para su nivel y objetivos\n\n");

        contextoRutina = contexto.toString();

        android.util.Log.d(TAG, "Contexto de rutina construido con " + ejerciciosRutina.size() + " ejercicios");
    }

    private void setupRecyclerView() {
        mensajeAdapter = new MensajeAdapter(mensajes);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Scroll automático al final

        recyclerViewMensajes.setLayoutManager(layoutManager);
        recyclerViewMensajes.setAdapter(mensajeAdapter);

        // NUEVO: Configurar listener para implementar ejercicios
        mensajeAdapter.setOnImplementarEjercicioListener(this::implementarEjercicioEnRutina);

        // Scroll automático cuando se agregan mensajes
        mensajeAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerViewMensajes.scrollToPosition(mensajeAdapter.getItemCount() - 1);
            }
        });
    }

    private void setupListeners() {
        btnCerrarChat.setOnClickListener(v -> {
            android.util.Log.d(TAG, "Cerrando chat");
            finish();
        });

        btnEnviar.setOnClickListener(v -> enviarMensaje());

        etMensaje.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                enviarMensaje();
                return true;
            }
            return false;
        });

        // Tap outside para cerrar
        findViewById(android.R.id.content).setOnClickListener(v -> {
            // Solo cerrar si el click es fuera del área del chat
            if (v.getId() == android.R.id.content) {
                finish();
            }
        });
    }

    private void cargarContextoPersonalizado() {
        android.util.Log.d(TAG, "Cargando contexto personalizado del usuario");

        repository.getCurrentUser(new FitnessRepository.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    if (user != null) {
                        // Construir contexto personalizado
                        StringBuilder contexto = new StringBuilder();
                        contexto.append("Eres un coach personal de fitness experto y motivador. ");
                        contexto.append("Datos del usuario:\n");
                        contexto.append("- Nombre: ").append(user.getName() != null ? user.getName() : "Usuario").append("\n");
                        contexto.append("- Edad: ").append(user.getAge() > 0 ? user.getAge() + " años" : "No especificada").append("\n");
                        contexto.append("- Género: ").append(user.getGender() != null ? user.getGender() : "No especificado").append("\n");
                        contexto.append("- Peso: ").append(user.getWeight() > 0 ? user.getWeight() + " kg" : "No especificado").append("\n");
                        contexto.append("- Altura: ").append(user.getHeight() > 0 ? user.getHeight() + " cm" : "No especificada").append("\n");
                        contexto.append("- Objetivo: ").append(user.getGoal() != null ? user.getGoal() : "No especificado").append("\n");
                        contexto.append("- Nivel: ").append(user.getFitnessLevel() != null ? user.getFitnessLevel() : "No especificado").append("\n");

                        // Procesar lesiones
                        if (user.getInjuries() != null && !user.getInjuries().isEmpty()) {
                            contexto.append("- Lesiones: ").append(String.join(", ", user.getInjuries())).append("\n");
                        } else {
                            contexto.append("- Lesiones: Ninguna\n");
                        }

                        contexto.append("\n");
                        contexto.append("INSTRUCCIONES IMPORTANTES:\n");
                        contexto.append("1. Personaliza TODAS tus respuestas según estos datos del usuario\n");
                        contexto.append("2. Si tiene lesiones, SIEMPRE advierte sobre ejercicios peligrosos y sugiere alternativas seguras\n");
                        contexto.append("3. Ajusta recomendaciones a su nivel de experiencia y objetivos específicos\n");
                        contexto.append("4. Si es principiante, enfócate en técnica y progresión gradual\n");
                        contexto.append("5. Si es avanzado, puedes sugerir técnicas más complejas\n");
                        contexto.append("6. Sé motivador, positivo y profesional\n");
                        contexto.append("7. Responde en español de forma clara y concisa\n");
                        contexto.append("8. Si no tienes información suficiente, pregunta más detalles\n\n");

                        contextoPersonalizado = contexto.toString();

                        // Guardar contexto en SharedPreferences
                        sharedPreferences.edit()
                            .putString(KEY_CONTEXTO_PERSONALIZADO, contextoPersonalizado)
                            .apply();

                        // Actualizar mensaje de bienvenida personalizado
                        String nombreUsuario = user.getName() != null ? user.getName() : "Usuario";
                        tvMensajeBienvenida.setText("¡Hola " + nombreUsuario + "! Soy tu coach personal");

                        android.util.Log.d(TAG, "Contexto personalizado creado para: " + nombreUsuario);
                    } else {
                        // Usuario no encontrado, usar contexto básico
                        contextoPersonalizado = "Eres un coach personal de fitness experto. " +
                            "Responde preguntas sobre ejercicios, técnicas y entrenamiento de forma motivadora y profesional.";
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e(TAG, "Error cargando usuario: " + error);
                    // Usar contexto básico si hay error
                    contextoPersonalizado = "Eres un coach personal de fitness experto. " +
                        "Responde preguntas sobre ejercicios, técnicas y entrenamiento de forma motivadora y profesional.";
                });
            }
        });
    }

    private void mostrarMensajeBienvenida() {
        if (mensajes.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewMensajes.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewMensajes.setVisibility(View.VISIBLE);
        }
    }

    private void enviarMensaje() {
        String textoMensaje = etMensaje.getText().toString().trim();

        if (TextUtils.isEmpty(textoMensaje)) {
            return;
        }

        android.util.Log.d(TAG, "Enviando mensaje: " + textoMensaje);

        // Limpiar campo de texto
        etMensaje.setText("");

        // Ocultar empty state si está visible
        if (layoutEmptyState.getVisibility() == View.VISIBLE) {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewMensajes.setVisibility(View.VISIBLE);
        }

        // Crear mensaje del usuario
        Mensaje mensajeUsuario = new Mensaje(textoMensaje, true);
        mensajeUsuario.setEnviando(true);

        // Agregar mensaje del usuario
        mensajeAdapter.agregarMensaje(mensajeUsuario);

        // Verificar límite de mensajes
        if (mensajes.size() > MAX_MENSAJES) {
            mensajes.remove(0);
            mensajeAdapter.notifyItemRemoved(0);
        }

        // Enviar a IA
        enviarMensajeAIA(textoMensaje, mensajes.size() - 1);
    }

    private void enviarMensajeAIA(String mensaje, int posicionMensajeUsuario) {
        // Construir contexto completo: usuario + rutina
        String contextoCompleto = construirContextoCompleto();

        if (TextUtils.isEmpty(contextoCompleto)) {
            android.util.Log.w(TAG, "Contexto completo no disponible, usando básico");
            contextoCompleto = "Eres un coach personal de fitness experto. " +
                "Responde preguntas sobre ejercicios, técnicas y entrenamiento.";
        }

        aiService.enviarMensaje(mensaje, contextoCompleto, new AIService.AICallback() {
            @Override
            public void onStartTyping() {
                runOnUiThread(() -> {
                    // Marcar mensaje del usuario como enviado
                    Mensaje mensajeUsuario = mensajes.get(posicionMensajeUsuario);
                    mensajeUsuario.setEnviando(false);
                    mensajeAdapter.actualizarMensaje(posicionMensajeUsuario, mensajeUsuario);

                    // Mostrar indicador de escritura
                    layoutTypingIndicator.setVisibility(View.VISIBLE);
                    tvStatusIA.setText("Escribiendo...");

                    android.util.Log.d(TAG, "IA comenzó a escribir");
                });
            }

            @Override
            public void onResponse(String respuesta) {
                runOnUiThread(() -> {
                    // Ocultar indicador de escritura
                    layoutTypingIndicator.setVisibility(View.GONE);
                    tvStatusIA.setText("En línea");

                    // Crear mensaje de respuesta de la IA
                    Mensaje mensajeIA = new Mensaje(respuesta, false);

                    // NUEVO: Detectar ejercicios recomendados en la respuesta
                    List<Exercise> ejerciciosDetectados = detectarEjerciciosEnRespuesta(respuesta);
                    if (ejerciciosDetectados != null && !ejerciciosDetectados.isEmpty()) {
                        mensajeIA.setEjerciciosRecomendados(ejerciciosDetectados);
                        android.util.Log.d(TAG, "Ejercicios detectados en respuesta: " + ejerciciosDetectados.size());
                    }

                    mensajeAdapter.agregarMensaje(mensajeIA);

                    android.util.Log.d(TAG, "Respuesta de IA recibida: " + respuesta.substring(0, Math.min(50, respuesta.length())));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Ocultar indicador de escritura
                    layoutTypingIndicator.setVisibility(View.GONE);
                    tvStatusIA.setText("Error de conexión");

                    // Marcar mensaje del usuario como no enviado (error)
                    Mensaje mensajeUsuario = mensajes.get(posicionMensajeUsuario);
                    mensajeUsuario.setEnviando(false);
                    mensajeAdapter.actualizarMensaje(posicionMensajeUsuario, mensajeUsuario);

                    // Mostrar mensaje de error
                    String mensajeError = "Lo siento, no pude procesar tu mensaje. " +
                        "Verifica tu conexión a internet e intenta de nuevo.";
                    Mensaje mensajeErrorIA = new Mensaje(mensajeError, false);
                    mensajeAdapter.agregarMensaje(mensajeErrorIA);

                    Toast.makeText(ChatIAActivity.this,
                        "Error de conexión: " + error, Toast.LENGTH_SHORT).show();

                    android.util.Log.e(TAG, "Error en respuesta de IA: " + error);

                    // Restaurar estado después de 3 segundos
                    new android.os.Handler().postDelayed(() ->
                        tvStatusIA.setText("En línea"), 3000);
                });
            }
        });
    }

    // NUEVO: Construir contexto completo combinando usuario y rutina
    private String construirContextoCompleto() {
        StringBuilder contextoCompleto = new StringBuilder();

        // Agregar contexto del usuario
        if (!TextUtils.isEmpty(contextoPersonalizado)) {
            contextoCompleto.append(contextoPersonalizado);
        }

        // Agregar contexto de la rutina actual
        if (!TextUtils.isEmpty(contextoRutina)) {
            contextoCompleto.append(contextoRutina);
        }

        android.util.Log.d(TAG, "Contexto completo construido - Longitud: " + contextoCompleto.length());
        return contextoCompleto.toString();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Reajustar tamaño de ventana cuando cambie la orientación
        WindowManager.LayoutParams params = getWindow().getAttributes();
        android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // Modo horizontal: más ancho, menos alto
            params.width = (int) (displayMetrics.widthPixels * 0.85);
            params.height = (int) (displayMetrics.heightPixels * 0.80);
            android.util.Log.d(TAG, "Cambiado a modo horizontal");
        } else {
            // Modo vertical: menos ancho, más alto
            params.width = (int) (displayMetrics.widthPixels * 0.90);
            params.height = (int) (displayMetrics.heightPixels * 0.75);
            android.util.Log.d(TAG, "Cambiado a modo vertical");
        }

        getWindow().setAttributes(params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiService != null) {
            aiService.shutdown();
        }
        android.util.Log.d(TAG, "ChatIAActivity destruida");
    }

    @Override
    public void onBackPressed() {
        android.util.Log.d(TAG, "Botón atrás presionado, cerrando chat");
        super.onBackPressed();
    }

    // NUEVO: Método para implementar ejercicio en la rutina (PERSISTIR EN BD)
    private void implementarEjercicioEnRutina(Exercise ejercicio) {
        if (ejercicio == null) {
            return;
        }

        android.util.Log.d(TAG, "Implementando ejercicio en rutina: " + ejercicio.getName());

        // Verificar si la rutina actual es válida
        if (rutinaActual == null || ejerciciosRutina == null) {
            Toast.makeText(this, "Rutina no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si el ejercicio ya existe en la rutina
        for (Exercise existente : ejerciciosRutina) {
            if (existente.getName().equalsIgnoreCase(ejercicio.getName()) || existente.getId() == ejercicio.getId()) {
                Toast.makeText(this, "Este ejercicio ya está en tu rutina", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Agregar ejercicio a la rutina local (para el contexto de la IA)
        ejerciciosRutina.add(ejercicio);

        // Actualizar contexto de rutina para que la IA sepa sobre el nuevo ejercicio
        construirContextoRutina();

        // NUEVO: Guardar ejercicio en la base de datos si es un ejercicio nuevo
        if (ejercicio.getId() >= 9000) { // Ejercicios generados por IA tienen ID >= 9000
            guardarEjercicioGenericoEnBD(ejercicio, () -> {
                // Callback después de guardar en BD
                agregarEjercicioARutinaEnBD(ejercicio);
            });
        } else {
            // Ejercicio existente, solo agregarlo a la rutina
            agregarEjercicioARutinaEnBD(ejercicio);
        }

        // Mostrar mensaje de éxito
        Toast.makeText(this, "✅ " + ejercicio.getName() + " agregado a tu rutina", Toast.LENGTH_SHORT).show();

        // Enviar mensaje a la IA informando sobre el nuevo ejercicio
        String mensaje = "✅ Perfecto! He agregado '" + ejercicio.getName() + "' a tu rutina \"" +
                        rutinaActual.getNombre() + "\". Ahora tienes " + ejerciciosRutina.size() + " ejercicios.";
        Mensaje mensajeIA = new Mensaje(mensaje, false);
        mensajeAdapter.agregarMensaje(mensajeIA);

        // Actualizar vista de mensajes
        recyclerViewMensajes.scrollToPosition(mensajeAdapter.getItemCount() - 1);

        android.util.Log.d(TAG, "Ejercicio agregado a rutina - Local: " + ejerciciosRutina.size() + " ejercicios");
    }

    // NUEVO: Guardar ejercicio genérico en la base de datos
    private void guardarEjercicioGenericoEnBD(Exercise ejercicio, Runnable onSuccess) {
        // Generar un ID único para el nuevo ejercicio
        long timestamp = System.currentTimeMillis();
        int nuevoId = (int) (timestamp % 100000); // ID único basado en timestamp
        ejercicio.setId(nuevoId);

        repository.addExercise(ejercicio, new FitnessRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    android.util.Log.d(TAG, "Ejercicio genérico guardado en BD: " + ejercicio.getName());
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e(TAG, "Error guardando ejercicio genérico: " + error);
                    // Continuar con ID temporal si falla el guardado
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
            }
        });
    }

    // NUEVO: Agregar ejercicio a la rutina en la base de datos
    private void agregarEjercicioARutinaEnBD(Exercise ejercicio) {
        if (rutinaActual == null) {
            return;
        }

        repository.addExerciseToRoutine(rutinaActual.getId(), ejercicio.getId(), new FitnessRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    android.util.Log.d(TAG, "Ejercicio agregado a rutina en BD: " + ejercicio.getName());

                    // Indicar que se hicieron cambios para actualizar la actividad padre
                    setResult(RESULT_OK);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    android.util.Log.e(TAG, "Error agregando ejercicio a rutina en BD: " + error);
                    Toast.makeText(ChatIAActivity.this,
                        "Ejercicio agregado localmente, pero no se pudo sincronizar",
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // NUEVO: Método avanzado para detectar ejercicios en la respuesta de la IA
    private List<Exercise> detectarEjerciciosEnRespuesta(String respuesta) {
        List<Exercise> ejerciciosGenerados = new ArrayList<>();
        String respuestaLower = respuesta.toLowerCase();

        android.util.Log.d(TAG, "Analizando respuesta para detectar recomendaciones de ejercicios nuevos");

        // MEJORADO: Patrones más específicos para detectar recomendaciones reales de ejercicios
        String[] patronesRecomendacionEspecificos = {
            "te recomiendo estos ejercicios",
            "ejercicios recomendados para ti",
            "nuevos ejercicios que puedes agregar",
            "aquí tienes algunos ejercicios",
            "ejercicios que deberías incluir",
            "te sugiero agregar estos ejercicios",
            "podrías incluir estos ejercicios",
            "ejercicios adicionales que te ayudarán",
            "considera agregar estos ejercicios",
            "estos ejercicios serían perfectos para",
            // NUEVOS PATRONES MÁS AMPLIOS
            "te propongo agregar",
            "propongo agregar los siguientes ejercicios",
            "para complementar esta rutina",
            "te recomiendo hacer",
            "puedes probar",
            "te gustaría probar",
            "añadir a tu rutina",
            "incluir en tu entrenamiento",
            "complementar con",
            "agregar también"
        };

        // MEJORADO: Patrones más amplios que indican que NO es una recomendación de ejercicios nuevos
        String[] patronesExclusion = {
            "los ejercicios que tienes",
            "tu rutina actual",
            "ejercicios que ya haces",
            "ejercicios existentes",
            "análisis de tu rutina",
            "sobre tu entrenamiento actual",
            "la rutina que me has proporcionado",
            "parece ser una buena base",
            "hay algunas consideraciones",
            "debemos tener en cuenta",
            "en cuanto a la",
            "en lugar de hacer",
            "podrías variar",
            "mi sugerencia es que",
            "recomiendo que",
            "por ahora"
        };

        // Verificar primero si es una exclusión (conversación sobre rutina actual)
        boolean esExclusion = false;
        String patronExclusion = "";
        for (String patron : patronesExclusion) {
            if (respuestaLower.contains(patron)) {
                esExclusion = true;
                patronExclusion = patron;
                android.util.Log.d(TAG, "Detectada conversación sobre rutina actual con patrón: " + patron);
                break;
            }
        }

        if (esExclusion) {
            android.util.Log.d(TAG, "❌ Excluyendo generación por patrón: " + patronExclusion);
            return ejerciciosGenerados; // Lista vacía
        }

        // Verificar si es una recomendación específica de ejercicios nuevos
        boolean esRecomendacionEspecifica = false;
        String patronDetectado = "";
        for (String patron : patronesRecomendacionEspecificos) {
            if (respuestaLower.contains(patron)) {
                esRecomendacionEspecifica = true;
                patronDetectado = patron;
                android.util.Log.d(TAG, "Detectada recomendación específica de ejercicios nuevos: " + patron);
                break;
            }
        }

        // MEJORADO: Detectar menciones directas de ejercicios específicos SOLO si hay contexto de recomendación
        if (!esRecomendacionEspecifica) {
            // Buscar patrones que indiquen ejercicios específicos mencionados junto con contexto de recomendación
            String[] patronesEjerciciosEspecificos = {
                "sentadillas",
                "peso muerto",
                "press de banca",
                "dominadas",
                "flexiones",
                "planchas",
                "remo",
                "curl",
                "extensions"
            };

            // NUEVA VALIDACIÓN: Solo considerar ejercicios específicos si hay palabras de recomendación cerca
            String[] palabrasRecomendacion = {
                "recomiendo",
                "sugiero",
                "propongo",
                "te ayudarán",
                "deberías probar",
                "puedes hacer",
                "sería bueno",
                "perfecto para ti"
            };

            for (String ejercicio : patronesEjerciciosEspecificos) {
                if (respuestaLower.contains(ejercicio)) {
                    // Verificar si hay palabras de recomendación cerca del ejercicio mencionado
                    boolean hayContextoRecomendacion = false;
                    for (String palabraRec : palabrasRecomendacion) {
                        if (respuestaLower.contains(palabraRec)) {
                            hayContextoRecomendacion = true;
                            break;
                        }
                    }

                    if (hayContextoRecomendacion) {
                        esRecomendacionEspecifica = true;
                        patronDetectado = "mención de ejercicio con contexto de recomendación: " + ejercicio;
                        android.util.Log.d(TAG, "Detectada mención de ejercicio con contexto de recomendación: " + ejercicio);
                        break;
                    } else {
                        android.util.Log.d(TAG, "❌ Ejercicio mencionado sin contexto de recomendación: " + ejercicio);
                    }
                }
            }
        }

        // Solo generar ejercicios si la respuesta es realmente una recomendación específica
        if (!esRecomendacionEspecifica) {
            android.util.Log.d(TAG, "❌ No se detectó recomendación específica de ejercicios nuevos");
            return ejerciciosGenerados; // Lista vacía
        }

        // Si llegamos aquí, es una recomendación real de ejercicios nuevos
        android.util.Log.d(TAG, "✅ Generando ejercicios basados en patrón: " + patronDetectado);
        ejerciciosGenerados.addAll(generarEjerciciosDeRespuesta(respuesta));

        android.util.Log.d(TAG, "Total de ejercicios generados: " + ejerciciosGenerados.size());
        return ejerciciosGenerados;
    }

    // NUEVO: Generar ejercicios completamente nuevos basados en la respuesta de la IA
    private List<Exercise> generarEjerciciosDeRespuesta(String respuesta) {
        List<Exercise> ejercicios = new ArrayList<>();

        // Detectar menciones de grupos musculares y tipos de ejercicios
        Map<String, List<String>> ejerciciosPorMusculo = detectarEjerciciosPorMusculo(respuesta);

        // MEJORADO: Solo generar ejercicios si se detectaron músculos específicos mencionados
        if (ejerciciosPorMusculo.isEmpty()) {
            android.util.Log.d(TAG, "No se detectaron músculos específicos en la respuesta, no generar ejercicios");
            return ejercicios; // Lista vacía
        }

        int ejercicioId = 9000 + (int)(System.currentTimeMillis() % 1000); // ID único temporal

        for (Map.Entry<String, List<String>> entry : ejerciciosPorMusculo.entrySet()) {
            String musculo = entry.getKey();
            List<String> tiposEjercicio = entry.getValue();

            for (String tipo : tiposEjercicio) {
                Exercise ejercicioNuevo = crearEjercicioPersonalizado(ejercicioId++, musculo, tipo);
                ejercicios.add(ejercicioNuevo);

                android.util.Log.d(TAG, "Ejercicio generado: " + ejercicioNuevo.getName() +
                                 " - Músculos: " + ejercicioNuevo.getMuscleNames());

                // Limitar a máximo 3 ejercicios por respuesta
                if (ejercicios.size() >= 3) {
                    break;
                }
            }

            if (ejercicios.size() >= 3) {
                break;
            }
        }

        // ELIMINADO: Ya no generar ejercicios genéricos automáticamente
        // Ahora solo se generan ejercicios cuando hay patrones específicos detectados

        return ejercicios;
    }

    // NUEVO: Detectar ejercicios específicos por grupo muscular mencionado en la respuesta
    private Map<String, List<String>> detectarEjerciciosPorMusculo(String respuesta) {
        Map<String, List<String>> ejerciciosPorMusculo = new HashMap<>();
        String respuestaLower = respuesta.toLowerCase();

        // Mapear palabras clave a grupos musculares y ejercicios
        if (respuestaLower.contains("pecho") || respuestaLower.contains("chest") ||
            respuestaLower.contains("press") || respuestaLower.contains("flexion")) {
            List<String> ejerciciosPecho = Arrays.asList("Press", "Flexiones", "Fly", "Dips");
            ejerciciosPorMusculo.put("Chest", ejerciciosPecho);
        }

        if (respuestaLower.contains("espalda") || respuestaLower.contains("back") ||
            respuestaLower.contains("remo") || respuestaLower.contains("pull")) {
            List<String> ejerciciosEspalda = Arrays.asList("Remo", "Pull-ups", "Dominadas", "Jalones");
            ejerciciosPorMusculo.put("Lats", ejerciciosEspalda);
        }

        if (respuestaLower.contains("hombro") || respuestaLower.contains("shoulder") ||
            respuestaLower.contains("deltoid")) {
            List<String> ejerciciosHombro = Arrays.asList("Press Militar", "Elevaciones", "Remo al Mentón");
            ejerciciosPorMusculo.put("Shoulders", ejerciciosHombro);
        }

        if (respuestaLower.contains("biceps") || respuestaLower.contains("curl")) {
            List<String> ejerciciosBiceps = Arrays.asList("Curl", "Hammer Curl", "Curl Concentrado");
            ejerciciosPorMusculo.put("Biceps", ejerciciosBiceps);
        }

        if (respuestaLower.contains("triceps") || respuestaLower.contains("tricep")) {
            List<String> ejerciciosTriceps = Arrays.asList("Press Francés", "Dips", "Extensiones");
            ejerciciosPorMusculo.put("Triceps", ejerciciosTriceps);
        }

        if (respuestaLower.contains("pierna") || respuestaLower.contains("leg") ||
            respuestaLower.contains("sentadilla") || respuestaLower.contains("squat")) {
            List<String> ejerciciosPiernas = Arrays.asList("Sentadillas", "Zancadas", "Peso Muerto");
            ejerciciosPorMusculo.put("Hamstrings", ejerciciosPiernas);
        }

        if (respuestaLower.contains("core") || respuestaLower.contains("abdomen") ||
            respuestaLower.contains("plancha") || respuestaLower.contains("abs")) {
            List<String> ejerciciosCore = Arrays.asList("Plancha", "Crunches", "Mountain Climbers");
            ejerciciosPorMusculo.put("Core", ejerciciosCore);
        }

        return ejerciciosPorMusculo;
    }

    // NUEVO: Crear un ejercicio personalizado específico
    private Exercise crearEjercicioPersonalizado(int id, String musculo, String tipo) {
        Exercise ejercicio = new Exercise();
        ejercicio.setId(id);

        // Generar nombre creativo combinando tipo y músculo
        String nombre = generarNombreEjercicio(tipo, musculo);
        ejercicio.setName(nombre);

        // Generar descripción personalizada
        String descripcion = generarDescripcionEjercicio(tipo, musculo);
        ejercicio.setDescription(descripcion);

        // Asignar músculos objetivo
        List<String> musculos = new ArrayList<>();
        musculos.add(musculo);

        // Agregar músculos secundarios según el tipo de ejercicio
        musculos.addAll(obtenerMusculosSecundarios(tipo, musculo));

        ejercicio.setMuscleNames(musculos);

        // Sin imagen por ahora
        ejercicio.setImageUrl(null);

        return ejercicio;
    }

    // NUEVO: Generar nombre creativo para el ejercicio
    private String generarNombreEjercicio(String tipo, String musculo) {
        String[] prefijos = {"Super", "Power", "Intenso", "Avanzado", "Dinámico", "Explosivo"};
        String[] sufijos = {"Pro", "Max", "Plus", "Elite", "Premium", "Ultimate"};

        Random random = new Random();

        // 60% de probabilidad de agregar prefijo/sufijo
        if (random.nextFloat() < 0.6f) {
            if (random.nextBoolean()) {
                String prefijo = prefijos[random.nextInt(prefijos.length)];
                return prefijo + " " + tipo + " de " + musculo;
            } else {
                String sufijo = sufijos[random.nextInt(sufijos.length)];
                return tipo + " " + sufijo + " de " + musculo;
            }
        }

        return tipo + " de " + musculo;
    }

    // NUEVO: Generar descripción personalizada para el ejercicio
    private String generarDescripcionEjercicio(String tipo, String musculo) {
        String[] intensidades = {"moderada", "alta", "intensa", "progresiva"};
        String[] beneficios = {"fortalece", "desarrolla", "tonifica", "potencia"};
        String[] enfoques = {"específicamente", "efectivamente", "directamente", "principalmente"};

        Random random = new Random();

        String intensidad = intensidades[random.nextInt(intensidades.length)];
        String beneficio = beneficios[random.nextInt(beneficios.length)];
        String enfoque = enfoques[random.nextInt(enfoques.length)];

        return String.format("Ejercicio de intensidad %s que %s %s el %s. " +
                           "Recomendado por tu Coach IA para optimizar tu entrenamiento.",
                           intensidad, beneficio, enfoque, musculo);
    }

    // NUEVO: Obtener músculos secundarios según el tipo de ejercicio
    private List<String> obtenerMusculosSecundarios(String tipo, String musculoPrincipal) {
        List<String> secundarios = new ArrayList<>();

        switch (tipo.toLowerCase()) {
            case "press":
            case "flexiones":
                if (!musculoPrincipal.equals("Triceps")) secundarios.add("Triceps");
                if (!musculoPrincipal.equals("Shoulders")) secundarios.add("Shoulders");
                break;

            case "remo":
            case "pull-ups":
            case "dominadas":
                if (!musculoPrincipal.equals("Biceps")) secundarios.add("Biceps");
                if (!musculoPrincipal.equals("Shoulders")) secundarios.add("Shoulders");
                break;

            case "sentadillas":
            case "zancadas":
                if (!musculoPrincipal.equals("Core")) secundarios.add("Core");
                break;

            case "plancha":
                if (!musculoPrincipal.equals("Shoulders")) secundarios.add("Shoulders");
                break;
        }

        return secundarios;
    }

    // NUEVO: Generar ejercicios genéricos cuando no se detectan específicos
    private List<Exercise> generarEjerciciosGenericos(int startId) {
        List<Exercise> ejercicios = new ArrayList<>();

        // Ejercicios genéricos balanceados
        String[][] ejerciciosGenericos = {
            {"Entrenamiento Funcional Total", "Core", "Ejercicio completo que trabaja múltiples grupos musculares simultáneamente"},
            {"Cardio Resistance Training", "Hamstrings", "Combinación de resistencia y cardio para quemar grasa y tonificar"},
            {"Dynamic Strength Circuit", "Chest", "Circuito dinámico para desarrollar fuerza y resistencia muscular"}
        };

        for (int i = 0; i < ejerciciosGenericos.length; i++) {
            Exercise ejercicio = new Exercise();
            ejercicio.setId(startId + i);
            ejercicio.setName(ejerciciosGenericos[i][0]);
            ejercicio.setDescription(ejerciciosGenericos[i][2] + " Personalizado por tu Coach IA.");

            List<String> musculos = new ArrayList<>();
            musculos.add(ejerciciosGenericos[i][1]);
            ejercicio.setMuscleNames(musculos);

            ejercicios.add(ejercicio);
        }

        return ejercicios;
    }
}
