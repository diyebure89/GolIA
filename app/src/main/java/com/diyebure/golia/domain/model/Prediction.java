package com.diyebure.golia.domain.model;

import java.util.UUID;

/**
 * Domain model representing a user's prediction for a football match.
 */
public class Prediction {
    private UUID id;
    private UUID userId;
    private UUID matchId;
    private PredictionOutcome predictedOutcome;
    private Integer predictedHomeScore;
    private Integer predictedAwayScore;
    private int pointsEarned;
    private Boolean isCorrect;
    private long createdAt;
    private Long finalizedAt;

    public Prediction() {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
    }

    public Prediction(UUID id, UUID userId, UUID matchId, PredictionOutcome predictedOutcome,
                      Integer predictedHomeScore, Integer predictedAwayScore, int pointsEarned,
                      Boolean isCorrect, long createdAt, Long finalizedAt) {
        this.id = id;
        this.userId = userId;
        this.matchId = matchId;
        this.predictedOutcome = predictedOutcome;
        this.predictedHomeScore = predictedHomeScore;
        this.predictedAwayScore = predictedAwayScore;
        this.pointsEarned = pointsEarned;
        this.isCorrect = isCorrect;
        this.createdAt = createdAt;
        this.finalizedAt = finalizedAt;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getMatchId() { return matchId; }
    public PredictionOutcome getPredictedOutcome() { return predictedOutcome; }
    public Integer getPredictedHomeScore() { return predictedHomeScore; }
    public Integer getPredictedAwayScore() { return predictedAwayScore; }
    public int getPointsEarned() { return pointsEarned; }
    public Boolean getIsCorrect() { return isCorrect; }
    public long getCreatedAt() { return createdAt; }
    public Long getFinalizedAt() { return finalizedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public void setPredictedOutcome(PredictionOutcome predictedOutcome) { this.predictedOutcome = predictedOutcome; }
    public void setPredictedHomeScore(Integer predictedHomeScore) { this.predictedHomeScore = predictedHomeScore; }
    public void setPredictedAwayScore(Integer predictedAwayScore) { this.predictedAwayScore = predictedAwayScore; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setFinalizedAt(Long finalizedAt) { this.finalizedAt = finalizedAt; }

    /**
     * Check if the prediction is still pending (match not finished).
     */
    public boolean isPending() {
        return isCorrect == null;
    }

    /**
     * Check if the prediction has been finalized (match finished).
     */
    public boolean isFinalized() {
        return isCorrect != null;
    }

    /**
     * Get a string representation of the predicted outcome.
     */
    public String getPredictedOutcomeString() {
        if (predictedOutcome == null) return "Not predicted";
        switch (predictedOutcome) {
            case HOME_WIN:
                return "Home Win";
            case DRAW:
                return "Draw";
            case AWAY_WIN:
                return "Away Win";
            default:
                return "Unknown";
        }
    }
}