package com.jossecm.myapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.jossecm.myapplication.models.HistorialEntrenamiento;
import java.util.List;

@Dao
public interface HistorialEntrenamientoDao {
    @Insert
    long insert(HistorialEntrenamiento historial);

    @Query("SELECT * FROM historial_entrenamientos ORDER BY fecha DESC")
    List<HistorialEntrenamiento> getAllHistorial();

    @Query("SELECT * FROM historial_entrenamientos WHERE rutinaId = :rutinaId ORDER BY fecha DESC")
    List<HistorialEntrenamiento> getHistorialByRutina(long rutinaId);

    @Query("SELECT * FROM historial_entrenamientos WHERE id = :id")
    HistorialEntrenamiento getHistorialById(long id);
}
