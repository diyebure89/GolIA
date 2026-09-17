package com.diyebure.golia.domain.model;

import androidx.annotation.NonNull;

/**
 * Domain model representing a user in the application.
 * Contains core user data and statistics for prediction performance.
 */
public class User {

    private final String id;
    private final String username;
    private final String email;
    private final String country;
    private final String avatarUrl;
    private final int totalPoints;
    private final int predictionsMade;
    private final int predictionsCorrect;
    private final String createdAt;

    public User(String id, String username, String email, String country, 
                String avatarUrl, int totalPoints, int predictionsMade, 
                int predictionsCorrect, String createdAt) {
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

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getCountry() {
        return country;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public int getPredictionsMade() {
        return predictionsMade;
    }

    public int getPredictionsCorrect() {
        return predictionsCorrect;
    }

    public String getCreatedAt() {
        return createdAt;
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

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", country='" + country + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", totalPoints=" + totalPoints +
                ", predictionsMade=" + predictionsMade +
                ", predictionsCorrect=" + predictionsCorrect +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}