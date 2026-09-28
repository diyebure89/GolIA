package com.diyebure.golia.domain.repository;

import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.Prediction;

import java.util.List;

/**
 * Repository interface for Prediction-related data operations.
 *
 * <p>Encapsulates the upsert of a user's prediction (keyed by
 * {@code (userId, matchId)}) plus the history reads and the resolution of
 * finished matches, replicating the {@code LocalAuthRepositoryImpl} pattern.
 *
 * <p>Design note: unlike {@link MatchRepository}, whose network-backed methods
 * are asynchronous, the prediction operations are purely local (Room only) and
 * are exposed as <strong>synchronous</strong> methods that return a
 * {@link com.diyebure.golia.domain.common.Result}. The callers (use cases)
 * are responsible for dispatching these calls onto the shared {@code @IoExecutor}
 * off the main thread. Each method returns either a success carrying the data or
 * a {@code Result.Error} carrying a
 * {@link com.diyebure.golia.domain.error.PredictionException} with the relevant
 * {@link com.diyebure.golia.domain.error.PredictionError}.
 */
public interface PredictionRepository {

    /**
     * Get the current user's prediction for a specific match.
     *
     * @param userId  the id of the user (as stored in {@code predictions.user_id})
     * @param matchId the primary-key id of the match (as stored in
     *                {@code predictions.match_id})
     * @return {@code Result.Success<Prediction>} with the existing prediction, or
     *         {@code Result.Error} when none exists or persistence fails
     */
    Result<Prediction> getUserPrediction(String userId, String matchId);

    /**
     * Save (upsert) the current user's prediction for a match. If a prediction
     * already exists for the {@code (userId, matchId)} pair it is updated in
     * place (reusing its {@code id} and {@code created_at}); otherwise a new
     * prediction is created. The predicted outcome is derived from the score and
     * the prediction starts unresolved ({@code isCorrect = null},
     * {@code pointsEarned = 0}).
     *
     * @param userId    the id of the user
     * @param matchId   the primary-key id of the match
     * @param homeScore the predicted home-team score
     * @param awayScore the predicted away-team score
     * @return {@code Result.Success<Prediction>} with the saved prediction, or
     *         {@code Result.Error} on validation or persistence failure
     */
    Result<Prediction> savePrediction(String userId, String matchId, int homeScore, int awayScore);

    /**
     * Get all predictions made by the given user, sorted for history display.
     *
     * @param userId the id of the user
     * @return {@code Result.Success<List<Prediction>>} with the user's
     *         predictions (possibly empty), or {@code Result.Error} on failure
     */
    Result<List<Prediction>> getUserPredictions(String userId);

    /**
     * Get every prediction registered for a specific match.
     *
     * @param matchId the primary-key id of the match
     * @return {@code Result.Success<List<Prediction>>} with the predictions for
     *         that match (possibly empty), or {@code Result.Error} on failure
     */
    Result<List<Prediction>> getPredictionsForMatch(String matchId);

    /**
     * Resolve every pending prediction for a finished match, scoring each one and
     * marking it as correct/incorrect. This operation is idempotent: predictions
     * that are already resolved are left untouched.
     *
     * @param finishedMatch the {@code FINISHED} match (with a final score) whose
     *                      pending predictions should be resolved
     * @return {@code Result.Success<Void>} when resolution completes (a no-op is
     *         still a success), or {@code Result.Error} on failure
     */
    Result<Void> resolveForMatch(Match finishedMatch);
}
