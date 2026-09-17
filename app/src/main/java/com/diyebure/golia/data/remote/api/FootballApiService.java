package com.diyebure.golia.data.remote.api;

import com.diyebure.golia.data.remote.dto.MatchDto.CompetitionResponseDto;
import com.diyebure.golia.data.remote.dto.MatchDto.MatchResponseDto;
import com.diyebure.golia.data.remote.dto.MatchDto.TeamResponseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit API service interface for Football data endpoints.
 * Provides methods to fetch competitions, matches, and team data from the football API.
 */
public interface FootballApiService {

    /**
     * Get all available competitions.
     *
     * @return Call containing list of competitions
     */
    @GET("competitions")
    Call<CompetitionResponseDto> getCompetitions();

    /**
     * Get matches for a specific competition.
     *
     * @param competitionId The competition ID (e.g., "PL" for Premier League)
     * @return Call containing list of matches for the competition
     */
    @GET("competitions/{id}/matches")
    Call<MatchResponseDto> getMatchesByCompetition(@Path("id") String competitionId);

    /**
     * Get matches scheduled for a specific date.
     *
     * @param date Date in YYYY-MM-DD format
     * @return Call containing list of matches on that date
     */
    @GET("matches")
    Call<MatchResponseDto> getMatchesByDate(@Query("date") String date);

    /**
     * Get matches within a date range.
     *
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Call containing list of matches within the date range
     */
    @GET("matches")
    Call<MatchResponseDto> getMatchesByDateRange(
            @Query("start_date") String startDate,
            @Query("end_date") String endDate
    );

    /**
     * Get detailed information for a specific match.
     *
     * @param matchId The match ID
     * @return Call containing match details
     */
    @GET("matches/{id}")
    Call<MatchResponseDto> getMatchById(@Path("id") String matchId);

    /**
     * Get detailed information for a specific team.
     *
     * @param teamId The team ID
     * @return Call containing team details
     */
    @GET("teams/{id}")
    Call<TeamResponseDto> getTeamById(@Path("id") String teamId);

    /**
     * Get all matches for a specific team.
     *
     * @param teamId The team ID
     * @return Call containing list of matches for the team
     */
    @GET("teams/{id}/matches")
    Call<MatchResponseDto> getTeamMatches(@Path("id") String teamId);
}