package com.diyebure.golia.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.diyebure.golia.data.Result;
import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.mapper.UserMapper;
import com.diyebure.golia.data.remote.api.AuthApiService;
import com.diyebure.golia.data.remote.dto.AuthDto.AuthResponse;
import com.diyebure.golia.data.remote.dto.AuthDto.LoginRequest;
import com.diyebure.golia.data.remote.dto.AuthDto.RegisterRequest;
import com.diyebure.golia.data.remote.dto.AuthDto.TokenRefreshRequest;
import com.diyebure.golia.data.remote.dto.AuthDto.UserDto;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.AuthRepository;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Implementation of AuthRepository that handles authentication API calls
 * and manages token storage using PreferencesManager.
 */
@Singleton
public class AuthRepositoryImpl implements AuthRepository {

    private final AuthApiService authApiService;
    private final PreferencesManager preferencesManager;
    private final Context context;

    @Inject
    public AuthRepositoryImpl(
            Context context,
            AuthApiService authApiService,
            PreferencesManager preferencesManager) {
        this.context = context.getApplicationContext();
        this.authApiService = authApiService;
        this.preferencesManager = preferencesManager;
    }

    /**
     * Get singleton instance of AuthRepositoryImpl.
     * This method is kept for backward compatibility but should be deprecated
     * in favor of dependency injection.
     */
    public static AuthRepositoryImpl getInstance(Context context) {
        throw new UnsupportedOperationException("Use dependency injection instead");
    }

    /**
     * Get singleton instance with PreferencesManager for backward compatibility.
     */
    public static AuthRepositoryImpl getInstance(Context context, PreferencesManager preferencesManager) {
        throw new UnsupportedOperationException("Use dependency injection instead");
    }

    @Override
    public Result<User> login(String email, String password) {
        try {
            LoginRequest request = new LoginRequest(email, password);
            Call<AuthResponse> call = authApiService.login(request);

            Response<AuthResponse> response = call.execute();

            if (response.isSuccessful()) {
                AuthResponse authResponse = response.body();
                if (authResponse != null) {
                    saveAuthTokens(
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken(),
                            authResponse.getExpiresIn()
                    );
                    User user = UserMapper.toDomain(authResponse.getUser());
                    return new Result.Success<>(user);
                }
            }

            // Handle error response
            String errorMessage = parseErrorResponse(response);
            return new Result.Error(new Exception(errorMessage));

        } catch (SocketTimeoutException e) {
            return new Result.Error(new IOException("Connection timed out. Please try again.", e));
        } catch (UnknownHostException e) {
            return new Result.Error(new IOException("Network error. Please check your connection.", e));
        } catch (Exception e) {
            return new Result.Error(new IOException("An unexpected error occurred.", e));
        }
    }

    @Override
    public Result<User> register(String username, String email, String password, String country) {
        try {
            RegisterRequest request = new RegisterRequest(username, email, password, country);
            Call<AuthResponse> call = authApiService.register(request);

            Response<AuthResponse> response = call.execute();

            if (response.isSuccessful()) {
                AuthResponse authResponse = response.body();
                if (authResponse != null) {
                    saveAuthTokens(
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken(),
                            authResponse.getExpiresIn()
                    );
                    User user = UserMapper.toDomain(authResponse.getUser());
                    return new Result.Success<>(user);
                }
            }

            // Handle error response
            String errorMessage = parseErrorResponse(response);
            return new Result.Error(new Exception(errorMessage));

        } catch (SocketTimeoutException e) {
            return new Result.Error(new IOException("Connection timed out. Please try again.", e));
        } catch (UnknownHostException e) {
            return new Result.Error(new IOException("Network error. Please check your connection.", e));
        } catch (Exception e) {
            return new Result.Error(new IOException("An unexpected error occurred.", e));
        }
    }

    @Override
    public Result<Void> logout() {
        try {
            String authHeader = preferencesManager.getAuthorizationHeader();
            if (authHeader != null) {
                Call<Void> call = authApiService.logout(authHeader);
                call.execute();
            }

            // Clear local session regardless of server response
            clearSession();
            return new Result.Success<>(null);

        } catch (Exception e) {
            // Clear local session even if server logout fails
            clearSession();
            return new Result.Success<>(null);
        }
    }

    @Override
    public Result<User> refreshToken() {
        try {
            String refreshToken = preferencesManager.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                return new Result.Error(new Exception("No refresh token available"));
            }

            TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);
            Call<AuthResponse> call = authApiService.refreshToken(request);

