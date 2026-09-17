package com.diyebure.golia.domain.model;

import java.util.UUID;

/**
 * Domain model representing a football match.
 */
public class Match {
    private UUID id;
    private String externalId;
    private String competitionId;
    private String competitionName;
    private int matchday;
    private String homeTeamId;
    private String homeTeamName;
    private String homeTeamLogoUrl;
    private String awayTeamId;
    private String awayTeamName;
    private String awayTeamLogoUrl;
    private long scheduledDateTime;
    private MatchStatus status;
    private Integer homeScore;
    private Integer awayScore;
    private double homeOdds;
    private double drawOdds;
    private double awayOdds;

    public Match() {
        this.id = UUID.randomUUID();
    }

    public Match(UUID id, String externalId, String competitionId, String competitionName,
                 int matchday, String homeTeamId, String homeTeamName, String homeTeamLogoUrl,
                 String awayTeamId, String awayTeamName, String awayTeamLogoUrl,
                 long scheduledDateTime, MatchStatus status, Integer homeScore, Integer awayScore,
                 double homeOdds, double drawOdds, double awayOdds) {
        this.id = id;
        this.externalId = externalId;
        this.competitionId = competitionId;
        this.competitionName = competitionName;
        this.matchday = matchday;
        this.homeTeamId = homeTeamId;
        this.homeTeamName = homeTeamName;
        this.homeTeamLogoUrl = homeTeamLogoUrl;
        this.awayTeamId = awayTeamId;
        this.awayTeamName = awayTeamName;
        this.awayTeamLogoUrl = awayTeamLogoUrl;
        this.scheduledDateTime = scheduledDateTime;
        this.status = status;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.homeOdds = homeOdds;
        this.drawOdds = drawOdds;
        this.awayOdds = awayOdds;
    }

    // Getters
    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getCompetitionId() { return competitionId; }
    public String getCompetitionName() { return competitionName; }
    public int getMatchday() { return matchday; }
    public String getHomeTeamId() { return homeTeamId; }
    public String getHomeTeamName() { return homeTeamName; }
    public String getHomeTeamLogoUrl() { return homeTeamLogoUrl; }
    public String getAwayTeamId() { return awayTeamId; }
    public String getAwayTeamName() { return awayTeamName; }
    public String getAwayTeamLogoUrl() { return awayTeamLogoUrl; }
    public long getScheduledDateTime() { return scheduledDateTime; }
    public MatchStatus getStatus() { return status; }
    public Integer getHomeScore() { return homeScore; }
    public Integer getAwayScore() { return awayScore; }
    public double getHomeOdds() { return homeOdds; }
    public double getDrawOdds() { return drawOdds; }
    public double getAwayOdds() { return awayOdds; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public void setCompetitionId(String competitionId) { this.competitionId = competitionId; }
    public void setCompetitionName(String competitionName) { this.competitionName = competitionName; }
    public void setMatchday(int matchday) { this.matchday = matchday; }
    public void setHomeTeamId(String homeTeamId) { this.homeTeamId = homeTeamId; }
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }
    public void setHomeTeamLogoUrl(String homeTeamLogoUrl) { this.homeTeamLogoUrl = homeTeamLogoUrl; }
    public void setAwayTeamId(String awayTeamId) { this.awayTeamId = awayTeamId; }
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }
    public void setAwayTeamLogoUrl(String awayTeamLogoUrl) { this.awayTeamLogoUrl = awayTeamLogoUrl; }
    public void setScheduledDateTime(long scheduledDateTime) { this.scheduledDateTime = scheduledDateTime; }
    public void setStatus(MatchStatus status) { this.status = status; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }
    public void setHomeOdds(double homeOdds) { this.homeOdds = homeOdds; }
    public void setDrawOdds(double drawOdds) { this.drawOdds = drawOdds; }
    public void setAwayOdds(double awayOdds) { this.awayOdds = awayOdds; }
}