package com.diyebure.golia.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

/**
 * Room entity for the N:M relation between a news article and a league key.
 *
 * <p>A single article may belong to several leagues and to the {@code "all"} feed.
 * The composite primary key is {@code (article_id, league_key)}. An index on
 * {@code league_key} supports per-league listing and purge. Room's default index
 * name for this column is {@code index_news_league_cross_ref_league_key}, matching
 * the {@code MIGRATION_3_4} SQL.</p>
 *
 * <p>Requirements: R6.6, R6.1.</p>
 */
@Entity(
        tableName = "news_league_cross_ref",
        primaryKeys = {"article_id", "league_key"},
        indices = {@Index(value = "league_key")}
)
public class NewsLeagueCrossRefEntity {

    @NonNull
    @ColumnInfo(name = "article_id")
    private String articleId;

    /** League key, e.g. "39", "wc_qualification", "all". */
    @NonNull
    @ColumnInfo(name = "league_key")
    private String leagueKey;

    public NewsLeagueCrossRefEntity() {
        this.articleId = "";
        this.leagueKey = "";
    }

    public NewsLeagueCrossRefEntity(@NonNull String articleId, @NonNull String leagueKey) {
        this.articleId = articleId;
        this.leagueKey = leagueKey;
    }

    // Getters
    @NonNull
    public String getArticleId() { return articleId; }
    @NonNull
    public String getLeagueKey() { return leagueKey; }

    // Setters
    public void setArticleId(@NonNull String articleId) { this.articleId = articleId; }
    public void setLeagueKey(@NonNull String leagueKey) { this.leagueKey = leagueKey; }
}
