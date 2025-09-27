package com.jossecm.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

    private FitnessRepository repository;
    private User currentUser;

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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
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

        repository = new FitnessRepository(requireContext());
    }

    private void setupButtons() {
        buttonEditarPerfil.setOnClickListener(v -> {
            // Navegar al onboarding para editar perfil
            Intent intent = new Intent(getActivity(), OnboardingActivity.class);
            intent.putExtra("edit_mode", true);
            startActivity(intent);
        });
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
}
