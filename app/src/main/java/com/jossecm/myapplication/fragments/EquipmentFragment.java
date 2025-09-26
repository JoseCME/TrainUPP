package com.jossecm.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.adapters.EquipmentAdapter;
import com.jossecm.myapplication.models.Equipment;
import com.jossecm.myapplication.models.User;
import com.jossecm.myapplication.repository.FitnessRepository;
import java.util.ArrayList;
import java.util.List;

public class EquipmentFragment extends Fragment {

    private RecyclerView recyclerViewEquipment;
    private ProgressBar progressBarEquipment;
    private TextView tvLoadingEquipment;
    private EquipmentAdapter equipmentAdapter;
    private FitnessRepository repository;
    private List<Equipment> equipmentList = new ArrayList<>();

    public static EquipmentFragment newInstance() {
        return new EquipmentFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_equipment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        loadEquipment();
    }

    private void initViews(View view) {
        recyclerViewEquipment = view.findViewById(R.id.recyclerViewEquipment);
        progressBarEquipment = view.findViewById(R.id.progressBarEquipment);
        tvLoadingEquipment = view.findViewById(R.id.tvLoadingEquipment);
        repository = new FitnessRepository(requireContext());
    }

    private void setupRecyclerView() {
        equipmentAdapter = new EquipmentAdapter(equipmentList);
        equipmentAdapter.setRepository(repository); // Agregar el repository al adapter
        recyclerViewEquipment.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewEquipment.setAdapter(equipmentAdapter);
    }

    private void loadEquipment() {
        // Primero intentar cargar desde la base de datos local
        repository.getAllEquipment(new FitnessRepository.DataCallback<List<Equipment>>() {
            @Override
            public void onSuccess(List<Equipment> data) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (data != null && !data.isEmpty()) {
                        // Hay datos en la base de datos local
                        equipmentList.clear();
                        equipmentList.addAll(data);
                        equipmentAdapter.notifyDataSetChanged();
                        showContent();
                    } else {
                        // No hay datos locales, cargar desde la API
                        loadFromApi();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    // Error en base de datos local, intentar cargar desde API
                    loadFromApi();
                });
            }
        });
    }

    private void loadFromApi() {
        tvLoadingEquipment.setText("Descargando equipamiento...");

        repository.loadEquipmentFromApi(new FitnessRepository.DataCallback<List<Equipment>>() {
            @Override
            public void onSuccess(List<Equipment> data) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    equipmentList.clear();
                    equipmentList.addAll(data);
                    equipmentAdapter.notifyDataSetChanged();
                    showContent();
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error cargando equipamiento: " + error,
                                 Toast.LENGTH_LONG).show();
                    tvLoadingEquipment.setText("Error cargando equipamiento. Toca para reintentar.");
                    tvLoadingEquipment.setOnClickListener(v -> {
                        tvLoadingEquipment.setOnClickListener(null);
                        loadFromApi();
                    });
                });
            }
        });
    }

    private void showContent() {
        progressBarEquipment.setVisibility(View.GONE);
        tvLoadingEquipment.setVisibility(View.GONE);
        recyclerViewEquipment.setVisibility(View.VISIBLE);
    }

    public boolean hasSelectedEquipment() {
        for (Equipment equipment : equipmentList) {
            if (equipment.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public void fillUserData(User user) {
        List<Integer> selectedIds = new ArrayList<>();
        for (Equipment equipment : equipmentList) {
            if (equipment.isSelected()) {
                selectedIds.add(equipment.getId());
            }
        }
        user.setSelectedEquipmentIds(selectedIds);
    }
}
