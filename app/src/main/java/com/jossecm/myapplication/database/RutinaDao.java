package com.jossecm.myapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.jossecm.myapplication.models.Rutina;
import java.util.List;

@Dao
public interface RutinaDao {
    @Insert
    long insert(Rutina rutina);

    @Update
    void updateRutina(Rutina rutina);

    @Query("SELECT * FROM rutinas ORDER BY fechaCreacion DESC")
    List<Rutina> getAllRutinas();

    @Query("SELECT * FROM rutinas WHERE id = :id")
    Rutina getRutinaById(long id);

    @Query("DELETE FROM rutinas WHERE id = :id")
    void deleteRutina(long id);
}
