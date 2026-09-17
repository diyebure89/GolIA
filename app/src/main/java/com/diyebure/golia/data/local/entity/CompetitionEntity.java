package com.diyebure.golia.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.diyebure.golia.domain.model.Competition;

/**
 * Room entity for storing competition data locally.
 */
@Entity(tableName = "competitions")
public class CompetitionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "country")
    private String country;

    @ColumnInfo(name = "logo_url")
    private String logoUrl;

    @ColumnInfo(name = "season")
    private String season;

    public CompetitionEntity() {}

    public CompetitionEntity(String id, String name, String country, String logoUrl, String season) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.logoUrl = logoUrl;
        this.season = season;
    }

    // Getters
    @NonNull
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getLogoUrl() { return logoUrl; }
    public String getSeason() { return season; }

    // Setters
    public void setId(@NonNull String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCountry(String country) { this.country = country; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public void setSeason(String season) { this.season = season; }

    /**
     * Convert entity to domain model.
     */
    public Competition toDomainModel() {
        return new Competition(id, name, country, logoUrl, season);
    }

    /**
     * Create entity from domain model.
     */
    public static CompetitionEntity fromDomainModel(Competition competition) {
        return new CompetitionEntity(
            competition.getId(),
            competition.getName(),
            competition.getCountry(),
            competition.getLogoUrl(),
            competition.getSeason()
        );
    }
}