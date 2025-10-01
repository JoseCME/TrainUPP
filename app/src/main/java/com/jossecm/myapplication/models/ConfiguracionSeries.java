package com.jossecm.myapplication.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "configuracion_series")
public class ConfiguracionSeries {
    @PrimaryKey
    private int exerciseId;

    private int numSeries;
    private long ultimaActualizacion;

    public ConfiguracionSeries() {}

    public ConfiguracionSeries(int exerciseId, int numSeries) {
        this.exerciseId = exerciseId;
        this.numSeries = numSeries;
        this.ultimaActualizacion = System.currentTimeMillis();
    }

    public int getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(int exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getNumSeries() {
        return numSeries;
    }

    public void setNumSeries(int numSeries) {
        this.numSeries = numSeries;
        this.ultimaActualizacion = System.currentTimeMillis();
    }

    public long getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(long ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }
}
