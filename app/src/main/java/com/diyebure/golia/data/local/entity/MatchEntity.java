package com.diyebure.golia.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;

/**
 * Room entity for storing match data locally.
 */
@Entity(tableName = "matches")
public class MatchEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "external_id")
    private String externalId;

    @ColumnInfo(name = "competition_id")
    private String competitionId;

    @ColumnInfo(name = "competition_name")
    private String competitionName;

    @ColumnInfo(name = "matchday")
    private int matchday;

    @ColumnInfo(name = "home_team_id")
    private String homeTeamId;

    @ColumnInfo(name = "home_team_name")
    private String homeTeamName;

    @ColumnInfo(name = "home_team_logo_url")
    private String homeTeamLogoUrl;

    @ColumnInfo(name = "away_team_id")
    private String awayTeamId;

    @ColumnInfo(name = "away_team_name")
    private String awayTeamName;

    @ColumnInfo(name = "away_team_logo_url")
    private String awayTeamLogoUrl;

    @ColumnInfo(name = "scheduled_date_time")
    private long scheduledDateTime;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "home_score")
    private Integer homeScore;

    @ColumnInfo(name = "away_score")
    private Integer awayScore;

    @ColumnInfo(name = "home_odds")
    private double homeOdds;

    @ColumnInfo(name = "draw_odds")
    private double drawOdds;

    @ColumnInfo(name = "away_odds")
    private double awayOdds;

    @ColumnInfo(name = "elapsed_minute")
    private Integer elapsedMinute;

    @ColumnInfo(name = "venue_name")
    private String venueName;

    @ColumnInfo(name = "venue_city")
    private String venueCity;

    public MatchEntity() {}

    public MatchEntity(@NonNull String id, String externalId, String competitionId,
                       String competitionName, int matchday, String homeTeamId,
                       String homeTeamName, String homeTeamLogoUrl, String awayTeamId,
                       String awayTeamName, String awayTeamLogoUrl, long scheduledDateTime,
                       String status, Integer homeScore, Integer awayScore, double homeOdds,
                       double drawOdds, double awayOdds) {
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
    @NonNull
    public String getId() { return id; }
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
    public String getStatus() { return status; }
    public Integer getHomeScore() { return homeScore; }
    public Integer getAwayScore() { return awayScore; }
    public double getHomeOdds() { return homeOdds; }
    public double getDrawOdds() { return drawOdds; }
    public double getAwayOdds() { return awayOdds; }
    public Integer getElapsedMinute() { return elapsedMinute; }
    public String getVenueName() { return venueName; }
    public String getVenueCity() { return venueCity; }

    // Setters
    public void setId(@NonNull String id) { this.id = id; }
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
    public void setStatus(String status) { this.status = status; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }
    public void setHomeOdds(double homeOdds) { this.homeOdds = homeOdds; }
    public void setDrawOdds(double drawOdds) { this.drawOdds = drawOdds; }
    public void setAwayOdds(double awayOdds) { this.awayOdds = awayOdds; }
    public void setElapsedMinute(Integer elapsedMinute) { this.elapsedMinute = elapsedMinute; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    public void setVenueCity(String venueCity) { this.venueCity = venueCity; }

    /**
     * Convert entity to domain model.
     */
    public Match toDomainModel() {
        Match match = new Match();
        if (id != null) {
            try {
                match.setId(java.util.UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {}
        }
        match.setExternalId(externalId);
        match.setCompetitionId(competitionId);
        match.setCompetitionName(competitionName);
        match.setMatchday(matchday);
        match.setHomeTeamId(homeTeamId);
        match.setHomeTeamName(homeTeamName);
        match.setHomeTeamLogoUrl(homeTeamLogoUrl);
        match.setAwayTeamId(awayTeamId);
        match.setAwayTeamName(awayTeamName);
        match.setAwayTeamLogoUrl(awayTeamLogoUrl);
        match.setScheduledDateTime(scheduledDateTime);
        if (status != null) {
            try {
                match.setStatus(MatchStatus.valueOf(status));
            } catch (IllegalArgumentException ignored) {}
        }
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setHomeOdds(homeOdds);
        match.setDrawOdds(drawOdds);
        match.setAwayOdds(awayOdds);
        match.setElapsedMinute(elapsedMinute);
        match.setVenueName(venueName);
        match.setVenueCity(venueCity);
        return match;
    }

    /**
     * Create entity from domain model.
     */
    public static MatchEntity fromDomainModel(Match match) {
        MatchEntity entity = new MatchEntity(
            match.getId() != null ? match.getId().toString() : null,
            match.getExternalId(),
            match.getCompetitionId(),
            match.getCompetitionName(),
            match.getMatchday(),
            match.getHomeTeamId(),
            match.getHomeTeamName(),
            match.getHomeTeamLogoUrl(),
            match.getAwayTeamId(),
            match.getAwayTeamName(),
            match.getAwayTeamLogoUrl(),
            match.getScheduledDateTime(),
            match.getStatus() != null ? match.getStatus().name() : null,
            match.getHomeScore(),
            match.getAwayScore(),
            match.getHomeOdds(),
            match.getDrawOdds(),
            match.getAwayOdds()
        );
        entity.setElapsedMinute(match.getElapsedMinute());
        entity.setVenueName(match.getVenueName());
        entity.setVenueCity(match.getVenueCity());
        return entity;
    }
}