package com.jossecm.myapplication.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.jossecm.myapplication.database.Converters;
import java.util.List;

@Entity(tableName = "exercises")
@TypeConverters(Converters.class)
public class Exercise {
    @PrimaryKey
    private int id;

    private String name;
    private String description;
    private List<Integer> muscleIds; // IDs de los músculos objetivo
    private List<String> muscleNames; // Nombres de los músculos (cacheados)
    private List<Integer> equipmentIds; // IDs del equipamiento requerido
    private String imageUrl; // URL de la imagen principal
    private int categoryId; // ID de la categoría del ejercicio
    private String categoryName; // Nombre de la categoría (cacheado)

    // Constructor vacío para Room
    public Exercise() {}

    @Ignore
    public Exercise(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Integer> getMuscleIds() { return muscleIds; }
    public void setMuscleIds(List<Integer> muscleIds) { this.muscleIds = muscleIds; }

    public List<String> getMuscleNames() { return muscleNames; }
    public void setMuscleNames(List<String> muscleNames) { this.muscleNames = muscleNames; }

    public List<Integer> getEquipmentIds() { return equipmentIds; }
    public void setEquipmentIds(List<Integer> equipmentIds) { this.equipmentIds = equipmentIds; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    // Método helper para obtener el primer músculo principal
    public String getPrimaryMuscle() {
        if (muscleNames != null && !muscleNames.isEmpty()) {
            return muscleNames.get(0);
        }
        return "Múltiples músculos";
    }
}
