package com.jossecm.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.jossecm.myapplication.ExerciseListActivity;
import com.jossecm.myapplication.R;

public class HomeFragment extends Fragment {

    private MaterialButton btnNewRoutine;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        btnNewRoutine = view.findViewById(R.id.btnNewRoutine);
    }

    private void setupClickListeners() {
        btnNewRoutine.setOnClickListener(v -> {
            // Navegar a la pantalla de ejercicios
            Intent intent = new Intent(getActivity(), ExerciseListActivity.class);
            startActivity(intent);
        });
    }
}
