package com.jossecm.myapplication.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Mensaje {
    private String texto;
    private boolean esUsuario; // true = usuario, false = IA
    private long timestamp;
    private String nombreUsuario;
    private boolean enviando; // Para mostrar estado "enviando..."

    // NUEVO: Soporte para ejercicios recomendados
    private List<Exercise> ejerciciosRecomendados;
    private boolean tieneEjerciciosRecomendados;

    // Constructor principal
    public Mensaje(String texto, boolean esUsuario) {
        this.texto = texto;
        this.esUsuario = esUsuario;
        this.timestamp = System.currentTimeMillis();
        this.enviando = false;
        this.nombreUsuario = esUsuario ? "Tú" : "Coach IA";
        this.tieneEjerciciosRecomendados = false;
    }

    // Constructor completo
    public Mensaje(String texto, boolean esUsuario, String nombreUsuario) {
        this.texto = texto;
        this.esUsuario = esUsuario;
        this.timestamp = System.currentTimeMillis();
        this.enviando = false;
        this.nombreUsuario = nombreUsuario;
        this.tieneEjerciciosRecomendados = false;
    }

    // Getters y setters existentes
    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isEsUsuario() {
        return esUsuario;
    }

    public void setEsUsuario(boolean esUsuario) {
        this.esUsuario = esUsuario;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public boolean isEnviando() {
        return enviando;
    }

    public void setEnviando(boolean enviando) {
        this.enviando = enviando;
    }

    // NUEVOS: Getters y setters para ejercicios recomendados
    public List<Exercise> getEjerciciosRecomendados() {
        return ejerciciosRecomendados;
    }

    public void setEjerciciosRecomendados(List<Exercise> ejerciciosRecomendados) {
        this.ejerciciosRecomendados = ejerciciosRecomendados;
        this.tieneEjerciciosRecomendados = ejerciciosRecomendados != null && !ejerciciosRecomendados.isEmpty();
    }

    public boolean isTieneEjerciciosRecomendados() {
        return tieneEjerciciosRecomendados;
    }

    public void setTieneEjerciciosRecomendados(boolean tieneEjerciciosRecomendados) {
        this.tieneEjerciciosRecomendados = tieneEjerciciosRecomendados;
    }

    // Método auxiliar para formatear tiempo
    public String getTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Método auxiliar para formatear fecha
    public String getDateFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
