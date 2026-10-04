package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Una fila del ranking global devuelta por el backend GolIA (GET /ranking).
 * Los nombres JSON coinciden con {@code RankingController.RankingEntryDto} del
 * servidor.
 */
public class RankingEntryDto {

    @SerializedName("position")
    private int position;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("username")
    private String username;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("total_points")
    private int totalPoints;

    @SerializedName("predictions_made")
    private int predictionsMade;

    @SerializedName("predictions_correct")
    private int predictionsCorrect;

    public int getPosition() {
        return position;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
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
}
