package com.jossecm.myapplication.ai;

import android.content.Context;
import android.util.Log;
import com.jossecm.myapplication.models.Exercise;
import com.jossecm.myapplication.models.Rutina;
import com.jossecm.myapplication.models.User;
import com.jossecm.myapplication.repository.FitnessRepository;
import org.json.JSONArray;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sistema de Inteligencia Artificial para generar rutinas automáticamente
 * basándose en los datos del usuario, equipamiento, objetivos y lesiones.
 */
public class RutinaGeneratorAI {
    private static final String TAG = "RutinaGeneratorAI";

    private FitnessRepository repository;
    private Context context;

    // Grupos musculares principales
    private static final List<String> MUSCLE_GROUPS = Arrays.asList(
        "Chest", "Lats", "Shoulders", "Biceps", "Triceps", "Hamstrings", "Core"
    );

    // Días de la semana para rutinas
    private static final List<String> WORKOUT_DAYS = Arrays.asList(
        "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    );

    public interface RutinaGeneratorCallback {
        void onRutinasGeneradas(List<Rutina> rutinas);
        void onError(String error);
        void onProgress(String status, int progress);
    }

    public RutinaGeneratorAI(Context context) {
        this.context = context;
        this.repository = new FitnessRepository(context);
    }

    /**
     * Genera rutinas automáticamente basándose en los datos del usuario
     */
    public void generarRutinasAutomaticas(User usuario, RutinaGeneratorCallback callback) {
        if (usuario == null) {
            callback.onError("Usuario no válido");
            return;
        }

        Log.d(TAG, "Iniciando generación de rutinas para usuario: " + usuario.getName());
        callback.onProgress("Analizando datos del usuario...", 10);

        // Obtener todos los ejercicios disponibles
        repository.getAllExercises(new FitnessRepository.DataCallback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> todosLosEjercicios) {
                callback.onProgress("Ejercicios cargados, generando rutinas...", 30);

                try {
                    // Filtrar ejercicios según equipamiento del usuario
                    List<Exercise> ejerciciosDisponibles = filtrarEjerciciosPorEquipamiento(
                        todosLosEjercicios, usuario.getSelectedEquipmentIds());

                    Log.d(TAG, "Ejercicios disponibles después del filtrado: " + ejerciciosDisponibles.size());

                    // Generar rutinas según los días seleccionados
                    List<Rutina> rutinasGeneradas = crearRutinasSegunDias(
                        usuario, ejerciciosDisponibles, callback);

                    callback.onProgress("Guardando rutinas generadas...", 80);

                    // Guardar todas las rutinas en la base de datos
                    guardarRutinasEnBD(rutinasGeneradas, callback);

                } catch (Exception e) {
                    Log.e(TAG, "Error generando rutinas", e);
                    callback.onError("Error generando rutinas: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error obteniendo ejercicios: " + error);
                callback.onError("Error obteniendo ejercicios: " + error);
            }
        });
    }

    /**
     * Filtra ejercicios según el equipamiento seleccionado por el usuario
     */
    private List<Exercise> filtrarEjerciciosPorEquipamiento(List<Exercise> todosLosEjercicios,
                                                           List<Integer> equipamientoSeleccionado) {
        if (equipamientoSeleccionado == null || equipamientoSeleccionado.isEmpty()) {
            return todosLosEjercicios;
        }

        List<Exercise> ejerciciosFiltrados = new ArrayList<>();

        for (Exercise ejercicio : todosLosEjercicios) {
            if (ejercicio.getEquipmentIds() != null && !ejercicio.getEquipmentIds().isEmpty()) {
                // Verificar si el ejercicio usa algún equipamiento que el usuario tiene
                for (Integer equipmentId : equipamientoSeleccionado) {
                    if (ejercicio.getEquipmentIds().contains(equipmentId)) {
                        ejerciciosFiltrados.add(ejercicio);
                        break;
                    }
                }
            }
        }

        Log.d(TAG, "Ejercicios filtrados por equipamiento: " + ejerciciosFiltrados.size() + "/" + todosLosEjercicios.size());
        return ejerciciosFiltrados;
    }

