package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Objects for Authentication API requests and responses.
 */
public class AuthDto {

    // ==================== Login Request ====================

    public static class LoginRequest {
        @SerializedName("email")
        private String email;

        @SerializedName("password")
        private String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // ==================== Register Request ====================

    public static class RegisterRequest {
        @SerializedName("username")
        private String username;

        @SerializedName("email")
        private String email;

        @SerializedName("password")
        private String password;

        @SerializedName("country")
        private String country;

        public RegisterRequest(String username, String email, String password, String country) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.country = country;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }

    // ==================== Token Refresh Request ====================

    public static class TokenRefreshRequest {
        @SerializedName("refresh_token")
        private String refreshToken;

        public TokenRefreshRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    // ==================== Authentication Response ====================

    public static class AuthResponse {
        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("refresh_token")
        private String refreshToken;

        @SerializedName("user")
        private UserDto user;

        @SerializedName("expires_in")
        private long expiresIn;

        public AuthResponse(String accessToken, String refreshToken, UserDto user, long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
            this.expiresIn = expiresIn;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public UserDto getUser() {
            return user;
        }

        public void setUser(UserDto user) {
            this.user = user;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
        }
    }

    // ==================== User Data Transfer Object ====================

    public static class UserDto {
        @SerializedName("id")
        private String id;

        @SerializedName("username")
        private String username;

        @SerializedName("email")
        private String email;

        @SerializedName("country")
        private String country;

        @SerializedName("avatar_url")
        private String avatarUrl;

        @SerializedName("total_points")
        private int totalPoints;

        @SerializedName("predictions_made")
        private int predictionsMade;

        @SerializedName("predictions_correct")
        private int predictionsCorrect;

        @SerializedName("created_at")
        private String createdAt;

        public UserDto() {
        }

        public UserDto(String id, String username, String email, String country, String avatarUrl,
                       int totalPoints, int predictionsMade, int predictionsCorrect, String createdAt) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.country = country;
            this.avatarUrl = avatarUrl;
            this.totalPoints = totalPoints;
            this.predictionsMade = predictionsMade;
            this.predictionsCorrect = predictionsCorrect;
            this.createdAt = createdAt;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public int getTotalPoints() {
            return totalPoints;
        }

        public void setTotalPoints(int totalPoints) {
            this.totalPoints = totalPoints;
        }

        public int getPredictionsMade() {
            return predictionsMade;
        }

        public void setPredictionsMade(int predictionsMade) {
            this.predictionsMade = predictionsMade;
        }

        public int getPredictionsCorrect() {
            return predictionsCorrect;
        }

        public void setPredictionsCorrect(int predictionsCorrect) {
            this.predictionsCorrect = predictionsCorrect;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        /**
         * Calculate prediction accuracy as a percentage.
         */
        public float getAccuracyPercentage() {
            if (predictionsMade == 0) {
                return 0f;
            }
            return (float) predictionsCorrect / predictionsMade * 100;
        }
    }

    // ==================== Auth Error Response ====================

    public static class AuthErrorResponse {
        @SerializedName("error")
        private String error;

        @SerializedName("message")
        private String message;

        @SerializedName("status_code")
        private int statusCode;

        public AuthErrorResponse(String error, String message, int statusCode) {
            this.error = error;
            this.message = message;
            this.statusCode = statusCode;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }
    }
}