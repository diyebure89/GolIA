package com.diyebure.golia.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.diyebure.golia.domain.model.ArticuloNoticia;

/**
 * Room entity for storing a football news article locally.
 *
 * <p>Identity is {@link #getArticleId()}, the hash of the canonical article URL.
 * Optional fields ({@code description}, {@code imageUrl}, {@code sourceName}) may be
 * {@code null} when the provider omits them.</p>
 *
 * <p>Requirements: R6.6, R6.1.</p>
 */
@Entity(tableName = "news_article")
public class NewsArticleEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "article_id")
    private String articleId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "image_url")
    private String imageUrl;

    @ColumnInfo(name = "source_name")
    private String sourceName;

    @ColumnInfo(name = "article_url")
    private String articleUrl;

    @ColumnInfo(name = "published_at_epoch_utc")
    private long publishedAtEpochUtc;

    public NewsArticleEntity() {}

    public NewsArticleEntity(@NonNull String articleId, String title, String description,
                             String imageUrl, String sourceName, String articleUrl,
                             long publishedAtEpochUtc) {
        this.articleId = articleId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sourceName = sourceName;
        this.articleUrl = articleUrl;
        this.publishedAtEpochUtc = publishedAtEpochUtc;
    }

    // Getters
    @NonNull
    public String getArticleId() { return articleId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getSourceName() { return sourceName; }
    public String getArticleUrl() { return articleUrl; }
    public long getPublishedAtEpochUtc() { return publishedAtEpochUtc; }

    // Setters
    public void setArticleId(@NonNull String articleId) { this.articleId = articleId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public void setArticleUrl(String articleUrl) { this.articleUrl = articleUrl; }
    public void setPublishedAtEpochUtc(long publishedAtEpochUtc) { this.publishedAtEpochUtc = publishedAtEpochUtc; }

    /**
     * Convert entity to domain model.
     */
    public ArticuloNoticia toDomainModel() {
        return new ArticuloNoticia(
                articleId,
                title,
                description,
                imageUrl,
                sourceName,
                articleUrl,
                publishedAtEpochUtc
        );
    }

    /**
     * Create entity from domain model.
     */
    public static NewsArticleEntity fromDomainModel(ArticuloNoticia article) {
        return new NewsArticleEntity(
                article.getArticleId(),
                article.getTitle(),
                article.getDescription(),
                article.getImageUrl(),
                article.getSourceName(),
                article.getArticleUrl(),
                article.getPublishedAtEpochUtc()
        );
    }
}
