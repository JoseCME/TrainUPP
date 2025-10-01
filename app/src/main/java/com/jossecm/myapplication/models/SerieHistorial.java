package com.jossecm.myapplication.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "serie_historial")
public class SerieHistorial {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "exercise_id")
    private int exerciseId;

    @ColumnInfo(name = "numero_serie")
    private int numeroSerie;

    @ColumnInfo(name = "peso")
    private double peso;

    @ColumnInfo(name = "repeticiones")
    private int repeticiones;

    @ColumnInfo(name = "completada")
    private boolean completada;

    @ColumnInfo(name = "fecha_entrenamiento")
    private long fechaEntrenamiento; // timestamp en milisegundos

    // Constructor vacío requerido por Room
    public SerieHistorial() {}

    public SerieHistorial(int exerciseId, int numeroSerie, double peso, int repeticiones,
                         boolean completada, long fechaEntrenamiento) {
        this.exerciseId = exerciseId;
        this.numeroSerie = numeroSerie;
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.completada = completada;
        this.fechaEntrenamiento = fechaEntrenamiento;
    }

    // Getters y Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(int exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(int numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public int getRepeticiones() {
        return repeticiones;
    }

    public void setRepeticiones(int repeticiones) {
        this.repeticiones = repeticiones;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    public long getFechaEntrenamiento() {
        return fechaEntrenamiento;
    }

    public void setFechaEntrenamiento(long fechaEntrenamiento) {
        this.fechaEntrenamiento = fechaEntrenamiento;
    }

    public double getVolumen() {
        return completada ? peso * repeticiones : 0.0;
    }
}
