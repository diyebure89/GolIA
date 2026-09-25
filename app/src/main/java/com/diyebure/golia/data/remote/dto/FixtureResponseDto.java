package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for the API-Football {@code /fixtures} endpoint.
 * The API returns the list of fixtures under the {@code response} array.
 */
public class FixtureResponseDto {

    @SerializedName("response")
    private List<FixtureItemDto> response;

    public List<FixtureItemDto> getResponse() {
        return response != null ? response : new ArrayList<>();
    }

    public void setResponse(List<FixtureItemDto> response) {
        this.response = response;
    }
}
