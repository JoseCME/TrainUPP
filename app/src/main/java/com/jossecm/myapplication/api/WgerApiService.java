package com.jossecm.myapplication.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import com.jossecm.myapplication.api.models.ApiResponse;
import com.jossecm.myapplication.api.models.EquipmentResponse;
import com.jossecm.myapplication.api.models.ExerciseResponse;
import com.jossecm.myapplication.api.models.ExerciseImageResponse;
import com.jossecm.myapplication.api.models.ExerciseTranslationResponse;
import com.jossecm.myapplication.api.models.MuscleResponse;

public interface WgerApiService {
    String BASE_URL = "https://wger.de/api/v2/";

    @GET("equipment/")
    Call<ApiResponse<EquipmentResponse>> getEquipment();

    @GET("exercise/")
    Call<ApiResponse<ExerciseResponse>> getExercises(
        @Query("equipment") String equipmentIds,
        @Query("status") int status,
        @Query("limit") int limit
    );

    @GET("exercise/")
    Call<ApiResponse<ExerciseResponse>> getExercises(
        @Query("status") int status,
        @Query("limit") int limit
    );

    @GET("exercise/")
    Call<ApiResponse<ExerciseResponse>> getExercisesWithLanguage(
        @Query("language") int language,
        @Query("status") int status,
        @Query("limit") int limit
    );

    @GET("exercise-translation/")
    Call<ApiResponse<ExerciseTranslationResponse>> getExerciseTranslations(
        @Query("language") int language,
        @Query("limit") int limit
    );

    @GET("exerciseimage/")
    Call<ApiResponse<ExerciseImageResponse>> getExerciseImages(
        @Query("exercise") int exerciseId,
        @Query("is_main") boolean isMain
    );

    @GET("muscle/{id}/")
    Call<MuscleResponse> getMuscle(@Path("id") int muscleId);

    @GET("muscle/")
    Call<ApiResponse<MuscleResponse>> getMuscles();
}
