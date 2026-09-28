package com.diyebure.golia.data.repository;

import android.database.sqlite.SQLiteConstraintException;

import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.dao.PredictionDao;
import com.diyebure.golia.data.local.entity.PredictionEntity;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.PredictionError;
import com.diyebure.golia.domain.error.PredictionException;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.model.PredictionOutcome;
import com.diyebure.golia.domain.model.Prediction_Scorer;
import com.diyebure.golia.domain.repository.PredictionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Local implementation of {@link PredictionRepository} backed by Room (SQLite).
 *
 * <p>Encapsulates the upsert of a user's prediction keyed by
 * {@code (userId, matchId)} and the resolution of finished matches, following
 * the synchronous {@code Result}-returning style of
 * {@link LocalAuthRepositoryImpl}. Callers (use cases) dispatch these calls onto
 * the shared {@code @IoExecutor}.</p>
 *
 * <p>Design note: the domain {@link Prediction} model stores its identity fields
 * as {@link UUID}, but the persisted {@link PredictionEntity} keeps them as
 * {@code String} columns. To avoid losing values through fallible
 * {@code UUID.fromString} conversions, this repository operates on the entity
 * with the incoming {@code String} ids directly and only uses
 * {@link PredictionEntity#toDomainModel()} to expose the domain model.</p>
 */
@Singleton
public class PredictionRepositoryImpl implements PredictionRepository {

    private final PredictionDao predictionDao;
    private final MatchDao matchDao;
    private final Prediction_Scorer scorer;

    @Inject
    public PredictionRepositoryImpl(PredictionDao predictionDao,
                                    MatchDao matchDao,
                                    Prediction_Scorer scorer) {
        this.predictionDao = predictionDao;
        this.matchDao = matchDao;
        this.scorer = scorer;
    }

    @Override
    public Result<Prediction> getUserPrediction(String userId, String matchId) {
        try {
            PredictionEntity entity = predictionDao.getUserPredictionForMatch(userId, matchId);
            if (entity == null) {
                return error(PredictionError.MATCH_NOT_FOUND);
            }
            return new Result.Success<>(entity.toDomainModel());
        } catch (Exception e) {
            return persistenceError(e);
        }
    }

    @Override
    public Result<Prediction> savePrediction(String userId, String matchId,
                                             int homeScore, int awayScore) {
        try {
            PredictionEntity entity = buildUpsertEntity(userId, matchId, homeScore, awayScore);
            try {
                // REPLACE by primary key: when an existing prediction is reused we
                // keep its id/created_at, so this respects the unique
                // (user_id, match_id) index without creating duplicates.
                predictionDao.insertPrediction(entity);
            } catch (SQLiteConstraintException conflict) {
                // Another row already owns the (user_id, match_id) pair under a
                // different primary key. Resolve the upsert as an in-place update
                // of that existing row.
                PredictionEntity existing =
                        predictionDao.getUserPredictionForMatch(userId, matchId);
                if (existing == null) {
                    return persistenceError(conflict);
                }
                entity.setId(existing.getId());
                entity.setCreatedAt(existing.getCreatedAt());
                predictionDao.updatePrediction(entity);
            }
            return new Result.Success<>(entity.toDomainModel());
        } catch (Exception e) {
            return persistenceError(e);
        }
    }

    @Override
    public Result<List<Prediction>> getUserPredictions(String userId) {
        try {
            List<PredictionEntity> entities = predictionDao.getUserPredictionsSorted(userId);
            return new Result.Success<>(toDomainList(entities));
        } catch (Exception e) {
            return persistenceErrorList(e);
        }
    }

    @Override
    public Result<List<Prediction>> getPredictionsForMatch(String matchId) {
        try {
            List<PredictionEntity> entities = predictionDao.getPredictionsByMatch(matchId);
            return new Result.Success<>(toDomainList(entities));
        } catch (Exception e) {
            return persistenceErrorList(e);
        }
    }

    @Override
    public Result<Void> resolveForMatch(Match finishedMatch) {
        try {
            if (finishedMatch == null
                    || finishedMatch.getStatus() != MatchStatus.FINISHED
                    || finishedMatch.getHomeScore() == null
                    || finishedMatch.getAwayScore() == null
                    || finishedMatch.getId() == null) {
                // Nothing to resolve; a no-op is still a success.
                return new Result.Success<>(null);
            }

            String matchId = finishedMatch.getId().toString();
            int actualHome = finishedMatch.getHomeScore();
            int actualAway = finishedMatch.getAwayScore();

            List<PredictionEntity> predictions = predictionDao.getPredictionsByMatch(matchId);
            long finalizedAt = System.currentTimeMillis();

            for (PredictionEntity prediction : predictions) {
                // Idempotency: only touch still-pending predictions.
                if (prediction.getIsCorrect() != null) {
                    continue;
                }
                if (prediction.getPredictedHomeScore() == null
                        || prediction.getPredictedAwayScore() == null) {
                    continue;
                }

                int predictedHome = prediction.getPredictedHomeScore();
                int predictedAway = prediction.getPredictedAwayScore();
                int points = scorer.score(predictedHome, predictedAway, actualHome, actualAway);

                prediction.setPointsEarned(points);
                prediction.setIsCorrect(scorer.isCorrect(points));
                prediction.setFinalizedAt(finalizedAt);
                predictionDao.updatePrediction(prediction);
            }

            return new Result.Success<>(null);
        } catch (Exception e) {
            return new Result.Error(new PredictionException(PredictionError.PERSISTENCE_ERROR, e));
        }
    }

    // ==================== Helpers ====================

    /**
     * Build the entity to persist for an upsert. Normalizes identity so that
     * {@code user_id} equals {@code userId} and {@code match_id} equals
     * {@code matchId}. Reuses the id and {@code created_at} of any existing
     * prediction for the pair; otherwise generates a fresh UUID string id. The
     * prediction starts unresolved ({@code isCorrect = null},
     * {@code pointsEarned = 0}).
     */
    private PredictionEntity buildUpsertEntity(String userId, String matchId,
                                               int homeScore, int awayScore) {
        PredictionEntity existing = predictionDao.getUserPredictionForMatch(userId, matchId);

        String id;
        long createdAt;
        if (existing != null) {
            id = existing.getId();
            createdAt = existing.getCreatedAt();
        } else {
            id = UUID.randomUUID().toString();
            createdAt = System.currentTimeMillis();
        }

        PredictionOutcome outcome = scorer.deriveOutcome(homeScore, awayScore);

        return new PredictionEntity(
                id,
                userId,
                matchId,
                outcome != null ? outcome.name() : null,
                homeScore,
                awayScore,
                0,          // pointsEarned
                null,       // isCorrect (pending)
                createdAt,
                null        // finalizedAt (pending)
        );
    }

    private static List<Prediction> toDomainList(List<PredictionEntity> entities) {
        List<Prediction> result = new ArrayList<>();
        if (entities != null) {
            for (PredictionEntity entity : entities) {
                result.add(entity.toDomainModel());
            }
        }
        return result;
    }

    private static Result<Prediction> error(PredictionError predictionError) {
        return new Result.Error(new PredictionException(predictionError));
    }

    private static Result<Prediction> persistenceError(Exception cause) {
        return new Result.Error(new PredictionException(PredictionError.PERSISTENCE_ERROR, cause));
    }

    private static Result<List<Prediction>> persistenceErrorList(Exception cause) {
        return new Result.Error(new PredictionException(PredictionError.PERSISTENCE_ERROR, cause));
    }
}
