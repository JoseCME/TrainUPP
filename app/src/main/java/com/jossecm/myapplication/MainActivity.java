package com.jossecm.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jossecm.myapplication.fragments.HomeFragment;
import com.jossecm.myapplication.fragments.HistorialFragment;
import com.jossecm.myapplication.fragments.PerfilFragment;
import com.jossecm.myapplication.repository.FitnessRepository;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FitnessRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificar si el usuario ha completado el onboarding
        repository = new FitnessRepository(this);
        checkOnboardingStatus();

        initViews();
        setupBottomNavigation();

        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            loadFragment(HomeFragment.newInstance());
        }
    }

    private void checkOnboardingStatus() {
        repository.getCurrentUser(new FitnessRepository.DataCallback<com.jossecm.myapplication.models.User>() {
            @Override
            public void onSuccess(com.jossecm.myapplication.models.User user) {
                if (user == null) {
                    // No hay usuario, redirigir al onboarding
                    runOnUiThread(() -> {
                        Intent intent = new Intent(MainActivity.this, OnboardingActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }
            }

            @Override
            public void onError(String error) {
                // Error accediendo al usuario, asumir que necesita onboarding
                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Inicio - Rutinas (HomeFragment con FAB para crear rutinas)
                selectedFragment = HomeFragment.newInstance();
            } else if (itemId == R.id.nav_progress) {
                // Progreso - Historial de entrenamientos
                selectedFragment = HistorialFragment.newInstance();
            } else if (itemId == R.id.nav_profile) {
                // Perfil - Todos los datos del onboarding
                selectedFragment = PerfilFragment.newInstance();
            }

            if (selectedFragment != null) {
                return loadFragment(selectedFragment);
            }
            return false;
        });

        // Seleccionar el primer item por defecto
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
            return true;
        }
        return false;
    }
}