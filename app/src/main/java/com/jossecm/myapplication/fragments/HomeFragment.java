package com.jossecm.myapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jossecm.myapplication.CrearRutinaActivity;
import com.jossecm.myapplication.EjecutarRutinaActivity;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.adapters.RutinaAdapter;
import com.jossecm.myapplication.models.Rutina;
import com.jossecm.myapplication.repository.FitnessRepository;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewRutinas;
    private LinearLayout layoutLoading, layoutEmpty;
    private FloatingActionButton fabCrearRutina;
    private RutinaAdapter rutinaAdapter;
    private FitnessRepository repository;
    private List<Rutina> rutinaList = new ArrayList<>();

    // Método estático newInstance() requerido por MainActivity
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupRecyclerView();
        setupFab();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRutinas();
    }

    private void initViews(View view) {
        recyclerViewRutinas = view.findViewById(R.id.recyclerViewRutinas);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        fabCrearRutina = view.findViewById(R.id.fabCrearRutina);

        repository = new FitnessRepository(requireContext());
    }

    private void setupRecyclerView() {
        rutinaAdapter = new RutinaAdapter(rutinaList);
        rutinaAdapter.setOnRutinaClickListener(rutina -> {
            // Navegación a EjecutarRutinaActivity
            Intent intent = new Intent(getActivity(), EjecutarRutinaActivity.class);
            intent.putExtra("rutina_id", rutina.getId());
            startActivity(intent);
        });

        recyclerViewRutinas.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRutinas.setAdapter(rutinaAdapter);
    }

    private void setupFab() {
        fabCrearRutina.setOnClickListener(v -> {
            // Navegación a CrearRutinaActivity
            Intent intent = new Intent(getActivity(), CrearRutinaActivity.class);
            startActivity(intent);
        });
    }

    private void loadRutinas() {
        showLoading();

        repository.getAllRutinas(new FitnessRepository.DataCallback<List<Rutina>>() {
            @Override
            public void onSuccess(List<Rutina> rutinas) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (rutinas != null && !rutinas.isEmpty()) {
                            rutinaList.clear();
                            rutinaList.addAll(rutinas);
                            rutinaAdapter.notifyDataSetChanged();
                            showContent();

                            // Log para debugging
                            android.util.Log.d("HomeFragment",
                                "Rutinas cargadas: " + rutinas.size());
                        } else {
                            showEmpty();
                            android.util.Log.d("HomeFragment",
                                "No hay rutinas creadas");
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.e("HomeFragment",
                            "Error cargando rutinas: " + error);
                        Toast.makeText(getContext(),
                                     "Error cargando rutinas: " + error, Toast.LENGTH_LONG).show();
                        showEmpty();
                    });
                }
            }
        });
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        recyclerViewRutinas.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewRutinas.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmpty() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewRutinas.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }
}
