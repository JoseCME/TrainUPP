package com.jossecm.myapplication.models;

public class Serie {
    private double peso;
    private int repeticiones;
    private boolean completada;
    private int numeroSerie; // NUEVO: Campo para preservar el número de serie

    public Serie() {
        this.peso = 0.0;
        this.repeticiones = 0;
        this.completada = false;
        this.numeroSerie = 0; // Inicializar con 0 (se asignará después)
    }

    public Serie(double peso, int repeticiones, boolean completada) {
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.completada = completada;
        this.numeroSerie = 0; // Inicializar con 0 (se asignará después)
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

    public double getVolumen() {
        return completada ? peso * repeticiones : 0.0;
    }

    // NUEVO: Getter y Setter para numeroSerie
    public int getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(int numeroSerie) {
        this.numeroSerie = numeroSerie;
    }
}
