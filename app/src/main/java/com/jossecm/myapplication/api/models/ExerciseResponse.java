package com.jossecm.myapplication.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ExerciseResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("creation_date")
    private String creationDate;

    @SerializedName("language")
    private int language;

    @SerializedName("description")
    private String description;

    @SerializedName("muscles")
    private List<Integer> muscles;

    @SerializedName("muscles_secondary")
    private List<Integer> musclesSecondary;

    @SerializedName("equipment")
    private List<Integer> equipment;

    @SerializedName("category")
    private int category;

    @SerializedName("status")
    private int status;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getUuid() { return uuid; }
    public String getCreationDate() { return creationDate; }
    public int getLanguage() { return language; }
    public String getDescription() { return description; }
    public List<Integer> getMuscles() { return muscles; }
    public List<Integer> getMusclesSecondary() { return musclesSecondary; }
    public List<Integer> getEquipment() { return equipment; }
    public int getCategory() { return category; }
    public int getStatus() { return status; }
}
