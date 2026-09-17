package com.diyebure.golia.data.remote.dto;

import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.model.PredictionOutcome;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Data Transfer Objects for Prediction API requests and responses.
 */
public class PredictionDto {

    /**
     * Request DTO for submitting a prediction.
     */
    public static class PredictionRequestDto {
        private String matchId;
        private String predictedOutcome;
        private Integer predictedHomeScore;
        private Integer predictedAwayScore;

        public PredictionRequestDto() {}

        public PredictionRequestDto(String matchId, PredictionOutcome predictedOutcome,
                                     Integer predictedHomeScore, Integer predictedAwayScore) {
            this.matchId = matchId;
            this.predictedOutcome = predictedOutcome != null ? predictedOutcome.name() : null;
            this.predictedHomeScore = predictedHomeScore;
            this.predictedAwayScore = predictedAwayScore;
        }

        public String getMatchId() { return matchId; }
        public void setMatchId(String matchId) { this.matchId = matchId; }

        public String getPredictedOutcome() { return predictedOutcome; }
        public void setPredictedOutcome(String predictedOutcome) { this.predictedOutcome = predictedOutcome; }

        public Integer getPredictedHomeScore() { return predictedHomeScore; }
        public void setPredictedHomeScore(Integer predictedHomeScore) { this.predictedHomeScore = predictedHomeScore; }

        public Integer getPredictedAwayScore() { return predictedAwayScore; }
        public void setPredictedAwayScore(Integer predictedAwayScore) { this.predictedAwayScore = predictedAwayScore; }

        /**
         * Create request DTO from domain model.
         */
        public static PredictionRequestDto fromDomainModel(Prediction prediction) {
            return new PredictionRequestDto(
                prediction.getMatchId() != null ? prediction.getMatchId().toString() : null,
                prediction.getPredictedOutcome(),
                prediction.getPredictedHomeScore(),
                prediction.getPredictedAwayScore()
            );
        }
    }

    /**
     * Response DTO for a single prediction.
     */
    public static class PredictionResponseDto {
        private String id;
        private String userId;
        private String matchId;
        private String predictedOutcome;
        private Integer predictedHomeScore;
        private Integer predictedAwayScore;
        private int pointsEarned;
        private Boolean isCorrect;
        private long createdAt;
        private Long finalizedAt;

        public PredictionResponseDto() {}

        // Getters
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
        public void setId(String id) { this.id = id; }
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
         * Convert response DTO to domain model.
         */
        public Prediction toDomainModel() {
            Prediction prediction = new Prediction();
            if (id != null && !id.isEmpty()) {
                try {
                    prediction.setId(UUID.fromString(id));
                } catch (IllegalArgumentException ignored) {}
            }
            if (userId != null && !userId.isEmpty()) {
                try {
                    prediction.setUserId(UUID.fromString(userId));
                } catch (IllegalArgumentException ignored) {}
            }
            if (matchId != null && !matchId.isEmpty()) {
                try {
                    prediction.setMatchId(UUID.fromString(matchId));
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
         * Create response DTO from domain model.
         */
        public static PredictionResponseDto fromDomainModel(Prediction prediction) {
            PredictionResponseDto dto = new PredictionResponseDto();
            dto.setId(prediction.getId() != null ? prediction.getId().toString() : null);
            dto.setUserId(prediction.getUserId() != null ? prediction.getUserId().toString() : null);
            dto.setMatchId(prediction.getMatchId() != null ? prediction.getMatchId().toString() : null);
            dto.setPredictedOutcome(prediction.getPredictedOutcome() != null ? prediction.getPredictedOutcome().name() : null);
            dto.setPredictedHomeScore(prediction.getPredictedHomeScore());
            dto.setPredictedAwayScore(prediction.getPredictedAwayScore());
            dto.setPointsEarned(prediction.getPointsEarned());
            dto.setIsCorrect(prediction.getIsCorrect());
            dto.setCreatedAt(prediction.getCreatedAt());
            dto.setFinalizedAt(prediction.getFinalizedAt());
            return dto;
        }
    }

    /**
     * Wrapper response for list of predictions.
     */
    public static class PredictionListResponseDto {
        private List<PredictionResponseDto> predictions;
        private int count;
        private int totalPoints;

        public PredictionListResponseDto() {}

        public List<PredictionResponseDto> getPredictions() { return predictions; }
        public void setPredictions(List<PredictionResponseDto> predictions) { this.predictions = predictions; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public int getTotalPoints() { return totalPoints; }
        public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

        /**
         * Convert list of response DTOs to domain models.
         */
        public List<Prediction> toDomainModelList() {
            if (predictions == null) return List.of();
            return predictions.stream()
                .map(PredictionResponseDto::toDomainModel)
                .collect(Collectors.toList());
        }
    }

    /**
     * Prediction result response after match is finalized.
     */
    public static class PredictionResultDto {
        private String predictionId;
        private Boolean isCorrect;
        private int pointsEarned;
        private String actualOutcome;

        public PredictionResultDto() {}

        public String getPredictionId() { return predictionId; }
        public void setPredictionId(String predictionId) { this.predictionId = predictionId; }

        public Boolean getIsCorrect() { return isCorrect; }
        public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

        public int getPointsEarned() { return pointsEarned; }
        public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

        public String getActualOutcome() { return actualOutcome; }
        public void setActualOutcome(String actualOutcome) { this.actualOutcome = actualOutcome; }
    }
}