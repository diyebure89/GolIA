package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.PredictionError;
import com.diyebure.golia.domain.error.PredictionException;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.model.PredictionWithMatch;
import com.diyebure.golia.domain.repository.PredictionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Reads all predictions made by a user for history display, resolving each one
 * against its {@link Match} from the local cache (R8.2).
 *
 * <p>The predictions are read through
 * {@link PredictionRepository#getUserPredictions(String)} and each is paired
 * with the match it references, looked up by {@code match_id} via
 * {@link MatchDao#getMatchById(String)}. When a match is no longer present in
 * the cache the prediction is still returned with a {@code null} match so the
 * UI can show a "match unavailable" placeholder (R8.3); the history never fails
 * as a whole because of a single missing match.</p>
 *
 * <p>All blocking work runs on the shared {@link IoExecutor}
 * {@link ExecutorService}; no network request is made.</p>
 */
public class GetUserPredictionsUseCase {

    private final PredictionRepository predictionRepository;
    private final MatchDao matchDao;
    private final ExecutorService executor;

    @Inject
    public GetUserPredictionsUseCase(PredictionRepository predictionRepository,
                                     MatchDao matchDao,
                                     @IoExecutor ExecutorService executor) {
        this.predictionRepository = predictionRepository;
        this.matchDao = matchDao;
        this.executor = executor;
    }

    /**
     * Delivers the user's predictions, each paired with its resolved match, off
     * the main thread.
     *
     * @param userId   the id of the user
     * @param callback receives {@code Result.Success<List<PredictionWithMatch>>}
     *                 with the combined models (possibly empty), or
     *                 {@code Result.Error} on failure
     */
    public void execute(String userId, Callback<List<PredictionWithMatch>> callback) {
        executor.execute(() -> callback.onResult(load(userId)));
    }

    private Result<List<PredictionWithMatch>> load(String userId) {
        Result<List<Prediction>> predictionsResult = predictionRepository.getUserPredictions(userId);
        if (predictionsResult.isError()) {
            return new Result.Error(predictionsResult.getErrorOrNull());
        }

        try {
            List<Prediction> predictions = predictionsResult.getOrNull();
            List<PredictionWithMatch> combined = new ArrayList<>();
            if (predictions != null) {
                for (Prediction prediction : predictions) {
                    combined.add(new PredictionWithMatch(prediction, resolveMatch(prediction)));
                }
            }
            return new Result.Success<>(combined);
        } catch (Exception e) {
            return new Result.Error(new PredictionException(PredictionError.PERSISTENCE_ERROR, e));
        }
    }

    private Match resolveMatch(Prediction prediction) {
        if (prediction == null) {
            return null;
        }
        // Priorizamos la PK ORIGINAL del partido (rawMatchId) preservada desde Room,
        // que es la identidad real de búsqueda de MatchDao.getMatchById. Solo caemos
        // a matchId.toString() cuando no hay rawMatchId (p. ej. pronósticos en
        // memoria que no provienen de Room). De este modo no se pierde la identidad
        // para PKs no-UUID y no se lanza NPE cuando matchId es null pero hay rawMatchId.
        String matchIdString = prediction.getRawMatchId() != null
                ? prediction.getRawMatchId()
                : (prediction.getMatchId() != null ? prediction.getMatchId().toString() : null);
        if (matchIdString == null) {
            return null;
        }
        MatchEntity entity = matchDao.getMatchById(matchIdString);
        return entity != null ? entity.toDomainModel() : null;
    }
}