            Response<AuthResponse> response = call.execute();

            if (response.isSuccessful()) {
                AuthResponse authResponse = response.body();
                if (authResponse != null) {
                    saveAuthTokens(
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken(),
                            authResponse.getExpiresIn()
                    );
                    User user = UserMapper.toDomain(authResponse.getUser());
                    return new Result.Success<>(user);
                }
            }

            // If refresh fails, user needs to log in again
            if (response.code() == 401 || response.code() == 403) {
                clearSession();
                return new Result.Error(new Exception("Session expired. Please log in again."));
            }

            String errorMessage = parseErrorResponse(response);
            return new Result.Error(new Exception(errorMessage));

        } catch (SocketTimeoutException e) {
            return new Result.Error(new IOException("Connection timed out. Please try again.", e));
        } catch (UnknownHostException e) {
            return new Result.Error(new IOException("Network error. Please check your connection.", e));
        } catch (Exception e) {
            return new Result.Error(new IOException("An unexpected error occurred.", e));
        }
    }

    @Override
    public boolean isLoggedIn() {
        return preferencesManager.isLoggedIn() && !preferencesManager.isTokenExpired();
    }

    @Override
    public Result<User> getCurrentUser() {
        try {
            String authHeader = preferencesManager.getAuthorizationHeader();
            if (authHeader == null) {
                return new Result.Error(new Exception("Not authenticated"));
            }

            Call<UserDto> call = authApiService.getCurrentUser(authHeader);
            Response<UserDto> response = call.execute();

            if (response.isSuccessful()) {
                UserDto userDto = response.body();
                if (userDto != null) {
                    User user = UserMapper.toDomain(userDto);
                    return new Result.Success<>(user);
                }
            }

            // If token is invalid, try refreshing
            if (response.code() == 401 || response.code() == 403) {
                Result<User> refreshResult = refreshToken();
                if (refreshResult instanceof Result.Success) {
                    return refreshResult;
                }
            }

            String errorMessage = parseErrorResponse(response);
            return new Result.Error(new Exception(errorMessage));

        } catch (SocketTimeoutException e) {
            return new Result.Error(new IOException("Connection timed out. Please try again.", e));
        } catch (UnknownHostException e) {
            return new Result.Error(new IOException("Network error. Please check your connection.", e));
        } catch (Exception e) {
            return new Result.Error(new IOException("An unexpected error occurred.", e));
        }
    }

    @Override
    public void saveAuthTokens(String accessToken, String refreshToken, long expiresIn) {
        preferencesManager.saveToken(accessToken);
        preferencesManager.saveRefreshToken(refreshToken);
        preferencesManager.saveTokenExpiry(System.currentTimeMillis() + (expiresIn * 1000));
        preferencesManager.setLoggedIn(true);
    }

    @Override
    public void clearSession() {
        preferencesManager.clearTokens();
    }

    /**
     * Parse error response from API.
     */
    private String parseErrorResponse(Response<?> response) {
        if (response.errorBody() != null) {
            try {
                String errorBody = response.errorBody().string();
                // Try to extract error message from response body
                // This is a simple implementation - could be enhanced with Gson parsing
                if (errorBody.contains("\"message\"")) {
                    int start = errorBody.indexOf("\"message\":\"") + 11;
                    int end = errorBody.indexOf("\"", start);
                    if (start > 10 && end > start) {
                        return errorBody.substring(start, end);
                    }
                }
                if (errorBody.contains("\"error\"")) {
                    int start = errorBody.indexOf("\"error\":\"") + 9;
                    int end = errorBody.indexOf("\"", start);
                    if (start > 8 && end > start) {
                        return errorBody.substring(start, end);
                    }
                }
                return errorBody;
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        // Return appropriate message based on HTTP status code
        switch (response.code()) {
            case 400:
                return "Invalid request. Please check your input.";
            case 401:
                return "Authentication failed. Please check your credentials.";
            case 403:
                return "Access denied. You don't have permission to access this resource.";
            case 404:
                return "Resource not found.";
            case 409:
                return "An account with this email already exists.";
            case 422:
                return "Validation error. Please check your input.";
            case 429:
                return "Too many requests. Please try again later.";
            case 500:
                return "Server error. Please try again later.";
            default:
                return "An error occurred. Please try again.";
        }
    }
}