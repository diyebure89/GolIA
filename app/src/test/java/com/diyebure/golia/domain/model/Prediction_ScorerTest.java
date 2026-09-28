package com.diyebure.golia.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Required JVM unit tests for {@link Prediction_Scorer} (R11.1, R11.2, R11.3).
 *
 * <p>Pure JVM tests (no Android/Robolectric). Expected values are computed by
 * applying the {@code Prediction_Scoring} rule:
 * +5 outcome (1X2), +2 home exact, +2 away exact, +1 difference, +9 full exact,
 * maximum 19.</p>
 */
public class Prediction_ScorerTest {

    private Prediction_Scorer scorer;

    @Before
    public void setUp() {
        scorer = new Prediction_Scorer();
    }

    // --- Isolated / combined scoring components -------------------------------

    /**
     * Winner (1X2) matches but nothing else: exact goals differ AND the goal
     * difference differs, so only the +5 outcome component applies.
     * Predicted 1-0 (HOME_WIN, diff 1) vs actual 3-1 (HOME_WIN, diff 2).
     */
    @Test
    public void winnerOnly_scores5() {
        assertEquals(5, scorer.score(1, 0, 3, 1));
    }

    /**
     * Exactly one team's goals match (away), while outcome and difference differ,
     * so only the +2 away-exact component applies.
     * Predicted 0-0 (DRAW, diff 0) vs actual 3-0 (HOME_WIN, diff 3): away 0==0.
     */
    @Test
    public void oneTeamExact_awayOnly_scores2() {
        assertEquals(2, scorer.score(0, 0, 3, 0));
    }

    /**
     * Exactly one team's goals match (home), while outcome and difference differ,
     * so only the +2 home-exact component applies.
     * Predicted 3-0 (HOME_WIN, diff 3) vs actual 3-5 (AWAY_WIN, diff -2): home 3==3.
     */
    @Test
    public void oneTeamExact_homeOnly_scores2() {
        assertEquals(2, scorer.score(3, 0, 3, 5));
    }

    /**
     * The goal-difference component (+1) can only co-occur with the outcome (+5),
     * because an equal difference implies the same 1X2 sign. This isolates the
     * difference contribution as the +1 delta over the winner-only case (no exact
     * goals): outcome(5) + difference(1) = 6.
     * Predicted 2-1 (HOME_WIN, diff 1) vs actual 3-2 (HOME_WIN, diff 1); no exact.
     */
    @Test
    public void outcomePlusDifference_noExact_scores6() {
        assertEquals(6, scorer.score(2, 1, 3, 2));
    }

    /**
     * Matching one team's exact goals in a winning prediction: outcome(5) +
     * home-exact(2) + difference... check difference does NOT match here.
     * Predicted 2-0 (HOME_WIN, diff 2) vs actual 2-1 (HOME_WIN, diff 1):
     * outcome(5) + home 2==2 (2) = 7 (away 0!=1, diff 2!=1, not full exact).
     */
    @Test
    public void outcomePlusHomeExact_scores7() {
        assertEquals(7, scorer.score(2, 0, 2, 1));
    }

    /**
     * Full exact score: every component applies.
     * Predicted 2-1 vs actual 2-1 = 5 + 2 + 2 + 1 + 9 = 19 (the maximum).
     */
    @Test
    public void exactFullScore_scores19() {
        assertEquals(19, scorer.score(2, 1, 2, 1));
    }

    /**
     * Total miss: wrong outcome, no exact goals, wrong difference.
     * Predicted 0-5 (AWAY_WIN, diff -5) vs actual 3-0 (HOME_WIN, diff 3) = 0.
     */
    @Test
    public void totalMiss_scores0() {
        assertEquals(0, scorer.score(0, 5, 3, 0));
    }

    /** The score is always bounded within 0..19 (verified at both extremes). */
    @Test
    public void scoreIsBoundedBetween0And19() {
        int miss = scorer.score(0, 5, 3, 0);
        int perfect = scorer.score(2, 1, 2, 1);
        assertTrue("score >= 0", miss >= 0);
        assertTrue("score <= 19", perfect <= 19);
        assertEquals(0, miss);
        assertEquals(19, perfect);
    }

    // --- isCorrect ------------------------------------------------------------

    @Test
    public void isCorrect_isFalseForZero() {
        assertFalse(scorer.isCorrect(0));
    }

    @Test
    public void isCorrect_isTrueForOne() {
        assertTrue(scorer.isCorrect(1));
    }

    @Test
    public void isCorrect_isTrueForMaximum() {
        assertTrue(scorer.isCorrect(19));
    }

    // --- deriveOutcome --------------------------------------------------------

    @Test
    public void deriveOutcome_homeGreater_isHomeWin() {
        assertEquals(PredictionOutcome.HOME_WIN, scorer.deriveOutcome(2, 1));
    }

    @Test
    public void deriveOutcome_equal_isDraw() {
        assertEquals(PredictionOutcome.DRAW, scorer.deriveOutcome(1, 1));
    }

    @Test
    public void deriveOutcome_homeLess_isAwayWin() {
        assertEquals(PredictionOutcome.AWAY_WIN, scorer.deriveOutcome(0, 2));
    }
}
