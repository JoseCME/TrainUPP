package com.jossecm.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.Equipment;
import com.jossecm.myapplication.repository.FitnessRepository;
import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder> {

    private List<Equipment> equipmentList;
    private FitnessRepository repository;

    public EquipmentAdapter(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
    }

    public void setRepository(FitnessRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cbEquipment;
        private TextView tvEquipmentName;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            cbEquipment = itemView.findViewById(R.id.cbEquipment);
            tvEquipmentName = itemView.findViewById(R.id.tvEquipmentName);
        }

        public void bind(Equipment equipment) {
            tvEquipmentName.setText(equipment.getName());
            cbEquipment.setChecked(equipment.isSelected());

            // Manejar cambios en la selección
            cbEquipment.setOnCheckedChangeListener((buttonView, isChecked) -> {
                equipment.setSelected(isChecked);

                // Actualizar en la base de datos si el repository está disponible
                if (repository != null) {
                    repository.updateEquipmentSelection(equipment.getId(), isChecked);
                }
            });

            // También permitir selección tocando el item completo
            itemView.setOnClickListener(v -> {
                cbEquipment.setChecked(!cbEquipment.isChecked());
            });
        }
    }
}
