package com.jossecm.myapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.jossecm.myapplication.models.ConfiguracionSeries;

@Dao
public interface ConfiguracionSeriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ConfiguracionSeries configuracion);

    @Query("SELECT * FROM configuracion_series WHERE exerciseId = :exerciseId")
    ConfiguracionSeries getConfiguracion(int exerciseId);

    @Query("SELECT numSeries FROM configuracion_series WHERE exerciseId = :exerciseId")
    Integer getNumSeries(int exerciseId);

    @Query("DELETE FROM configuracion_series WHERE exerciseId = :exerciseId")
    void deleteConfiguracion(int exerciseId);

    @Query("DELETE FROM configuracion_series")
    void deleteAll();
}
