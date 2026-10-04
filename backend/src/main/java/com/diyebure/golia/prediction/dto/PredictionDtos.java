package com.diyebure.golia.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTOs de pronosticos. Nombres JSON en snake_case, alineados con el estilo del
 * resto de la API para que la app los consuma directamente con Gson.
 */
public final class PredictionDtos {

    private PredictionDtos() {
    }

    /** POST /predictions (upsert del pronostico del usuario para un partido). */
    public static class SavePredictionRequest {
        @JsonProperty("match_id")
        @NotBlank
        public String matchId;

        @JsonProperty("home_score")
        @NotNull
        @Min(0)
        public Integer homeScore;

        @JsonProperty("away_score")
        @NotNull
        @Min(0)
        public Integer awayScore;
    }

    /** Representacion de un pronostico devuelto por la API. */
    public static class PredictionDto {
        @JsonProperty("id")
        public String id;

        @JsonProperty("user_id")
        public String userId;

        @JsonProperty("match_id")
        public String matchId;

        @JsonProperty("predicted_outcome")
        public String predictedOutcome;

        @JsonProperty("predicted_home_score")
        public Integer predictedHomeScore;

        @JsonProperty("predicted_away_score")
        public Integer predictedAwayScore;

        @JsonProperty("points_earned")
        public int pointsEarned;

        @JsonProperty("is_correct")
        public Boolean isCorrect;

        @JsonProperty("created_at")
        public long createdAt;

        @JsonProperty("finalized_at")
        public Long finalizedAt;
    }
}
