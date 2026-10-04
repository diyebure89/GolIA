package com.diyebure.golia.prediction;

import org.springframework.stereotype.Component;

/**
 * Replica exacta de la regla de puntuacion de la app Android
 * ({@code Prediction_Scorer}).
 *
 * <p>Componentes (se suman sobre el marcador final real):
 * <ul>
 *   <li>+5 si el resultado 1X2 (ganador) coincide</li>
 *   <li>+2 si los goles locales predichos son exactos</li>
 *   <li>+2 si los goles visitantes predichos son exactos</li>
 *   <li>+1 si la diferencia de goles (local - visitante) coincide</li>
 *   <li>+9 si el marcador completo es exacto (local Y visitante)</li>
 * </ul>
 * Maximo posible: 19 puntos. Correcto = score >= 1.</p>
 */
@Component
public class PredictionScorer {

    private static final int POINTS_OUTCOME     = 5;
    private static final int POINTS_HOME_EXACT  = 2;
    private static final int POINTS_AWAY_EXACT  = 2;
    private static final int POINTS_DIFFERENCE  = 1;
    private static final int POINTS_EXACT_SCORE = 9;

    /**
     * Calcula los puntos ganados por un pronostico frente al marcador final.
     *
     * @param predictedHome goles predichos del equipo local
     * @param predictedAway goles predichos del equipo visitante
     * @param actualHome    goles reales del equipo local
     * @param actualAway    goles reales del equipo visitante
     * @return puntos en el rango 0..19
     */
    public int score(int predictedHome, int predictedAway,
                     int actualHome, int actualAway) {
        int total = 0;

        if (outcome(predictedHome, predictedAway) == outcome(actualHome, actualAway)) {
            total += POINTS_OUTCOME;
        }
        if (predictedHome == actualHome) {
            total += POINTS_HOME_EXACT;
        }
        if (predictedAway == actualAway) {
            total += POINTS_AWAY_EXACT;
        }
        if ((predictedHome - predictedAway) == (actualHome - actualAway)) {
            total += POINTS_DIFFERENCE;
        }
        if (predictedHome == actualHome && predictedAway == actualAway) {
            total += POINTS_EXACT_SCORE;
        }

        return total;
    }

    /** Un pronostico es correcto cuando gano al menos 1 punto. */
    public boolean isCorrect(int score) {
        return score >= 1;
    }

    /** Resultado 1X2: 1=local gana, X=empate, 2=visitante gana. */
    private int outcome(int home, int away) {
        return Integer.compare(home, away); // 1, 0, -1
    }
}
