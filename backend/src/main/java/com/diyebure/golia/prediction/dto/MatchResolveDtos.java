package com.diyebure.golia.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTOs del endpoint de resolucion de pronosticos de un partido.
 */
public final class MatchResolveDtos {

    private MatchResolveDtos() {
    }

    /**
     * Cuerpo de POST /matches/{matchId}/resolve.
     * La app envia el marcador final cuando detecta que un partido termino.
     */
    public static class ResolveRequest {
        @JsonProperty("home_score")
        @NotNull
        @Min(0)
        public Integer homeScore;

        @JsonProperty("away_score")
        @NotNull
        @Min(0)
        public Integer awayScore;
    }

    /** Resumen de la resolucion: cuantos pronosticos se procesaron y cuantos acertaron. */
    public static class ResolveResponse {
        @JsonProperty("match_id")
        public String matchId;

        @JsonProperty("resolved_count")
        public int resolvedCount;

        @JsonProperty("correct_count")
        public int correctCount;

        @JsonProperty("already_resolved")
        public int alreadyResolved;

        public ResolveResponse(String matchId, int resolvedCount,
                               int correctCount, int alreadyResolved) {
            this.matchId = matchId;
            this.resolvedCount = resolvedCount;
            this.correctCount = correctCount;
            this.alreadyResolved = alreadyResolved;
        }
    }
}
