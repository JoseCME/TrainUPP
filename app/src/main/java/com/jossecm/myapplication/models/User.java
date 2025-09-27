package com.jossecm.myapplication.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.jossecm.myapplication.database.Converters;
import java.util.List;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;

    // Datos personales
    private String name;
    private int age;
    private String gender; // "Masculino", "Femenino", etc.

    // Datos físicos
    private float weight; // kg
    private float height; // cm

    // Datos de fitness
    private String fitnessLevel; // "Principiante", "Intermedio", "Avanzado"
    private String goal; // "Perder peso", "Ganar masa muscular", etc.

    // Equipamiento y preferencias
    @TypeConverters(Converters.class)
    private List<Integer> selectedEquipmentIds;

    @TypeConverters(Converters.class)
    private List<String> injuries; // Lista de lesiones

    private boolean useAI; // Preferencia de uso de IA

    // NUEVO: Días de entrenamiento disponibles por semana
    private int daysPerWeek; // Días que el usuario puede entrenar por semana
    private int workoutDuration; // Duración preferida en minutos (30, 45, 60, etc.)

    // Constructor vacío requerido por Room
    public User() {}

    // Constructor completo
    @Ignore
    public User(String name, int age, String gender, float weight, float height,
                String fitnessLevel, String goal, List<Integer> selectedEquipmentIds,
                List<String> injuries, boolean useAI, int daysPerWeek, int workoutDuration) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.weight = weight;
        this.height = height;
        this.fitnessLevel = fitnessLevel;
        this.goal = goal;
        this.selectedEquipmentIds = selectedEquipmentIds;
        this.injuries = injuries;
        this.useAI = useAI;
        this.daysPerWeek = daysPerWeek;
        this.workoutDuration = workoutDuration;
    }

    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public String getFitnessLevel() {
        return fitnessLevel;
    }

    public void setFitnessLevel(String fitnessLevel) {
        this.fitnessLevel = fitnessLevel;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public List<Integer> getSelectedEquipmentIds() {
        return selectedEquipmentIds;
    }

    public void setSelectedEquipmentIds(List<Integer> selectedEquipmentIds) {
        this.selectedEquipmentIds = selectedEquipmentIds;
    }

    public List<String> getInjuries() {
        return injuries;
    }

    public void setInjuries(List<String> injuries) {
        this.injuries = injuries;
    }

    public boolean isUseAI() {
        return useAI;
    }

    public void setUseAI(boolean useAI) {
        this.useAI = useAI;
    }

    public int getDaysPerWeek() {
        return daysPerWeek;
    }

    public void setDaysPerWeek(int daysPerWeek) {
        this.daysPerWeek = daysPerWeek;
    }

    public int getWorkoutDuration() {
        return workoutDuration;
    }

    public void setWorkoutDuration(int workoutDuration) {
        this.workoutDuration = workoutDuration;
    }

    // Métodos auxiliares para compatibilidad con fragmentos existentes
    public void setWantsAiRoutines(boolean wantsAi) {
        this.useAI = wantsAi;
    }

    public void setExperienceLevel(String level) {
        this.fitnessLevel = level;
    }

    public void setFitnessGoal(String goal) {
        this.goal = goal;
    }

    public void setAdditionalInjuries(String additionalInjuries) {
        // Agregar lesiones adicionales a la lista existente
        if (this.injuries == null) {
            this.injuries = new java.util.ArrayList<>();
        }
        if (additionalInjuries != null && !additionalInjuries.trim().isEmpty()) {
            this.injuries.add(additionalInjuries);
        }
    }
}
