package com.diyebure.golia.data.remote.api;

import com.diyebure.golia.data.remote.dto.NewsResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit API service interface for the NewsData.io provider.
 *
 * <p>All requests target the {@code /news} endpoint. The response is the JSON
 * structure exposed by {@link NewsResponseDto} ({@code results[]} plus paging
 * metadata).</p>
 *
 * <p>The required {@code apikey} authentication parameter is NOT declared per
 * method here; it is injected transparently as a query parameter by an OkHttp
 * interceptor ({@code NewsApiKeyInterceptor}) configured on the shared client,
 * so the key never leaks into call sites (Requirements 5.3, 5.4).</p>
 */
public interface NewsDataApiService {

    /**
     * Get news articles matching the given search terms, language and category.
     *
     * @param query    Search terms (NewsData.io {@code q} parameter)
     * @param language Two-letter language code (e.g. {@code "es"})
     * @param category Content category (e.g. {@code "sports"})
     * @return Call containing the news response
     */
    @GET("news")
    Call<NewsResponseDto> getNews(
            @Query("q") String query,
            @Query("language") String language,
            @Query("category") String category
    );
}
