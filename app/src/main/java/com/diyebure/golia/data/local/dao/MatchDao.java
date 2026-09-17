package com.diyebure.golia.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.diyebure.golia.data.local.entity.MatchEntity;

import java.util.List;

/**
 * Data Access Object for match operations.
 */
@Dao
public interface MatchDao {

    /**
     * Get all upcoming matches (SCHEDULED status).
     */
    @Query("SELECT * FROM matches WHERE status = 'SCHEDULED' ORDER BY scheduled_date_time ASC")
    List<MatchEntity> getUpcomingMatches();

    /**
     * Get all live matches (LIVE status).
     */
    @Query("SELECT * FROM matches WHERE status = 'LIVE' ORDER BY scheduled_date_time ASC")
    List<MatchEntity> getLiveMatches();

    /**
     * Get match by ID.
     */
    @Query("SELECT * FROM matches WHERE id = :matchId")
    MatchEntity getMatchById(String matchId);

    /**
     * Get match by external ID.
     */
    @Query("SELECT * FROM matches WHERE external_id = :externalId")
    MatchEntity getMatchByExternalId(String externalId);

    /**
     * Get matches by competition ID.
     */
    @Query("SELECT * FROM matches WHERE competition_id = :competitionId ORDER BY scheduled_date_time ASC")
    List<MatchEntity> getMatchesByCompetition(String competitionId);

    /**
     * Get matches within a date range.
     */
    @Query("SELECT * FROM matches WHERE scheduled_date_time BETWEEN :startTime AND :endTime ORDER BY scheduled_date_time ASC")
    List<MatchEntity> getMatchesByDateRange(long startTime, long endTime);

    /**
     * Get all matches.
     */
    @Query("SELECT * FROM matches ORDER BY scheduled_date_time DESC")
    List<MatchEntity> getAllMatches();

    /**
     * Insert a single match.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMatch(MatchEntity match);

    /**
     * Insert multiple matches.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMatches(List<MatchEntity> matches);

    /**
     * Update a match.
     */
    @Update
    void updateMatch(MatchEntity match);

    /**
     * Delete old matches (finished or cancelled) older than the specified days.
     */
    @Query("DELETE FROM matches WHERE (status = 'FINISHED' OR status = 'CANCELLED' OR status = 'POSTPONED') AND scheduled_date_time < :cutoffTime")
    void deleteOldMatches(long cutoffTime);

    /**
     * Delete all matches.
     */
    @Query("DELETE FROM matches")
    void deleteAllMatches();

    /**
     * Delete match by ID.
     */
    @Query("DELETE FROM matches WHERE id = :matchId")
    void deleteMatchById(String matchId);

    /**
     * Get count of matches.
     */
    @Query("SELECT COUNT(*) FROM matches")
    int getMatchCount();
}