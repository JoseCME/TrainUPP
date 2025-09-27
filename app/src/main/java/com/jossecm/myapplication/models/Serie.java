package com.jossecm.myapplication.models;

public class Serie {
    private double peso;
    private int repeticiones;
    private boolean completada;

    public Serie() {
        this.peso = 0.0;
        this.repeticiones = 0;
        this.completada = false;
    }

    public Serie(double peso, int repeticiones, boolean completada) {
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.completada = completada;
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
}
