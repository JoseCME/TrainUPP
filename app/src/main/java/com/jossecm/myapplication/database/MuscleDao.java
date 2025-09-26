package com.jossecm.myapplication.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.jossecm.myapplication.models.Muscle;
import java.util.List;

@Dao
public interface MuscleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Muscle muscle);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Muscle> muscles);

    @Update
    void update(Muscle muscle);

    @Delete
    void delete(Muscle muscle);

    @Query("SELECT * FROM muscles WHERE id = :id LIMIT 1")
    Muscle getMuscleById(int id);

    @Query("SELECT * FROM muscles")
    List<Muscle> getAllMuscles();

    @Query("SELECT * FROM muscles WHERE id IN (:muscleIds)")
    List<Muscle> getMusclesByIds(List<Integer> muscleIds);

    @Query("DELETE FROM muscles")
    void deleteAll();
}
