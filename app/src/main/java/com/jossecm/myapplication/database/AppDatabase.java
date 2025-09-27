package com.jossecm.myapplication.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.jossecm.myapplication.models.*;

@Database(
    entities = {
        User.class,
        Equipment.class,
        Muscle.class,
        Exercise.class,
        Rutina.class,
        HistorialEntrenamiento.class
    },
    version = 2,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract EquipmentDao equipmentDao();
    public abstract MuscleDao muscleDao();
    public abstract ExerciseDao exerciseDao();
    public abstract RutinaDao rutinaDao();
    public abstract HistorialEntrenamientoDao historialEntrenamientoDao();

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "fitness_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
