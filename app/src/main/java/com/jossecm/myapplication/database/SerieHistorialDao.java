package com.jossecm.myapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.jossecm.myapplication.models.SerieHistorial;
import java.util.List;

@Dao
public interface SerieHistorialDao {

    @Insert
    void insertSerie(SerieHistorial serie);

    @Insert
    void insertSeries(List<SerieHistorial> series);

    // Obtener las series de la última sesión de un ejercicio específico
    @Query("SELECT * FROM serie_historial " +
           "WHERE exercise_id = :exerciseId " +
           "AND fecha_entrenamiento = (" +
           "    SELECT MAX(fecha_entrenamiento) " +
           "    FROM serie_historial " +
           "    WHERE exercise_id = :exerciseId" +
           ") " +
           "ORDER BY numero_serie ASC")
    List<SerieHistorial> getUltimasSeriesEjercicio(int exerciseId);

    // Obtener todas las series históricas de un ejercicio
    @Query("SELECT * FROM serie_historial " +
           "WHERE exercise_id = :exerciseId " +
           "ORDER BY fecha_entrenamiento DESC, numero_serie ASC")
    List<SerieHistorial> getHistorialCompleto(int exerciseId);

    // Eliminar una serie específica por número y fecha de sesión
    @Query("DELETE FROM serie_historial " +
           "WHERE exercise_id = :exerciseId " +
           "AND numero_serie = :numeroSerie " +
           "AND fecha_entrenamiento = :fechaEntrenamiento")
    void eliminarSeriePorNumeroYFecha(int exerciseId, int numeroSerie, long fechaEntrenamiento);

    // Eliminar series más antiguas (mantener solo las últimas N sesiones)
    @Query("DELETE FROM serie_historial " +
           "WHERE exercise_id = :exerciseId " +
           "AND fecha_entrenamiento NOT IN (" +
           "    SELECT DISTINCT fecha_entrenamiento " +
           "    FROM serie_historial " +
           "    WHERE exercise_id = :exerciseId " +
           "    ORDER BY fecha_entrenamiento DESC " +
           "    LIMIT :limiteSesiones" +
           ")")
    void limpiarHistorialAntiguo(int exerciseId, int limiteSesiones);

    // NUEVO: Obtener todas las series de una fecha específica
    @Query("SELECT * FROM serie_historial " +
           "WHERE fecha_entrenamiento = :fechaEntrenamiento " +
           "ORDER BY exercise_id ASC, numero_serie ASC")
    List<SerieHistorial> getSeriesPorFecha(long fechaEntrenamiento);
}
