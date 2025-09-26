package com.jossecm.myapplication.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "muscles")
public class Muscle {
    @PrimaryKey
    private int id;

    private String name;
    private String nameEn; // Nombre en inglés (desde la API)
    private boolean isFront; // Si es un músculo frontal

    // Constructor vacío para Room
    public Muscle() {}

    @Ignore
    public Muscle(int id, String name, String nameEn, boolean isFront) {
        this.id = id;
        this.name = name;
        this.nameEn = nameEn;
        this.isFront = isFront;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public boolean isFront() { return isFront; }
    public void setFront(boolean front) { isFront = front; }
}
