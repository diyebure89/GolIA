package com.diyebure.golia.data.remote.api;

import com.diyebure.golia.data.remote.dto.FixtureResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit API service interface for the API-Football (API-SPORTS) provider.
 *
 * <p>All requests target the {@code /fixtures} endpoint and rely on numeric league
 * identifiers passed as query parameters (never the football-data.org string codes
 * such as {@code "PL"}). The response is the nested JSON structure exposed by
 * {@link FixtureResponseDto} ({@code response[].fixture/teams/goals/league}).</p>
 *
 * <p>The required {@code x-apisports-key} authentication header is NOT declared per
 * method here; it is injected transparently by an OkHttp interceptor
 * ({@code ApiKeyInterceptor}) configured on the shared client.</p>
 */
public interface FootballApiService {

    /**
     * Get fixtures scheduled for a specific date.
     *
     * <p>Preferred entry point: a single request by date covers all target leagues
     * for that day, minimising the number of requests against the daily quota.</p>
     *
     * @param date Date in {@code YYYY-MM-DD} format
     * @return Call containing the nested fixtures response for that date
     */
    @GET("fixtures")
    Call<FixtureResponseDto> getFixturesByDate(@Query("date") String date);

    /**
     * Get fixtures for a specific league and season.
     *
     * @param leagueId The numeric API-Football league identifier
     *                 (e.g. 39 Premier League, 140 LaLiga, 135 Serie A,
     *                 78 Bundesliga, 61 Ligue 1, 239 Colombia Primera A)
     * @param season   The season year (e.g. 2024)
     * @return Call containing the nested fixtures response for that league and season
     */
    @GET("fixtures")
    Call<FixtureResponseDto> getFixturesByLeague(
            @Query("league") int leagueId,
            @Query("season") int season
    );
}
