package com.diyebure.golia.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTOs de autenticacion. Los nombres JSON coinciden EXACTAMENTE con los
 * {@code @SerializedName} que usa la app Android en {@code AuthDto}, para que
 * Retrofit/Gson (app) y Jackson (backend) hablen el mismo contrato.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** POST /auth/login */
    public static class LoginRequest {
        // La app envia el identificador (username o email) en el campo "email".
        @JsonProperty("email")
        @NotBlank
        public String email;

        @JsonProperty("password")
        @NotBlank
        public String password;
    }

    /** POST /auth/register */
    public static class RegisterRequest {
        @JsonProperty("username")
        public String username;

        @JsonProperty("email")
        @NotBlank
        @Email
        public String email;

        @JsonProperty("password")
        @NotBlank
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        public String password;

        @JsonProperty("country")
        public String country;
    }

    /** POST /auth/refresh */
    public static class TokenRefreshRequest {
        @JsonProperty("refresh_token")
        @NotBlank
        public String refreshToken;
    }

    /** Respuesta de login/register/refresh. */
    public static class AuthResponse {
        @JsonProperty("access_token")
        public String accessToken;

        @JsonProperty("refresh_token")
        public String refreshToken;

        @JsonProperty("user")
        public UserDto user;

        @JsonProperty("expires_in")
        public long expiresIn;

        public AuthResponse(String accessToken, String refreshToken, UserDto user, long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
            this.expiresIn = expiresIn;
        }
    }

    /** Vista publica del usuario (sin credenciales). Coincide con la app. */
    public static class UserDto {
        @JsonProperty("id")
        public String id;

        @JsonProperty("username")
        public String username;

        @JsonProperty("email")
        public String email;

        @JsonProperty("country")
        public String country;

        @JsonProperty("avatar_url")
        public String avatarUrl;

        @JsonProperty("total_points")
        public int totalPoints;

        @JsonProperty("predictions_made")
        public int predictionsMade;

        @JsonProperty("predictions_correct")
        public int predictionsCorrect;

        @JsonProperty("created_at")
        public String createdAt;
    }

    /** Cuerpo de error. Coincide con {@code AuthErrorResponse} de la app. */
    public static class ErrorResponse {
        @JsonProperty("error")
        public String error;

        @JsonProperty("message")
        public String message;

        @JsonProperty("status_code")
        public int statusCode;

        public ErrorResponse(String error, String message, int statusCode) {
            this.error = error;
            this.message = message;
            this.statusCode = statusCode;
        }
    }
}
