package com.diyebure.golia.domain.repository;

import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.model.Match;

import java.util.List;

/**
 * Repository interface for Match-related data operations.
 *
 * <p>Defines the contract for fetching and managing match data following an
 * asynchronous, <strong>cache-first</strong> strategy. All operations run their
 * blocking network/persistence work off the main thread (in the data-layer
 * implementation via the shared IO executor) and publish their outcome through
 * a {@link Callback}. The callback delivers a {@link com.diyebure.golia.domain.common.Result}
 * that is either a success carrying the data or a typed error.
 *
 * <p>Design note: the previous football-data.org style and synchronous methods
 * (e.g. {@code getUpcomingMatches}, {@code getLiveMatches}, {@code getMatchById},
 * {@code getMatchesByCompetition}, {@code getMatchesByDateRange},
 * {@code getCompetitions}, {@code getTeamById}, {@code getTeamMatches},
 * {@code refreshMatches} and the old boolean {@code refreshLiveMatches}) have been
 * removed in favour of the async signatures below. API-Football exposes fixtures
 * through a single {@code /fixtures} endpoint, so a single date-based refresh
 * covers every target league.
 */
public interface MatchRepository {

    /**
     * Get matches following a cache-first strategy: delivers the currently
     * cached matches without hitting the network.
     *
     * @param callback receives {@code Result.Success<List<Match>>} with the
     *                 cached matches, or {@code Result.Error} on failure
     */
    void getMatches(Callback<List<Match>> callback);

    /**
     * Refresh match data for a specific date from the remote API. A single
     * request covers all target leagues for that date. The freshly fetched
     * matches are persisted to the cache and delivered through the callback.
     *
     * @param isoDate  the target date in ISO-8601 format ({@code yyyy-MM-dd})
     * @param callback receives {@code Result.Success<List<Match>>} with the
     *                 refreshed matches, or {@code Result.Error} on failure
     */
    void refreshMatchesByDate(String isoDate, Callback<List<Match>> callback);

    /**
     * Refresh live match data (scores and elapsed minute) from the remote API.
     * Intended to be called more frequently than a full refresh while there are
     * live matches visible.
     *
     * @param callback receives {@code Result.Success<List<Match>>} with the
     *                 updated matches, or {@code Result.Error} on failure
     */
    void refreshLiveMatches(Callback<List<Match>> callback);

    /**
     * Clear cached data. Should be used sparingly (e.g. on user request or when
     * the cache must be invalidated).
     */
    void clearCache();
}
