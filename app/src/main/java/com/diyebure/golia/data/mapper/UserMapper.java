package com.diyebure.golia.data.mapper;

import com.diyebure.golia.data.remote.dto.AuthDto.UserDto;
import com.diyebure.golia.domain.model.User;

/**
 * Mapper class for converting between User DTOs and domain models.
 * Follows the principles of Clean Architecture by separating data layer
 * representations from domain models.
 */
public class UserMapper {

    private UserMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert UserDto from API response to User domain model.
     */
    public static User toDomain(UserDto userDto) {
        if (userDto == null) {
            return null;
        }

        return new User(
                userDto.getId(),
                userDto.getUsername(),
                userDto.getEmail(),
                userDto.getCountry(),
                userDto.getAvatarUrl(),
                userDto.getTotalPoints(),
                userDto.getPredictionsMade(),
                userDto.getPredictionsCorrect(),
                userDto.getCreatedAt()
        );
    }

    /**
     * Convert User domain model to UserDto for API requests.
     * Note: This may not be needed if we only receive UserDto from API,
     * but included for completeness.
     */
    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setCountry(user.getCountry());
        userDto.setAvatarUrl(user.getAvatarUrl());
        userDto.setTotalPoints(user.getTotalPoints());
        userDto.setPredictionsMade(user.getPredictionsMade());
        userDto.setPredictionsCorrect(user.getPredictionsCorrect());
        userDto.setCreatedAt(user.getCreatedAt());
        
        return userDto;
    }
}