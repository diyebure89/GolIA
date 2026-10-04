package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTOs para el endpoint de resolucion de pronosticos de un partido.
 * Nombres JSON alineados con {@code MatchResolveDtos} del backend.
 */
public class MatchResolveDto {

    /** Cuerpo de POST /matches/{matchId}/resolve. */
    public static class ResolveRequest {
        @SerializedName("home_score")
        private final int homeScore;

        @SerializedName("away_score")
        private final int awayScore;

        public ResolveRequest(int homeScore, int awayScore) {
            this.homeScore = homeScore;
            this.awayScore = awayScore;
        }

        public int getHomeScore() {
            return homeScore;
        }

        public int getAwayScore() {
            return awayScore;
        }
    }

    /** Respuesta del endpoint: resumen de pronosticos procesados. */
    public static class ResolveResponse {
        @SerializedName("match_id")
        private String matchId;

        @SerializedName("resolved_count")
        private int resolvedCount;

        @SerializedName("correct_count")
        private int correctCount;

        @SerializedName("already_resolved")
        private int alreadyResolved;

        public String getMatchId() {
            return matchId;
        }

        public int getResolvedCount() {
            return resolvedCount;
        }

        public int getCorrectCount() {
            return correctCount;
        }

        public int getAlreadyResolved() {
            return alreadyResolved;
        }
    }
}
