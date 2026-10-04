package com.diyebure.golia.data.remote.api;

import com.diyebure.golia.data.remote.dto.RankingEntryDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Servicio Retrofit para el ranking global, respaldado por el backend GolIA
 * (MySQL). El endpoint es publico, pero el {@code AuthInterceptor} agrega el
 * token si existe sin causar problema.
 */
public interface RankingApiService {

    /** Ranking global: usuarios ordenados por puntos (posicion 1..N). */
    @GET("ranking")
    Call<List<RankingEntryDto>> getGlobalRanking();
}
