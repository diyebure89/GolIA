package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.repository.PredictionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Resolves the pending predictions of finished matches, scoring each one and
 * marking it as correct/incorrect (R9.6, R9.7, R9.8).
 *
 * <p>For every {@code FINISHED} {@link Match} that carries a final score, it
 * delegates to {@link PredictionRepository#resolveForMatch(Match)}, which loads
 * the match's predictions ({@code getPredictionsByMatch}) and, for each still
 * pending one ({@code isCorrect == null}), computes the points with the
 * {@code Prediction_Scorer}, sets {@code isCorrect}, {@code pointsEarned} and
 * {@code finalizedAt}, and persists the change ({@code updatePrediction}). The
 * operation is <strong>idempotent</strong>: already resolved predictions are
 * left untouched, so re-running the resolver over the same matches is safe.
 *
 * <p>No network calls are made here; this use case only reads/writes the local
 * cache. The blocking repository work runs on the shared {@link IoExecutor}
 * {@link ExecutorService} and the outcome is delivered through the supplied
 * {@link Callback} (same skeleton as the other use cases). It is intended to be
 * invoked from the match-refresh flow when matches transition to
 * {@code FINISHED} with a score (R9.8).
 */
public class PredictionResolverUseCase {

    private final PredictionRepository predictionRepository;
    private final ExecutorService executor;

    @Inject
    public PredictionResolverUseCase(PredictionRepository predictionRepository,
                                     @IoExecutor ExecutorService executor) {
        this.predictionRepository = predictionRepository;
        this.executor = executor;
    }

    /**
     * Resolves the pending predictions for the given finished matches off the
     * main thread.
     *
     * <p>Each match is resolved through the repository. Matches that are not
     * finished or lack a score are treated as no-ops by the repository. If any
     * match fails to resolve, the first error is delivered; otherwise a
     * {@code Result.Success<Void>} is delivered once all matches are processed.
     *
     * @param finishedMatches the matches whose pending predictions should be
     *                        resolved (may be {@code null} or empty)
     * @param callback        receives {@code Result.Success<Void>} on completion
     *                        or {@code Result.Error} if resolution fails
     */
    public void execute(List<Match> finishedMatches, Callback<Void> callback) {
        executor.execute(() -> callback.onResult(resolveAll(finishedMatches)));
    }

    private Result<Void> resolveAll(List<Match> finishedMatches) {
        if (finishedMatches == null || finishedMatches.isEmpty()) {
            // Nothing to resolve; a no-op is still a success.
            return new Result.Success<>(null);
        }

        // Defensive copy to avoid surprises if the caller mutates the list.
        List<Match> matches = new ArrayList<>(finishedMatches);
        for (Match match : matches) {
            Result<Void> result = predictionRepository.resolveForMatch(match);
            if (result.isError()) {
                return result;
            }
        }
        return new Result.Success<>(null);
    }
}
