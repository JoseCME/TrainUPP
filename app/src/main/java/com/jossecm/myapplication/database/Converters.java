package com.jossecm.myapplication.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jossecm.myapplication.models.User;
import java.lang.reflect.Type;
import java.util.List;

public class Converters {
    private static final Gson gson = new Gson();

    // Converters para List<Integer>
    @TypeConverter
    public static String fromIntegerList(List<Integer> value) {
        return gson.toJson(value);
    }

    @TypeConverter
    public static List<Integer> fromIntegerString(String value) {
        Type listType = new TypeToken<List<Integer>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    // Converters para List<String>
    @TypeConverter
    public static String fromStringList(List<String> value) {
        return gson.toJson(value);
    }

    @TypeConverter
    public static List<String> fromStringString(String value) {
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    // Converters para enums
    @TypeConverter
    public static String fromGender(User.Gender gender) {
        return gender == null ? null : gender.name();
    }

    @TypeConverter
    public static User.Gender toGender(String gender) {
        return gender == null ? null : User.Gender.valueOf(gender);
    }

    @TypeConverter
    public static String fromExperienceLevel(User.ExperienceLevel level) {
        return level == null ? null : level.name();
    }

    @TypeConverter
    public static User.ExperienceLevel toExperienceLevel(String level) {
        return level == null ? null : User.ExperienceLevel.valueOf(level);
    }

    @TypeConverter
    public static String fromFitnessGoal(User.FitnessGoal goal) {
        return goal == null ? null : goal.name();
    }

    @TypeConverter
    public static User.FitnessGoal toFitnessGoal(String goal) {
        return goal == null ? null : User.FitnessGoal.valueOf(goal);
    }
}
