package com.diyebure.golia.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.diyebure.golia.data.local.entity.NewsArticleEntity;
import com.diyebure.golia.data.local.entity.NewsLeagueCrossRefEntity;
import com.diyebure.golia.data.local.entity.NewsLeagueMetaEntity;

import java.util.List;

/**
 * Data Access Object for the news feed cache (R6.6).
 *
 * <p>Backs the local {@code Cache_Noticias}: articles ({@code news_article}), the
 * N:M article-to-league relation ({@code news_league_cross_ref}) and the per-league
 * last-fetch metadata ({@code news_league_meta}). Methods are synchronous and are
 * expected to be invoked from the {@code @IoExecutor} by {@code NewsRepositoryImpl}.</p>
 *
 * <p>Requirements: R3.10 (listing ordered by publication date DESC), R6.6 (local
 * cache), R6.11 (retention of at most 50 articles per league).</p>
 */
@Dao
public interface NewsArticleDao {

    // --- Upsert: articles ---

    /**
     * Insert or replace a single article (identity is {@code article_id}).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertArticle(NewsArticleEntity article);

    /**
     * Insert or replace multiple articles.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertArticles(List<NewsArticleEntity> articles);

    // --- Upsert: cross-refs ---

    /**
     * Insert or replace a single article-to-league cross-reference.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertCrossRef(NewsLeagueCrossRefEntity crossRef);

    /**
     * Insert or replace multiple article-to-league cross-references.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertCrossRefs(List<NewsLeagueCrossRefEntity> crossRefs);

    // --- Per-league last-fetch metadata ---

    /**
     * Persist the last successful fetch instant for a league (R6.1/R6.2).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setLastFetchedAt(NewsLeagueMetaEntity meta);

    /**
     * Read the last successful fetch instant (epoch ms) for a league.
     *
     * <p>Returns {@code null} when the league has never been refreshed, so callers
     * can distinguish "never fetched" from a genuine {@code 0L} value (R6.1).</p>
     */
    @Nullable
    @Query("SELECT last_fetched_at_epoch_ms FROM news_league_meta WHERE league_key = :leagueKey")
    Long lastFetchedAt(String leagueKey);

    // --- Listing ---

    /**
     * List the articles for a league ordered by publication date descending (R3.10).
     */
    @Query("SELECT a.* FROM news_article a "
            + "INNER JOIN news_league_cross_ref x ON a.article_id = x.article_id "
            + "WHERE x.league_key = :leagueKey "
            + "ORDER BY a.published_at_epoch_utc DESC")
    List<NewsArticleEntity> getArticlesForLeague(String leagueKey);

    /**
     * Count the articles currently referenced by a league.
     */
    @Query("SELECT COUNT(*) FROM news_league_cross_ref WHERE league_key = :leagueKey")
    int countForLeague(String leagueKey);

    // --- Retention / purge ---

    /**
     * Keep at most 50 articles per league, dropping the oldest by publication date (R6.11).
     *
     * <p>Deletes the cross-ref rows for {@code leagueKey} whose article is not among the
     * newest 50 for that league. The article rows themselves are removed separately by
     * {@link #deleteOrphanArticles()} once they are no longer referenced by any league.</p>
     */
    @Query("DELETE FROM news_league_cross_ref "
            + "WHERE league_key = :leagueKey AND article_id NOT IN ("
            + "  SELECT x.article_id FROM news_league_cross_ref x "
            + "  INNER JOIN news_article a ON a.article_id = x.article_id "
            + "  WHERE x.league_key = :leagueKey "
            + "  ORDER BY a.published_at_epoch_utc DESC LIMIT 50)")
    void purgeOldForLeague(String leagueKey);

    /**
     * Remove articles that are no longer referenced by any league cross-ref, keeping
     * {@code news_article} from growing unbounded after purges.
     */
    @Query("DELETE FROM news_article "
            + "WHERE article_id NOT IN (SELECT article_id FROM news_league_cross_ref)")
    void deleteOrphanArticles();
}
