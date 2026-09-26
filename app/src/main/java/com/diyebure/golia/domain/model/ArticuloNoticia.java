package com.diyebure.golia.domain.model;

import java.util.Objects;

/**
 * Domain model representing a football news article.
 *
 * <p>This is the domain representation of a news article exposing only the fields
 * available in the NewsData.io free plan. It is an immutable, pure-Java class with
 * no Android dependencies, following the domain-layer conventions of the project
 * (e.g. {@code Competition}) but sealed against mutation.</p>
 *
 * <p>Identity is defined by {@link #getArticleId()}, which is the hash of the
 * canonical article URL. Optional fields ({@code description}, {@code imageUrl},
 * {@code sourceName}) may be {@code null} when the provider omits them.</p>
 *
 * <p>Requirements: R3.7, R4.4.</p>
 */
public final class ArticuloNoticia {

    /** Hash of the canonical URL. Identity of the article (required). */
    private final String articleId;

    /** Article title (required). */
    private final String title;

    /** Short description/summary (optional, may be {@code null}). */
    private final String description;

    /** Image URL, expected https (optional, may be {@code null}). */
    private final String imageUrl;

    /** Human-readable source name (optional, may be {@code null}). */
    private final String sourceName;

    /** Canonical article URL, expected https (required). */
    private final String articleUrl;

    /** Publication instant as epoch seconds in UTC. */
    private final long publishedAtEpochUtc;

    /**
     * Creates an immutable news article.
     *
     * @param articleId           hash of the canonical URL (required)
     * @param title               article title (required)
     * @param description         short description (optional, nullable)
     * @param imageUrl            image URL (optional, nullable, https)
     * @param sourceName          source name (optional, nullable)
     * @param articleUrl          canonical article URL (required, https)
     * @param publishedAtEpochUtc publication instant as epoch UTC
     */
    public ArticuloNoticia(String articleId,
                           String title,
                           String description,
                           String imageUrl,
                           String sourceName,
                           String articleUrl,
                           long publishedAtEpochUtc) {
        this.articleId = articleId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sourceName = sourceName;
        this.articleUrl = articleUrl;
        this.publishedAtEpochUtc = publishedAtEpochUtc;
    }

    public String getArticleId() { return articleId; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public String getImageUrl() { return imageUrl; }

    public String getSourceName() { return sourceName; }

    public String getArticleUrl() { return articleUrl; }

    public long getPublishedAtEpochUtc() { return publishedAtEpochUtc; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticuloNoticia that = (ArticuloNoticia) o;
        return Objects.equals(articleId, that.articleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId);
    }

    @Override
    public String toString() {
        return "ArticuloNoticia{" +
                "articleId='" + articleId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", sourceName='" + sourceName + '\'' +
                ", articleUrl='" + articleUrl + '\'' +
                ", publishedAtEpochUtc=" + publishedAtEpochUtc +
                '}';
    }
}
