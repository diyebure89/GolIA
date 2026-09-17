package com.diyebure.golia.domain.repository;

import com.diyebure.golia.domain.model.Competition;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.Team;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Match-related data operations.
 * Defines the contract for fetching and managing match data.
 * Implementations should handle data retrieval from remote and local sources.
 */
public interface MatchRepository {

    /**
     * Get all upcoming matches (scheduled for future dates).
     *
     * @return List of upcoming matches sorted by scheduled time
     */
    List<Match> getUpcomingMatches();

    /**
     * Get all currently live matches.
     *
     * @return List of matches with LIVE status
     */
    List<Match> getLiveMatches();

    /**
     * Get a specific match by its ID.
     *
     * @param id The unique identifier of the match (UUID)
     * @return The match if found, null otherwise
     */
    Match getMatchById(UUID id);

    /**
     * Get matches filtered by competition.
     *
     * @param competitionId The competition ID (e.g., "PL", "CL")
     * @return List of matches for the specified competition
     */
    List<Match> getMatchesByCompetition(String competitionId);

    /**
     * Get matches within a date range.
     *
     * @param start Start timestamp in milliseconds
     * @param end End timestamp in milliseconds
     * @return List of matches scheduled between start and end times
     */
    List<Match> getMatchesByDateRange(long start, long end);

    /**
     * Get all available competitions.
     *
     * @return List of competitions
     */
    List<Competition> getCompetitions();

    /**
     * Get team details by ID.
     *
     * @param teamId The team ID
     * @return The team if found, null otherwise
     */
    Team getTeamById(String teamId);

    /**
     * Get all matches for a specific team.
     *
     * @param teamId The team ID
     * @return List of matches involving the specified team
     */
    List<Match> getTeamMatches(String teamId);

    /**
     * Refresh match data from the remote API.
     * This should be called when the user explicitly requests a refresh
     * or when the cached data is potentially stale.
     *
     * @return true if refresh was successful, false otherwise
     */
    boolean refreshMatches();

    /**
     * Refresh live match data (more frequently than regular refresh).
     * Used for updating scores during live matches.
     *
     * @return true if refresh was successful, false otherwise
     */
    boolean refreshLiveMatches();

    /**
     * Clear cached data and perform a full refresh.
     * Should be used sparingly (e.g., on app restart or user request).
     */
    void clearCache();
}