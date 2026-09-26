package com.diyebure.golia.data.repository;

import android.util.Log;

import com.diyebure.golia.data.local.dao.NewsArticleDao;
import com.diyebure.golia.data.local.entity.NewsArticleEntity;
import com.diyebure.golia.data.local.entity.NewsLeagueCrossRefEntity;
import com.diyebure.golia.data.local.entity.NewsLeagueMetaEntity;
import com.diyebure.golia.data.mapper.NewsMapper;
import com.diyebure.golia.data.remote.NewsDataQueryBuilder;
import com.diyebure.golia.data.remote.api.NewsDataApiService;
import com.diyebure.golia.data.remote.dto.NewsResponseDto;
import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.NewsError;
import com.diyebure.golia.domain.model.ArticuloNoticia;
import com.diyebure.golia.domain.news.LeagueNewsQuery;
import com.diyebure.golia.domain.repository.NewsRepository;
import com.diyebure.golia.util.NewsRequestBudgetManager;
import com.diyebure.golia.util.RefreshDecision;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Asynchronous, cache-first implementation of {@link NewsRepository} for the
 * NewsData.io provider.
 *
 * <p>Orchestrates the full news flow (design "NewsRepository ... NewsRepositoryImpl"):
 * {@link LeagueNewsQuery} &rarr; {@link NewsDataQueryBuilder} &rarr;
 * {@link NewsDataApiService} &rarr; {@link NewsMapper} &rarr; {@link NewsArticleDao},
 * coordinated by {@link NewsRequestBudgetManager}.</p>
 *
 * <p>All network (Retrofit) and persistence (Room) I/O runs on the injected
 * {@link IoExecutor} {@link ExecutorService}; nothing blocks the main thread (R11.4).
 * Outcomes are published through the supplied {@link Callback} as a typed
 * {@link Result}. Failures are always translated into a concrete
 * {@link NewsError} subclass and delivered as {@link Result.Error}; the repository
 * never leaks provider/persistence exceptions (R11.5).</p>
 *
 * <p>Cache handling: the local cache is <strong>always preserved</strong> on failure
 * (R3.6, R6.8). When the budget/interval gate decides the request cannot proceed the
 * cached articles are served instead of hitting the network (R6.7); an HTTP 429 forces
 * cache-only mode until the UTC reset (R6.10) and surfaces
 * {@link NewsError.QuotaExceededError}.</p>
 *
 * <p>Requirements: 3.1, 3.6, 3.9, 4.1, 6.1, 6.2, 6.5, 6.7, 6.8, 6.10, 11.4, 11.5.</p>
 */
@Singleton
public class NewsRepositoryImpl implements NewsRepository {

    private static final String TAG = "NewsRepository";

    /** Fixed provider filters (design "NewsDataApiService"): Spanish sports news. */
    private static final String LANGUAGE_ES = "es";
    private static final String CATEGORY_SPORTS = "sports";

    /** HTTP status returned by the provider when the quota is exhausted (R6.10). */
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final NewsDataApiService apiService;
    private final NewsDataQueryBuilder queryBuilder;
    private final NewsMapper newsMapper;
    private final NewsArticleDao newsArticleDao;
    private final NewsRequestBudgetManager budgetManager;
    private final LeagueNewsQuery leagueNewsQuery;
    private final ExecutorService executor;

    @Inject
    public NewsRepositoryImpl(
            NewsDataApiService apiService,
            NewsDataQueryBuilder queryBuilder,
            NewsMapper newsMapper,
            NewsArticleDao newsArticleDao,
            NewsRequestBudgetManager budgetManager,
            LeagueNewsQuery leagueNewsQuery,
            @IoExecutor ExecutorService executor) {
        this.apiService = apiService;
        this.queryBuilder = queryBuilder;
        this.newsMapper = newsMapper;
        this.newsArticleDao = newsArticleDao;
        this.budgetManager = budgetManager;
        this.leagueNewsQuery = leagueNewsQuery;
        this.executor = executor;
    }

