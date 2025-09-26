package com.jossecm.myapplication.api.models;

import com.google.gson.annotations.SerializedName;

public class EquipmentResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
}
