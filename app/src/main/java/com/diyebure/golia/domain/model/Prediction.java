package com.diyebure.golia.domain.model;

import java.util.UUID;

/**
 * Domain model representing a user's prediction for a football match.
 */
public class Prediction {
    private UUID id;
    private UUID userId;
    private UUID matchId;
    /**
     * Identidad ORIGINAL del partido tal y como está almacenada en la columna
     * {@code match_id} de la tabla {@code predictions} (igual a
     * {@code MatchEntity.id}, de tipo String).
     *
     * <p>El campo {@link #matchId} es un {@code UUID} y, por diseño, no puede
     * representar una PK que no sea un UUID canónico (p. ej. "match-1" o
     * "1035048" de filas persistidas). Cuando el parseo a UUID falla en
     * {@code PredictionEntity.toDomainModel()}, {@code matchId} queda {@code null}
     * y se pierde la identidad de búsqueda del partido.</p>
     *
     * <p>Por eso conservamos aquí la PK exacta: {@code rawMatchId} porta la
     * identidad de búsqueda sin depender del parseo UUID, igual que
     * {@code Match.rawId}, de modo que {@code MatchDao.getMatchById} localice
     * siempre la fila del partido al resolver el historial. Puede ser
     * {@code null} para pronósticos construidos en memoria que aún no provienen
     * de Room (en ese caso se cae al {@code matchId.toString()} como antes).</p>
     */
    private String rawMatchId;
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
    /** Devuelve la PK original (String) del partido preservada desde Room, o null si no aplica. */
    public String getRawMatchId() { return rawMatchId; }
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
    /** Fija la PK original (String) del partido preservada desde Room. */
    public void setRawMatchId(String rawMatchId) { this.rawMatchId = rawMatchId; }
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