    @Override
    public void getCachedNews(String leagueKey, Callback<List<ArticuloNoticia>> callback) {
        executor.execute(() -> {
            try {
                callback.onResult(new Result.Success<>(readCache(leagueKey)));
            } catch (Exception e) {
                Log.e(TAG, "Error reading cached news for " + leagueKey, e);
                callback.onResult(new Result.Error(
                        new NewsError.PersistenceError("Failed to read cached news", e)));
            }
        });
    }

    @Override
    public void refreshNews(String leagueKey, Callback<List<ArticuloNoticia>> callback) {
        executor.execute(() -> refreshInternal(leagueKey, callback));
    }

    /**
     * Core refresh routine, always invoked on the IO executor.
     *
     * <p>Steps: read the per-league {@code lastFetchedAt}, evaluate the budget/interval
     * atomically (R6.9), and either serve cache (interval not elapsed / quota) or perform
     * a single remote request. On any failure the cache is left intact and a typed
     * {@link NewsError} is delivered (R3.6, R6.8, R11.5).</p>
     */
    private void refreshInternal(String leagueKey, Callback<List<ArticuloNoticia>> callback) {
        try {
            long lastFetchedAt = readLastFetchedAt(leagueKey);
            RefreshDecision decision = budgetManager.decide(leagueKey, lastFetchedAt);

            switch (decision) {
                case SERVE_CACHE:
                    // Per-league minimum interval not elapsed: serve cache, no error (R6.7).
                    callback.onResult(new Result.Success<>(readCache(leagueKey)));
                    return;
                case SERVE_CACHE_QUOTA:
                    // Budget exhausted or 429-forced quota: cache-only + QUOTA error (R6.10).
                    callback.onResult(new Result.Error(new NewsError.QuotaExceededError(
                            "News daily budget exhausted; serving cache only")));
                    return;
                case PROCEED:
                default:
                    performRemoteRefresh(leagueKey, callback);
            }
        } catch (Exception e) {
            // Unexpected failure before/around the network call: preserve cache.
            Log.e(TAG, "Unexpected error refreshing news for " + leagueKey, e);
            callback.onResult(new Result.Error(
                    new NewsError.PersistenceError("Unexpected error refreshing news", e)));
        }
    }

    /**
     * Performs the single remote request, maps the response, persists it and delivers
     * the ordered articles. Translates provider/network failures into typed errors while
     * always keeping the existing cache (R3.6, R6.8, R6.10, R11.5).
     */
    private void performRemoteRefresh(String leagueKey, Callback<List<ArticuloNoticia>> callback) {
        String query = queryBuilder.build(termsFor(leagueKey));
        try {
            Response<NewsResponseDto> response =
                    apiService.getNews(query, LANGUAGE_ES, CATEGORY_SPORTS).execute();

            if (response.code() == HTTP_TOO_MANY_REQUESTS) {
                // 429: force cache-only mode until the UTC reset, independent of the
                // local counter (R6.10). Cache is preserved.
                budgetManager.markQuotaExhausted();
                callback.onResult(new Result.Error(new NewsError.QuotaExceededError(
                        "Provider returned HTTP 429 (quota exceeded)")));
                return;
            }

            if (!response.isSuccessful() || response.body() == null) {
                callback.onResult(new Result.Error(new NewsError.ProviderError(
                        "Provider returned HTTP " + response.code())));
                return;
            }

            List<ArticuloNoticia> articles =
                    newsMapper.toDomain(response.body().getResults());

            // Persist (upsert + cross-ref + last-fetch + purge). If persistence fails,
            // preserve the cache and surface a typed persistence error.
            try {
                persist(leagueKey, articles);
            } catch (Exception e) {
                Log.e(TAG, "Error persisting refreshed news for " + leagueKey, e);
                callback.onResult(new Result.Error(
                        new NewsError.PersistenceError("Failed to persist news", e)));
                return;
            }

            // Deliver the freshly persisted articles ordered by date desc (R3.9/R3.10).
            callback.onResult(new Result.Success<>(readCache(leagueKey)));
        } catch (SocketTimeoutException e) {
            // >10s timeout budget (R2.7/R3.6): cache preserved.
            Log.e(TAG, "Timeout refreshing news for " + leagueKey, e);
            callback.onResult(new Result.Error(
                    new NewsError.TimeoutError("News request timed out", e)));
        } catch (IOException e) {
            // No connectivity / IO failure: cache preserved (R6.8).
            Log.e(TAG, "Network error refreshing news for " + leagueKey, e);
            callback.onResult(new Result.Error(
                    new NewsError.NetworkError("Network error refreshing news", e)));
        } catch (Exception e) {
            // Any other failure (e.g. malformed body): treat as provider error, keep cache.
            Log.e(TAG, "Provider error refreshing news for " + leagueKey, e);
            callback.onResult(new Result.Error(
                    new NewsError.ProviderError("Provider error refreshing news", e)));
        }
    }

