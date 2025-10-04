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
import com.jossecm.myapplication.DetalleHistorialActivity;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.adapters.HistorialAdapter;
import com.jossecm.myapplication.models.HistorialEntrenamiento;
import com.jossecm.myapplication.repository.FitnessRepository;
import java.util.ArrayList;
import java.util.List;

public class HistorialFragment extends Fragment {

    private RecyclerView recyclerViewHistorial;
    private LinearLayout layoutLoading, layoutEmpty;
    private HistorialAdapter historialAdapter;
    private FitnessRepository repository;
    private List<HistorialEntrenamiento> historialList = new ArrayList<>();

    public static HistorialFragment newInstance() {
        return new HistorialFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        initViews(view);
        setupRecyclerView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistorial();
    }

    private void initViews(View view) {
        recyclerViewHistorial = view.findViewById(R.id.recyclerViewHistorial);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        repository = new FitnessRepository(requireContext());
    }

    private void setupRecyclerView() {
        historialAdapter = new HistorialAdapter(historialList);

        // Configurar listener de clicks
        historialAdapter.setOnHistorialClickListener(historial -> {
            // Abrir DetalleHistorialActivity
            Intent intent = new Intent(getActivity(), DetalleHistorialActivity.class);
            intent.putExtra("historial_id", historial.getId());
            startActivity(intent);
        });

        recyclerViewHistorial.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewHistorial.setAdapter(historialAdapter);
    }

    private void loadHistorial() {
        showLoading();

        repository.getAllHistorial(new FitnessRepository.DataCallback<List<HistorialEntrenamiento>>() {
            @Override
            public void onSuccess(List<HistorialEntrenamiento> historial) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (historial != null && !historial.isEmpty()) {
                            historialList.clear();
                            historialList.addAll(historial);
                            historialAdapter.notifyDataSetChanged();
                            showContent();

                            android.util.Log.d("HistorialFragment",
                                "Historial cargado: " + historial.size() + " entrenamientos");
                        } else {
                            showEmpty();
                            android.util.Log.d("HistorialFragment",
                                "No hay entrenamientos en el historial");
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.e("HistorialFragment",
                            "Error cargando historial: " + error);
                        Toast.makeText(getContext(),
                                     "Error cargando historial: " + error, Toast.LENGTH_LONG).show();
                        showEmpty();
                    });
                }
            }
        });
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        recyclerViewHistorial.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewHistorial.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmpty() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewHistorial.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }
}
