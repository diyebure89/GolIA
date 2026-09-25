package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for the {@code teams} node of an API-Football fixture item.
 */
public class TeamsDto {

    @SerializedName("home")
    private TeamSideDto home;

    @SerializedName("away")
    private TeamSideDto away;

    public TeamSideDto getHome() {
        return home;
    }

    public void setHome(TeamSideDto home) {
        this.home = home;
    }

    public TeamSideDto getAway() {
        return away;
    }

    public void setAway(TeamSideDto away) {
        this.away = away;
    }
}