    /**
     * Crea rutinas según los días seleccionados por el usuario
     */
    private List<Rutina> crearRutinasSegunDias(User usuario, List<Exercise> ejerciciosDisponibles,
                                              RutinaGeneratorCallback callback) {
        List<Rutina> rutinas = new ArrayList<>();

        // Determinar número de días de entrenamiento
        int diasEntrenamiento = determinarDiasEntrenamiento(usuario);
        Log.d(TAG, "Generando " + diasEntrenamiento + " rutinas");

        // Agrupar ejercicios por grupo muscular
        Map<String, List<Exercise>> ejerciciosPorMusculo = agruparEjerciciosPorMusculo(ejerciciosDisponibles);

        // Generar rutinas según el número de días
        for (int dia = 0; dia < diasEntrenamiento; dia++) {
            callback.onProgress("Generando rutina " + (dia + 1) + "...", 40 + (dia * 30 / diasEntrenamiento));

            String nombreDia = WORKOUT_DAYS.get(dia % WORKOUT_DAYS.size());
            Rutina rutina = crearRutinaParaDia(usuario, ejerciciosPorMusculo, nombreDia, dia, diasEntrenamiento);

            if (rutina != null) {
                rutinas.add(rutina);
                Log.d(TAG, "Rutina generada: " + rutina.getNombre() + " con " + rutina.getCantidadEjercicios() + " ejercicios");
            }
        }

        return rutinas;
    }

    /**
     * Determina el número de días de entrenamiento basándose en los datos del onboarding del usuario
     */
    private int determinarDiasEntrenamiento(User usuario) {
        // CORREGIDO: Priorizar los días seleccionados por el usuario en el onboarding
        int diasPreferidos = usuario.getDaysPerWeek();

        if (diasPreferidos > 0 && diasPreferidos <= 7) {
            Log.d(TAG, "Usando días del onboarding del usuario: " + diasPreferidos);
            return diasPreferidos;
        }

        // Fallback: usar nivel de fitness si no hay datos del onboarding
        String nivelFitness = usuario.getFitnessLevel();
        if (nivelFitness == null) {
            Log.w(TAG, "No hay datos de días disponibles ni nivel de fitness, usando 3 días por defecto");
            return 3;
        }

        Log.d(TAG, "Usando días basados en nivel de fitness: " + nivelFitness);
        switch (nivelFitness.toLowerCase()) {
            case "principiante":
            case "beginner":
                return 3;
            case "intermedio":
            case "intermediate":
                return 4;
            case "avanzado":
            case "advanced":
                return 5;
            default:
                return 3;
        }
    }

    /**
     * Agrupa ejercicios por grupo muscular
     */
    private Map<String, List<Exercise>> agruparEjerciciosPorMusculo(List<Exercise> ejercicios) {
        Map<String, List<Exercise>> grupos = new HashMap<>();

        // Inicializar grupos
        for (String musculo : MUSCLE_GROUPS) {
            grupos.put(musculo, new ArrayList<>());
        }

        // Clasificar ejercicios
        for (Exercise ejercicio : ejercicios) {
            if (ejercicio.getMuscleNames() != null) {
                for (String musculo : ejercicio.getMuscleNames()) {
                    if (grupos.containsKey(musculo)) {
                        grupos.get(musculo).add(ejercicio);
                    }
                }
            }
        }

        // Log para debugging
        for (String musculo : MUSCLE_GROUPS) {
            Log.d(TAG, "Ejercicios para " + musculo + ": " + grupos.get(musculo).size());
        }

        return grupos;
    }

