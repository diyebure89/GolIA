package com.diyebure.golia.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.diyebure.golia.data.local.entity.TeamEntity;

import java.util.List;

/**
 * Data Access Object for team operations.
 */
@Dao
public interface TeamDao {

    /**
     * Get all teams.
     */
    @Query("SELECT * FROM teams ORDER BY name ASC")
    List<TeamEntity> getAllTeams();

    /**
     * Get team by ID.
     */
    @Query("SELECT * FROM teams WHERE id = :teamId")
    TeamEntity getTeamById(String teamId);

    /**
     * Get teams by country.
     */
    @Query("SELECT * FROM teams WHERE country = :country ORDER BY name ASC")
    List<TeamEntity> getTeamsByCountry(String country);

    /**
     * Search teams by name.
     */
    @Query("SELECT * FROM teams WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    List<TeamEntity> searchTeams(String searchQuery);

    /**
     * Insert a single team.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTeam(TeamEntity team);

    /**
     * Insert multiple teams.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTeams(List<TeamEntity> teams);

    /**
     * Delete team by ID.
     */
    @Query("DELETE FROM teams WHERE id = :teamId")
    void deleteTeamById(String teamId);

    /**
     * Delete all teams.
     */
    @Query("DELETE FROM teams")
    void deleteAllTeams();

    /**
     * Get team count.
     */
    @Query("SELECT COUNT(*) FROM teams")
    int getTeamCount();
}