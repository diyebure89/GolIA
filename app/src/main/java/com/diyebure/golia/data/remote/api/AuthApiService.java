package com.diyebure.golia.data.remote.api;

import com.diyebure.golia.data.remote.dto.AuthDto.AuthErrorResponse;
import com.diyebure.golia.data.remote.dto.AuthDto.AuthResponse;
import com.diyebure.golia.data.remote.dto.AuthDto.LoginRequest;
import com.diyebure.golia.data.remote.dto.AuthDto.RegisterRequest;
import com.diyebure.golia.data.remote.dto.AuthDto.TokenRefreshRequest;
import com.diyebure.golia.data.remote.dto.AuthDto.UserDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit API service interface for authentication endpoints.
 * All endpoints follow the REST API conventions for the authentication service.
 */
public interface AuthApiService {

    /**
     * Authenticate user with email and password.
     *
     * @param loginRequest Login credentials containing email and password
     * @return AuthResponse with access token, refresh token, and user data
     */
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest loginRequest);

    /**
     * Register a new user account.
     *
     * @param registerRequest Registration data containing username, email, password, and country
     * @return AuthResponse with access token, refresh token, and user data
     */
    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest registerRequest);

    /**
     * Refresh access token using refresh token.
     *
     * @param refreshRequest Token refresh request containing refresh token
     * @return AuthResponse with new access token and optionally new refresh token
     */
    @POST("auth/refresh")
    Call<AuthResponse> refreshToken(@Body TokenRefreshRequest refreshRequest);

    /**
     * Logout user and invalidate tokens on server side.
     *
     * @param authorization Bearer token for authentication (format: "Bearer {token}")
     * @return Empty response on success
     */
    @POST("auth/logout")
    Call<Void> logout(@Header("Authorization") String authorization);

    /**
     * Get current authenticated user's profile.
     *
     * @param authorization Bearer token for authentication (format: "Bearer {token}")
     * @return UserDto with current user's profile data
     */
    @GET("user/profile")
    Call<UserDto> getCurrentUser(@Header("Authorization") String authorization);
}