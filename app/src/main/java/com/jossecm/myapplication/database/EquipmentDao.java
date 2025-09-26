package com.jossecm.myapplication.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.jossecm.myapplication.models.Equipment;
import java.util.List;

@Dao
public interface EquipmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Equipment equipment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Equipment> equipment);

    @Update
    void update(Equipment equipment);

    @Delete
    void delete(Equipment equipment);

    @Query("SELECT * FROM equipment WHERE id = :id LIMIT 1")
    Equipment getEquipmentById(int id);

    @Query("SELECT * FROM equipment")
    List<Equipment> getAllEquipment();

    @Query("SELECT * FROM equipment WHERE selected = 1")
    List<Equipment> getSelectedEquipment();

    @Query("UPDATE equipment SET selected = :selected WHERE id = :id")
    void updateSelection(int id, boolean selected);

    @Query("DELETE FROM equipment")
    void deleteAll();
}
