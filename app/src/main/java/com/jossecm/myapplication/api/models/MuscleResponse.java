package com.jossecm.myapplication.api.models;

import com.google.gson.annotations.SerializedName;

public class MuscleResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("name_en")
    private String nameEn;

    @SerializedName("is_front")
    private boolean isFront;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getNameEn() { return nameEn; }
    public boolean isFront() { return isFront; }
}
