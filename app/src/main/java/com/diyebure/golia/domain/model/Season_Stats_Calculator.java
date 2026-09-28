package com.diyebure.golia.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

/**
 * Pure domain calculator that derives per-team season statistics from a list of
 * {@link Match} instances (R3, R11).
 *
 * <p>The calculator is intentionally free of any Android dependency so it can be
 * exercised directly from plain JVM unit tests. It is also Hilt-injectable via a
 * public no-arg {@link Inject} constructor. All methods are pure: they do not
 * mutate their inputs nor keep any state.</p>
 *
 * <p>Only matches with {@link MatchStatus#FINISHED} and non-null
 * {@code homeScore}/{@code awayScore} are considered, evaluating whether the
 * team played as the home side or the away side.</p>
 */
public class Season_Stats_Calculator {

    /** Maximum number of matches considered for the recent-form window. */
    private static final int RECENT_FORM_WINDOW = 5;

    @Inject
    public Season_Stats_Calculator() {
        // No dependencies: pure domain component.
    }

    /**
     * Count the FINISHED matches in which {@code teamId} scored more goals than
     * its opponent, regardless of whether it played at home or away.
     *
     * @param teamId  the team whose wins are counted
     * @param matches the matches to inspect (may contain other teams)
     * @return the number of wins
     */
    public int wins(String teamId, List<Match> matches) {
        int count = 0;
        if (teamId == null || matches == null) {
            return 0;
        }
        for (Match match : matches) {
            if (!isFinishedWithScore(match) || !involvesTeam(match, teamId)) {
                continue;
            }
            int scored = goalsFor(match, teamId);
            int conceded = goalsAgainst(match, teamId);
            if (scored > conceded) {
                count++;
            }
        }
        return count;
    }

    /**
     * Average goals scored FOR {@code teamId} across its FINISHED matches,
     * rounded to one decimal. Returns {@code 0.0} when the team has no such
     * matches (avoids division by zero).
     *
     * @param teamId  the team whose average is computed
     * @param matches the matches to inspect (may contain other teams)
     * @return the average goals for, rounded to one decimal, or {@code 0.0}
     */
    public double goalsAverage(String teamId, List<Match> matches) {
        if (teamId == null || matches == null) {
            return 0.0;
        }
        int totalGoals = 0;
        int played = 0;
        for (Match match : matches) {
            if (!isFinishedWithScore(match) || !involvesTeam(match, teamId)) {
                continue;
            }
            totalGoals += goalsFor(match, teamId);
            played++;
        }
        if (played == 0) {
            return 0.0;
        }
        double average = (double) totalGoals / played;
        return Math.round(average * 10.0) / 10.0;
    }

    /**
     * Count the wins among the team's most recent FINISHED matches.
     *
     * <p>The team's FINISHED matches are ordered by
     * {@link Match#getScheduledDateTime()} descending, using a stable tie-break
     * by {@link Match#getId()} so the result is deterministic. Up to the five
     * most recent matches are considered and classified as V (win) / E (draw) /
     * D (loss) from {@code teamId}'s perspective.</p>
     *
     * @param teamId  the team whose recent form is computed
     * @param matches the matches to inspect (may contain other teams)
     * @return the number of wins (V) among the matches considered
     */
    public int recentForm(String teamId, List<Match> matches) {
        if (teamId == null || matches == null) {
            return 0;
        }

        List<Match> teamMatches = new ArrayList<>();
        for (Match match : matches) {
            if (isFinishedWithScore(match) && involvesTeam(match, teamId)) {
                teamMatches.add(match);
            }
        }

        teamMatches.sort(new Comparator<Match>() {
            @Override
            public int compare(Match a, Match b) {
                int byDate = Long.compare(b.getScheduledDateTime(), a.getScheduledDateTime());
                if (byDate != 0) {
                    return byDate;
                }
                return compareIds(a.getId(), b.getId());
            }
        });

        int limit = Math.min(RECENT_FORM_WINDOW, teamMatches.size());
        int recentWins = 0;
        for (int i = 0; i < limit; i++) {
            Match match = teamMatches.get(i);
            if (goalsFor(match, teamId) > goalsAgainst(match, teamId)) {
                recentWins++;
            }
        }
        return recentWins;
    }

    /**
     * Proportional bar ratio for {@code a} against the total {@code a + b}.
     *
     * <p>Returns {@code a / (a + b)}, or {@code 0} when {@code a + b == 0} (safe
     * against a zero sum). By construction {@code barRatio(a, b) + barRatio(b, a)}
     * equals {@code 1} whenever {@code a + b > 0}.</p>
     *
     * @param a the value whose share is computed
     * @param b the complementary value
     * @return the normalized ratio in {@code [0, 1]}, or {@code 0} for a zero sum
     */
    public double barRatio(double a, double b) {
        double sum = a + b;
        if (sum == 0) {
            return 0.0;
        }
        return a / sum;
    }

    // --- Internal helpers -------------------------------------------------

    private boolean isFinishedWithScore(Match match) {
        return match != null
                && match.getStatus() == MatchStatus.FINISHED
                && match.getHomeScore() != null
                && match.getAwayScore() != null;
    }

    private boolean involvesTeam(Match match, String teamId) {
        return teamId.equals(match.getHomeTeamId()) || teamId.equals(match.getAwayTeamId());
    }

    private int goalsFor(Match match, String teamId) {
        if (teamId.equals(match.getHomeTeamId())) {
            return match.getHomeScore();
        }
        return match.getAwayScore();
    }

    private int goalsAgainst(Match match, String teamId) {
        if (teamId.equals(match.getHomeTeamId())) {
            return match.getAwayScore();
        }
        return match.getHomeScore();
    }

    private int compareIds(java.util.UUID a, java.util.UUID b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }
}
