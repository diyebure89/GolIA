package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for a single side (home or away) team within the
 * {@code teams} node of an API-Football fixture item.
 */
public class TeamSideDto {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("logo")
    private String logo;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}
