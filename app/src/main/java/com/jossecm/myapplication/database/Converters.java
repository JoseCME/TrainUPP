package com.jossecm.myapplication.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

    // Los convertidores de enums se eliminan porque User usa Strings directamente
    // en lugar de enums para gender, fitnessLevel y goal
}
