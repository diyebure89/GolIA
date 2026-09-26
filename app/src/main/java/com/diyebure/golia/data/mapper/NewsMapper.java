package com.diyebure.golia.data.mapper;

import com.diyebure.golia.data.remote.dto.NewsArticleDto;
import com.diyebure.golia.domain.model.ArticuloNoticia;
import com.diyebure.golia.util.CanonicalUrl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

/**
 * Maps NewsData.io provider DTOs ({@link NewsArticleDto}) into the domain model
 * {@link ArticuloNoticia}.
 *
 * <p>Mapping rules (design "NewsMapper (DTO → dominio)", Requirements 3.7, 3.8,
 * 3.11, 4.5, 13.3):</p>
 * <ol>
 *   <li>Preserve title, description, imageUrl, sourceName, articleUrl and the
 *       publication date (R3.7).</li>
 *   <li>Discard articles without a title or without a URL (R3.8, R4.5).</li>
 *   <li>Normalize {@code pubDate} to an epoch (UTC) value; articles whose date is
 *       not parseable are discarded (R3.11).</li>
 *   <li>Promote {@code http}&rarr;{@code https} on the image and article URLs; if
 *       an image URL cannot be promoted it is treated as non-loadable
 *       ({@code null}) (R13.3).</li>
 *   <li>The article identity ({@code articleId}) is the SHA-256 of the canonical
 *       URL computed by {@link CanonicalUrl}.</li>
 * </ol>
 *
 * <p>This class is provider-facing: it only references DTOs and domain types, never
 * Room entities, keeping the provider contract isolated from persistence (R4.3).</p>
 *
 * <p>Requirements: 3.7, 3.8, 3.11, 4.5, 13.3.</p>
 */
public class NewsMapper {

    /**
     * Expected {@code pubDate} format from NewsData.io free plan, interpreted as UTC
     * (design "NewsMapper": {@code "yyyy-MM-dd HH:mm:ss"}). A value that does not
     * parse with this pattern causes the article to be discarded (R3.11).
     */
    private static final DateTimeFormatter PUB_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    @Inject
    public NewsMapper() {
        // No dependencies: mapping is stateless.
    }

    /**
     * Maps a list of provider DTOs to valid domain articles, discarding invalid ones.
     *
     * @param dtos provider article DTOs (may be {@code null})
     * @return a non-null list of valid {@link ArticuloNoticia}; invalid entries
     *         (missing title/URL or non-parseable date) are omitted
     */
    public List<ArticuloNoticia> toDomain(List<NewsArticleDto> dtos) {
        List<ArticuloNoticia> result = new ArrayList<>();
        if (dtos == null) {
            return result;
        }
        for (NewsArticleDto dto : dtos) {
            ArticuloNoticia article = toDomain(dto);
            if (article != null) {
                result.add(article);
            }
        }
        return result;
    }

    /**
     * Maps a single provider DTO to a domain article, or returns {@code null} when the
     * article is invalid and must be discarded (missing title/URL, or non-parseable
     * publication date).
     *
     * @param dto the provider article DTO (may be {@code null})
     * @return the mapped {@link ArticuloNoticia}, or {@code null} when discarded
     */
    public ArticuloNoticia toDomain(NewsArticleDto dto) {
        if (dto == null) {
            return null;
        }

        // R3.8/R4.5: discard articles without a title or without a URL.
        String title = trimToNull(dto.getTitle());
        String rawUrl = trimToNull(dto.getLink());
        if (title == null || rawUrl == null) {
            return null;
        }

        // R3.11: normalize pubDate to epoch UTC; discard when not parseable.
        Long publishedAtEpochUtc = parsePubDate(dto.getPubDate());
        if (publishedAtEpochUtc == null) {
            return null;
        }

        // R13.3: promote http -> https on the article URL. The canonical form of the
        // URL already promotes the scheme, so the identity is scheme-stable.
        String articleUrl = promoteToHttps(rawUrl);
        String articleId = CanonicalUrl.articleId(articleUrl);
        if (articleId == null) {
            return null;
        }

        // R13.3: promote http -> https on the image; non-promotable images are dropped.
        String imageUrl = promoteImageOrNull(trimToNull(dto.getImageUrl()));

        String description = trimToNull(dto.getDescription());
        String sourceName = trimToNull(dto.getSourceName());

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
     * Parses {@code pubDate} as {@code "yyyy-MM-dd HH:mm:ss"} in UTC and returns the
     * epoch seconds, or {@code null} when the value is missing or not parseable (R3.11).
     */
    private Long parsePubDate(String pubDate) {
        String trimmed = trimToNull(pubDate);
        if (trimmed == null) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(trimmed, PUB_DATE_FORMAT);
            return dateTime.toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Promotes an {@code http://} URL to {@code https://}; other schemes are returned
     * unchanged (R13.3).
     */
    private String promoteToHttps(String url) {
        if (url == null) {
            return null;
        }
        if (url.regionMatches(true, 0, "http://", 0, "http://".length())) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    /**
     * Promotes an image URL to https, or returns {@code null} when the image cannot be
     * served over https (treated as non-loadable, R13.3). {@code https} and {@code http}
     * URLs are accepted (the latter promoted); anything else is treated as non-loadable.
     */
    private String promoteImageOrNull(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }
        if (imageUrl.regionMatches(true, 0, "https://", 0, "https://".length())) {
            return imageUrl;
        }
        if (imageUrl.regionMatches(true, 0, "http://", 0, "http://".length())) {
            return promoteToHttps(imageUrl);
        }
        return null;
    }

    /**
     * Trims a string and returns {@code null} when it is {@code null} or blank.
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
