package com.diyebure.golia.data.mapper;

import com.diyebure.golia.data.local.entity.UserEntity;
import com.diyebure.golia.domain.model.User;

/**
 * Mapper for converting a persisted {@link UserEntity} into the domain
 * {@link User} model.
 *
 * <p>There is no public {@code toEntity} that takes a credential: entities are
 * created via {@link UserEntity#newUser} inside the repository, keeping the
 * hashing/credential concern out of the mapper. The domain model never carries
 * credential data.</p>
 */
public final class UserEntityMapper {

    private UserEntityMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert a {@link UserEntity} to a {@link User} domain model.
     *
     * @return the mapped domain model, or {@code null} if {@code e} is null.
     */
    public static User toDomain(UserEntity e) {
        return e == null ? null : e.toDomainModel();
    }
}
