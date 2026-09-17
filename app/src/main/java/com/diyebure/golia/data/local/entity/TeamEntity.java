package com.diyebure.golia.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.diyebure.golia.domain.model.Team;

import java.util.UUID;

/**
 * Room entity for storing team data locally.
 */
@Entity(tableName = "teams")
public class TeamEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "logo_url")
    private String logoUrl;

    @ColumnInfo(name = "country")
    private String country;

    public TeamEntity() {}

    public TeamEntity(@NonNull String id, String name, String logoUrl, String country) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.country = country;
    }

    // Getters
    @NonNull
    public String getId() { return id; }
    public String getName() { return name; }
    public String getLogoUrl() { return logoUrl; }
    public String getCountry() { return country; }

    // Setters
    public void setId(@NonNull String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public void setCountry(String country) { this.country = country; }

    /**
     * Convert entity to domain model.
     */
    public Team toDomainModel() {
        Team team = new Team();
        if (id != null) {
            try {
                team.setId(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {}
        }
        team.setName(name);
        team.setLogoUrl(logoUrl);
        team.setCountry(country);
        return team;
    }

    /**
     * Create entity from domain model.
     */
    public static TeamEntity fromDomainModel(Team team) {
        return new TeamEntity(
            team.getId() != null ? team.getId().toString() : null,
            team.getName(),
            team.getLogoUrl(),
            team.getCountry()
        );
    }
}