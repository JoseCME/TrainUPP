package com.jossecm.myapplication.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String BASE_URL = "https://wger.de/api/v2/";
    private static Retrofit retrofit = null;
    private static WgerApiService apiService = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Configurar OkHttpClient con timeouts
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static WgerApiService getApiService() {
        if (apiService == null) {
            apiService = getClient().create(WgerApiService.class);
        }
        return apiService;
    }
}
