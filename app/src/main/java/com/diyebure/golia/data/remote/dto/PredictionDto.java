package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTOs de pronosticos para hablar con el backend GolIA. Los nombres JSON
 * ({@link SerializedName}) coinciden con los que expone el backend Spring Boot
 * ({@code PredictionDtos} del servidor), en snake_case.
 */
public class PredictionDto {

    // ==================== Request de guardado (upsert) ====================

    /** Cuerpo de POST /predictions. */
    public static class SavePredictionRequest {
        @SerializedName("match_id")
        private final String matchId;

        @SerializedName("home_score")
        private final int homeScore;

        @SerializedName("away_score")
        private final int awayScore;

        public SavePredictionRequest(String matchId, int homeScore, int awayScore) {
            this.matchId = matchId;
            this.homeScore = homeScore;
            this.awayScore = awayScore;
        }

        public String getMatchId() {
            return matchId;
        }

        public int getHomeScore() {
            return homeScore;
        }

        public int getAwayScore() {
            return awayScore;
        }
    }

    // ==================== Respuesta ====================

    /** Un pronostico tal y como lo devuelve el backend. */
    public static class PredictionResponse {
        @SerializedName("id")
        private String id;

        @SerializedName("user_id")
        private String userId;

        @SerializedName("match_id")
        private String matchId;

        @SerializedName("predicted_outcome")
        private String predictedOutcome;

        @SerializedName("predicted_home_score")
        private Integer predictedHomeScore;

        @SerializedName("predicted_away_score")
        private Integer predictedAwayScore;

        @SerializedName("points_earned")
        private int pointsEarned;

        @SerializedName("is_correct")
        private Boolean isCorrect;

        @SerializedName("created_at")
        private long createdAt;

        @SerializedName("finalized_at")
        private Long finalizedAt;

        public String getId() {
            return id;
        }

        public String getUserId() {
            return userId;
        }

        public String getMatchId() {
            return matchId;
        }

        public String getPredictedOutcome() {
            return predictedOutcome;
        }

        public Integer getPredictedHomeScore() {
            return predictedHomeScore;
        }

        public Integer getPredictedAwayScore() {
            return predictedAwayScore;
        }

        public int getPointsEarned() {
            return pointsEarned;
        }

        public Boolean getIsCorrect() {
            return isCorrect;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public Long getFinalizedAt() {
            return finalizedAt;
        }
    }
}
