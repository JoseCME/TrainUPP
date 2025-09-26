package com.jossecm.myapplication.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;
import com.jossecm.myapplication.models.User;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.models.Equipment;
import com.jossecm.myapplication.models.Muscle;

@Database(
    entities = {User.class, Exercise.class, Equipment.class, Muscle.class},
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract ExerciseDao exerciseDao();
    public abstract EquipmentDao equipmentDao();
    public abstract MuscleDao muscleDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "fitness_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