    /**
     * Crea una rutina específica para un día determinado
     */
    private Rutina crearRutinaParaDia(User usuario, Map<String, List<Exercise>> ejerciciosPorMusculo,
                                    String nombreDia, int indiceDia, int totalDias) {

        // Determinar grupos musculares para este día basándose en el split
        List<String> gruposMusculares = determinarGruposMusculares(indiceDia, totalDias, usuario);

        List<Exercise> ejerciciosSeleccionados = new ArrayList<>();

        // Seleccionar ejercicios para cada grupo muscular
        for (String grupoMuscular : gruposMusculares) {
            List<Exercise> ejerciciosDisponibles = ejerciciosPorMusculo.get(grupoMuscular);

            if (ejerciciosDisponibles != null && !ejerciciosDisponibles.isEmpty()) {
                // Filtrar ejercicios seguros según lesiones del usuario
                List<Exercise> ejerciciosSegsuros = filtrarEjerciciosSegurosPorLesiones(
                    ejerciciosDisponibles, usuario.getInjuries());

                // Seleccionar los mejores ejercicios para este grupo muscular
                List<Exercise> mejoresEjercicios = seleccionarMejoresEjercicios(
                    ejerciciosSegsuros, usuario, 2); // Máximo 2 ejercicios por grupo

                ejerciciosSeleccionados.addAll(mejoresEjercicios);
            }
        }

        if (ejerciciosSeleccionados.isEmpty()) {
            Log.w(TAG, "No se encontraron ejercicios para el día " + nombreDia);
            return null;
        }

        // Crear rutina
        Rutina rutina = new Rutina();
        rutina.setNombre(nombreDia);
        rutina.setCantidadEjercicios(ejerciciosSeleccionados.size());

        // Convertir ejercicios a JSON
        JSONArray ejerciciosIds = new JSONArray();
        for (Exercise ejercicio : ejerciciosSeleccionados) {
            ejerciciosIds.put(ejercicio.getId());
        }
        rutina.setEjerciciosIds(ejerciciosIds.toString());

        return rutina;
    }

    /**
     * Determina qué grupos musculares entrenar según el día y el split
     */
    private List<String> determinarGruposMusculares(int indiceDia, int totalDias, User usuario) {
        List<String> grupos = new ArrayList<>();

        // Split basado en el número total de días
        switch (totalDias) {
            case 3: // Full body x3
                grupos.addAll(Arrays.asList("Chest", "Lats", "Shoulders", "Hamstrings"));
                if (indiceDia == 1) grupos.add("Biceps");
                if (indiceDia == 2) grupos.add("Triceps");
                grupos.add("Core");
                break;

            case 4: // Upper/Lower split
                if (indiceDia % 2 == 0) {
                    // Día superior
                    grupos.addAll(Arrays.asList("Chest", "Lats", "Shoulders", "Biceps", "Triceps"));
                } else {
                    // Día inferior
                    grupos.addAll(Arrays.asList("Hamstrings", "Core"));
                }
                break;

            case 5: // Push/Pull/Legs/Upper/Lower
                switch (indiceDia) {
                    case 0: grupos.addAll(Arrays.asList("Chest", "Shoulders", "Triceps")); break;
                    case 1: grupos.addAll(Arrays.asList("Lats", "Biceps")); break;
                    case 2: grupos.addAll(Arrays.asList("Hamstrings", "Core")); break;
                    case 3: grupos.addAll(Arrays.asList("Chest", "Shoulders")); break;
                    case 4: grupos.addAll(Arrays.asList("Lats", "Hamstrings")); break;
                }
                break;

            default:
                grupos.addAll(Arrays.asList("Chest", "Lats", "Hamstrings"));
                break;
        }

        return grupos;
    }

    /**
     * Filtra ejercicios peligrosos según las lesiones del usuario
     */
    private List<Exercise> filtrarEjerciciosSegurosPorLesiones(List<Exercise> ejercicios, List<String> lesiones) {
        if (lesiones == null || lesiones.isEmpty()) {
            return ejercicios;
        }

        List<Exercise> ejerciciosSegsuros = new ArrayList<>();

        for (Exercise ejercicio : ejercicios) {
            boolean esSeguro = true;

            // Verificar si el ejercicio es peligroso para alguna lesión
            for (String lesion : lesiones) {
                if (esPeligrosoParaLesion(ejercicio, lesion)) {
                    esSeguro = false;
                    Log.d(TAG, "Ejercicio filtrado por lesión (" + lesion + "): " + ejercicio.getName());
                    break;
                }
            }

            if (esSeguro) {
                ejerciciosSegsuros.add(ejercicio);
            }
        }

        return ejerciciosSegsuros;
    }

