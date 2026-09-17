package com.diyebure.golia.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.model.PredictionOutcome;

/**
 * Room entity for storing prediction data locally.
 */
@Entity(tableName = "predictions")
public class PredictionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "match_id")
    private String matchId;

    @ColumnInfo(name = "predicted_outcome")
    private String predictedOutcome;

    @ColumnInfo(name = "predicted_home_score")
    private Integer predictedHomeScore;

    @ColumnInfo(name = "predicted_away_score")
    private Integer predictedAwayScore;

    @ColumnInfo(name = "points_earned")
    private int pointsEarned;

    @ColumnInfo(name = "is_correct")
    private Boolean isCorrect;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "finalized_at")
    private Long finalizedAt;

    public PredictionEntity() {
        this.id = "";
    }

    public PredictionEntity(@NonNull String id, String userId, String matchId,
                            String predictedOutcome, Integer predictedHomeScore,
                            Integer predictedAwayScore, int pointsEarned, Boolean isCorrect,
                            long createdAt, Long finalizedAt) {
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
    @NonNull
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMatchId() { return matchId; }
    public String getPredictedOutcome() { return predictedOutcome; }
    public Integer getPredictedHomeScore() { return predictedHomeScore; }
    public Integer getPredictedAwayScore() { return predictedAwayScore; }
    public int getPointsEarned() { return pointsEarned; }
    public Boolean getIsCorrect() { return isCorrect; }
    public long getCreatedAt() { return createdAt; }
    public Long getFinalizedAt() { return finalizedAt; }

    // Setters
    public void setId(@NonNull String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public void setPredictedOutcome(String predictedOutcome) { this.predictedOutcome = predictedOutcome; }
    public void setPredictedHomeScore(Integer predictedHomeScore) { this.predictedHomeScore = predictedHomeScore; }
    public void setPredictedAwayScore(Integer predictedAwayScore) { this.predictedAwayScore = predictedAwayScore; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setFinalizedAt(Long finalizedAt) { this.finalizedAt = finalizedAt; }

    /**
     * Convert entity to domain model.
     */
    public Prediction toDomainModel() {
        Prediction prediction = new Prediction();
        if (id != null && !id.isEmpty()) {
            try {
                prediction.setId(java.util.UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {}
        }
        if (userId != null && !userId.isEmpty()) {
            try {
                prediction.setUserId(java.util.UUID.fromString(userId));
            } catch (IllegalArgumentException ignored) {}
        }
        if (matchId != null && !matchId.isEmpty()) {
            try {
                prediction.setMatchId(java.util.UUID.fromString(matchId));
            } catch (IllegalArgumentException ignored) {}
        }
        if (predictedOutcome != null) {
            try {
                prediction.setPredictedOutcome(PredictionOutcome.valueOf(predictedOutcome));
            } catch (IllegalArgumentException ignored) {}
        }
        prediction.setPredictedHomeScore(predictedHomeScore);
        prediction.setPredictedAwayScore(predictedAwayScore);
        prediction.setPointsEarned(pointsEarned);
        prediction.setIsCorrect(isCorrect);
        prediction.setCreatedAt(createdAt);
        prediction.setFinalizedAt(finalizedAt);
        return prediction;
    }

    /**
     * Create entity from domain model.
     */
    public static PredictionEntity fromDomainModel(Prediction prediction) {
        return new PredictionEntity(
            prediction.getId() != null ? prediction.getId().toString() : "",
            prediction.getUserId() != null ? prediction.getUserId().toString() : null,
            prediction.getMatchId() != null ? prediction.getMatchId().toString() : null,
            prediction.getPredictedOutcome() != null ? prediction.getPredictedOutcome().name() : null,
            prediction.getPredictedHomeScore(),
            prediction.getPredictedAwayScore(),
            prediction.getPointsEarned(),
            prediction.getIsCorrect(),
            prediction.getCreatedAt(),
            prediction.getFinalizedAt()
        );
    }
}