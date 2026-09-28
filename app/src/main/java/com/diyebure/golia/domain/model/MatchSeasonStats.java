package com.diyebure.golia.domain.model;

/**
 * Immutable domain pairing of the {@link SeasonStats} of the home team and the
 * away team of a match, derived from the cached finished matches of each team
 * (R3).
 */
public class MatchSeasonStats {

    private final SeasonStats home;
    private final SeasonStats away;

    public MatchSeasonStats(SeasonStats home, SeasonStats away) {
        this.home = home;
        this.away = away;
    }

    /** Season statistics of the home team. */
    public SeasonStats getHome() {
        return home;
    }

    /** Season statistics of the away team. */
    public SeasonStats getAway() {
        return away;
    }
}
