package com.jossecm.myapplication.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "equipment")
public class Equipment {
    @PrimaryKey
    private int id;

    private String name;
    private boolean selected; // Para tracking de selección durante onboarding

    // Constructor vacío para Room
    public Equipment() {}

    @Ignore
    public Equipment(int id, String name) {
        this.id = id;
        this.name = name;
        this.selected = false;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
