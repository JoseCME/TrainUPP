package com.jossecm.myapplication.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.jossecm.myapplication.models.Exercise;
import java.util.List;

@Dao
public interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Exercise exercise);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Exercise> exercises);

    @Update
    void update(Exercise exercise);

    @Delete
    void delete(Exercise exercise);

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    Exercise getExerciseById(int id);

    @Query("SELECT * FROM exercises")
    List<Exercise> getAllExercises();

    @Query("SELECT * FROM exercises WHERE muscleIds LIKE '%' || :muscleId || '%'")
    List<Exercise> getExercisesByMuscle(int muscleId);

    @Query("SELECT * FROM exercises WHERE equipmentIds LIKE '%' || :equipmentId || '%'")
    List<Exercise> getExercisesByEquipment(int equipmentId);

    @Query("DELETE FROM exercises")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM exercises")
    int getExerciseCount();
}

