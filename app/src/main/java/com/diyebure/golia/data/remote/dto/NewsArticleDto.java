package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for a single article returned by the NewsData.io {@code /news} endpoint.
 *
 * <p>This is the definitive DTO for the provider contract (task 4.1). Field
 * names mirror the NewsData.io JSON keys via {@link SerializedName}. This type
 * is only referenced by the remote and mapper layers and must not leak into the
 * domain or presentation layers.</p>
 */
public class NewsArticleDto {

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("link")
    private String link;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("source_id")
    private String sourceId;

    @SerializedName("source_name")
    private String sourceName;

    @SerializedName("pubDate")
    private String pubDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }
}