    /**
     * Verifica si un ejercicio es peligroso para una lesión específica
     */
    private boolean esPeligrosoParaLesion(Exercise ejercicio, String lesion) {
        String nombreEjercicio = ejercicio.getName().toLowerCase();
        String lesionLower = lesion.toLowerCase();

        // Reglas de seguridad básicas
        if (lesionLower.contains("cuello") || lesionLower.contains("neck")) {
            return nombreEjercicio.contains("military") || nombreEjercicio.contains("overhead") ||
                   nombreEjercicio.contains("press militar");
        }

        if (lesionLower.contains("espalda") || lesionLower.contains("back")) {
            return nombreEjercicio.contains("deadlift") || nombreEjercicio.contains("peso muerto") ||
                   nombreEjercicio.contains("row") && nombreEjercicio.contains("bent");
        }

        if (lesionLower.contains("hombro") || lesionLower.contains("shoulder")) {
            return nombreEjercicio.contains("overhead") || nombreEjercicio.contains("military") ||
                   nombreEjercicio.contains("lateral raise");
        }

        return false;
    }

    /**
     * Selecciona los mejores ejercicios para el usuario basándose en su objetivo y duración preferida
     */
    private List<Exercise> seleccionarMejoresEjercicios(List<Exercise> ejercicios, User usuario, int maxEjercicios) {
        if (ejercicios.isEmpty()) {
            return new ArrayList<>();
        }

        // CORREGIDO: Ajustar número de ejercicios según duración del entrenamiento y objetivo
        int ejerciciosASeleccionar = calcularNumeroEjercicios(usuario, maxEjercicios);

        Log.d(TAG, "Seleccionando " + ejerciciosASeleccionar + " ejercicios para objetivo: " + usuario.getGoal());

        // Clasificar ejercicios según tipo
        List<Exercise> ejerciciosCompuestos = new ArrayList<>();
        List<Exercise> ejerciciosAislamiento = new ArrayList<>();
        List<Exercise> ejerciciosCardio = new ArrayList<>();

        for (Exercise ejercicio : ejercicios) {
            if (esEjercicioCardio(ejercicio)) {
                ejerciciosCardio.add(ejercicio);
            } else if (esEjercicioCompuesto(ejercicio)) {
                ejerciciosCompuestos.add(ejercicio);
            } else {
                ejerciciosAislamiento.add(ejercicio);
            }
        }

        // Mezclar para variedad
        Collections.shuffle(ejerciciosCompuestos);
        Collections.shuffle(ejerciciosAislamiento);
        Collections.shuffle(ejerciciosCardio);

        List<Exercise> seleccionados = new ArrayList<>();

        // Seleccionar ejercicios según el objetivo del usuario
        String objetivo = usuario.getGoal();
        if (objetivo != null) {
            objetivo = objetivo.toLowerCase();

            if (objetivo.contains("perder peso") || objetivo.contains("quemar grasa") || objetivo.contains("cardio")) {
                // Para pérdida de peso: priorizar cardio y ejercicios compuestos
                seleccionarEjerciciosParaPerdidaPeso(ejerciciosCompuestos, ejerciciosCardio,
                                                   ejerciciosAislamiento, seleccionados, ejerciciosASeleccionar);

            } else if (objetivo.contains("masa muscular") || objetivo.contains("ganar músculo") || objetivo.contains("hipertrofia")) {
                // Para ganancia muscular: priorizar ejercicios compuestos y aislamiento
                seleccionarEjerciciosParaHipertrofia(ejerciciosCompuestos, ejerciciosAislamiento,
                                                   seleccionados, ejerciciosASeleccionar);

            } else if (objetivo.contains("fuerza") || objetivo.contains("potencia")) {
                // Para fuerza: priorizar ejercicios compuestos
                seleccionarEjerciciosParaFuerza(ejerciciosCompuestos, ejerciciosAislamiento,
                                              seleccionados, ejerciciosASeleccionar);
            } else {
                // Objetivo general: balance entre todos los tipos
                seleccionarEjerciciosBalanceados(ejerciciosCompuestos, ejerciciosAislamiento,
                                               ejerciciosCardio, seleccionados, ejerciciosASeleccionar);
            }
        } else {
            // Sin objetivo definido: usar selección balanceada
            seleccionarEjerciciosBalanceados(ejerciciosCompuestos, ejerciciosAislamiento,
                                           ejerciciosCardio, seleccionados, ejerciciosASeleccionar);
        }

        Log.d(TAG, "Ejercicios seleccionados: " + seleccionados.size() + " para objetivo: " + usuario.getGoal());
        return seleccionados;
    }

