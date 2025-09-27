package com.jossecm.myapplication.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIService {
    private static final String TAG = "AIService";
    private static final String INVOKE_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String API_KEY = "Bearer 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";
    private static final String MODEL = "meta/llama-3.1-8b-instruct";

    private ExecutorService executorService;
    private Handler mainHandler;

    public interface AICallback {
        void onResponse(String respuesta);
        void onError(String error);
        void onStartTyping(); // Para mostrar "IA escribiendo..."
    }

    public AIService() {
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void enviarMensaje(String mensaje, String contextoPersonalizado, AICallback callback) {
        Log.d(TAG, "Enviando mensaje: " + mensaje);

        // Notificar que la IA comenzó a escribir
        mainHandler.post(() -> callback.onStartTyping());

        executorService.execute(() -> {
            try {
                // Crear conexión HTTP
                URL url = new URL(INVOKE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Configurar headers
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", API_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setConnectTimeout(30000); // 30 segundos
                connection.setReadTimeout(30000);

                // Crear payload JSON
                JSONObject payload = new JSONObject();
                payload.put("model", MODEL);
                payload.put("max_tokens", 512);
                payload.put("temperature", 0.7);
                payload.put("top_p", 1.0);
                payload.put("stream", false); // Usar response simple por ahora

                // Crear array de mensajes
                JSONArray messages = new JSONArray();

                // Mensaje del sistema (contexto personalizado)
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", contextoPersonalizado != null ? contextoPersonalizado :
                    "Eres un coach personal de fitness experto. Responde preguntas sobre ejercicios, técnicas y entrenamiento de forma motivadora y profesional.");
                messages.put(systemMessage);

                // Mensaje del usuario
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", mensaje);
                messages.put(userMessage);

                payload.put("messages", messages);

                Log.d(TAG, "Payload: " + payload.toString());

                // Enviar request
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(payload.toString());
                writer.flush();
                writer.close();

                // Leer response
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String responseBody = response.toString();
                Log.d(TAG, "Response: " + responseBody);

                if (responseCode >= 200 && responseCode < 300) {
                    // Parsear respuesta exitosa
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    if (jsonResponse.has("choices")) {
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject messageResponse = choice.getJSONObject("message");
                            String respuestaIA = messageResponse.getString("content");

                            // Llamar callback en el hilo principal
                            mainHandler.post(() -> callback.onResponse(respuestaIA.trim()));
                        } else {
                            mainHandler.post(() -> callback.onError("No se recibió respuesta de la IA"));
                        }
                    } else {
                        mainHandler.post(() -> callback.onError("Formato de respuesta inválido"));
                    }
                } else {
                    // Error HTTP
                    String errorMessage = "Error HTTP: " + responseCode;
                    try {
                        JSONObject errorJson = new JSONObject(responseBody);
                        if (errorJson.has("error")) {
                            JSONObject error = errorJson.getJSONObject("error");
                            if (error.has("message")) {
                                errorMessage = error.getString("message");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parseando mensaje de error", e);
                    }

                    final String finalErrorMessage = errorMessage;
                    mainHandler.post(() -> callback.onError(finalErrorMessage));
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error en request a IA", e);
                mainHandler.post(() -> callback.onError("Error de conexión: " + e.getMessage()));
            }
        });
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
