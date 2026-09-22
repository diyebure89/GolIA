package com.diyebure.golia.domain.repository;

import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.User;

/**
 * Repository interface for authentication operations.
 * Defines the contract for authentication functionality that will be implemented
 * in the data layer.
 */
public interface AuthRepository {

    /**
     * Authenticate a user with an identifier (username OR email) and password.
     *
     * @param identifier User's username or email address
     * @param password User's password
     * @return Result containing User on success or exception on failure
     */
    Result<User> login(String identifier, String password);

    /**
     * Register a new user account.
     *
     * @param fullName User's full name
     * @param username Desired username (nullable when not provided)
     * @param email User's email address
     * @param password User's password (minimum 8 characters)
     * @return Result containing User on success or exception on failure
     */
    Result<User> register(String fullName, String username, String email, String password);

    /**
     * Logout current user and clear session.
     *
     * @return Result indicating success or failure
     */
    Result<Void> logout();

    /**
     * Refresh the access token using the refresh token.
     *
     * @return Result containing new AuthResponse on success or exception on failure
     */
    Result<User> refreshToken();

    /**
     * Check if user is currently logged in.
     *
     * @return true if user has valid session, false otherwise
     */
    boolean isLoggedIn();

    /**
     * Get the currently authenticated user's profile.
     *
     * @return Result containing User on success or exception on failure
     */
    Result<User> getCurrentUser();

    /**
     * Save authentication tokens after successful login.
     * This is called internally by login/register methods.
     *
     * @param accessToken JWT access token
     * @param refreshToken Refresh token for getting new access tokens
     * @param expiresIn Expiry time in seconds
     */
    void saveAuthTokens(String accessToken, String refreshToken, long expiresIn);

    /**
     * Clear all authentication data.
     */
    void clearSession();
}