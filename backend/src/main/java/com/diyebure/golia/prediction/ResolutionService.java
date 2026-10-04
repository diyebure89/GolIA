package com.diyebure.golia.prediction;

import com.diyebure.golia.prediction.dto.MatchResolveDtos.ResolveResponse;
import com.diyebure.golia.user.UserEntity;
import com.diyebure.golia.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resuelve los pronosticos pendientes de un partido finalizado y actualiza los
 * contadores de ranking de cada usuario afectado ({@code total_points},
 * {@code predictions_made}, {@code predictions_correct}) en MySQL.
 *
 * <h3>Flujo</h3>
 * <ol>
 *   <li>Carga todos los pronosticos del partido ({@code match_id}).</li>
 *   <li>Ignora los que ya estan resueltos ({@code is_correct != null}) — idempotente.</li>
 *   <li>Para cada pronostico pendiente calcula puntos con {@link PredictionScorer}
 *       (misma regla exacta que la app: outcome=5, home=2, away=2, diff=1,
 *       exacto=9; maximo=19).</li>
 *   <li>Persiste {@code points_earned}, {@code is_correct} y {@code finalized_at}
 *       en la fila del pronostico.</li>
 *   <li>Acumula los deltas por usuario y aplica una sola actualizacion por
 *       usuario al final (una lectura + una escritura, sin N+1).</li>
 * </ol>
 *
 * <p>Todo ocurre en una unica transaccion: si algo falla no quedan datos a medio
 * resolver.</p>
 */
@Service
public class ResolutionService {

    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;
    private final PredictionScorer scorer;

    public ResolutionService(PredictionRepository predictionRepository,
                             UserRepository userRepository,
                             PredictionScorer scorer) {
        this.predictionRepository = predictionRepository;
        this.userRepository = userRepository;
        this.scorer = scorer;
    }

    /**
     * Resuelve todos los pronosticos pendientes del partido {@code matchId}
     * usando el marcador final ({@code homeScore}, {@code awayScore}).
     *
     * @return resumen con cuantos se resolvieron, cuantos acertaron y cuantos
     *         ya estaban resueltos (idempotencia)
     */
    @Transactional
    public ResolveResponse resolve(String matchId, int homeScore, int awayScore) {
        List<PredictionEntity> predictions = predictionRepository.findByMatchId(matchId);

        // Contadores del resumen
        int resolvedCount = 0;
        int correctCount  = 0;
        int alreadyDone   = 0;

        // Deltas por usuario: [puntosGanados, predictionsMade, predictionsCorrect]
        Map<String, int[]> deltas = new HashMap<>();

        long now = System.currentTimeMillis();

        for (PredictionEntity prediction : predictions) {
            if (prediction.getIsCorrect() != null) {
                // Ya resuelto en una llamada anterior — idempotente, se ignora.
                alreadyDone++;
                continue;
            }

            Integer predHome = prediction.getPredictedHomeScore();
            Integer predAway = prediction.getPredictedAwayScore();
            if (predHome == null || predAway == null) {
                // Sin marcador predicho (no deberia ocurrir, pero se ignora seguro).
                continue;
            }

            int points  = scorer.score(predHome, predAway, homeScore, awayScore);
            boolean hit = scorer.isCorrect(points);

            // Actualizar la fila del pronostico.
            prediction.setPointsEarned(points);
            prediction.setIsCorrect(hit);
            prediction.setFinalizedAt(now);
            predictionRepository.save(prediction);

            // Acumular delta para el usuario.
            String userId = prediction.getUserId();
            int[] d = deltas.computeIfAbsent(userId, k -> new int[3]);
            d[0] += points; // puntos ganados
            d[1] += 1;      // predictions_made
            d[2] += hit ? 1 : 0; // predictions_correct

            resolvedCount++;
            if (hit) correctCount++;
        }

        // Aplicar los deltas a cada usuario afectado (una sola lectura+escritura por usuario).
        applyUserDeltas(deltas);

        return new ResolveResponse(matchId, resolvedCount, correctCount, alreadyDone);
    }

    /**
     * Actualiza {@code total_points}, {@code predictions_made} y
     * {@code predictions_correct} de cada usuario afectado, sumando los deltas
     * calculados durante la resolucion.
     *
     * <p>Los usuarios que no existan en la base se ignoran silenciosamente (no
     * deberia ocurrir en produccion).</p>
     */
    private void applyUserDeltas(Map<String, int[]> deltas) {
        if (deltas.isEmpty()) {
            return;
        }

        // Carga todos los usuarios afectados en un solo query.
        List<UserEntity> users = userRepository.findAllById(deltas.keySet());

        for (UserEntity user : users) {
            int[] d = deltas.get(user.getId());
            if (d == null) {
                continue;
            }
            user.setTotalPoints(user.getTotalPoints() + d[0]);
            user.setPredictionsMade(user.getPredictionsMade() + d[1]);
            user.setPredictionsCorrect(user.getPredictionsCorrect() + d[2]);
        }

        // Persiste todos los cambios en un batch.
        userRepository.saveAll(users);
    }
}
