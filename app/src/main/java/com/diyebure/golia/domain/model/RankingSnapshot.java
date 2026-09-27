package com.diyebure.golia.domain.model;

/**
 * Immutable value object with the ranking metrics shown in the "Historial" section
 * of {@code Pantalla_Perfil} (and in the Inicio welcome card via the same source).
 *
 * <p>Invariants enforced in the constructor (R11.2):
 * <ul>
 *   <li>{@code predictionsMade >= 0}</li>
 *   <li>{@code accuracyPercentage} in the closed range {@code [0, 100]}</li>
 *   <li>{@code totalPoints >= 0}</li>
 *   <li>{@code rankingPosition >= 1}</li>
 * </ul>
 *
 * <p>All fields are {@code final} and exposed only through getters, so instances are
 * immutable and safe to share across the app-scoped {@code RankingDataSource} singleton.
 */
public final class RankingSnapshot {

    private final int predictionsMade;
    private final float accuracyPercentage;
    private final int totalPoints;
    private final int rankingPosition;

    /**
     * @param predictionsMade    number of predictions made; must be {@code >= 0}
     * @param accuracyPercentage accuracy percentage; must be in {@code [0, 100]}
     * @param totalPoints        total points; must be {@code >= 0}
     * @param rankingPosition    ranking position; must be {@code >= 1}
     * @throws IllegalArgumentException when any invariant is violated
     */
    public RankingSnapshot(int predictionsMade, float accuracyPercentage,
                           int totalPoints, int rankingPosition) {
        if (predictionsMade < 0) {
            throw new IllegalArgumentException("predictionsMade must be >= 0");
        }
        if (accuracyPercentage < 0f || accuracyPercentage > 100f) {
            throw new IllegalArgumentException("accuracyPercentage must be in [0, 100]");
        }
        if (totalPoints < 0) {
            throw new IllegalArgumentException("totalPoints must be >= 0");
        }
        if (rankingPosition < 1) {
            throw new IllegalArgumentException("rankingPosition must be >= 1");
        }
        this.predictionsMade = predictionsMade;
        this.accuracyPercentage = accuracyPercentage;
        this.totalPoints = totalPoints;
        this.rankingPosition = rankingPosition;
    }

    public int getPredictionsMade() {
        return predictionsMade;
    }

    public float getAccuracyPercentage() {
        return accuracyPercentage;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public int getRankingPosition() {
        return rankingPosition;
    }
}
