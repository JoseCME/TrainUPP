package com.jossecm.myapplication.api.models;

import com.google.gson.annotations.SerializedName;

public class ExerciseTranslationResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("exercise")
    private int exerciseId;

    @SerializedName("language")
    private int language;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    // Constructor vacío
    public ExerciseTranslationResponse() {}

    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(int exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getLanguage() {
        return language;
    }

    public void setLanguage(int language) {
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
