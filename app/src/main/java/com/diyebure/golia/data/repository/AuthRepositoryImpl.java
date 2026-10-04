package com.diyebure.golia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.mapper.UserMapper;
import com.diyebure.golia.data.remote.api.AuthApiService;
import com.diyebure.golia.data.remote.dto.AuthDto.AuthErrorResponse;
import com.diyebure.golia.data.remote.dto.AuthDto.AuthResponse;
import com.diyebure.golia.data.remote.dto.AuthDto.LoginRequest;
import com.diyebure.golia.data.remote.dto.AuthDto.RegisterRequest;
import com.diyebure.golia.data.remote.dto.AuthDto.TokenRefreshRequest;
import com.diyebure.golia.data.remote.dto.AuthDto.UserDto;
import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.AuthRepository;
import com.google.gson.Gson;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

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

    @Inject
    public AuthRepositoryImpl(
            AuthApiService authApiService,
            PreferencesManager preferencesManager) {
        this.authApiService = authApiService;
        this.preferencesManager = preferencesManager;
    }

    @Override
    public Result<User> login(String identifier, String password) {
        try {
            // NOTE: This is the remote (inactive) implementation. The remote API only
            // accepts an email field, so the identifier (username or email) is sent as-is.
            LoginRequest request = new LoginRequest(identifier, password);
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

            // Error del backend: se mapea a un AuthError tipado para la UI.
            Log.e("GolIAAuth", "login: respuesta no exitosa, code=" + response.code());
            return new Result.Error(mapLoginError(response));

        } catch (SocketTimeoutException | UnknownHostException e) {
            Log.e("GolIAAuth", "login: error de red hacia el backend", e);
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        } catch (Exception e) {
            Log.e("GolIAAuth", "login: excepcion inesperada", e);
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        }
    }

    @Override
    public Result<User> register(String fullName, String username, String email, String password) {
        try {
            // NOTE: This is the remote (inactive) implementation. The remote API does not
            // yet have fields for fullName/country, so null is passed for country and the
            // fullName is not sent until the API supports it.
            RegisterRequest request = new RegisterRequest(username, email, password, null);
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

            // Error del backend: se mapea a un AuthError tipado (email/username tomado, etc.).
            Log.e("GolIAAuth", "register: respuesta no exitosa, code=" + response.code());
            return new Result.Error(mapRegisterError(response));

        } catch (SocketTimeoutException | UnknownHostException e) {
            Log.e("GolIAAuth", "register: error de red hacia el backend", e);
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        } catch (Exception e) {
            Log.e("GolIAAuth", "register: excepcion inesperada", e);
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
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
                return new Result.Error(new AuthException(AuthError.SESSION_UNAVAILABLE));
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

            // Si el refresco falla, hay que volver a iniciar sesion.
            if (response.code() == 401 || response.code() == 403) {
                clearSession();
                return new Result.Error(new AuthException(AuthError.SESSION_UNAVAILABLE));
            }

            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR));

        } catch (SocketTimeoutException | UnknownHostException e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        } catch (Exception e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
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
                return new Result.Error(new AuthException(AuthError.SESSION_UNAVAILABLE));
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

            // Si el token es invalido, intenta refrescarlo.
            if (response.code() == 401 || response.code() == 403) {
                Result<User> refreshResult = refreshToken();
                if (refreshResult instanceof Result.Success) {
                    return refreshResult;
                }
            }

            return new Result.Error(new AuthException(AuthError.SESSION_UNAVAILABLE));

        } catch (SocketTimeoutException | UnknownHostException e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        } catch (Exception e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
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
     * Traduce un error de login del backend a un {@link AuthException} tipado.
     * Un 401 significa credenciales invalidas; cualquier otro fallo se trata
     * como error de persistencia/servidor.
     */
    private AuthException mapLoginError(Response<?> response) {
        if (response.code() == 401) {
            return new AuthException(AuthError.INVALID_CREDENTIALS);
        }
        if (response.code() == 422) {
            return new AuthException(AuthError.VALIDATION_ERROR);
        }
        return new AuthException(AuthError.PERSISTENCE_ERROR);
    }

    /**
     * Traduce un error de registro del backend a un {@link AuthException} tipado.
     * Un 409 (conflicto) se desambigua a EMAIL_TAKEN o USERNAME_TAKEN leyendo el
     * mensaje del cuerpo; un 422 es error de validacion; el resto, persistencia.
     */
    private AuthException mapRegisterError(Response<?> response) {
        if (response.code() == 409) {
            String message = readErrorMessage(response);
            String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
            // El backend usa "usuario" para el username y "correo" para el email.
            if (lower.contains("usuario") || lower.contains("username")) {
                return new AuthException(AuthError.USERNAME_TAKEN);
            }
            // Por defecto, un conflicto en registro es el email ya usado.
            return new AuthException(AuthError.EMAIL_TAKEN);
        }
        if (response.code() == 422) {
            return new AuthException(AuthError.VALIDATION_ERROR);
        }
        return new AuthException(AuthError.PERSISTENCE_ERROR);
    }

    /**
     * Lee el campo {@code message} del cuerpo de error {@code {error, message,
     * status_code}} que devuelve el backend, o {@code null} si no se puede parsear.
     */
    private String readErrorMessage(Response<?> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            String body = response.errorBody().string();
            AuthErrorResponse parsed = new Gson().fromJson(body, AuthErrorResponse.class);
            return parsed != null ? parsed.getMessage() : null;
        } catch (Exception e) {
            return null;
        }
    }
}