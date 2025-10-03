package com.jossecm.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;
import java.util.Random;

public class CelebrationActivity extends AppCompatActivity {

    private TextView textViewTitle;
    private TextView textViewSubtitle;
    private TextView textViewDuration;
    private TextView textViewVolume;
    private LottieAnimationView lottieAnimation;
    private MaterialButton buttonContinue;
    private View statsContainer;
    private ImageView particle1, particle2, particle3;

    // Datos recibidos de la rutina completada
    private String rutinaNombre;
    private int duracionMinutos;
    private double volumenTotal;

    private Handler handler;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_celebration);

        // Configurar pantalla completa inmersiva
        setupFullscreenMode();

        // Obtener datos del Intent
        getDataFromIntent();

        // Inicializar vistas
        initViews();

        // Configurar datos
        setupData();

        // Iniciar secuencia de animaciones
        startCelebrationSequence();
    }

    private void setupFullscreenMode() {
        // Configurar modo pantalla completa inmersivo
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().getInsetsController().hide(
                android.view.WindowInsets.Type.statusBars() |
                android.view.WindowInsets.Type.navigationBars()
            );
        } else {
            // Para versiones anteriores
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        rutinaNombre = intent.getStringExtra("rutina_nombre");
        duracionMinutos = intent.getIntExtra("duracion_minutos", 0);
        volumenTotal = intent.getDoubleExtra("volumen_total", 0.0);

        // Valores por defecto si no se reciben datos
        if (rutinaNombre == null || rutinaNombre.isEmpty()) {
            rutinaNombre = "Tu rutina";
        }
    }

    private void initViews() {
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewSubtitle = findViewById(R.id.textViewSubtitle);
        textViewDuration = findViewById(R.id.textViewDuration);
        textViewVolume = findViewById(R.id.textViewVolume);
        lottieAnimation = findViewById(R.id.lottieAnimation);
        buttonContinue = findViewById(R.id.buttonContinue);
        statsContainer = findViewById(R.id.statsContainer);
        particle1 = findViewById(R.id.particle1);
        particle2 = findViewById(R.id.particle2);
        particle3 = findViewById(R.id.particle3);

        handler = new Handler();

        // Configurar click del botón
        buttonContinue.setOnClickListener(v -> finishCelebration());
    }

    private void setupData() {
        // Configurar subtitle personalizado
        textViewSubtitle.setText("¡Has completado " + rutinaNombre + "!");

        // Configurar estadísticas
        String duracionTexto;
        if (duracionMinutos < 60) {
            duracionTexto = duracionMinutos + " min";
        } else {
            int horas = duracionMinutos / 60;
            int minutos = duracionMinutos % 60;
            duracionTexto = horas + "h " + minutos + "m";
        }
        textViewDuration.setText(duracionTexto);

        // Formatear volumen total
        String volumenTexto = String.format(Locale.getDefault(), "%.1f lbs", volumenTotal);
        textViewVolume.setText(volumenTexto);
    }

    private void startCelebrationSequence() {
        // Secuencia de animaciones escalonadas
        animateTitle();

        handler.postDelayed(this::animateSubtitle, 300);
        handler.postDelayed(this::animateLottie, 600);
        handler.postDelayed(this::animateStats, 1200);
        handler.postDelayed(this::animateButton, 1800);
        handler.postDelayed(this::animateParticles, 900);
    }

    private void animateTitle() {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textViewTitle, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(textViewTitle, "translationY", 50f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(textViewTitle, "scaleX", 0.8f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(textViewTitle, "scaleY", 0.8f, 1.2f, 1f);

        AnimatorSet titleAnimator = new AnimatorSet();
        titleAnimator.playTogether(fadeIn, slideUp, scaleX, scaleY);
        titleAnimator.setDuration(800);
        titleAnimator.setInterpolator(new OvershootInterpolator());
        titleAnimator.start();
    }

    private void animateSubtitle() {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textViewSubtitle, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(textViewSubtitle, "translationY", 50f, 0f);

        AnimatorSet subtitleAnimator = new AnimatorSet();
        subtitleAnimator.playTogether(fadeIn, slideUp);
        subtitleAnimator.setDuration(600);
        subtitleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        subtitleAnimator.start();
    }

    private void animateLottie() {
        // Animar entrada de Lottie de forma simple - solo aparición
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(lottieAnimation, "alpha", 0f, 1f);

        fadeIn.setDuration(800);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        fadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Iniciar animación Lottie cuando termine la animación de entrada
                startLottieAnimation();
            }
        });

        fadeIn.start();
    }

    private void startLottieAnimation() {
        // Intentar cargar animación Lottie de manera segura con más logging
        try {
            // Verificar si existe un archivo Lottie en assets
            String[] assets = getAssets().list("lottie");
            String foundFile = null;

            android.util.Log.d("CelebrationActivity", "Archivos encontrados en assets/lottie/: " +
                (assets != null ? java.util.Arrays.toString(assets) : "null"));

            if (assets != null && assets.length > 0) {
                for (String asset : assets) {
                    android.util.Log.d("CelebrationActivity", "Revisando archivo: " + asset);
                    if (asset.endsWith(".json")) {
                        foundFile = asset;
                        android.util.Log.d("CelebrationActivity", "Intentando cargar: " + asset);

                        // Intentar cargar el archivo Lottie
                        try {
                            lottieAnimation.setAnimation("lottie/" + asset);

                            // Verificar si se cargó correctamente después de un breve delay
                            final String finalAsset = asset; // Variable final para lambda
                            handler.postDelayed(() -> {
                                if (lottieAnimation.getComposition() != null) {
                                    android.util.Log.d("CelebrationActivity", "✅ Animación Lottie cargada exitosamente: " + finalAsset);
                                    lottieAnimation.playAnimation();
                                } else {
                                    android.util.Log.w("CelebrationActivity", "❌ Composición Lottie es null para: " + finalAsset);
                                    createFallbackAnimation();
                                }
                            }, 100);

                            return; // Salir del método si encontramos un archivo

                        } catch (Exception fileException) {
                            android.util.Log.e("CelebrationActivity", "Error cargando archivo específico " + asset + ": " + fileException.getMessage());
                            continue; // Intentar con el siguiente archivo
                        }
                    }
                }
            }

            // Si llegamos aquí, no se encontró ningún archivo válido
            if (foundFile == null) {
                android.util.Log.w("CelebrationActivity", "No se encontraron archivos .json en assets/lottie/");
            } else {
                android.util.Log.w("CelebrationActivity", "Se encontró " + foundFile + " pero no se pudo cargar");
            }

            // Usar animación de respaldo
            android.util.Log.d("CelebrationActivity", "Usando animación de respaldo");
            createFallbackAnimation();

        } catch (Exception e) {
            android.util.Log.e("CelebrationActivity", "Error general cargando animación Lottie: " + e.getMessage());
            createFallbackAnimation();
        }
    }

    private void createFallbackAnimation() {
        // Crear una animación de celebración más elaborada como respaldo
        android.util.Log.d("CelebrationActivity", "Creando animación de celebración de respaldo");

        // Cambiar el fondo del LottieAnimationView a una estrella grande
        lottieAnimation.setBackground(getDrawable(R.drawable.ic_star));

        // Animación principal de pulso y rotación
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(lottieAnimation, "scaleX", 1f, 1.4f, 1.1f, 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(lottieAnimation, "scaleY", 1f, 1.4f, 1.1f, 1.3f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(lottieAnimation, "rotation", 0f, 360f);

        // Animación de brillo (cambio de alpha)
        ObjectAnimator shine = ObjectAnimator.ofFloat(lottieAnimation, "alpha", 1f, 0.7f, 1f, 0.8f, 1f);

        // Configurar duraciones y repeticiones
        scaleX.setDuration(2000);
        scaleX.setRepeatCount(3);
        scaleX.setInterpolator(new BounceInterpolator());

        scaleY.setDuration(2000);
        scaleY.setRepeatCount(3);
        scaleY.setInterpolator(new BounceInterpolator());

        rotation.setDuration(1500);
        rotation.setRepeatCount(4);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());

        shine.setDuration(800);
        shine.setRepeatCount(6);
        shine.setInterpolator(new AccelerateDecelerateInterpolator());

        // Iniciar todas las animaciones
        scaleX.start();
        scaleY.start();
        rotation.start();

        // Iniciar el brillo con un pequeño delay
        handler.postDelayed(() -> shine.start(), 200);

        // Agregar un efecto de "explosión" al final
        handler.postDelayed(this::createExplosionEffect, 3000);
    }

    private void createExplosionEffect() {
        // Efecto de "explosión" al final de la animación principal
        ObjectAnimator explodeScale = ObjectAnimator.ofFloat(lottieAnimation, "scaleX", 1f, 2f, 1f);
        ObjectAnimator explodeScaleY = ObjectAnimator.ofFloat(lottieAnimation, "scaleY", 1f, 2f, 1f);
        ObjectAnimator explodeFade = ObjectAnimator.ofFloat(lottieAnimation, "alpha", 1f, 0.3f, 1f);

        explodeScale.setDuration(500);
        explodeScale.setInterpolator(new OvershootInterpolator());

        explodeScaleY.setDuration(500);
        explodeScaleY.setInterpolator(new OvershootInterpolator());

        explodeFade.setDuration(500);
        explodeFade.setInterpolator(new AccelerateDecelerateInterpolator());

        explodeScale.start();
        explodeScaleY.start();
        explodeFade.start();
    }

    private void animateStats() {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(statsContainer, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(statsContainer, "translationY", 50f, 0f);

        AnimatorSet statsAnimator = new AnimatorSet();
        statsAnimator.playTogether(fadeIn, slideUp);
        statsAnimator.setDuration(600);
        statsAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        statsAnimator.start();

        // Animar números de las estadísticas
        animateStatNumbers();
    }

    private void animateStatNumbers() {
        // Animar contador de duración
        ValueAnimator durationAnimator = ValueAnimator.ofInt(0, duracionMinutos);
        durationAnimator.setDuration(1000);
        durationAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            String duracionTexto;
            if (value < 60) {
                duracionTexto = value + " min";
            } else {
                int horas = value / 60;
                int minutos = value % 60;
                duracionTexto = horas + "h " + minutos + "m";
            }
            textViewDuration.setText(duracionTexto);
        });

        // Animar contador de volumen
        ValueAnimator volumeAnimator = ValueAnimator.ofFloat(0f, (float) volumenTotal);
        volumeAnimator.setDuration(1000);
        volumeAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            String volumenTexto = String.format(Locale.getDefault(), "%.1f lbs", value);
            textViewVolume.setText(volumenTexto);
        });

        // Iniciar animaciones de contadores
        handler.postDelayed(() -> {
            durationAnimator.start();
            volumeAnimator.start();
        }, 300);
    }

    private void animateButton() {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(buttonContinue, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(buttonContinue, "translationY", 50f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(buttonContinue, "scaleX", 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(buttonContinue, "scaleY", 0.9f, 1f);

        AnimatorSet buttonAnimator = new AnimatorSet();
        buttonAnimator.playTogether(fadeIn, slideUp, scaleX, scaleY);
        buttonAnimator.setDuration(600);
        buttonAnimator.setInterpolator(new OvershootInterpolator());
        buttonAnimator.start();
    }

    private void animateParticles() {
        animateParticle(particle1, 2000);

        handler.postDelayed(() -> animateParticle(particle2, 1800), 300);
        handler.postDelayed(() -> animateParticle(particle3, 2200), 600);
    }

    private void animateParticle(ImageView particle, long duration) {
        // Fade in
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(particle, "alpha", 0f, 1f);
        fadeIn.setDuration(300);

        // Movimiento flotante
        ObjectAnimator floatY = ObjectAnimator.ofFloat(particle, "translationY", 0f, -50f, 0f);
        floatY.setDuration(duration);
        floatY.setRepeatCount(ValueAnimator.INFINITE);
        floatY.setInterpolator(new AccelerateDecelerateInterpolator());

        // Rotación suave
        ObjectAnimator rotation = ObjectAnimator.ofFloat(particle, "rotation", 0f, 360f);
        rotation.setDuration(duration * 2);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());

        // Escala pulsante - CORREGIDO: Configurar repetición en animadores individuales
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(particle, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(particle, "scaleY", 1f, 1.2f, 1f);

        scaleX.setDuration(1000);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleY.setDuration(1000);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        // Combinar todas las animaciones
        fadeIn.start();

        fadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                floatY.start();
                rotation.start();
                scaleX.start();
                scaleY.start();
            }
        });
    }

    private void finishCelebration() {
        // Animación de salida suave
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(findViewById(android.R.id.content), "alpha", 1f, 0f);
        fadeOut.setDuration(300);

        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Terminar activity y regresar al menú principal
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.slide_down);
            }
        });

        fadeOut.start();
    }

    @Override
    public void onBackPressed() {
        // Permitir salir con el botón de regreso después de 2 segundos
        handler.postDelayed(() -> {
            finishCelebration();
        }, 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Limpiar handler para evitar memory leaks
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
