package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for the {@code fixture} node of an API-Football fixture item.
 */
public class FixtureDto {

    @SerializedName("id")
    private long id;

    /** ISO8601 date-time with zone offset. */
    @SerializedName("date")
    private String date;

    /** Epoch seconds. */
    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("status")
    private StatusDto status;

    @SerializedName("venue")
    private VenueDto venue;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public StatusDto getStatus() {
        return status;
    }

    public void setStatus(StatusDto status) {
        this.status = status;
    }

    public VenueDto getVenue() {
        return venue;
    }

    public void setVenue(VenueDto venue) {
        this.venue = venue;
    }
}
