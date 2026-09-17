package com.diyebure.golia.data.remote.dto;

import com.diyebure.golia.data.local.entity.CompetitionEntity;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.data.local.entity.TeamEntity;
import com.diyebure.golia.domain.model.Competition;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.Team;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Objects for Football API responses.
 * Contains DTO classes and mappers for converting between API responses and domain models.
 */
public class MatchDto {

    /**
     * Response wrapper for list of matches.
     */
    public static class MatchResponseDto {
        @SerializedName("matches")
        private List<MatchItemDto> matches;

        @SerializedName("count")
        private int count;

        public List<MatchItemDto> getMatches() {
            return matches != null ? matches : new ArrayList<>();
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Response wrapper for competition list.
     */
    public static class CompetitionResponseDto {
        @SerializedName("competitions")
        private List<CompetitionDto> competitions;

        @SerializedName("count")
        private int count;

        public List<CompetitionDto> getCompetitions() {
            return competitions != null ? competitions : new ArrayList<>();
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Response wrapper for team details.
     */
    public static class TeamResponseDto {
        @SerializedName("team")
        private TeamDto team;

        public TeamDto getTeam() {
            return team;
        }
    }

    /**
     * Data Transfer Object for a single match.
     */
    public static class MatchItemDto {
        @SerializedName("id")
        private String id;

        @SerializedName("external_id")
        private String externalId;

        @SerializedName("competition")
        private CompetitionDto competition;

        @SerializedName("matchday")
        private int matchday;

        @SerializedName("home_team")
        private TeamDto homeTeam;

        @SerializedName("away_team")
        private TeamDto awayTeam;

        @SerializedName("utc_date")
        private String utcDate;

        @SerializedName("status")
        private String status;

        @SerializedName("score")
        private ScoreDto score;

        @SerializedName("odds")
        private OddsDto odds;

        // Getters
        public String getId() { return id; }
        public String getExternalId() { return externalId; }
        public CompetitionDto getCompetition() { return competition; }
        public int getMatchday() { return matchday; }
        public TeamDto getHomeTeam() { return homeTeam; }
        public TeamDto getAwayTeam() { return awayTeam; }
        public String getUtcDate() { return utcDate; }
        public String getStatus() { return status; }
        public ScoreDto getScore() { return score; }
        public OddsDto getOdds() { return odds; }

        /**
         * Convert DTO to domain model.
         */
        public Match toDomainModel() {
            Match match = new Match();
            match.setId(id != null ? UUID.fromString(id) : UUID.randomUUID());
            match.setExternalId(externalId);

            if (competition != null) {
                match.setCompetitionId(competition.getId());
                match.setCompetitionName(competition.getName());
            }

            match.setMatchday(matchday);

            if (homeTeam != null) {
                match.setHomeTeamId(homeTeam.getId());
                match.setHomeTeamName(homeTeam.getName());
                match.setHomeTeamLogoUrl(homeTeam.getCrestUrl());
            }

            if (awayTeam != null) {
                match.setAwayTeamId(awayTeam.getId());
                match.setAwayTeamName(awayTeam.getName());
                match.setAwayTeamLogoUrl(awayTeam.getCrestUrl());
            }

            // Parse UTC date to timestamp
            if (utcDate != null) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    java.util.Date date = sdf.parse(utcDate);
                    match.setScheduledDateTime(date != null ? date.getTime() : 0L);
                } catch (java.text.ParseException e) {
                    match.setScheduledDateTime(0L);
                }
            }

            if (status != null) {
                try {
                    match.setStatus(com.diyebure.golia.domain.model.MatchStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    match.setStatus(com.diyebure.golia.domain.model.MatchStatus.SCHEDULED);
                }
            }

            if (score != null) {
                match.setHomeScore(score.getHomeScore());
                match.setAwayScore(score.getAwayScore());
            }

            if (odds != null) {
                match.setHomeOdds(odds.getHomeWin());
                match.setDrawOdds(odds.getDraw());
                match.setAwayOdds(odds.getAwayWin());
            }

            return match;
        }

        /**
         * Convert DTO to entity for local storage.
         */
        public MatchEntity toEntity() {
            MatchEntity entity = new MatchEntity();
            entity.setId(id != null ? id : UUID.randomUUID().toString());
            entity.setExternalId(externalId);

            if (competition != null) {
                entity.setCompetitionId(competition.getId());
                entity.setCompetitionName(competition.getName());
            }

            entity.setMatchday(matchday);

            if (homeTeam != null) {
                entity.setHomeTeamId(homeTeam.getId());
                entity.setHomeTeamName(homeTeam.getName());
                entity.setHomeTeamLogoUrl(homeTeam.getCrestUrl());
            }

            if (awayTeam != null) {
                entity.setAwayTeamId(awayTeam.getId());
                entity.setAwayTeamName(awayTeam.getName());
                entity.setAwayTeamLogoUrl(awayTeam.getCrestUrl());
            }

            // Parse UTC date to timestamp
            if (utcDate != null) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    java.util.Date date = sdf.parse(utcDate);
                    entity.setScheduledDateTime(date != null ? date.getTime() : 0L);
                } catch (java.text.ParseException e) {
                    entity.setScheduledDateTime(0L);
                }
            }

            entity.setStatus(status);

            if (score != null) {
                entity.setHomeScore(score.getHomeScore());
                entity.setAwayScore(score.getAwayScore());
            }

            if (odds != null) {
                entity.setHomeOdds(odds.getHomeWin());
                entity.setDrawOdds(odds.getDraw());
                entity.setAwayOdds(odds.getAwayWin());
            }

            return entity;
        }
    }

    /**
     * Data Transfer Object for competition information.
     */
    public static class CompetitionDto {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("area")
        private AreaDto area;

