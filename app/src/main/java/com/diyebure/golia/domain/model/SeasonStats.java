package com.diyebure.golia.domain.model;

/**
 * Immutable domain value object holding the season statistics of a single team,
 * derived from the matches available in cache (R3).
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code wins}: number of FINISHED matches the team won (home or away).</li>
 *   <li>{@code goalsAverage}: average goals scored for the team across its
 *       FINISHED matches, rounded to one decimal ({@code 0.0} when none).</li>
 *   <li>{@code recentWins}: number of wins among the team's most recent
 *       (up to five) FINISHED matches.</li>
 * </ul>
 */
public class SeasonStats {
    private final int wins;
    private final double goalsAverage;
    private final int recentWins;

    public SeasonStats(int wins, double goalsAverage, int recentWins) {
        this.wins = wins;
        this.goalsAverage = goalsAverage;
        this.recentWins = recentWins;
    }

    public int getWins() { return wins; }
    public double getGoalsAverage() { return goalsAverage; }
    public int getRecentWins() { return recentWins; }
}
