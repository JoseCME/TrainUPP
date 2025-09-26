package com.jossecm.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.jossecm.myapplication.fragments.*;
import com.jossecm.myapplication.models.User;
import com.jossecm.myapplication.repository.FitnessRepository;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnPrevious, btnNext;
    private OnboardingPagerAdapter adapter;
    private FitnessRepository repository;
    private User currentUser;

    // Lista de fragmentos
    private PersonalDataFragment personalDataFragment;
    private FitnessDataFragment fitnessDataFragment;
    private EquipmentFragment equipmentFragment;
    private InjuriesFragment injuriesFragment;
    private PreferencesFragment preferencesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar barras del sistema para evitar solapamiento
        setupSystemBars();

        setContentView(R.layout.activity_onboarding);

        initViews();
        setupViewPager();
        setupButtons();

        repository = new FitnessRepository(this);
        currentUser = new User();
    }

    private void setupSystemBars() {
        // Configurar para que el contenido no se solape con las barras del sistema
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                getWindow().getDecorView().getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
    }

    private void setupViewPager() {
        // Crear fragmentos
        personalDataFragment = PersonalDataFragment.newInstance();
        fitnessDataFragment = FitnessDataFragment.newInstance();
        equipmentFragment = EquipmentFragment.newInstance();
        injuriesFragment = InjuriesFragment.newInstance();
        preferencesFragment = PreferencesFragment.newInstance();

        adapter = new OnboardingPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Deshabilitar swipe manual
        viewPager.setUserInputEnabled(false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateButtons(position);
            }
        });
    }

    private void setupButtons() {
        btnPrevious.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1);
            }
        });

        btnNext.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();

            if (validateCurrentPage(currentItem)) {
                if (currentItem < adapter.getItemCount() - 1) {
                    viewPager.setCurrentItem(currentItem + 1);
                } else {
                    // Última página - completar onboarding
                    completeOnboarding();
                }
            }
        });
    }

    private boolean validateCurrentPage(int position) {
        switch (position) {
            case 0: // Personal Data
                if (!personalDataFragment.isValid()) {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            case 1: // Fitness Data
                if (!fitnessDataFragment.isValid()) {
                    Toast.makeText(this, "Por favor selecciona tu nivel y objetivo", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            case 2: // Equipment
                if (!equipmentFragment.hasSelectedEquipment()) {
                    Toast.makeText(this, "Por favor selecciona al menos un equipamiento", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            case 3: // Injuries (siempre válido)
                break;
            case 4: // Preferences
                if (!preferencesFragment.isValid()) {
                    Toast.makeText(this, "Por favor selecciona tu preferencia sobre IA", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
        }
        return true;
    }

    private void updateButtons(int position) {
        // Actualizar visibilidad del botón anterior
        btnPrevious.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);

        // Actualizar texto del botón siguiente
        if (position == adapter.getItemCount() - 1) {
            btnNext.setText("Finalizar");
        } else {
            btnNext.setText("Siguiente");
        }
    }

    private void completeOnboarding() {
        // Recopilar datos de todos los fragmentos
        personalDataFragment.fillUserData(currentUser);
        fitnessDataFragment.fillUserData(currentUser);
        equipmentFragment.fillUserData(currentUser);
        injuriesFragment.fillUserData(currentUser);
        preferencesFragment.fillUserData(currentUser);

        // Mostrar progreso
        btnNext.setEnabled(false);
        btnNext.setText("Guardando...");

        // Guardar usuario en base de datos
        repository.saveUser(currentUser, new FitnessRepository.DataCallback<Long>() {
            @Override
            public void onSuccess(Long userId) {
                runOnUiThread(() -> {
                    currentUser.setId(userId.intValue());

                    // Navegar a la pantalla de descarga en lugar de descargar aquí
                    Intent intent = new Intent(OnboardingActivity.this, DataDownloadActivity.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(OnboardingActivity.this,
                                 "Error guardando usuario: " + error, Toast.LENGTH_LONG).show();
                    btnNext.setEnabled(true);
                    btnNext.setText("Finalizar");
                });
            }
        });
    }

    // Adapter para ViewPager2
    private class OnboardingPagerAdapter extends FragmentStateAdapter {

        public OnboardingPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return personalDataFragment;
                case 1: return fitnessDataFragment;
                case 2: return equipmentFragment;
                case 3: return injuriesFragment;
                case 4: return preferencesFragment;
                default: return personalDataFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
