package com.diyebure.golia.presentation.ui.match;

/**
 * Carries the existing prediction scores to preload into the exact-score fields
 * (R5.2). Emitted only when the user already has a prediction for the match.
 */
public final class PronosticoPrefill {

    public final int homeScore;
    public final int awayScore;

    public PronosticoPrefill(int homeScore, int awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
}
