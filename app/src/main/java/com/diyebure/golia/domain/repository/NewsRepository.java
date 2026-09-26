package com.diyebure.golia.domain.repository;

import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.model.ArticuloNoticia;

import java.util.List;

/**
 * Repository interface for football news operations.
 *
 * <p>Defines the contract for fetching news following an asynchronous,
 * <strong>cache-first</strong> strategy. As with {@link MatchRepository}, every
 * operation runs its blocking network/persistence work off the main thread (in the
 * data-layer implementation via the shared IO executor) and publishes its outcome
 * through a {@link Callback}. The callback delivers a
 * {@link com.diyebure.golia.domain.common.Result} that is either a success carrying
 * domain articles or a {@code Result.Error} wrapping a typed
 * {@link com.diyebure.golia.domain.error.NewsError} (R11.5).</p>
 *
 * <p>The contract exposes <strong>only domain types</strong> ({@link ArticuloNoticia});
 * provider DTOs and Room entities never cross this boundary (R4.4).</p>
 *
 * <p>{@code leagueKey} is the stable textual key that identifies a chip's news scope,
 * e.g. an API-Football league id as text ({@code "39"}), the World Cup qualification
 * aggregate ({@code "wc_qualification"}) or the "all leagues" key ({@code "all"}).</p>
 */
public interface NewsRepository {

    /**
     * Get news following a cache-first strategy: delivers the currently cached
     * articles for the given league scope without hitting the network. Articles are
     * ordered by publication date, most recent first.
     *
     * @param leagueKey the stable textual key of the chip's scope (e.g. {@code "39"},
     *                  {@code "wc_qualification"}, {@code "all"})
     * @param callback  receives {@code Result.Success<List<ArticuloNoticia>>} with the
     *                  cached articles, or {@code Result.Error} carrying a
     *                  {@link com.diyebure.golia.domain.error.NewsError} on failure
     */
    void getCachedNews(String leagueKey, Callback<List<ArticuloNoticia>> callback);

    /**
     * Refresh news for the given league scope from the remote provider, subject to the
     * daily budget and minimum refresh interval. Freshly fetched articles are persisted
     * to the cache and delivered through the callback; on network/quota failures the
     * existing cache is preserved and served alongside the corresponding error signal.
     *
     * @param leagueKey the stable textual key of the chip's scope (e.g. {@code "39"},
     *                  {@code "wc_qualification"}, {@code "all"})
     * @param callback  receives {@code Result.Success<List<ArticuloNoticia>>} with the
     *                  refreshed articles, or {@code Result.Error} carrying a
     *                  {@link com.diyebure.golia.domain.error.NewsError} on failure
     */
    void refreshNews(String leagueKey, Callback<List<ArticuloNoticia>> callback);
}
