package com.jossecm.myapplication.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.jossecm.myapplication.database.Converters;
import java.util.List;

@Entity(tableName = "users")
@TypeConverters(Converters.class)
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;

    // Datos personales
    private String name;
    private int age;
    private double height; // en cm
    private double weight; // en kg
    private Gender gender;
    private double bmi; // calculado automáticamente

    // Datos fitness
    private ExperienceLevel experienceLevel;
    private FitnessGoal fitnessGoal;
    private int daysPerWeek;

    // Equipamiento seleccionado (IDs de la API)
    private List<Integer> selectedEquipmentIds;

    // Lesiones
    private List<String> injuries;
    private String additionalInjuries;

    // Preferencias
    private boolean wantsAiRoutines;

    public enum Gender {
        MALE, FEMALE
    }

    public enum ExperienceLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public enum FitnessGoal {
        LOSE_WEIGHT, GAIN_MUSCLE, MAINTENANCE, STRENGTH
    }

    // Constructor vacío para Room
    public User() {}

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getHeight() { return height; }
    public void setHeight(double height) {
        this.height = height;
        calculateBMI();
    }

    public double getWeight() { return weight; }
    public void setWeight(double weight) {
        this.weight = weight;
        calculateBMI();
    }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public double getBmi() { return bmi; }
    public void setBmi(double bmi) { this.bmi = bmi; }

    public ExperienceLevel getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(ExperienceLevel experienceLevel) { this.experienceLevel = experienceLevel; }

    public FitnessGoal getFitnessGoal() { return fitnessGoal; }
    public void setFitnessGoal(FitnessGoal fitnessGoal) { this.fitnessGoal = fitnessGoal; }

    public int getDaysPerWeek() { return daysPerWeek; }
    public void setDaysPerWeek(int daysPerWeek) { this.daysPerWeek = daysPerWeek; }

    public List<Integer> getSelectedEquipmentIds() { return selectedEquipmentIds; }
    public void setSelectedEquipmentIds(List<Integer> selectedEquipmentIds) { this.selectedEquipmentIds = selectedEquipmentIds; }

    public List<String> getInjuries() { return injuries; }
    public void setInjuries(List<String> injuries) { this.injuries = injuries; }

    public String getAdditionalInjuries() { return additionalInjuries; }
    public void setAdditionalInjuries(String additionalInjuries) { this.additionalInjuries = additionalInjuries; }

    public boolean isWantsAiRoutines() { return wantsAiRoutines; }
    public void setWantsAiRoutines(boolean wantsAiRoutines) { this.wantsAiRoutines = wantsAiRoutines; }

    // Método para calcular BMI automáticamente
    private void calculateBMI() {
        if (height > 0 && weight > 0) {
            double heightInMeters = height / 100.0;
            this.bmi = weight / (heightInMeters * heightInMeters);
        }
    }

    // Método para obtener descripción del BMI
    public String getBMIDescription() {
        if (bmi < 18.5) return "Bajo peso";
        else if (bmi < 25) return "Peso normal";
        else if (bmi < 30) return "Sobrepeso";
        else return "Obesidad";
    }
}
