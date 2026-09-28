package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.PredictionError;
import com.diyebure.golia.domain.error.PredictionException;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.repository.PredictionRepository;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Saves (upserts) the current user's exact-score prediction for a match, after
 * enforcing the prediction lock threshold (R5.7).
 *
 * <p>The lock check is <strong>authoritative</strong>: a match admits a
 * prediction only while it is {@link MatchStatus#SCHEDULED} and its kickoff is
 * still more than {@link #PREDICTION_LOCK_MILLIS} in the future (predictions
 * close 10 minutes before kickoff). When the match is closed, missing from the
 * cache, or the lock window has passed, a typed
 * {@link PredictionError#PREDICTION_CLOSED} (or
 * {@link PredictionError#MATCH_NOT_FOUND}) is returned and no write occurs.</p>
 *
 * <p>When the match is open, the upsert is delegated to
 * {@link PredictionRepository#savePrediction(String, String, int, int)}. All
 * blocking work runs on the shared {@link IoExecutor} {@link ExecutorService}.
 */
public class SavePredictionUseCase {

    /** Predictions close this many milliseconds before kickoff (10 minutes). */
    public static final long PREDICTION_LOCK_MILLIS = 10L * 60L * 1000L;

    private final PredictionRepository predictionRepository;
    private final MatchDao matchDao;
    private final ExecutorService executor;

    @Inject
    public SavePredictionUseCase(PredictionRepository predictionRepository,
                                 MatchDao matchDao,
                                 @IoExecutor ExecutorService executor) {
        this.predictionRepository = predictionRepository;
        this.matchDao = matchDao;
        this.executor = executor;
    }

    /**
     * Validates the lock threshold and, if the match is open, saves the
     * prediction off the main thread.
     *
     * @param userId    the id of the user
     * @param matchId   the primary-key id of the match
     * @param homeScore the predicted home-team score
     * @param awayScore the predicted away-team score
     * @param callback  receives {@code Result.Success<Prediction>} with the saved
     *                  prediction, or {@code Result.Error} when the prediction is
     *                  closed or persistence fails
     */
    public void execute(String userId, String matchId, int homeScore, int awayScore,
                        Callback<Prediction> callback) {
        executor.execute(() -> callback.onResult(save(userId, matchId, homeScore, awayScore)));
    }

    private Result<Prediction> save(String userId, String matchId, int homeScore, int awayScore) {
        try {
            MatchEntity entity = matchDao.getMatchById(matchId);
            if (entity == null) {
                return new Result.Error(new PredictionException(PredictionError.MATCH_NOT_FOUND));
            }
            if (!isOpenForPrediction(entity.toDomainModel())) {
                return new Result.Error(new PredictionException(PredictionError.PREDICTION_CLOSED));
            }
        } catch (Exception e) {
            return new Result.Error(new PredictionException(PredictionError.PERSISTENCE_ERROR, e));
        }
        return predictionRepository.savePrediction(userId, matchId, homeScore, awayScore);
    }

    /**
     * A match is open for prediction only while it is {@code SCHEDULED} and its
     * kickoff is still more than the lock window away from now.
     */
    private boolean isOpenForPrediction(Match match) {
        if (match == null || match.getStatus() != MatchStatus.SCHEDULED) {
            return false;
        }
        long lockTime = match.getScheduledDateTime() - PREDICTION_LOCK_MILLIS;
        return System.currentTimeMillis() < lockTime;
    }
}
