package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.domain.model.PredictionOutcome;
import com.diyebure.golia.util.Constants;

import java.util.Objects;

/**
 * Use case for calculating points earned from a prediction.
 * 
 * The points calculation is based on:
 * - Whether the prediction matches the actual outcome
 * - The odds for the predicted outcome
 * - Weights applied to different outcome types (draw predictions are worth more)
 * 
 * Points formula:
 * - Incorrect prediction: 0 points
 * - Correct HOME_WIN prediction: min(MAX_POINTS, Math.round(10 * homeOdds))
 * - Correct DRAW prediction: min(MAX_POINTS, Math.round(10 * drawOdds * POINTS_DRAW_WEIGHT))
 * - Correct AWAY_WIN prediction: min(MAX_POINTS, Math.round(10 * awayOdds))
 */
public class CalculatePointsUseCase {

    /**
     * Calculate points earned for a prediction.
     * 
     * @param predicted The user's predicted outcome
     * @param actual The actual outcome of the match
     * @param homeOdds The odds for home team win
     * @param drawOdds The odds for draw
     * @param awayOdds The odds for away team win
     * @return Points earned (0 if prediction was incorrect, calculated value if correct)
     */
    public int execute(PredictionOutcome predicted, PredictionOutcome actual,
                       double homeOdds, double drawOdds, double awayOdds) {
        
        // If prediction doesn't match actual outcome, return 0
        if (!Objects.equals(predicted, actual)) {
            return 0;
        }
        
        // Calculate points based on the predicted outcome
        switch (predicted) {
            case HOME_WIN:
                return calculateHomeWinPoints(homeOdds);
            case DRAW:
                return calculateDrawPoints(drawOdds);
            case AWAY_WIN:
                return calculateAwayWinPoints(awayOdds);
            default:
                return 0;
        }
    }

    /**
     * Calculate points for a correct home win prediction.
     * 
     * @param homeOdds The odds for home team win
     * @return Points capped at MAX_POINTS
     */
    private int calculateHomeWinPoints(double homeOdds) {
        if (homeOdds <= 0) {
            return 0;
        }
        double rawPoints = 10 * homeOdds * Constants.POINTS_HOME_WIN_WEIGHT;
        return (int) Math.round(Math.min(Constants.MAX_POINTS, rawPoints));
    }

    /**
     * Calculate points for a correct draw prediction.
     * Draw predictions are weighted higher (POINTS_DRAW_WEIGHT = 1.5) since
     * correctly predicting a draw is more difficult.
     * 
     * @param drawOdds The odds for draw
     * @return Points capped at MAX_POINTS
     */
    private int calculateDrawPoints(double drawOdds) {
        if (drawOdds <= 0) {
            return 0;
        }
        double rawPoints = 10 * drawOdds * Constants.POINTS_DRAW_WEIGHT;
        return (int) Math.round(Math.min(Constants.MAX_POINTS, rawPoints));
    }

    /**
     * Calculate points for a correct away win prediction.
     * 
     * @param awayOdds The odds for away team win
     * @return Points capped at MAX_POINTS
     */
    private int calculateAwayWinPoints(double awayOdds) {
        if (awayOdds <= 0) {
            return 0;
        }
        double rawPoints = 10 * awayOdds * Constants.POINTS_AWAY_WIN_WEIGHT;
        return (int) Math.round(Math.min(Constants.MAX_POINTS, rawPoints));
    }
}