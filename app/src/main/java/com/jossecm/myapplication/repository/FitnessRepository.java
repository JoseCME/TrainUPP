package com.jossecm.myapplication.repository;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.jossecm.myapplication.api.ApiClient;
import com.jossecm.myapplication.api.WgerApiService;
import com.jossecm.myapplication.api.models.*;
import com.jossecm.myapplication.database.AppDatabase;
import com.jossecm.myapplication.models.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FitnessRepository {
    private static final String TAG = "FitnessRepository";
    private final AppDatabase database;
    private final WgerApiService apiService;
    private final ExecutorService executor;

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    public FitnessRepository(Context context) {
        database = AppDatabase.getDatabase(context);
        apiService = ApiClient.getApiService();
        executor = Executors.newFixedThreadPool(4);
    }

    // Métodos para Usuario
    public void saveUser(User user, DataCallback<Long> callback) {
        executor.execute(() -> {
            try {
                long userId = database.userDao().insert(user);
                callback.onSuccess(userId);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void getCurrentUser(DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                User user = database.userDao().getCurrentUser();
                callback.onSuccess(user);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // Métodos para Equipamiento
    public void loadEquipmentFromApi(DataCallback<List<Equipment>> callback) {
        apiService.getEquipment().enqueue(new Callback<ApiResponse<EquipmentResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<EquipmentResponse>> call,
                                 @NonNull Response<ApiResponse<EquipmentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Equipment> equipmentList = new ArrayList<>();
                    for (EquipmentResponse equipResponse : response.body().getResults()) {
                        Equipment equipment = new Equipment(equipResponse.getId(), equipResponse.getName());
                        equipmentList.add(equipment);
                    }

                    // Guardar en base de datos local
                    executor.execute(() -> {
                        database.equipmentDao().insertAll(equipmentList);
                        callback.onSuccess(equipmentList);
                    });
                } else {
                    callback.onError("Error cargando equipamiento: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<EquipmentResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error en API de equipamiento", t);
                callback.onError("Error de conexión: " + t.getMessage());
            }
        });
    }

    public void getAllEquipment(DataCallback<List<Equipment>> callback) {
        executor.execute(() -> {
            try {
                List<Equipment> equipment = database.equipmentDao().getAllEquipment();
                callback.onSuccess(equipment);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void updateEquipmentSelection(int equipmentId, boolean selected) {
        executor.execute(() -> {
            database.equipmentDao().updateSelection(equipmentId, selected);
        });
    }

    // Métodos para Músculos
    public void loadMusclesFromApi(DataCallback<List<Muscle>> callback) {
        apiService.getMuscles().enqueue(new Callback<ApiResponse<MuscleResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MuscleResponse>> call,
                                 @NonNull Response<ApiResponse<MuscleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Muscle> muscleList = new ArrayList<>();
                    for (MuscleResponse muscleResponse : response.body().getResults()) {
                        Muscle muscle = new Muscle(
                            muscleResponse.getId(),
                            muscleResponse.getName(),
                            muscleResponse.getNameEn(),
                            muscleResponse.isFront()
                        );
                        muscleList.add(muscle);
                    }

                    // Guardar en base de datos local
                    executor.execute(() -> {
                        database.muscleDao().insertAll(muscleList);
                        callback.onSuccess(muscleList);
                    });
                } else {
                    callback.onError("Error cargando músculos: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MuscleResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error en API de músculos", t);
                callback.onError("Error de conexión: " + t.getMessage());
            }
        });
    }

    // Método para cargar ejercicios filtrados por equipamiento
    public void loadExercisesFromApi(List<Integer> equipmentIds, DataCallback<List<Exercise>> callback) {
        Log.d(TAG, "Iniciando descarga con nueva arquitectura");
        Log.d(TAG, "Equipamientos seleccionados: " + (equipmentIds != null ? equipmentIds.toString() : "null"));

        // PASO 1: Descargar TODOS los ejercicios sin filtrar desde exerciseinfo
        // (contiene metadatos: músculos, equipamiento, categorías)
        apiService.getExercisesWithLanguage(2, 2, 1000).enqueue(new Callback<ApiResponse<ExerciseResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ExerciseResponse>> call,
                                 @NonNull Response<ApiResponse<ExerciseResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ExerciseResponse> allExercises = response.body().getResults();
                    Log.d(TAG, "PASO 1 - Ejercicios descargados desde exerciseinfo: " + allExercises.size());

                    // PASO 2: Descargar traducciones en español (language=1 para español)
                    downloadExerciseTranslations(allExercises, equipmentIds, callback);
                } else {
                    String errorMsg = "Error descargando metadatos de ejercicios: " + response.code() + " - " + response.message();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ExerciseResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error en API de ejercicios (metadatos)", t);
                callback.onError("Error de conexión obteniendo metadatos: " + t.getMessage());
            }
        });
    }

    // PASO 2: Descargar traducciones
    private void downloadExerciseTranslations(List<ExerciseResponse> exerciseMetadata,
                                            List<Integer> equipmentIds,
                                            DataCallback<List<Exercise>> callback) {

        // Primero intentar español (language=1), si falla usar inglés (language=2)
        apiService.getExerciseTranslations(1, 1000).enqueue(new Callback<ApiResponse<ExerciseTranslationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ExerciseTranslationResponse>> call,
                                 @NonNull Response<ApiResponse<ExerciseTranslationResponse>> response) {

                List<ExerciseTranslationResponse> translations = new ArrayList<>();

                if (response.isSuccessful() && response.body() != null) {
                    translations = response.body().getResults();
                    Log.d(TAG, "PASO 2 - Traducciones en español obtenidas: " + translations.size());
                } else {
                    Log.w(TAG, "No se pudieron obtener traducciones en español, intentando inglés...");
                }

                // Si no hay traducciones en español o son pocas, obtener también en inglés
                if (translations.size() < 100) {
                    downloadEnglishTranslations(exerciseMetadata, translations, equipmentIds, callback);
                } else {
                    // Proceder con las traducciones en español únicamente
                    fuseExerciseDataWithTranslations(exerciseMetadata, translations, equipmentIds, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ExerciseTranslationResponse>> call, @NonNull Throwable t) {
                Log.w(TAG, "Error obteniendo traducciones en español, intentando inglés...", t);
                // Si falla español, intentar inglés directamente
                downloadEnglishTranslations(exerciseMetadata, new ArrayList<>(), equipmentIds, callback);
            }
        });
    }

    // PASO 2b: Descargar traducciones en inglés como fallback
    private void downloadEnglishTranslations(List<ExerciseResponse> exerciseMetadata,
                                           List<ExerciseTranslationResponse> spanishTranslations,
                                           List<Integer> equipmentIds,
                                           DataCallback<List<Exercise>> callback) {

        apiService.getExerciseTranslations(2, 1000).enqueue(new Callback<ApiResponse<ExerciseTranslationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ExerciseTranslationResponse>> call,
                                 @NonNull Response<ApiResponse<ExerciseTranslationResponse>> response) {

                List<ExerciseTranslationResponse> allTranslations = new ArrayList<>(spanishTranslations);

                if (response.isSuccessful() && response.body() != null) {
                    List<ExerciseTranslationResponse> englishTranslations = response.body().getResults();
                    Log.d(TAG, "PASO 2b - Traducciones en inglés obtenidas: " + englishTranslations.size());

                    // Agregar traducciones en inglés que no estén ya en español
                    for (ExerciseTranslationResponse englishTrans : englishTranslations) {
                        boolean exists = false;
                        for (ExerciseTranslationResponse spanishTrans : spanishTranslations) {
                            if (spanishTrans.getExerciseId() == englishTrans.getExerciseId()) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            allTranslations.add(englishTrans);
                        }
                    }
                } else {
                    Log.w(TAG, "Error obteniendo traducciones en inglés: " + response.code());
                }

                Log.d(TAG, "Total traducciones combinadas: " + allTranslations.size());
                fuseExerciseDataWithTranslations(exerciseMetadata, allTranslations, equipmentIds, callback);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ExerciseTranslationResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error obteniendo traducciones en inglés", t);
                // Continuar sin traducciones si todo falla
                fuseExerciseDataWithTranslations(exerciseMetadata, spanishTranslations, equipmentIds, callback);
            }
        });
    }

    // PASO 3: Fusionar metadatos de ejercicios con traducciones
    private void fuseExerciseDataWithTranslations(List<ExerciseResponse> exerciseMetadata,
                                                List<ExerciseTranslationResponse> translations,
                                                List<Integer> equipmentIds,
                                                DataCallback<List<Exercise>> callback) {

        Log.d(TAG, "PASO 3 - Fusionando " + exerciseMetadata.size() + " ejercicios con " + translations.size() + " traducciones");

        // Crear un mapa de traducciones por exerciseId para acceso rápido
        java.util.Map<Integer, ExerciseTranslationResponse> translationMap = new java.util.HashMap<>();
        for (ExerciseTranslationResponse translation : translations) {
            if (translation.getName() != null && !translation.getName().trim().isEmpty()) {
                translationMap.put(translation.getExerciseId(), translation);
            }
        }

        Log.d(TAG, "Mapa de traducciones creado con " + translationMap.size() + " entradas válidas");

        // Fusionar datos: crear Exercise directamente con datos combinados
        List<Exercise> fusedExercises = new ArrayList<>();

        for (ExerciseResponse metadata : exerciseMetadata) {
            // Verificar si tiene traducción
            ExerciseTranslationResponse translation = translationMap.get(metadata.getId());

            if (translation != null) {
                // Verificar filtro de equipamiento (si se especificó)
                boolean matchesEquipment = true;

                if (equipmentIds != null && !equipmentIds.isEmpty()) {
                    matchesEquipment = false;
                    if (metadata.getEquipment() != null && !metadata.getEquipment().isEmpty()) {
                        for (Integer equipmentId : equipmentIds) {
                            if (metadata.getEquipment().contains(equipmentId)) {
                                matchesEquipment = true;
                                break;
                            }
                        }
                    }
                }

                if (matchesEquipment) {
                    // Crear Exercise directamente con datos fusionados
                    Exercise fusedExercise = new Exercise();
                    fusedExercise.setId(metadata.getId());
                    fusedExercise.setName(translation.getName()); // ¡NOMBRE DESDE TRADUCCIÓN!
                    fusedExercise.setDescription(translation.getDescription() != null ?
                        translation.getDescription() : metadata.getDescription());
                    fusedExercise.setMuscleIds(metadata.getMuscles());
                    fusedExercise.setEquipmentIds(metadata.getEquipment());
                    fusedExercise.setCategoryId(metadata.getCategory());
                    // La imagen se agregará después en processExercisesWithImages

                    fusedExercises.add(fusedExercise);

                    Log.d(TAG, "Ejercicio fusionado: ID=" + metadata.getId() +
                          ", Nombre='" + translation.getName() + "'" +
                          ", Músculos=" + (metadata.getMuscles() != null ? metadata.getMuscles().size() : 0) +
                          ", Equipamiento=" + (metadata.getEquipment() != null ? metadata.getEquipment().size() : 0));
                }
            }
        }

        Log.d(TAG, "Ejercicios fusionados y filtrados: " + fusedExercises.size());

        if (fusedExercises.isEmpty()) {
            callback.onError("No se encontraron ejercicios con nombres válidos para tu equipamiento seleccionado");
            return;
        }

        // PASO 4: Procesar imágenes para los ejercicios fusionados
        processExercisesWithImagesDirectly(fusedExercises, callback);
    }

    // Nuevo método para procesar imágenes directamente con objetos Exercise
    private void processExercisesWithImagesDirectly(List<Exercise> exercises,
                                                   DataCallback<List<Exercise>> callback) {
        List<Exercise> exercisesWithImages = new ArrayList<>();
        int totalExercises = exercises.size();
        int[] processedCount = {0};

        if (totalExercises == 0) {
            callback.onSuccess(exercisesWithImages);
            return;
        }

        Log.d(TAG, "PASO 4 - Procesando imágenes para " + totalExercises + " ejercicios (SOLO se incluirán los que tengan imagen)");

        for (Exercise exercise : exercises) {
            // Verificar si el ejercicio tiene imagen
            apiService.getExerciseImages(exercise.getId(), true)
                .enqueue(new Callback<ApiResponse<ExerciseImageResponse>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<ExerciseImageResponse>> call,
                                     @NonNull Response<ApiResponse<ExerciseImageResponse>> response) {
                    synchronized (exercisesWithImages) {
                        if (response.isSuccessful() && response.body() != null &&
                            !response.body().getResults().isEmpty()) {

                            // SOLO agregar ejercicio SI TIENE imagen válida
                            String imageUrl = response.body().getResults().get(0).getImage();
                            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                                exercise.setImageUrl(imageUrl);
                                exercisesWithImages.add(exercise);

                                Log.d(TAG, "✓ Ejercicio CON imagen agregado: " + exercise.getName() +
                                      " - URL: " + imageUrl);
                            } else {
                                Log.w(TAG, "✗ Ejercicio descartado (imagen vacía): " + exercise.getName());
                            }
                        } else {
                            // NO incluir ejercicio sin imagen
                            Log.w(TAG, "✗ Ejercicio descartado (sin imagen): " + exercise.getName());
                        }

                        processedCount[0]++;
                        if (processedCount[0] == totalExercises) {
                            Log.d(TAG, "PASO 4 COMPLETADO - Ejercicios con imagen: " + exercisesWithImages.size() + "/" + totalExercises);

                            if (exercisesWithImages.isEmpty()) {
                                callback.onError("No se encontraron ejercicios con imágenes válidas para tu equipamiento seleccionado");
                                return;
                            }

                            // Obtener nombres de músculos y guardar
                            loadMuscleNamesForExercises(exercisesWithImages, callback);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<ExerciseImageResponse>> call,
                                    @NonNull Throwable t) {
                    synchronized (exercisesWithImages) {
                        // NO incluir ejercicio si falla la carga de imagen
                        Log.w(TAG, "✗ Ejercicio descartado (error obteniendo imagen): " + exercise.getName(), t);

                        processedCount[0]++;
                        if (processedCount[0] == totalExercises) {
                            Log.d(TAG, "PASO 4 COMPLETADO - Ejercicios con imagen: " + exercisesWithImages.size() + "/" + totalExercises);

                            if (exercisesWithImages.isEmpty()) {
                                callback.onError("No se encontraron ejercicios con imágenes válidas para tu equipamiento seleccionado");
                                return;
                            }

                            loadMuscleNamesForExercises(exercisesWithImages, callback);
                        }
                    }
                }
            });
        }
    }

    private void processExercisesWithImages(List<ExerciseResponse> exerciseResponses,
                                          DataCallback<List<Exercise>> callback) {
        List<Exercise> exercisesWithImages = new ArrayList<>();
        int totalExercises = exerciseResponses.size();
        int[] processedCount = {0};

        if (totalExercises == 0) {
            callback.onSuccess(exercisesWithImages);
            return;
        }

        for (ExerciseResponse exerciseResponse : exerciseResponses) {
            // Verificar si el ejercicio tiene imagen
            apiService.getExerciseImages(exerciseResponse.getId(), true)
                .enqueue(new Callback<ApiResponse<ExerciseImageResponse>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<ExerciseImageResponse>> call,
                                     @NonNull Response<ApiResponse<ExerciseImageResponse>> response) {
                    synchronized (exercisesWithImages) {
                        if (response.isSuccessful() && response.body() != null &&
                            !response.body().getResults().isEmpty()) {

                            Exercise exercise = convertToExercise(exerciseResponse,
                                response.body().getResults().get(0).getImage());
                            exercisesWithImages.add(exercise);
                        }

                        processedCount[0]++;
                        if (processedCount[0] == totalExercises) {
                            // Obtener nombres de músculos y guardar
                            loadMuscleNamesForExercises(exercisesWithImages, callback);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<ExerciseImageResponse>> call,
                                    @NonNull Throwable t) {
                    synchronized (exercisesWithImages) {
                        processedCount[0]++;
                        if (processedCount[0] == totalExercises) {
                            loadMuscleNamesForExercises(exercisesWithImages, callback);
                        }
                    }
                }
            });
        }
    }

    private Exercise convertToExercise(ExerciseResponse response, String imageUrl) {
        Exercise exercise = new Exercise();

        // CRÍTICO: Verificar y loggear los datos que llegan de la API
        Log.d(TAG, "Convirtiendo ejercicio - ID: " + response.getId() +
              ", Nombre desde API: '" + response.getName() + "'" +
              ", UUID: '" + response.getUuid() + "'" +
              ", Language: " + response.getLanguage() +
              ", Descripción: '" + response.getDescription() + "'");

        exercise.setId(response.getId());

        // Mejorar el manejo del nombre del ejercicio
        String exerciseName = null;

        // Prioridad 1: Campo name si existe y no está vacío
        if (response.getName() != null && !response.getName().trim().isEmpty()) {
            exerciseName = response.getName().trim();
        }
        // Prioridad 2: Extraer nombre de la descripción HTML
        else if (response.getDescription() != null && !response.getDescription().trim().isEmpty()) {
            String desc = response.getDescription().trim();
            // Remover HTML tags y extraer nombre descriptivo
            String cleanDesc = desc.replaceAll("<[^>]*>", "").trim();

            if (!cleanDesc.isEmpty()) {
                // Tomar las primeras 4 palabras como nombre
                String[] words = cleanDesc.split("\\s+");
                StringBuilder nameFromDesc = new StringBuilder();
                int maxWords = Math.min(4, words.length);

                for (int i = 0; i < maxWords; i++) {
                    String word = words[i].trim();
                    if (!word.isEmpty() && !word.matches("\\d+")) { // Evitar números
                        nameFromDesc.append(word).append(" ");
                    }
                }

                String extractedName = nameFromDesc.toString().trim();
                if (!extractedName.isEmpty()) {
                    exerciseName = extractedName;
                } else {
                    exerciseName = "Ejercicio " + response.getId();
                }
            } else {
                exerciseName = "Ejercicio " + response.getId();
            }
            Log.i(TAG, "Nombre extraído de descripción para ID " + response.getId() + ": " + exerciseName);
        }
        // Prioridad 3: Fallback con ID
        else {
            exerciseName = "Ejercicio " + response.getId();
            Log.w(TAG, "Sin descripción disponible para ejercicio ID " + response.getId() + " - usando fallback");
        }

        exercise.setName(exerciseName);
        exercise.setDescription(response.getDescription());
        exercise.setMuscleIds(response.getMuscles());
        exercise.setEquipmentIds(response.getEquipment());
        exercise.setImageUrl(imageUrl);
        exercise.setCategoryId(response.getCategory());

        Log.d(TAG, "Ejercicio convertido - ID: " + exercise.getId() +
              ", Nombre final: '" + exercise.getName() + "'");

        return exercise;
    }

    private void loadMuscleNamesForExercises(List<Exercise> exercises, DataCallback<List<Exercise>> callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Procesando " + exercises.size() + " ejercicios para mapear músculos");

                for (Exercise exercise : exercises) {
                    // CRÍTICO: Verificar que el ejercicio tenga nombre antes de procesar
                    if (exercise.getName() == null || exercise.getName().trim().isEmpty()) {
                        Log.e(TAG, "¡ERROR! Ejercicio sin nombre encontrado - ID: " + exercise.getId());
                        continue; // Saltar ejercicios sin nombre
                    }

                    if (exercise.getMuscleIds() != null && !exercise.getMuscleIds().isEmpty()) {
                        List<Muscle> muscles = database.muscleDao().getMusclesByIds(exercise.getMuscleIds());
                        List<String> muscleNames = new ArrayList<>();
                        for (Muscle muscle : muscles) {
                            // Priorizar el nombre en inglés si existe, sino usar el nombre normal
                            String muscleName = (muscle.getNameEn() != null && !muscle.getNameEn().isEmpty())
                                ? muscle.getNameEn() : muscle.getName();
                            muscleNames.add(muscleName);
                        }
                        exercise.setMuscleNames(muscleNames);

                        Log.d(TAG, "Ejercicio: " + exercise.getName() +
                              " - Músculos: " + muscleNames.toString());
                    } else {
                        Log.w(TAG, "Ejercicio sin músculos asignados: " + exercise.getName());
                    }
                }

                // Filtrar ejercicios sin nombre antes de guardar
                List<Exercise> validExercises = new ArrayList<>();
                for (Exercise exercise : exercises) {
                    if (exercise.getName() != null && !exercise.getName().trim().isEmpty()) {
                        validExercises.add(exercise);
                    }
                }

                Log.d(TAG, "Ejercicios válidos (con nombre): " + validExercises.size() + "/" + exercises.size());

                // Guardar solo ejercicios válidos en base de datos
                if (!validExercises.isEmpty()) {
                    database.exerciseDao().insertAll(validExercises);

                    // Verificar que se guardaron correctamente
                    int savedCount = database.exerciseDao().getExerciseCount();
                    Log.d(TAG, "Verificación: Total ejercicios en BD después del guardado: " + savedCount);

                    callback.onSuccess(validExercises);
                } else {
                    Log.e(TAG, "No hay ejercicios válidos para guardar");
                    callback.onError("No se encontraron ejercicios válidos con nombres");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error procesando músculos", e);
                callback.onError("Error procesando músculos: " + e.getMessage());
            }
        });
    }

    // Métodos para obtener ejercicios de la base de datos local
    public void getAllExercises(DataCallback<List<Exercise>> callback) {
        executor.execute(() -> {
            try {
                List<Exercise> exercises = database.exerciseDao().getAllExercises();
                Log.d(TAG, "Ejercicios cargados desde BD local (antes del mapeo): " + exercises.size());

                // CRÍTICO: Asegurar que TODOS los ejercicios tengan nombres de músculos mapeados
                boolean needsUpdate = false;
                for (Exercise exercise : exercises) {
                    if ((exercise.getMuscleNames() == null || exercise.getMuscleNames().isEmpty())
                        && exercise.getMuscleIds() != null && !exercise.getMuscleIds().isEmpty()) {

                        // Mapear nombres de músculos desde la base de datos local
                        List<Muscle> muscles = database.muscleDao().getMusclesByIds(exercise.getMuscleIds());
                        List<String> muscleNames = new ArrayList<>();

                        Log.d(TAG, "Mapeando músculos para ejercicio: " + exercise.getName() +
                              " - IDs de músculos: " + exercise.getMuscleIds().toString());

                        for (Muscle muscle : muscles) {
                            // Usar el nombre en inglés si existe, sino el nombre normal
                            String muscleName = (muscle.getNameEn() != null && !muscle.getNameEn().isEmpty())
                                ? muscle.getNameEn() : muscle.getName();
                            muscleNames.add(muscleName);
                            Log.d(TAG, "Músculo ID " + muscle.getId() + " -> " + muscleName);
                        }

                        exercise.setMuscleNames(muscleNames);
                        needsUpdate = true;

                        Log.d(TAG, "Ejercicio " + exercise.getName() + " ahora tiene músculos: " + muscleNames.toString());
                    } else {
                        Log.d(TAG, "Ejercicio " + exercise.getName() + " ya tiene músculos: " +
                              (exercise.getMuscleNames() != null ? exercise.getMuscleNames().toString() : "null"));
                    }
                }

                // Actualizar ejercicios con nombres mapeados si es necesario
                if (needsUpdate && !exercises.isEmpty()) {
                    Log.d(TAG, "Actualizando ejercicios con nombres de músculos mapeados");
                    database.exerciseDao().insertAll(exercises);
                }

                Log.d(TAG, "Ejercicios finales cargados desde BD local: " + exercises.size());

                // Verificar que todos los ejercicios tengan nombres de músculos
                int exercisesWithMuscles = 0;
                for (Exercise ex : exercises) {
                    if (ex.getMuscleNames() != null && !ex.getMuscleNames().isEmpty()) {
                        exercisesWithMuscles++;
                    }
                }
                Log.d(TAG, "Ejercicios con nombres de músculos: " + exercisesWithMuscles + "/" + exercises.size());

                callback.onSuccess(exercises);
            } catch (Exception e) {
                Log.e(TAG, "Error cargando ejercicios locales", e);
                callback.onError(e.getMessage());
            }
        });
    }

    // Método para verificar si hay ejercicios descargados
    public void hasDownloadedExercises(DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                int count = database.exerciseDao().getExerciseCount();
                callback.onSuccess(count > 0);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // Método para limpiar datos existentes si es necesario
    public void clearAllData(DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.exerciseDao().deleteAll();
                database.muscleDao().deleteAll();
                database.equipmentDao().deleteAll();
                Log.d(TAG, "Todos los datos locales eliminados");
                callback.onSuccess(true);
            } catch (Exception e) {
                Log.e(TAG, "Error limpiando datos", e);
                callback.onError(e.getMessage());
            }
        });
    }

    // Métodos para Rutinas
    public void getAllRutinas(DataCallback<List<Rutina>> callback) {
        executor.execute(() -> {
            try {
                List<Rutina> rutinas = database.rutinaDao().getAllRutinas();
                callback.onSuccess(rutinas);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void insertRutina(Rutina rutina, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                long id = database.rutinaDao().insert(rutina);
                callback.onSuccess(id > 0);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void getRutinaById(long id, DataCallback<Rutina> callback) {
        executor.execute(() -> {
            try {
                Rutina rutina = database.rutinaDao().getRutinaById(id);
                callback.onSuccess(rutina);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // Métodos para Historial de Entrenamientos
    public void insertHistorialEntrenamiento(HistorialEntrenamiento historial, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                long id = database.historialEntrenamientoDao().insert(historial);
                callback.onSuccess(id > 0);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void getAllHistorial(DataCallback<List<HistorialEntrenamiento>> callback) {
        executor.execute(() -> {
            try {
                List<HistorialEntrenamiento> historial = database.historialEntrenamientoDao().getAllHistorial();
                callback.onSuccess(historial);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // NUEVO: Método para agregar un ejercicio personalizado a la base de datos
    public void addExercise(Exercise exercise, DataCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Agregando ejercicio personalizado: " + exercise.getName());
                
                // Verificar que el ejercicio tenga los datos mínimos necesarios
                if (exercise.getName() == null || exercise.getName().trim().isEmpty()) {
                    callback.onError("El ejercicio debe tener un nombre");
                    return;
                }

                // Insertar el ejercicio en la base de datos
                database.exerciseDao().insert(exercise);

                Log.d(TAG, "Ejercicio personalizado agregado: " + exercise.getName());
                callback.onSuccess(null);
                
            } catch (Exception e) {
                Log.e(TAG, "Error agregando ejercicio personalizado", e);
                callback.onError("Error guardando ejercicio: " + e.getMessage());
            }
        });
    }

    // NUEVO: Método para agregar un ejercicio a una rutina específica
    public void addExerciseToRoutine(long rutinaId, int ejercicioId, DataCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Agregando ejercicio " + ejercicioId + " a rutina " + rutinaId);
                
                // Verificar que la rutina existe
                Rutina rutina = database.rutinaDao().getRutinaById(rutinaId);
                if (rutina == null) {
                    callback.onError("La rutina especificada no existe");
                    return;
                }

                // Verificar que el ejercicio existe
                Exercise ejercicio = database.exerciseDao().getExerciseById(ejercicioId);
                if (ejercicio == null) {
                    callback.onError("El ejercicio especificado no existe");
                    return;
                }

                // Obtener los IDs de ejercicios actuales
                String ejerciciosIdsActuales = rutina.getEjerciciosIds();
                List<Integer> listaIds = new ArrayList<>();

                // Parsear los IDs existentes si hay alguno
                if (ejerciciosIdsActuales != null && !ejerciciosIdsActuales.trim().isEmpty()) {
                    try {
                        org.json.JSONArray jsonArray = new org.json.JSONArray(ejerciciosIdsActuales);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            listaIds.add(jsonArray.getInt(i));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error parseando ejercicios existentes, iniciando lista nueva", e);
                    }
                }

                // Verificar si el ejercicio ya está en la rutina
                if (listaIds.contains(ejercicioId)) {
                    callback.onError("El ejercicio ya está en la rutina");
                    return;
                }

                // Agregar el nuevo ejercicio
                listaIds.add(ejercicioId);

                // Convertir la lista a JSON
                org.json.JSONArray jsonArray = new org.json.JSONArray();
                for (Integer id : listaIds) {
                    jsonArray.put(id);
                }

                // Actualizar la rutina con el nuevo ejercicio
                rutina.setEjerciciosIds(jsonArray.toString());
                rutina.setCantidadEjercicios(listaIds.size());

                // Guardar la rutina actualizada en la base de datos
                database.rutinaDao().updateRutina(rutina);

                Log.d(TAG, "Ejercicio " + ejercicio.getName() + " agregado exitosamente a rutina " + rutina.getNombre());
                Log.d(TAG, "Rutina actualizada - Total ejercicios: " + listaIds.size());
                callback.onSuccess(null);
                
            } catch (Exception e) {
                Log.e(TAG, "Error agregando ejercicio a rutina", e);
                callback.onError("Error agregando ejercicio a rutina: " + e.getMessage());
            }
        });
    }

    // NUEVOS: Métodos para Series Históricas
    public void guardarSeriesEjercicio(int exerciseId, List<Serie> series, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                long fechaActual = System.currentTimeMillis();
                List<SerieHistorial> seriesHistorial = new ArrayList<>();

                for (int i = 0; i < series.size(); i++) {
                    Serie serie = series.get(i);
                    // Solo guardar series completadas
                    if (serie.isCompletada()) {
                        // CORREGIDO: Usar el numeroSerie de la serie en lugar del índice de la lista
                        int numeroSerieReal = serie.getNumeroSerie();
                        if (numeroSerieReal <= 0) {
                            // Fallback: si no tiene numeroSerie asignado, usar el índice
                            numeroSerieReal = i + 1;
                        }

                        SerieHistorial serieHistorial = new SerieHistorial(
                            exerciseId,
                            numeroSerieReal, // CRÍTICO: usar el número real de la serie
                            serie.getPeso(),
                            serie.getRepeticiones(),
                            true,
                            fechaActual
                        );
                        seriesHistorial.add(serieHistorial);

                        Log.d(TAG, "Guardando serie " + numeroSerieReal + " para ejercicio " + exerciseId +
                              ": " + serie.getPeso() + " lbs, " + serie.getRepeticiones() + " reps");
                    }
                }

                if (!seriesHistorial.isEmpty()) {
                    database.serieHistorialDao().insertSeries(seriesHistorial);
                    Log.d(TAG, "Guardadas " + seriesHistorial.size() + " series para ejercicio " + exerciseId);
                }

                callback.onSuccess(true);
            } catch (Exception e) {
                Log.e(TAG, "Error guardando series históricas", e);
                callback.onError(e.getMessage());
            }
        });
    }

    public void getUltimasSeriesEjercicio(int exerciseId, DataCallback<List<SerieHistorial>> callback) {
        executor.execute(() -> {
            try {
                List<SerieHistorial> series = database.serieHistorialDao().getUltimasSeriesEjercicio(exerciseId);
                Log.d(TAG, "Cargadas " + series.size() + " series históricas para ejercicio " + exerciseId);
                callback.onSuccess(series);
            } catch (Exception e) {
                Log.e(TAG, "Error cargando series históricas", e);
                callback.onError(e.getMessage());
            }
        });
    }

    public void getHistorialCompletoEjercicio(int exerciseId, DataCallback<List<SerieHistorial>> callback) {
        executor.execute(() -> {
            try {
                List<SerieHistorial> historial = database.serieHistorialDao().getHistorialCompleto(exerciseId);
                callback.onSuccess(historial);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    // NUEVOS: Métodos para configuración de series por ejercicio
    public void guardarConfiguracionSeries(int exerciseId, int numSeries) {
        executor.execute(() -> {
            try {
                ConfiguracionSeries config = new ConfiguracionSeries(exerciseId, numSeries);
                database.configuracionSeriesDao().insertOrUpdate(config);

                Log.d(TAG, "Configuración guardada en BD - Ejercicio " + exerciseId + ": " + numSeries + " series");
            } catch (Exception e) {
                Log.e(TAG, "Error guardando configuración de series en BD", e);
            }
        });
    }

    public void getConfiguracionSeries(int exerciseId, DataCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                Integer numSeries = database.configuracionSeriesDao().getNumSeries(exerciseId);

                if (numSeries == null || numSeries < 3) {
                    numSeries = 3; // Valor por defecto mínimo
                }

                Log.d(TAG, "Configuración cargada desde BD - Ejercicio " + exerciseId + ": " + numSeries + " series");
                callback.onSuccess(numSeries);
            } catch (Exception e) {
                Log.e(TAG, "Error cargando configuración de series desde BD", e);
                callback.onSuccess(3); // Valor por defecto en caso de error
            }
        });
    }
}
