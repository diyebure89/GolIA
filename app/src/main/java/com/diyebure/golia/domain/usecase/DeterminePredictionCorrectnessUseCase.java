package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.model.PredictionOutcome;

import java.util.Objects;

/**
 * Use case for determining if a prediction is correct based on match results.
 * 
 * The correctness is determined by comparing the predicted outcome with
 * the actual match outcome (derived from homeScore and awayScore).
 * 
 * Returns:
 * - true if prediction matches actual outcome
 * - false if prediction does not match actual outcome
 * - null if match is not yet finished (cannot determine correctness)
 */
public class DeterminePredictionCorrectnessUseCase {

    /**
     * Determine if a prediction is correct.
     * 
     * @param predicted The user's predicted outcome
     * @param match The match with actual results
     * @return Boolean: true if correct, false if incorrect, null if match not finished
     */
    public Boolean execute(PredictionOutcome predicted, Match match) {
        if (match == null) {
            return null;
        }
        
        // If match is not finished, we cannot determine correctness yet
        if (!isMatchFinalized(match)) {
            return null;
        }
        
        PredictionOutcome actualOutcome = determineActualOutcome(match);
        return Objects.equals(predicted, actualOutcome);
    }

    /**
     * Check if a match has been finalized (has a result).
     * 
     * @param match The match to check
     * @return true if match is finished, postponed, or cancelled
     */
    public boolean isMatchFinalized(Match match) {
        if (match == null || match.getStatus() == null) {
            return false;
        }
        
        MatchStatus status = match.getStatus();
        return status == MatchStatus.FINISHED || 
               status == MatchStatus.POSTPONED || 
               status == MatchStatus.CANCELLED;
    }

    /**
     * Determine the actual outcome of a match based on scores.
     * 
     * @param match The match with final scores
     * @return The actual outcome (HOME_WIN, DRAW, or AWAY_WIN)
     * @throws IllegalStateException if match is not finalized or has no scores
     */
    public PredictionOutcome determineActualOutcome(Match match) {
        if (match == null) {
            throw new IllegalStateException("Match cannot be null");
        }
        
        if (!isMatchFinalized(match)) {
            throw new IllegalStateException("Match is not finalized, cannot determine outcome");
        }
        
        Integer homeScore = match.getHomeScore();
        Integer awayScore = match.getAwayScore();
        
        if (homeScore == null || awayScore == null) {
            throw new IllegalStateException("Match scores are not available");
        }
        
        if (homeScore > awayScore) {
            return PredictionOutcome.HOME_WIN;
        } else if (homeScore == awayScore) {
            return PredictionOutcome.DRAW;
        } else {
            return PredictionOutcome.AWAY_WIN;
        }
    }

    /**
     * Check if a prediction is valid (can be submitted).
     * A prediction is valid if:
     * - The match exists and is scheduled for the future
     * - The prediction is made before the match starts
     * 
     * @param predicted The predicted outcome
     * @param match The match being predicted
     * @return true if the prediction is valid
     */
    public boolean isPredictionValid(PredictionOutcome predicted, Match match) {
        if (predicted == null || match == null) {
            return false;
        }
        
        // Check if match is in the future (can still predict)
        return match.getScheduledDateTime() > System.currentTimeMillis() &&
               match.getStatus() == MatchStatus.SCHEDULED;
    }
}