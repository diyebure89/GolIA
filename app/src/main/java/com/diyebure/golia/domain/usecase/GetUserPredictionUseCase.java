package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.PredictionError;
import com.diyebure.golia.domain.error.PredictionException;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.repository.PredictionRepository;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Reads the current user's existing prediction for a match, if any (R4.4).
 *
 * <p>The synchronous {@link PredictionRepository#getUserPrediction(String, String)}
 * call runs on the shared {@link IoExecutor} {@link ExecutorService} and the
 * outcome is delivered through the supplied {@link Callback}.</p>
 *
 * <p>This use case is <strong>null-safe</strong>: when the user has not yet made
 * a prediction for the match, the repository reports a
 * {@link PredictionError#MATCH_NOT_FOUND}; that "no prediction" case is
 * translated here into a {@code Result.Success<Prediction>} carrying {@code null}
 * so callers can distinguish "no prediction yet" from a real persistence error.
 */
public class GetUserPredictionUseCase {

    private final PredictionRepository predictionRepository;
    private final ExecutorService executor;

    @Inject
    public GetUserPredictionUseCase(PredictionRepository predictionRepository,
                                    @IoExecutor ExecutorService executor) {
        this.predictionRepository = predictionRepository;
        this.executor = executor;
    }

    /**
     * Delivers the user's prediction for the match off the main thread.
     *
     * @param userId   the id of the user
     * @param matchId  the primary-key id of the match
     * @param callback receives {@code Result.Success<Prediction>} with the
     *                 existing prediction (or {@code null} when none exists), or
     *                 {@code Result.Error} on a persistence failure
     */
    public void execute(String userId, String matchId, Callback<Prediction> callback) {
        executor.execute(() -> callback.onResult(read(userId, matchId)));
    }

    private Result<Prediction> read(String userId, String matchId) {
        Result<Prediction> result = predictionRepository.getUserPrediction(userId, matchId);
        if (result.isSuccess()) {
            return result;
        }
        // The repository signals "no prediction for this pair" with MATCH_NOT_FOUND;
        // surface it as a null success so callers treat it as "no prediction yet".
        Exception error = result.getErrorOrNull();
        if (error instanceof PredictionException
                && ((PredictionException) error).getPredictionError() == PredictionError.MATCH_NOT_FOUND) {
            return new Result.Success<>(null);
        }
        return result;
    }
}
