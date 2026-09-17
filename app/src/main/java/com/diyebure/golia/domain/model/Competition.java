package com.diyebure.golia.domain.model;

/**
 * Domain model representing a football competition.
 */
public class Competition {
    private String id;
    private String name;
    private String country;
    private String logoUrl;
    private String season;

    public Competition() {}

    public Competition(String id, String name, String country, String logoUrl, String season) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.logoUrl = logoUrl;
        this.season = season;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getLogoUrl() { return logoUrl; }
    public String getSeason() { return season; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCountry(String country) { this.country = country; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public void setSeason(String season) { this.season = season; }
}