    /**
     * Calcula el número de ejercicios según la duración del entrenamiento y objetivo
     */
    private int calcularNumeroEjercicios(User usuario, int maxEjerciciosPorGrupo) {
        int duracion = usuario.getWorkoutDuration();
        String objetivo = usuario.getGoal();

        if (duracion <= 0) {
            // Sin duración definida, usar máximo por defecto
            return maxEjerciciosPorGrupo;
        }

        // Calcular ejercicios basándose en duración (asumiendo ~10-15 minutos por ejercicio)
        int ejerciciosPorDuracion;
        if (duracion <= 30) {
            ejerciciosPorDuracion = 1; // Rutina rápida
        } else if (duracion <= 45) {
            ejerciciosPorDuracion = 2; // Rutina intermedia
        } else if (duracion <= 60) {
            ejerciciosPorDuracion = 3; // Rutina completa
        } else {
            ejerciciosPorDuracion = 4; // Rutina extendida
        }

        // Ajustar según objetivo
        if (objetivo != null && objetivo.toLowerCase().contains("masa muscular")) {
            ejerciciosPorDuracion = Math.min(ejerciciosPorDuracion + 1, 4); // Más volumen para hipertrofia
        }

        return Math.min(ejerciciosPorDuracion, maxEjerciciosPorGrupo);
    }

    /**
     * Selecciona ejercicios para pérdida de peso (cardio + compuestos)
     */
    private void seleccionarEjerciciosParaPerdidaPeso(List<Exercise> compuestos, List<Exercise> cardio,
                                                     List<Exercise> aislamiento, List<Exercise> seleccionados,
                                                     int totalEjercicios) {
        int agregados = 0;

        // 50% cardio si está disponible
        int cardioCount = Math.min(cardio.size(), totalEjercicios / 2);
        for (int i = 0; i < cardioCount && agregados < totalEjercicios; i++) {
            seleccionados.add(cardio.get(i));
            agregados++;
        }

        // 40% compuestos
        int compuestosCount = Math.min(compuestos.size(), (totalEjercicios * 4) / 10);
        for (int i = 0; i < compuestosCount && agregados < totalEjercicios; i++) {
            seleccionados.add(compuestos.get(i));
            agregados++;
        }

        // Completar con aislamiento si es necesario
        for (int i = 0; i < aislamiento.size() && agregados < totalEjercicios; i++) {
            seleccionados.add(aislamiento.get(i));
            agregados++;
        }
    }

    /**
     * Selecciona ejercicios para hipertrofia (compuestos + aislamiento)
     */
    private void seleccionarEjerciciosParaHipertrofia(List<Exercise> compuestos, List<Exercise> aislamiento,
                                                     List<Exercise> seleccionados, int totalEjercicios) {
        int agregados = 0;

        // 60% compuestos
        int compuestosCount = Math.min(compuestos.size(), (totalEjercicios * 6) / 10);
        for (int i = 0; i < compuestosCount && agregados < totalEjercicios; i++) {
            seleccionados.add(compuestos.get(i));
            agregados++;
        }

        // 40% aislamiento
        for (int i = 0; i < aislamiento.size() && agregados < totalEjercicios; i++) {
            seleccionados.add(aislamiento.get(i));
            agregados++;
        }
    }

    /**
     * Selecciona ejercicios para fuerza (principalmente compuestos)
     */
    private void seleccionarEjerciciosParaFuerza(List<Exercise> compuestos, List<Exercise> aislamiento,
                                               List<Exercise> seleccionados, int totalEjercicios) {
        int agregados = 0;

        // 80% compuestos
        int compuestosCount = Math.min(compuestos.size(), (totalEjercicios * 8) / 10);
        for (int i = 0; i < compuestosCount && agregados < totalEjercicios; i++) {
            seleccionados.add(compuestos.get(i));
            agregados++;
        }

        // Completar con aislamiento si es necesario
        for (int i = 0; i < aislamiento.size() && agregados < totalEjercicios; i++) {
            seleccionados.add(aislamiento.get(i));
            agregados++;
        }
    }

