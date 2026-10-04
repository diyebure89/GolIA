package com.diyebure.golia.data.remote.api;

import com.diyebure.golia.data.remote.dto.PredictionDto.PredictionResponse;
import com.diyebure.golia.data.remote.dto.PredictionDto.SavePredictionRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Servicio Retrofit para los pronosticos, respaldados por el backend GolIA
 * (MySQL). El token de sesion lo agrega automaticamente el {@code AuthInterceptor}
 * del stack GOLIA_BACKEND, asi que estos endpoints van autenticados.
 */
public interface PredictionApiService {

    /** Crea o actualiza (upsert) el pronostico del usuario autenticado. */
    @POST("predictions")
    Call<PredictionResponse> savePrediction(@Body SavePredictionRequest request);

    /** Historial de pronosticos del usuario autenticado. */
    @GET("predictions")
    Call<List<PredictionResponse>> getMyPredictions();

    /** Todos los pronosticos de un partido. */
    @GET("predictions/by-match")
    Call<List<PredictionResponse>> getByMatch(@Query("matchId") String matchId);
}
