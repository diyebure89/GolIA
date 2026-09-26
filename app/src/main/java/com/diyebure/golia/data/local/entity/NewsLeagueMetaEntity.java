package com.diyebure.golia.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity storing the last successful fetch instant per league key.
 *
 * <p>Backs the per-league minimum-interval logic of the request budget manager
 * (R6.1/R6.2/R6.3). {@code lastFetchedAtEpochMs} is epoch milliseconds.</p>
 *
 * <p>Requirements: R6.6, R6.1.</p>
 */
@Entity(tableName = "news_league_meta")
public class NewsLeagueMetaEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "league_key")
    private String leagueKey;

    @ColumnInfo(name = "last_fetched_at_epoch_ms")
    private long lastFetchedAtEpochMs;

    public NewsLeagueMetaEntity() {
        this.leagueKey = "";
    }

    public NewsLeagueMetaEntity(@NonNull String leagueKey, long lastFetchedAtEpochMs) {
        this.leagueKey = leagueKey;
        this.lastFetchedAtEpochMs = lastFetchedAtEpochMs;
    }

    // Getters
    @NonNull
    public String getLeagueKey() { return leagueKey; }
    public long getLastFetchedAtEpochMs() { return lastFetchedAtEpochMs; }

    // Setters
    public void setLeagueKey(@NonNull String leagueKey) { this.leagueKey = leagueKey; }
    public void setLastFetchedAtEpochMs(long lastFetchedAtEpochMs) { this.lastFetchedAtEpochMs = lastFetchedAtEpochMs; }
}