    /**
     * Selecciona ejercicios de forma balanceada
     */
    private void seleccionarEjerciciosBalanceados(List<Exercise> compuestos, List<Exercise> aislamiento,
                                                List<Exercise> cardio, List<Exercise> seleccionados,
                                                int totalEjercicios) {
        int agregados = 0;

        // Balance: 50% compuestos, 30% aislamiento, 20% cardio
        int compuestosCount = Math.min(compuestos.size(), totalEjercicios / 2);
        for (int i = 0; i < compuestosCount && agregados < totalEjercicios; i++) {
            seleccionados.add(compuestos.get(i));
            agregados++;
        }

        int aislamientoCount = Math.min(aislamiento.size(), (totalEjercicios * 3) / 10);
        for (int i = 0; i < aislamientoCount && agregados < totalEjercicios; i++) {
            seleccionados.add(aislamiento.get(i));
            agregados++;
        }

        // Completar con cardio si está disponible
        for (int i = 0; i < cardio.size() && agregados < totalEjercicios; i++) {
            seleccionados.add(cardio.get(i));
            agregados++;
        }

        // Si aún faltan ejercicios, agregar más compuestos
        for (int i = compuestosCount; i < compuestos.size() && agregados < totalEjercicios; i++) {
            seleccionados.add(compuestos.get(i));
            agregados++;
        }
    }

    /**
     * Determina si un ejercicio es de tipo cardio
     */
    private boolean esEjercicioCardio(Exercise ejercicio) {
        if (ejercicio.getName() == null) return false;

        String nombreLower = ejercicio.getName().toLowerCase();
        return nombreLower.contains("running") || nombreLower.contains("cycling") ||
               nombreLower.contains("cardio") || nombreLower.contains("burpee") ||
               nombreLower.contains("jumping") || nombreLower.contains("mountain climber");
    }

    /**
     * Determina si un ejercicio es compuesto o de aislamiento
     */
    private boolean esEjercicioCompuesto(Exercise ejercicio) {
        if (ejercicio.getMuscleNames() == null) {
            return false;
        }

        // Si trabaja más de 2 grupos musculares, es compuesto
        return ejercicio.getMuscleNames().size() >= 2;
    }

    /**
     * Guarda todas las rutinas generadas en la base de datos
     */
    private void guardarRutinasEnBD(List<Rutina> rutinas, RutinaGeneratorCallback callback) {
        if (rutinas.isEmpty()) {
            callback.onError("No se generaron rutinas");
            return;
        }

        Log.d(TAG, "Guardando " + rutinas.size() + " rutinas en la base de datos");

        guardarRutinaRecursiva(rutinas, 0, callback);
    }

    /**
     * Guarda rutinas de forma recursiva para asegurar que se guarden todas
     */
    private void guardarRutinaRecursiva(List<Rutina> rutinas, int indice, RutinaGeneratorCallback callback) {
        if (indice >= rutinas.size()) {
            // Todas las rutinas guardadas
            callback.onProgress("¡Rutinas generadas con éxito!", 100);
            callback.onRutinasGeneradas(rutinas);
            return;
        }

        Rutina rutina = rutinas.get(indice);

        repository.insertRutina(rutina, new FitnessRepository.DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if (success) {
                    Log.d(TAG, "Rutina guardada: " + rutina.getNombre());

                    // Actualizar progreso
                    int progreso = 80 + ((indice + 1) * 20 / rutinas.size());
                    callback.onProgress("Rutina " + (indice + 1) + " guardada", progreso);

                    // Guardar siguiente rutina
                    guardarRutinaRecursiva(rutinas, indice + 1, callback);
                } else {
                    callback.onError("Error guardando rutina: " + rutina.getNombre());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error guardando rutina " + rutina.getNombre() + ": " + error);
                callback.onError("Error guardando rutina: " + error);
            }
        });
    }
}
