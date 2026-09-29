package com.diyebure.golia.domain.model;

/**
 * Immutable value object that unifies a real user prediction and a seed result as
 * the single input to the {@code Ranking_Calculator}.
 *
 * <p>{@code actualHome} and {@code actualAway} are nullable: a {@link MatchStatus#LIVE}
 * match may not yet have a score, and a match that has not started will carry no
 * actual result. The {@code timestamp} is the {@code finalizedAt} for a real
 * prediction or the resolved timestamp of a seed result (epoch millis).</p>
 */
public final class ScoredPrediction {

    private final int predictedHome;
    private final int predictedAway;
    private final Integer actualHome;
    private final Integer actualAway;
    private final MatchStatus status;
    private final long timestamp;

    /**
     * @param predictedHome predicted goals for the home team
     * @param predictedAway predicted goals for the away team
     * @param actualHome    actual home goals, or {@code null} when unavailable
     * @param actualAway    actual away goals, or {@code null} when unavailable
     * @param status        the match status backing this prediction
     * @param timestamp     epoch millis used for period filtering and streak ordering
     */
    public ScoredPrediction(int predictedHome, int predictedAway,
                            Integer actualHome, Integer actualAway,
                            MatchStatus status, long timestamp) {
        this.predictedHome = predictedHome;
        this.predictedAway = predictedAway;
        this.actualHome = actualHome;
        this.actualAway = actualAway;
        this.status = status;
        this.timestamp = timestamp;
    }

    public int getPredictedHome() {
        return predictedHome;
    }

    public int getPredictedAway() {
        return predictedAway;
    }

    public Integer getActualHome() {
        return actualHome;
    }

    public Integer getActualAway() {
        return actualAway;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
