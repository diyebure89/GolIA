package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.model.ArticuloNoticia;
import com.diyebure.golia.domain.repository.NewsRepository;

import java.util.List;

import javax.inject.Inject;

/**
 * Retrieves the currently cached news for a chip's league scope following a
 * cache-first strategy.
 *
 * <p>A use case represents one business action. It depends only on the domain
 * {@link NewsRepository} interface (never on a concrete data class), so business
 * rules stay independent of networking/persistence details and expose
 * <strong>only domain types</strong> ({@link ArticuloNoticia}) (R4.1).
 *
 * <p>The {@link NewsRepository} methods are already asynchronous: the data-layer
 * implementation runs its blocking Room/network work on the shared IO executor
 * and publishes the outcome through a {@link Callback}. This use case is
 * therefore a thin, threading-free delegation to
 * {@link NewsRepository#getCachedNews(String, Callback)}, providing the immediate
 * cached source for the selected chip (R6.7).
 */
public class GetNewsUseCase {

    private final NewsRepository newsRepository;

    @Inject
    public GetNewsUseCase(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    /**
     * Delivers the cached news for the given league scope without hitting the
     * network.
     *
     * @param leagueKey the stable textual key of the chip's scope (e.g.
     *                  {@code "39"}, {@code "wc_qualification"}, {@code "all"})
     * @param callback  receives {@code Result.Success<List<ArticuloNoticia>>} with
     *                  the cached articles, or {@code Result.Error} carrying a
     *                  typed {@link com.diyebure.golia.domain.error.NewsError} on
     *                  failure (R11.5)
     */
    public void execute(String leagueKey, Callback<List<ArticuloNoticia>> callback) {
        newsRepository.getCachedNews(leagueKey, callback);
    }
}
