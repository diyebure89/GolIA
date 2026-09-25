package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for the {@code goals} node of an API-Football fixture item.
 * Scores are nullable ({@link Integer}) because they are absent for scheduled matches.
 */
public class GoalsDto {

    @SerializedName("home")
    private Integer home;

    @SerializedName("away")
    private Integer away;

    public Integer getHome() {
        return home;
    }

    public void setHome(Integer home) {
        this.home = home;
    }

    public Integer getAway() {
        return away;
    }

    public void setAway(Integer away) {
        this.away = away;
    }
}
