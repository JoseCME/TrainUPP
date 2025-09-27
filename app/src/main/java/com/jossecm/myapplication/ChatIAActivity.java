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
import com.jossecm.myapplication.models.Mensaje;
import com.jossecm.myapplication.models.User;
import com.jossecm.myapplication.repository.FitnessRepository;
import com.jossecm.myapplication.services.AIService;
import java.util.ArrayList;
import java.util.List;

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
    }

    private void setupRecyclerView() {
        mensajeAdapter = new MensajeAdapter(mensajes);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Scroll automático al final

        recyclerViewMensajes.setLayoutManager(layoutManager);
        recyclerViewMensajes.setAdapter(mensajeAdapter);

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
        if (TextUtils.isEmpty(contextoPersonalizado)) {
            android.util.Log.w(TAG, "Contexto personalizado no cargado, usando básico");
            contextoPersonalizado = "Eres un coach personal de fitness experto. " +
                "Responde preguntas sobre ejercicios, técnicas y entrenamiento.";
        }

        aiService.enviarMensaje(mensaje, contextoPersonalizado, new AIService.AICallback() {
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
}
