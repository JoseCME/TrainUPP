package com.jossecm.myapplication.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "historial_entrenamientos")
public class HistorialEntrenamiento {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long rutinaId;
    private String nombreRutina;
    private long fecha; // Cambiar Date por long (timestamp)
    private int duracionMinutos;
    private double volumenTotal;
    private String ejerciciosRealizados; // JSON string con los ejercicios y series realizadas

    public HistorialEntrenamiento() {
        this.fecha = System.currentTimeMillis();
    }

    @Ignore
    public HistorialEntrenamiento(long rutinaId, String nombreRutina, int duracionMinutos,
                                double volumenTotal, String ejerciciosRealizados) {
        this.rutinaId = rutinaId;
        this.nombreRutina = nombreRutina;
        this.duracionMinutos = duracionMinutos;
        this.volumenTotal = volumenTotal;
        this.ejerciciosRealizados = ejerciciosRealizados;
        this.fecha = System.currentTimeMillis();
    }

    // Getters y setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRutinaId() {
        return rutinaId;
    }

    public void setRutinaId(long rutinaId) {
        this.rutinaId = rutinaId;
    }

    public String getNombreRutina() {
        return nombreRutina;
    }

    public void setNombreRutina(String nombreRutina) {
        this.nombreRutina = nombreRutina;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public double getVolumenTotal() {
        return volumenTotal;
    }

    public void setVolumenTotal(double volumenTotal) {
        this.volumenTotal = volumenTotal;
    }

    public String getEjerciciosRealizados() {
        return ejerciciosRealizados;
    }

    public void setEjerciciosRealizados(String ejerciciosRealizados) {
        this.ejerciciosRealizados = ejerciciosRealizados;
    }
}
