package com.jossecm.myapplication.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.jossecm.myapplication.OnboardingActivity;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.User;
import com.jossecm.myapplication.repository.FitnessRepository;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PerfilFragment extends Fragment {

    private LinearLayout layoutLoading, layoutContent;
    private TextView textViewNombre;
    private TextView textViewEdad;
    private TextView textViewGenero;
    private TextView textViewPeso;
    private TextView textViewAltura;
    private TextView textViewNivelFitness;
    private TextView textViewObjetivo;
    private TextView textViewEquipamiento;
    private TextView textViewLesiones;
    private TextView textViewPreferenciaIA;
    private TextView textViewFechaRegistro;
    private Button buttonEditarPerfil;
    private ImageView imageViewAvatar;

    private FitnessRepository repository;
    private User currentUser;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private SharedPreferences profilePrefs;
    private static final String PREFS_NAME = "profile_prefs";
    private static final String KEY_PROFILE_IMAGE_URI = "profile_image_uri";

    public static PerfilFragment newInstance() {
        return new PerfilFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        initViews(view);
        setupButtons();
        setupAvatarPicker();
        loadAvatarFromPrefs();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
        // Reintentar cargar el avatar por si regresamos del picker
        loadAvatarFromPrefs();
    }

    private void initViews(View view) {
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutContent = view.findViewById(R.id.layoutContent);

        textViewNombre = view.findViewById(R.id.textViewNombre);
        textViewEdad = view.findViewById(R.id.textViewEdad);
        textViewGenero = view.findViewById(R.id.textViewGenero);
        textViewPeso = view.findViewById(R.id.textViewPeso);
        textViewAltura = view.findViewById(R.id.textViewAltura);
        textViewNivelFitness = view.findViewById(R.id.textViewNivelFitness);
        textViewObjetivo = view.findViewById(R.id.textViewObjetivo);
        textViewEquipamiento = view.findViewById(R.id.textViewEquipamiento);
        textViewLesiones = view.findViewById(R.id.textViewLesiones);
        textViewPreferenciaIA = view.findViewById(R.id.textViewPreferenciaIA);
        textViewFechaRegistro = view.findViewById(R.id.textViewFechaRegistro);
        buttonEditarPerfil = view.findViewById(R.id.buttonEditarPerfil);
        imageViewAvatar = view.findViewById(R.id.imageViewAvatar);

        repository = new FitnessRepository(requireContext());
        profilePrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void setupButtons() {
        buttonEditarPerfil.setOnClickListener(v -> {
            // Navegar al onboarding para editar perfil
            Intent intent = new Intent(getActivity(), OnboardingActivity.class);
            intent.putExtra("edit_mode", true);
            startActivity(intent);
        });
    }

    // Configurar el selector de imágenes desde la galería (SAF) y manejar el resultado
    private void setupAvatarPicker() {
        pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Intentar persistir permisos para acceso posterior
                        try {
                            // Persistir permiso de lectura para acceder al recurso en sesiones futuras
                            requireContext().getContentResolver().takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (SecurityException ignored) { }

                        // Guardar en preferencias y mostrar
                        profilePrefs.edit().putString(KEY_PROFILE_IMAGE_URI, uri.toString()).apply();
                        Glide.with(this).load(uri).into(imageViewAvatar);
                    }
                }
            }
        );

        if (imageViewAvatar != null) {
            imageViewAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                pickImageLauncher.launch(intent);
            });
        }
    }

    private void loadUserData() {
        showLoading();

        repository.getCurrentUser(new FitnessRepository.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (user != null) {
                            currentUser = user;
                            displayUserData();
                            showContent();
                        } else {
                            Toast.makeText(getContext(), "No se encontraron datos de usuario", Toast.LENGTH_SHORT).show();
                            showLoading();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.e("PerfilFragment", "Error cargando usuario: " + error);
                        Toast.makeText(getContext(), "Error cargando perfil: " + error, Toast.LENGTH_LONG).show();
                        showLoading();
                    });
                }
            }
        });
    }

    private void displayUserData() {
        // Datos personales
        textViewNombre.setText(currentUser.getName() != null ? currentUser.getName() : "No especificado");
        textViewEdad.setText(currentUser.getAge() > 0 ? currentUser.getAge() + " años" : "No especificado");
        textViewGenero.setText(currentUser.getGender() != null ? currentUser.getGender() : "No especificado");

        // Datos físicos
        textViewPeso.setText(currentUser.getWeight() > 0 ?
            String.format(Locale.getDefault(), "%.1f kg", currentUser.getWeight()) : "No especificado");
        textViewAltura.setText(currentUser.getHeight() > 0 ?
            String.format(Locale.getDefault(), "%.0f cm", currentUser.getHeight()) : "No especificado");

        // Datos de fitness
        textViewNivelFitness.setText(currentUser.getFitnessLevel() != null ?
            currentUser.getFitnessLevel() : "No especificado");
        textViewObjetivo.setText(currentUser.getGoal() != null ?
            currentUser.getGoal() : "No especificado");

        // Equipamiento (mostrar cantidad)
        if (currentUser.getSelectedEquipmentIds() != null && !currentUser.getSelectedEquipmentIds().isEmpty()) {
            textViewEquipamiento.setText(currentUser.getSelectedEquipmentIds().size() + " equipos seleccionados");
        } else {
            textViewEquipamiento.setText("Ningún equipo seleccionado");
        }

        // Lesiones
        if (currentUser.getInjuries() != null && !currentUser.getInjuries().isEmpty()) {
            textViewLesiones.setText("Tiene lesiones registradas");
        } else {
            textViewLesiones.setText("Sin lesiones registradas");
        }

        // Preferencia de IA
        textViewPreferenciaIA.setText(currentUser.isUseAI() ? "Habilitada" : "Deshabilitada");

        // Fecha de registro (si tienes este campo en User)
        textViewFechaRegistro.setText("Usuario registrado");
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
    }

    private void loadAvatarFromPrefs() {
        if (imageViewAvatar == null || profilePrefs == null) return;
        String uriString = profilePrefs.getString(KEY_PROFILE_IMAGE_URI, null);
        if (uriString != null) {
            try {
                Uri uri = Uri.parse(uriString);
                Glide.with(this).load(uri).into(imageViewAvatar);
            } catch (Exception ignored) { }
        } else {
            // Mantener el placeholder actual (ic_person) definido en el layout
        }
    }
}
