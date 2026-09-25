package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for the {@code league} node of an API-Football fixture item.
 */
public class LeagueDto {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("round")
    private String round;

    @SerializedName("logo")
    private String logo;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}
