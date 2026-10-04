package com.diyebure.golia.user;

import com.diyebure.golia.auth.dto.AuthDtos.UserDto;

/**
 * Convierte la entidad persistida a la vista publica {@code UserDto} (sin
 * credenciales). El {@code created_at} se expone como texto (epoch millis en
 * String), igual que en la app.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserDto toDto(UserEntity entity) {
        UserDto dto = new UserDto();
        dto.id = entity.getId();
        dto.username = entity.getUsername();
        dto.email = entity.getEmail();
        dto.country = entity.getCountry();
        dto.avatarUrl = entity.getAvatarUrl();
        dto.totalPoints = entity.getTotalPoints();
        dto.predictionsMade = entity.getPredictionsMade();
        dto.predictionsCorrect = entity.getPredictionsCorrect();
        dto.createdAt = String.valueOf(entity.getCreatedAt());
        return dto;
    }
}
