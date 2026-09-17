package com.diyebure.golia.domain.model;

import java.util.UUID;

/**
 * Domain model representing a football team.
 */
public class Team {
    private UUID id;
    private String name;
    private String logoUrl;
    private String country;

    public Team() {
        this.id = UUID.randomUUID();
    }

    public Team(UUID id, String name, String logoUrl, String country) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.country = country;
    }

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getLogoUrl() { return logoUrl; }
    public String getCountry() { return country; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public void setCountry(String country) { this.country = country; }
}