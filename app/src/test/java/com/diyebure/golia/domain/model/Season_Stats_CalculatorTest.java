package com.diyebure.golia.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Required JVM unit tests for {@link Season_Stats_Calculator} (R11.1, R11.2, R11.3).
 *
 * <p>Pure JVM tests (no Android/Robolectric). {@link Match} instances are built via
 * the domain model, setting only the fields the calculator reads: status,
 * home/away team ids, home/away scores, scheduled date-time and id.</p>
 */
public class Season_Stats_CalculatorTest {

    private static final String TEAM = "team-A";
    private static final String RIVAL = "team-B";

    private Season_Stats_Calculator calculator;

    @Before
    public void setUp() {
        calculator = new Season_Stats_Calculator();
    }

    /**
     * Build a FINISHED match with an explicit id and scheduled date-time.
     */
    private Match finished(UUID id, long scheduledDateTime,
                           String homeTeamId, String awayTeamId,
                           int homeScore, int awayScore) {
        Match match = new Match();
        match.setId(id);
        match.setStatus(MatchStatus.FINISHED);
        match.setHomeTeamId(homeTeamId);
        match.setAwayTeamId(awayTeamId);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setScheduledDateTime(scheduledDateTime);
        return match;
    }

    private Match finished(long scheduledDateTime, String homeTeamId, String awayTeamId,
                           int homeScore, int awayScore) {
        return finished(UUID.randomUUID(), scheduledDateTime, homeTeamId, awayTeamId,
                homeScore, awayScore);
    }

    // --- Empty input ----------------------------------------------------------

    @Test
    public void zeroMatches_yieldsZeroStats() {
        List<Match> none = Collections.emptyList();
        assertEquals(0, calculator.wins(TEAM, none));
        assertEquals(0.0, calculator.goalsAverage(TEAM, none), 0.0);
        assertEquals(0, calculator.recentForm(TEAM, none));
    }

    // --- wins -----------------------------------------------------------------

    @Test
    public void wins_countsWinsHomeAndAway() {
        List<Match> matches = Arrays.asList(
                finished(1000L, TEAM, RIVAL, 3, 1),   // home win
                finished(2000L, RIVAL, TEAM, 0, 2),   // away win
                finished(3000L, TEAM, RIVAL, 1, 1),   // draw
                finished(4000L, TEAM, RIVAL, 0, 2)    // loss
        );
        assertEquals(2, calculator.wins(TEAM, matches));
    }

    // --- goalsAverage ---------------------------------------------------------

    @Test
    public void goalsAverage_roundsToOneDecimal() {
        // Goals FOR the team: 2 (home) + 1 (away) + 2 (home) = 5 over 3 matches
        // 5 / 3 = 1.6666... which must round to 1.7.
        List<Match> matches = Arrays.asList(
                finished(1000L, TEAM, RIVAL, 2, 0),
                finished(2000L, RIVAL, TEAM, 4, 1),
                finished(3000L, TEAM, RIVAL, 2, 3)
        );
        assertEquals(1.7, calculator.goalsAverage(TEAM, matches), 0.0001);
    }

    @Test
    public void goalsAverage_emptyIsZero() {
        assertEquals(0.0, calculator.goalsAverage(TEAM, Collections.<Match>emptyList()), 0.0);
    }

    // --- recentForm -----------------------------------------------------------

    @Test
    public void recentForm_fewerThanFiveMatches_countsWinsWithoutError() {
        // 3 FINISHED matches: 2 wins, 1 loss.
        List<Match> matches = Arrays.asList(
                finished(1000L, TEAM, RIVAL, 2, 0),   // win
                finished(2000L, RIVAL, TEAM, 0, 1),   // win (away)
                finished(3000L, TEAM, RIVAL, 0, 1)    // loss
        );
        assertEquals(2, calculator.recentForm(TEAM, matches));
    }

    @Test
    public void recentForm_considersOnlyFiveMostRecentByDateDesc() {
        // Seven FINISHED matches. The five most recent (highest scheduledDateTime)
        // are those at 7000..3000. Older ones (2000, 1000) must be ignored even
        // though they are wins.
        List<Match> matches = new ArrayList<>(Arrays.asList(
                finished(7000L, TEAM, RIVAL, 3, 0),   // recent win
                finished(6000L, TEAM, RIVAL, 0, 1),   // recent loss
                finished(5000L, RIVAL, TEAM, 0, 2),   // recent win (away)
                finished(4000L, TEAM, RIVAL, 1, 1),   // recent draw
                finished(3000L, TEAM, RIVAL, 2, 1),   // recent win  -> 3 wins in window
                finished(2000L, TEAM, RIVAL, 5, 0),   // OLD win (excluded)
                finished(1000L, TEAM, RIVAL, 5, 0)    // OLD win (excluded)
        ));
        // Shuffle-proof: pass in non-sorted order to exercise the internal sort.
        Collections.reverse(matches);
        assertEquals(3, calculator.recentForm(TEAM, matches));
    }

    @Test
    public void recentForm_stableTieBreakByIdWhenSameDate() {
        // Six matches share the SAME scheduledDateTime. The window keeps 5 of them;
        // the tie-break by Match.id decides which single match is dropped. We make
        // the match with the LARGEST id a loss and all others wins. Since the sort
        // is date-desc then id-asc, the largest-id match is ordered last and thus
        // dropped from the 5-match window, leaving 5 wins.
        UUID[] ids = new UUID[6];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = UUID.randomUUID();
        }
        Arrays.sort(ids); // ascending
        UUID largestId = ids[ids.length - 1];

        long sameDate = 5000L;
        List<Match> matches = new ArrayList<>();
        for (UUID id : ids) {
            boolean isLargest = id.equals(largestId);
            // largest-id match is a loss (0-1); the rest are wins (2-0)
            matches.add(finished(id, sameDate, TEAM, RIVAL,
                    isLargest ? 0 : 2, isLargest ? 1 : 0));
        }
        Collections.shuffle(matches);

        // Window of 5 excludes the largest-id (dropped) match -> 5 wins remain.
        assertEquals(5, calculator.recentForm(TEAM, matches));
    }

    // --- barRatio -------------------------------------------------------------

    @Test
    public void barRatio_complementarySumsToOneWhenPositive() {
        double a = 3.0;
        double b = 7.0;
        assertEquals(1.0, calculator.barRatio(a, b) + calculator.barRatio(b, a), 0.0001);
        assertEquals(0.3, calculator.barRatio(a, b), 0.0001);
    }

    @Test
    public void barRatio_bothZeroWhenSumIsZero() {
        assertEquals(0.0, calculator.barRatio(0.0, 0.0), 0.0);
        // barRatio(a,b) + barRatio(b,a) is 0, not 1, when the sum is zero.
        assertTrue(calculator.barRatio(0.0, 0.0) + calculator.barRatio(0.0, 0.0) == 0.0);
    }
}
