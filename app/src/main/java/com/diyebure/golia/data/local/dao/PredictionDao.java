package com.diyebure.golia.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.diyebure.golia.data.local.entity.PredictionEntity;

import java.util.List;

/**
 * Data Access Object for prediction operations.
 */
@Dao
public interface PredictionDao {

    /**
     * Get prediction by ID.
     */
    @Query("SELECT * FROM predictions WHERE id = :predictionId")
    PredictionEntity getPredictionById(String predictionId);

    /**
     * Get all predictions for a specific user.
     */
    @Query("SELECT * FROM predictions WHERE user_id = :userId ORDER BY created_at DESC")
    List<PredictionEntity> getPredictionsByUser(String userId);

    /**
     * Get all predictions for a specific match.
     */
    @Query("SELECT * FROM predictions WHERE match_id = :matchId ORDER BY created_at DESC")
    List<PredictionEntity> getPredictionsByMatch(String matchId);

    /**
     * Get a specific user's prediction for a specific match.
     */
    @Query("SELECT * FROM predictions WHERE user_id = :userId AND match_id = :matchId LIMIT 1")
    PredictionEntity getUserPredictionForMatch(String userId, String matchId);

    /**
     * Get predictions by status (correct, incorrect, or pending).
     * isCorrect can be true, false, or null for pending predictions.
     */
    @Query("SELECT * FROM predictions WHERE is_correct = :isCorrect ORDER BY created_at DESC")
    List<PredictionEntity> getPredictionsByStatus(Boolean isCorrect);

    /**
     * Get all predictions for a user with a specific status.
     */
    @Query("SELECT * FROM predictions WHERE user_id = :userId AND is_correct = :isCorrect ORDER BY created_at DESC")
    List<PredictionEntity> getUserPredictionsByStatus(String userId, Boolean isCorrect);

    /**
     * Get user's predictions sorted by various criteria.
     */
    @Query("SELECT * FROM predictions WHERE user_id = :userId ORDER BY created_at DESC")
    List<PredictionEntity> getUserPredictionsSorted(String userId);

    /**
     * Get user's predictions sorted by points earned (highest first).
     */
    @Query("SELECT * FROM predictions WHERE user_id = :userId ORDER BY points_earned DESC")
    List<PredictionEntity> getUserPredictionsByPoints(String userId);

    /**
     * Get user's pending predictions (match not yet finalized).
     */
    @Query("SELECT * FROM predictions WHERE user_id = :userId AND is_correct IS NULL ORDER BY created_at DESC")
    List<PredictionEntity> getUserPendingPredictions(String userId);

    /**
     * Get user's finalized predictions (match finished).
     */
    @Query("SELECT * FROM predictions WHERE user_id = :userId AND is_correct IS NOT NULL ORDER BY finalized_at DESC")
    List<PredictionEntity> getUserFinalizedPredictions(String userId);

    /**
     * Get count of predictions for a user.
     */
    @Query("SELECT COUNT(*) FROM predictions WHERE user_id = :userId")
    int getUserPredictionCount(String userId);

    /**
     * Get count of correct predictions for a user.
     */
    @Query("SELECT COUNT(*) FROM predictions WHERE user_id = :userId AND is_correct = 1")
    int getUserCorrectPredictionCount(String userId);

    /**
     * Get total points earned by a user.
     */
    @Query("SELECT COALESCE(SUM(points_earned), 0) FROM predictions WHERE user_id = :userId AND is_correct = 1")
    int getUserTotalPoints(String userId);

    /**
     * Insert a single prediction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPrediction(PredictionEntity prediction);

    /**
     * Insert multiple predictions.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPredictions(List<PredictionEntity> predictions);

    /**
     * Update a prediction.
     */
    @Update
    void updatePrediction(PredictionEntity prediction);

    /**
     * Delete a prediction.
     */
    @Delete
    void deletePrediction(PredictionEntity prediction);

    /**
     * Delete prediction by ID.
     */
    @Query("DELETE FROM predictions WHERE id = :predictionId")
    void deletePredictionById(String predictionId);

    /**
     * Delete all predictions for a user.
     */
    @Query("DELETE FROM predictions WHERE user_id = :userId")
    void deleteAllUserPredictions(String userId);

    /**
     * Delete all predictions.
     */
    @Query("DELETE FROM predictions")
    void deleteAllPredictions();

    /**
     * Check if a user has already predicted for a match.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM predictions WHERE user_id = :userId AND match_id = :matchId)")
    boolean userHasPredictionForMatch(String userId, String matchId);
}