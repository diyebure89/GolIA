package com.diyebure.golia.domain.repository;

import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.User;

/**
 * Repository interface for profile management operations.
 * Defines the contract for reading and updating the current user's profile,
 * to be implemented in the data layer.
 *
 * <p>All methods are synchronous and return {@link Result} directly (identical
 * pattern to {@link AuthRepository}); use cases are responsible for dispatching
 * the calls to the IO executor.
 */
public interface ProfileRepository {

    /**
     * Get the profile of the user with the given id.
     *
     * @param id User's unique identifier
     * @return Result containing User on success or exception on failure
     */
    // R1.2, R2.1
    Result<User> getCurrentUser(String id);

    /**
     * Update the user's editable personal data (username and email).
     *
     * @param id User's unique identifier
     * @param username New username
     * @param email New email address
     * @return Result containing the updated User on success or exception on failure
     */
    // R5, R6
    Result<User> updatePersonalData(String id, String username, String email);

    /**
     * Change the user's password.
     *
     * @param id User's unique identifier
     * @param currentPassword User's current password (for verification)
     * @param newPassword User's new password
     * @return Result indicating success or failure
     */
    // R7
    Result<Void> changePassword(String id, String currentPassword, String newPassword);

    /**
     * Update the user's avatar image.
     *
     * @param id User's unique identifier
     * @param absolutePath Absolute file path of the new avatar image
     * @return Result containing the updated User on success or exception on failure
     */
    // R9
    Result<User> updateAvatar(String id, String absolutePath);
}
