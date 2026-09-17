package com.diyebure.golia.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.diyebure.golia.data.local.entity.CompetitionEntity;

import java.util.List;

/**
 * Data Access Object for competition operations.
 */
@Dao
public interface CompetitionDao {

    /**
     * Get all competitions.
     */
    @Query("SELECT * FROM competitions ORDER BY name ASC")
    List<CompetitionEntity> getAllCompetitions();

    /**
     * Get competition by ID.
     */
    @Query("SELECT * FROM competitions WHERE id = :competitionId")
    CompetitionEntity getCompetitionById(String competitionId);

    /**
     * Get competitions by country.
     */
    @Query("SELECT * FROM competitions WHERE country = :country ORDER BY name ASC")
    List<CompetitionEntity> getCompetitionsByCountry(String country);

    /**
     * Get competitions by season.
     */
    @Query("SELECT * FROM competitions WHERE season = :season ORDER BY name ASC")
    List<CompetitionEntity> getCompetitionsBySeason(String season);

    /**
     * Insert a single competition.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCompetition(CompetitionEntity competition);

    /**
     * Insert multiple competitions.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCompetitions(List<CompetitionEntity> competitions);

    /**
     * Delete competition by ID.
     */
    @Query("DELETE FROM competitions WHERE id = :competitionId")
    void deleteCompetitionById(String competitionId);

    /**
     * Delete all competitions.
     */
    @Query("DELETE FROM competitions")
    void deleteAllCompetitions();

    /**
     * Get competition count.
     */
    @Query("SELECT COUNT(*) FROM competitions")
    int getCompetitionCount();
}