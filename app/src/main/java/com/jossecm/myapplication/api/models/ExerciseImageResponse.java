package com.jossecm.myapplication.api.models;

import com.google.gson.annotations.SerializedName;

public class ExerciseImageResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("exercise")
    private int exercise;

    @SerializedName("image")
    private String image;

    @SerializedName("is_main")
    private boolean isMain;

    // Getters
    public int getId() { return id; }
    public int getExercise() { return exercise; }
    public String getImage() { return image; }
    public boolean isMain() { return isMain; }
}
