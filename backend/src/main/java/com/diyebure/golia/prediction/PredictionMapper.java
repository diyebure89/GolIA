package com.diyebure.golia.prediction;

import com.diyebure.golia.prediction.dto.PredictionDtos.PredictionDto;

/**
 * Convierte la entidad de pronostico a su DTO de API.
 */
public final class PredictionMapper {

    private PredictionMapper() {
    }

    public static PredictionDto toDto(PredictionEntity entity) {
        PredictionDto dto = new PredictionDto();
        dto.id = entity.getId();
        dto.userId = entity.getUserId();
        dto.matchId = entity.getMatchId();
        dto.predictedOutcome = entity.getPredictedOutcome();
        dto.predictedHomeScore = entity.getPredictedHomeScore();
        dto.predictedAwayScore = entity.getPredictedAwayScore();
        dto.pointsEarned = entity.getPointsEarned();
        dto.isCorrect = entity.getIsCorrect();
        dto.createdAt = entity.getCreatedAt();
        dto.finalizedAt = entity.getFinalizedAt();
        return dto;
    }
}
