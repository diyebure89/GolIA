package com.diyebure.golia.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for a single item of the API-Football {@code /fixtures}
 * {@code response[]} array, grouping fixture, league, teams and goals data.
 */
public class FixtureItemDto {

    @SerializedName("fixture")
    private FixtureDto fixture;

    @SerializedName("league")
    private LeagueDto league;

    @SerializedName("teams")
    private TeamsDto teams;

    @SerializedName("goals")
    private GoalsDto goals;

    public FixtureDto getFixture() {
        return fixture;
    }

    public void setFixture(FixtureDto fixture) {
        this.fixture = fixture;
    }

    public LeagueDto getLeague() {
        return league;
    }

    public void setLeague(LeagueDto league) {
        this.league = league;
    }

    public TeamsDto getTeams() {
        return teams;
    }

    public void setTeams(TeamsDto teams) {
        this.teams = teams;
    }

    public GoalsDto getGoals() {
        return goals;
    }

    public void setGoals(GoalsDto goals) {
        this.goals = goals;
    }
}