    /**
     * Persists the mapped articles for a league: upserts articles and their cross-refs,
     * records the last-fetch instant and purges to the per-league retention cap (R6.11).
     */
    private void persist(String leagueKey, List<ArticuloNoticia> articles) {
        if (!articles.isEmpty()) {
            List<NewsArticleEntity> entities = new ArrayList<>(articles.size());
            List<NewsLeagueCrossRefEntity> crossRefs = new ArrayList<>(articles.size());
            for (ArticuloNoticia article : articles) {
                entities.add(NewsArticleEntity.fromDomainModel(article));
                crossRefs.add(new NewsLeagueCrossRefEntity(article.getArticleId(), leagueKey));
            }
            newsArticleDao.upsertArticles(entities);
            newsArticleDao.upsertCrossRefs(crossRefs);
            newsArticleDao.purgeOldForLeague(leagueKey);
            newsArticleDao.deleteOrphanArticles();
        }
        // Record the successful fetch instant even when zero articles came back, so the
        // per-league minimum interval throttles subsequent refreshes (R6.1/R6.2).
        newsArticleDao.setLastFetchedAt(
                new NewsLeagueMetaEntity(leagueKey, System.currentTimeMillis()));
    }

    /**
     * Reads the cached articles for a league ordered by publication date desc (R3.10),
     * converting Room entities to domain models.
     */
    private List<ArticuloNoticia> readCache(String leagueKey) {
        List<NewsArticleEntity> cached = newsArticleDao.getArticlesForLeague(leagueKey);
        List<ArticuloNoticia> articles = new ArrayList<>(cached.size());
        for (NewsArticleEntity entity : cached) {
            articles.add(entity.toDomainModel());
        }
        return articles;
    }

    /**
     * Reads the last-fetch instant (epoch ms) for a league, or {@code 0} when the league
     * has never been fetched.
     */
    private long readLastFetchedAt(String leagueKey) {
        Long lastFetchedAt = newsArticleDao.lastFetchedAt(leagueKey);
        return lastFetchedAt != null ? lastFetchedAt : 0L;
    }

    /**
     * Resolves the prioritized search terms for a league key (R3.1, R3.3). Handles the
     * aggregated feed ({@code "all"}), the World Cup qualification aggregate
     * ({@code "wc_qualification"}) and individual numeric league keys; unknown or
     * empty keys fall back to the aggregated terms so the query is never empty.
     */
    private List<String> termsFor(String leagueKey) {
        if (leagueKey == null
                || LeagueNewsQuery.KEY_ALL.equals(leagueKey)
                || leagueKey.trim().isEmpty()) {
            return leagueNewsQuery.aggregatedTerms();
        }
        if (LeagueNewsQuery.KEY_WC_QUALIFICATION.equals(leagueKey)) {
            // Resolve via any of the aggregated qualification ids.
            return leagueNewsQuery.termsFor(
                    LeagueNewsQuery.WC_QUALIFICATION_IDS.iterator().next());
        }
        try {
            return leagueNewsQuery.termsFor(Integer.parseInt(leagueKey.trim()));
        } catch (NumberFormatException e) {
            return leagueNewsQuery.aggregatedTerms();
        }
    }
}
