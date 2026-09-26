package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for the NewsData.io {@code /news} endpoint.
 *
 * <p>This is the definitive DTO for the provider contract (task 4.1). The API
 * returns the list of articles under the {@code results} array, along with
 * paging metadata ({@code nextPage}). This type is only referenced by the
 * remote and mapper layers and must not leak into the domain or presentation
 * layers.</p>
 */
public class NewsResponseDto {

    @SerializedName("status")
    private String status;

    @SerializedName("totalResults")
    private int totalResults;

    @SerializedName("results")
    private List<NewsArticleDto> results;

    @SerializedName("nextPage")
    private String nextPage;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public List<NewsArticleDto> getResults() {
        return results != null ? results : new ArrayList<>();
    }

    public void setResults(List<NewsArticleDto> results) {
        this.results = results;
    }

    public String getNextPage() {
        return nextPage;
    }

    public void setNextPage(String nextPage) {
        this.nextPage = nextPage;
    }
}
