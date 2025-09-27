package com.jossecm.myapplication.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "rutinas")
public class Rutina {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String nombre;
    private long fechaCreacion; // Cambiar Date por long (timestamp)
    private String ejerciciosIds; // JSON string con los IDs de ejercicios
    private int cantidadEjercicios;

    public Rutina() {
        this.fechaCreacion = System.currentTimeMillis();
    }

    @Ignore
    public Rutina(String nombre, String ejerciciosIds, int cantidadEjercicios) {
        this.nombre = nombre;
        this.ejerciciosIds = ejerciciosIds;
        this.cantidadEjercicios = cantidadEjercicios;
        this.fechaCreacion = System.currentTimeMillis();
    }

    // Getters y setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getEjerciciosIds() {
        return ejerciciosIds;
    }

    public void setEjerciciosIds(String ejerciciosIds) {
        this.ejerciciosIds = ejerciciosIds;
    }

    public int getCantidadEjercicios() {
        return cantidadEjercicios;
    }

    public void setCantidadEjercicios(int cantidadEjercicios) {
        this.cantidadEjercicios = cantidadEjercicios;
    }
}
