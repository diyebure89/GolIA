package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.model.PredictionOutcome;
import com.diyebure.golia.domain.repository.MatchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Use case for updating prediction results when a match is finalized.
 * 
 * This use case orchestrates the process of:
 * 1. Finding all predictions for a finalized match
 * 2. Determining if each prediction is correct
 * 3. Calculating points earned for correct predictions
 * 4. Updating user total points
 * 
 * This is typically called when:
 * - Match results are received from the API
 * - A scheduled match transitions to FINISHED status
 * - After refreshing match data
 */
public class UpdatePredictionResultUseCase {

    private final MatchRepository matchRepository;
    private final CalculatePointsUseCase calculatePointsUseCase;
    private final DeterminePredictionCorrectnessUseCase determineCorrectnessUseCase;
    private final PredictionRepositoryProvider predictionRepositoryProvider;

    /**
     * Functional interface for providing access to prediction storage.
     * This allows the use case to work with different storage implementations.
     */
    public interface PredictionRepositoryProvider {
        
        /**
         * Get all predictions for a specific match.
         * 
         * @param matchId The match ID
         * @return List of predictions for the match
         */
        List<Prediction> getPredictionsForMatch(UUID matchId);
        
        /**
         * Update a prediction in storage.
         * 
         * @param prediction The prediction to update
         * @return true if update was successful
         */
        boolean updatePrediction(Prediction prediction);
        
        /**
         * Get the current total points for a user.
         * 
         * @param userId The user ID
         * @return Current total points
         */
        int getUserTotalPoints(UUID userId);
        
        /**
         * Update a user's total points.
         * 
         * @param userId The user ID
         * @param newTotalPoints The new total points value
         * @return true if update was successful
         */
        boolean updateUserTotalPoints(UUID userId, int newTotalPoints);
    }

    /**
     * Result of updating prediction results.
     */
    public static class UpdateResult {
        private final int predictionsUpdated;
        private final int totalPointsAwarded;
        private final List<UUID> updatedPredictionIds;
        private final boolean success;
        private final String errorMessage;

        private UpdateResult(int predictionsUpdated, int totalPointsAwarded,
                            List<UUID> updatedPredictionIds, boolean success, String errorMessage) {
            this.predictionsUpdated = predictionsUpdated;
            this.totalPointsAwarded = totalPointsAwarded;
            this.updatedPredictionIds = updatedPredictionIds;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static UpdateResult success(int predictionsUpdated, int totalPointsAwarded, List<UUID> updatedPredictionIds) {
            return new UpdateResult(predictionsUpdated, totalPointsAwarded, updatedPredictionIds, true, null);
        }

        public static UpdateResult failure(String errorMessage) {
            return new UpdateResult(0, 0, new ArrayList<>(), false, errorMessage);
        }

        public int getPredictionsUpdated() { return predictionsUpdated; }
        public int getTotalPointsAwarded() { return totalPointsAwarded; }
        public List<UUID> getUpdatedPredictionIds() { return updatedPredictionIds; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Constructor with dependencies.
     * 
     * @param matchRepository Repository for accessing match data
     * @param calculatePointsUseCase Use case for calculating points
     * @param determineCorrectnessUseCase Use case for determining prediction correctness
     * @param predictionRepositoryProvider Provider for prediction storage operations
     */
    public UpdatePredictionResultUseCase(
            MatchRepository matchRepository,
            CalculatePointsUseCase calculatePointsUseCase,
            DeterminePredictionCorrectnessUseCase determineCorrectnessUseCase,
            PredictionRepositoryProvider predictionRepositoryProvider) {
        this.matchRepository = matchRepository;
        this.calculatePointsUseCase = calculatePointsUseCase;
        this.determineCorrectnessUseCase = determineCorrectnessUseCase;
        this.predictionRepositoryProvider = predictionRepositoryProvider;
    }

    /**
     * Update results for all predictions associated with a finalized match.
     * 
     * @param match The finalized match with results
     * @return UpdateResult containing details of the update operation
     */
    public UpdateResult execute(Match match) {
        if (match == null) {
            return UpdateResult.failure("Match cannot be null");
        }

        // Verify match is finalized
        if (!determineCorrectnessUseCase.isMatchFinalized(match)) {
            return UpdateResult.failure("Match is not finalized");
        }

        try {
            return updatePredictionsForMatch(match);
        } catch (Exception e) {
            return UpdateResult.failure("Failed to update predictions: " + e.getMessage());
        }
    }

    /**
     * Update predictions for a single match.
     */
    private UpdateResult updatePredictionsForMatch(Match match) {
        List<Prediction> predictions = predictionRepositoryProvider.getPredictionsForMatch(match.getId());
        
        if (predictions.isEmpty()) {
            return UpdateResult.success(0, 0, new ArrayList<>());
        }

        List<UUID> updatedPredictionIds = new ArrayList<>();
        int totalPointsAwarded = 0;
        int predictionsUpdated = 0;

        // Determine the actual outcome once
        PredictionOutcome actualOutcome = determineCorrectnessUseCase.determineActualOutcome(match);

        for (Prediction prediction : predictions) {
            // Skip if already finalized
            if (prediction.isFinalized()) {
                continue;
            }

            PredictionOutcome predicted = prediction.getPredictedOutcome();
            
            // Determine if prediction is correct
            boolean isCorrect = predicted == actualOutcome;
            prediction.setIsCorrect(isCorrect);

            // Calculate points if correct
            int pointsEarned = 0;
            if (isCorrect) {
                pointsEarned = calculatePointsUseCase.execute(
                    predicted,
                    actualOutcome,
                    match.getHomeOdds(),
                    match.getDrawOdds(),
                    match.getAwayOdds()
                );
            }
            prediction.setPointsEarned(pointsEarned);
            prediction.setFinalizedAt(System.currentTimeMillis());

            // Update prediction in storage
            boolean updated = predictionRepositoryProvider.updatePrediction(prediction);
            if (updated) {
                updatedPredictionIds.add(prediction.getId());
                predictionsUpdated++;
                totalPointsAwarded += pointsEarned;

                // Update user's total points
                if (pointsEarned > 0 && prediction.getUserId() != null) {
                    updateUserPoints(prediction.getUserId(), pointsEarned);
                }
            }
        }

        return UpdateResult.success(predictionsUpdated, totalPointsAwarded, updatedPredictionIds);
    }

    /**
     * Update a user's total points by adding the awarded points.
     * 
     * @param userId The user to update
     * @param pointsToAdd Points to add to the user's total
     */
    private void updateUserPoints(UUID userId, int pointsToAdd) {
        int currentTotal = predictionRepositoryProvider.getUserTotalPoints(userId);
        int newTotal = currentTotal + pointsToAdd;
        predictionRepositoryProvider.updateUserTotalPoints(userId, newTotal);
    }

    /**
     * Update results for multiple matches at once.
     * Useful when refreshing all match data.
     * 
     * @param matches List of finalized matches to process
     * @return List of UpdateResult for each match
     */
    public List<UpdateResult> executeAll(List<Match> matches) {
        List<UpdateResult> results = new ArrayList<>();
        for (Match match : matches) {
            results.add(execute(match));
        }
        return results;
    }

    /**
     * Check if a match has any predictions that need to be updated.
     * 
     * @param matchId The match ID to check
     * @return true if there are pending predictions for this match
     */
    public boolean hasPendingPredictions(UUID matchId) {
        List<Prediction> predictions = predictionRepositoryProvider.getPredictionsForMatch(matchId);
        for (Prediction prediction : predictions) {
            if (prediction.isPending()) {
                return true;
            }
        }
        return false;
    }
}