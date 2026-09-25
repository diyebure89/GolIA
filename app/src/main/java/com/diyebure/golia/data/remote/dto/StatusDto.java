package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for the {@code fixture.status} node of an API-Football fixture.
 * The API short status code is mapped from the JSON key {@code short} to {@code shortCode}.
 */
public class StatusDto {

    @SerializedName("short")
    private String shortCode;

    @SerializedName("elapsed")
    private Integer elapsed;

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public Integer getElapsed() {
        return elapsed;
    }

    public void setElapsed(Integer elapsed) {
        this.elapsed = elapsed;
    }
}
