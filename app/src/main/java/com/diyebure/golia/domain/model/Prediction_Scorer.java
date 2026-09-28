package com.diyebure.golia.domain.model;

import javax.inject.Inject;

/**
 * Pure domain scorer that implements the {@code Prediction_Scoring} rule for an
 * exact-score prediction (R9, R11).
 *
 * <p>The scorer is intentionally free of any Android dependency so it can be
 * exercised directly from plain JVM unit tests. It is also Hilt-injectable via a
 * public no-arg {@link Inject} constructor.</p>
 *
 * <p>Scoring components (summed against the actual final score):</p>
 * <ul>
 *   <li>+5 when the 1X2 winner ({@link PredictionOutcome}) matches</li>
 *   <li>+2 when the predicted home goals match exactly</li>
 *   <li>+2 when the predicted away goals match exactly</li>
 *   <li>+1 when the goal difference (home - away) matches</li>
 *   <li>+9 when the whole score is exact (both home and away)</li>
 * </ul>
 * The maximum achievable score is 19 (5 + 2 + 2 + 1 + 9).
 */
public class Prediction_Scorer {

    /** Points awarded for correctly predicting the 1X2 winner. */
    private static final int POINTS_OUTCOME = 5;
    /** Points awarded for exactly predicting the home team's goals. */
    private static final int POINTS_HOME_EXACT = 2;
    /** Points awarded for exactly predicting the away team's goals. */
    private static final int POINTS_AWAY_EXACT = 2;
    /** Points awarded for correctly predicting the goal difference. */
    private static final int POINTS_DIFFERENCE = 1;
    /** Additional points awarded when the full score is exact. */
    private static final int POINTS_EXACT_SCORE = 9;

    @Inject
    public Prediction_Scorer() {
        // No dependencies: pure domain component.
    }

    /**
     * Compute the points earned by a prediction against the actual final score.
     *
     * @param predictedHome predicted goals for the home team
     * @param predictedAway predicted goals for the away team
     * @param actualHome    actual goals for the home team
     * @param actualAway    actual goals for the away team
     * @return the total score in the range {@code 0..19}
     */
    public int score(int predictedHome, int predictedAway, int actualHome, int actualAway) {
        int total = 0;

        if (deriveOutcome(predictedHome, predictedAway) == deriveOutcome(actualHome, actualAway)) {
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

    /**
     * A prediction is considered correct when it earned at least one point.
     *
     * @param score the score produced by {@link #score(int, int, int, int)}
     * @return {@code true} when {@code score >= 1}
     */
    public boolean isCorrect(int score) {
        return score >= 1;
    }

    /**
     * Derive the 1X2 outcome from a score.
     *
     * @param home home team goals
     * @param away away team goals
     * @return {@link PredictionOutcome#HOME_WIN} when {@code home > away},
     *         {@link PredictionOutcome#DRAW} when equal,
     *         {@link PredictionOutcome#AWAY_WIN} when {@code home < away}
     */
    public PredictionOutcome deriveOutcome(int home, int away) {
        if (home > away) {
            return PredictionOutcome.HOME_WIN;
        } else if (home == away) {
            return PredictionOutcome.DRAW;
        } else {
            return PredictionOutcome.AWAY_WIN;
        }
    }
}
