package com.diyebure.golia.domain.model;

/**
 * Immutable domain pairing of a {@link Prediction} with the {@link Match} it
 * refers to, resolved by {@code match_id} from the cache (R8.2).
 *
 * <p>The {@code match} may be {@code null} when the referenced match is no
 * longer available in the local cache; in that case the presentation layer
 * renders a "match unavailable" placeholder (R8.3). The prediction itself is
 * never {@code null}.</p>
 */
public class PredictionWithMatch {

    private final Prediction prediction;
    private final Match match;

    public PredictionWithMatch(Prediction prediction, Match match) {
        this.prediction = prediction;
        this.match = match;
    }

    /** The prediction (never {@code null}). */
    public Prediction getPrediction() {
        return prediction;
    }

    /**
     * The resolved match for this prediction, or {@code null} when the match is
     * not available in the cache.
     */
    public Match getMatch() {
        return match;
    }

    /** Whether the referenced match was resolved from the cache. */
    public boolean hasMatch() {
        return match != null;
    }
}
