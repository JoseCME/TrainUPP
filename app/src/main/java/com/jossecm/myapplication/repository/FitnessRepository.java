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
        Log.d(TAG, "Cargando ejercicios para equipamientos: " + equipmentIds.toString());

        // Si no hay equipamientos seleccionados, cargar todos los ejercicios
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            loadAllExercisesFromApi(callback);
            return;
        }

        // Cargar ejercicios en inglés (language=2) sin filtro de equipamiento
        // La API no soporta múltiples equipamientos, filtraremos localmente
        apiService.getExercisesWithLanguage(2, 2, 500).enqueue(new Callback<ApiResponse<ExerciseResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ExerciseResponse>> call,
                                 @NonNull Response<ApiResponse<ExerciseResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ExerciseResponse> allExercises = response.body().getResults();
                    Log.d(TAG, "Total ejercicios recibidos (inglés): " + allExercises.size());

                    // Debug: ver algunos ejercicios para entender la estructura
                    for (int i = 0; i < Math.min(5, allExercises.size()); i++) {
                        ExerciseResponse exercise = allExercises.get(i);
                        Log.d(TAG, "Ejercicio " + i + ": ID=" + exercise.getId() +
                              ", Name='" + exercise.getName() + "'" +
                              ", Description='" + exercise.getDescription() + "'" +
                              ", Language=" + exercise.getLanguage() +
                              ", Status=" + exercise.getStatus());
                    }

                    // Filtrar ejercicios que tengan nombres válidos PRIMERO
                    List<ExerciseResponse> exercisesWithNames = new ArrayList<>();
                    for (ExerciseResponse exercise : allExercises) {
                        // Relajar el filtro - solo verificar que no sea completamente nulo
                        if (exercise.getName() != null) {
                            String name = exercise.getName().trim();
                            if (!name.isEmpty() && !name.equals("null") && !name.equals("")) {
                                exercisesWithNames.add(exercise);
                            }
                        }
                    }

                    Log.d(TAG, "Ejercicios con nombres válidos: " + exercisesWithNames.size());

                    // Si aún no hay ejercicios con nombres válidos, intentar con todos
                    if (exercisesWithNames.isEmpty()) {
                        Log.w(TAG, "No se encontraron ejercicios con nombres válidos, usando todos los ejercicios");
                        exercisesWithNames = allExercises;
                    }

                    // DESPUÉS filtrar por equipamiento
                    List<ExerciseResponse> filteredExercises = new ArrayList<>();
                    for (ExerciseResponse exercise : exercisesWithNames) {
                        if (exercise.getEquipment() != null && !exercise.getEquipment().isEmpty()) {
                            // Verificar si el ejercicio usa alguno de los equipamientos seleccionados
                            for (Integer equipmentId : equipmentIds) {
                                if (exercise.getEquipment().contains(equipmentId)) {
                                    filteredExercises.add(exercise);
                                    break; // No agregar duplicados
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Ejercicios filtrados por equipamiento (con nombres): " + filteredExercises.size());
                    processExercisesWithImages(filteredExercises, callback);
                } else {
                    String errorMsg = "Error cargando ejercicios: " + response.code() + " - " + response.message();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ExerciseResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error en API de ejercicios", t);
                callback.onError("Error de conexión: " + t.getMessage());
            }
        });
    }

    // Método auxiliar para cargar todos los ejercicios
    private void loadAllExercisesFromApi(DataCallback<List<Exercise>> callback) {
        apiService.getExercisesWithLanguage(2, 2, 500).enqueue(new Callback<ApiResponse<ExerciseResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ExerciseResponse>> call,
                                 @NonNull Response<ApiResponse<ExerciseResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ExerciseResponse> exerciseResponses = response.body().getResults();
                    Log.d(TAG, "Ejercicios recibidos (sin filtro): " + exerciseResponses.size());
                    processExercisesWithImages(exerciseResponses, callback);
                } else {
                    String errorMsg = "Error cargando ejercicios: " + response.code() + " - " + response.message();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ExerciseResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error en API de ejercicios", t);
                callback.onError("Error de conexión: " + t.getMessage());
            }
        });
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
}
