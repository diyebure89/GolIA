package com.diyebure.golia.prediction;

import com.diyebure.golia.common.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Logica de pronosticos.
 *
 * <p>El {@code savePrediction} es un <em>upsert</em> por par {@code (userId,
 * matchId)}: si ya existe se actualiza en sitio (conservando su {@code id} y
 * {@code created_at}); si no, se crea uno nuevo sin resolver
 * ({@code isCorrect = null}, {@code pointsEarned = 0}). El resultado
 * ({@code predicted_outcome}) se deriva del marcador, igual que en la app.</p>
 */
@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;

    public PredictionService(PredictionRepository predictionRepository) {
        this.predictionRepository = predictionRepository;
    }

    @Transactional
    public PredictionEntity savePrediction(String userId, String matchId,
                                           int homeScore, int awayScore) {
        String outcome = deriveOutcome(homeScore, awayScore);

        PredictionEntity existing = predictionRepository
                .findByUserIdAndMatchId(userId, matchId)
                .orElse(null);

        if (existing != null) {
            existing.setPredictedHomeScore(homeScore);
            existing.setPredictedAwayScore(awayScore);
            existing.setPredictedOutcome(outcome);
            // Un cambio de pronostico vuelve a estado sin resolver.
            existing.setIsCorrect(null);
            existing.setPointsEarned(0);
            existing.setFinalizedAt(null);
            return predictionRepository.save(existing);
        }

        PredictionEntity created = new PredictionEntity(
                UUID.randomUUID().toString(),
                userId,
                matchId,
                outcome,
                homeScore,
                awayScore,
                0,
                null,
                System.currentTimeMillis(),
                null);

        try {
            return predictionRepository.save(created);
        } catch (DataIntegrityViolationException e) {
            // Carrera contra el indice unico (user_id, match_id): reintenta como update.
            PredictionEntity race = predictionRepository
                    .findByUserIdAndMatchId(userId, matchId)
                    .orElseThrow(() -> ApiException.badRequest("No se pudo guardar el pronostico."));
            race.setPredictedHomeScore(homeScore);
            race.setPredictedAwayScore(awayScore);
            race.setPredictedOutcome(outcome);
            race.setIsCorrect(null);
            race.setPointsEarned(0);
            race.setFinalizedAt(null);
            return predictionRepository.save(race);
        }
    }

    @Transactional(readOnly = true)
    public List<PredictionEntity> getUserPredictions(String userId) {
        return predictionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<PredictionEntity> getPredictionsForMatch(String matchId) {
        return predictionRepository.findByMatchId(matchId);
    }

    /** HOME_WIN / AWAY_WIN / DRAW, coherente con el enum de la app. */
    private static String deriveOutcome(int homeScore, int awayScore) {
        if (homeScore > awayScore) {
            return "HOME_WIN";
        }
        if (homeScore < awayScore) {
            return "AWAY_WIN";
        }
        return "DRAW";
    }
}