        @SerializedName("code")
        private String code;

        @SerializedName("season")
        private String season;

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public AreaDto getArea() { return area; }
        public String getCode() { return code; }
        public String getSeason() { return season; }

        /**
         * Convert DTO to domain model.
         */
        public Competition toDomainModel() {
            Competition competition = new Competition();
            competition.setId(id);
            competition.setName(name);
            competition.setSeason(season);
            if (area != null) {
                competition.setCountry(area.getName());
            }
            return competition;
        }

        /**
         * Convert DTO to entity for local storage.
         */
        public CompetitionEntity toEntity() {
            CompetitionEntity entity = new CompetitionEntity();
            entity.setId(id);
            entity.setName(name);
            entity.setSeason(season);
            if (area != null) {
                entity.setCountry(area.getName());
            }
            return entity;
        }
    }

    /**
     * Data Transfer Object for area/country information.
     */
    public static class AreaDto {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("code")
        private String code;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCode() { return code; }
    }

    /**
     * Data Transfer Object for team information.
     */
    public static class TeamDto {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("short_name")
        private String shortName;

        @SerializedName("tla")
        private String tla;

        @SerializedName("crest_url")
        private String crestUrl;

        @SerializedName("area")
        private AreaDto area;

        @SerializedName("country")
        private String country;

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getShortName() { return shortName; }
        public String getTla() { return tla; }
        public String getCrestUrl() { return crestUrl; }
        public AreaDto getArea() { return area; }
        public String getCountry() { return country; }

        /**
         * Convert DTO to domain model.
         */
        public Team toDomainModel() {
            Team team = new Team();
            team.setId(id != null ? UUID.fromString(id) : UUID.randomUUID());
            team.setName(name);
            team.setLogoUrl(crestUrl);
            if (area != null) {
                team.setCountry(area.getName());
            } else {
                team.setCountry(country);
            }
            return team;
        }

        /**
         * Convert DTO to entity for local storage.
         */
        public TeamEntity toEntity() {
            TeamEntity entity = new TeamEntity();
            entity.setId(id != null ? id : UUID.randomUUID().toString());
            entity.setName(name);
            entity.setLogoUrl(crestUrl);
            if (area != null) {
                entity.setCountry(area.getName());
            } else {
                entity.setCountry(country);
            }
            return entity;
        }
    }

    /**
     * Data Transfer Object for match score.
     */
    public static class ScoreDto {
        @SerializedName("home_team")
        private String homeTeam;

        @SerializedName("away_team")
        private String awayTeam;

        @SerializedName("home_score")
        private Integer homeScore;

        @SerializedName("away_score")
        private Integer awayScore;

        @SerializedName("winner")
        private String winner;

        @SerializedName("duration")
        private String duration;

        public String getHomeTeam() { return homeTeam; }
        public String getAwayTeam() { return awayTeam; }
        public Integer getHomeScore() { return homeScore; }
        public Integer getAwayScore() { return awayScore; }
        public String getWinner() { return winner; }
        public String getDuration() { return duration; }
    }

    /**
     * Data Transfer Object for match odds.
     */
    public static class OddsDto {
        @SerializedName("home_win")
        private double homeWin;

        @SerializedName("draw")
        private double draw;

        @SerializedName("away_win")
        private double awayWin;

        public double getHomeWin() { return homeWin; }
        public double getDraw() { return draw; }
        public double getAwayWin() { return awayWin; }
    }

    /**
     * Mapper class for converting between DTOs and domain models/entities.
     */
    public static class MatchMapper {

        /**
         * Convert list of match DTOs to domain models.
         */
        public static List<Match> toDomainList(List<MatchItemDto> dtos) {
            if (dtos == null) return new ArrayList<>();
            List<Match> matches = new ArrayList<>();
            for (MatchItemDto dto : dtos) {
                matches.add(dto.toDomainModel());
            }
            return matches;
        }

        /**
         * Convert list of match DTOs to entities.
         */
        public static List<MatchEntity> toEntityList(List<MatchItemDto> dtos) {
            if (dtos == null) return new ArrayList<>();
            List<MatchEntity> entities = new ArrayList<>();
            for (MatchItemDto dto : dtos) {
                entities.add(dto.toEntity());
            }
            return entities;
        }

        /**
         * Convert list of competition DTOs to domain models.
         */
        public static List<Competition> competitionToDomainList(List<CompetitionDto> dtos) {
            if (dtos == null) return new ArrayList<>();
            List<Competition> competitions = new ArrayList<>();
            for (CompetitionDto dto : dtos) {
                competitions.add(dto.toDomainModel());
            }
            return competitions;
        }

        /**
         * Convert list of competition DTOs to entities.
         */
        public static List<CompetitionEntity> competitionToEntityList(List<CompetitionDto> dtos) {
            if (dtos == null) return new ArrayList<>();
            List<CompetitionEntity> entities = new ArrayList<>();
            for (CompetitionDto dto : dtos) {
                entities.add(dto.toEntity());
            }
            return entities;
        }

        /**
         * Convert list of team DTOs to domain models.
         */
        public static List<Team> teamToDomainList(List<TeamDto> dtos) {
            if (dtos == null) return new ArrayList<>();
            List<Team> teams = new ArrayList<>();
            for (TeamDto dto : dtos) {
                teams.add(dto.toDomainModel());
            }
            return teams;
        }

        /**
         * Convert list of team DTOs to entities.
         */
        public static List<TeamEntity> teamToEntityList(List<TeamDto> dtos) {
            if (dtos == null) return new ArrayList<>();
            List<TeamEntity> entities = new ArrayList<>();
            for (TeamDto dto : dtos) {
                entities.add(dto.toEntity());
            }
            return entities;
        }
    }
}