package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.model.ArticuloNoticia;
import com.diyebure.golia.domain.repository.NewsRepository;

import java.util.List;

import javax.inject.Inject;

/**
 * Triggers a remote refresh of the news for a chip's league scope.
 *
 * <p>A use case represents one business action. It depends only on the domain
 * {@link NewsRepository} interface (never on a concrete data class) and exposes
 * <strong>only domain types</strong> ({@link ArticuloNoticia}) (R4.1). The daily
 * budget and minimum refresh interval that gate remote calls (R6.1, R6.2) are
 * enforced inside the data-layer repository implementation, not here.
 *
 * <p>The {@link NewsRepository} methods are already asynchronous: the data-layer
 * implementation runs its blocking network/Room work on the shared IO executor
 * (R11.4) and publishes the outcome through a {@link Callback}. This use case is
 * therefore a thin, threading-free delegation to
 * {@link NewsRepository#refreshNews(String, Callback)}.
 */
public class RefreshNewsUseCase {

    private final NewsRepository newsRepository;

    @Inject
    public RefreshNewsUseCase(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    /**
     * Refreshes the news for the given league scope from the remote provider,
     * subject to the budget/min-interval enforced by the repository implementation.
     *
     * @param leagueKey the stable textual key of the chip's scope (e.g.
     *                  {@code "39"}, {@code "wc_qualification"}, {@code "all"})
     * @param callback  receives {@code Result.Success<List<ArticuloNoticia>>} with
     *                  the refreshed articles, or {@code Result.Error} carrying a
     *                  typed {@link com.diyebure.golia.domain.error.NewsError} on
     *                  failure (R11.5)
     */
    public void execute(String leagueKey, Callback<List<ArticuloNoticia>> callback) {
        newsRepository.refreshNews(leagueKey, callback);
    }
}
