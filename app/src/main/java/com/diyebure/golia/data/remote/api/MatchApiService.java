package com.diyebure.golia.data.remote.api;

import com.diyebure.golia.data.remote.dto.MatchResolveDto.ResolveRequest;
import com.diyebure.golia.data.remote.dto.MatchResolveDto.ResolveResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Servicio Retrofit para los endpoints de partido del backend GolIA.
 * El token de sesion lo agrega automaticamente el {@code AuthInterceptor}.
 */
public interface MatchApiService {

    /**
     * Notifica al backend que un partido termino con un marcador final. El
     * backend resuelve los pronosticos pendientes, calcula puntos y actualiza
     * {@code users.total_points} / {@code predictions_correct} en MySQL para
     * que el ranking sea real y compartido entre dispositivos.
     *
     * @param matchId id del partido (el mismo que usa la app internamente)
     * @param request marcador final
     */
    @POST("matches/{matchId}/resolve")
    Call<ResolveResponse> resolveMatch(
            @Path("matchId") String matchId,
            @Body ResolveRequest request);
}
