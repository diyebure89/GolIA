package com.diyebure.golia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.jqwik.api.Example;

/**
 * Border example unit tests for {@link Ranking_Calculator}: 0 predictions, total
 * ties, a single participant with data, live 0-0; Win_Percentage Math.round
 * rounding; streak ordered from most recent by timestamp.
 *
 * <p>All tests inject a fixed literal {@code now} for determinism.</p>
 *
 * <b>Validates: Requirements 9.1, 9.2, 9.3, 9.4</b>
 */
class RankingCalculatorBorderTest {

    private static final long NOW = RankingCalculatorTestSupport.NOW;
    private static final long DAY = RankingCalculatorTestSupport.DAY;

    private final Ranking_Calculator calculator = RankingCalculatorTestSupport.calculator();

    // --- 0 predictions ----------------------------------------------------

    @Example
    void zeroPredictionsYieldZeroMetrics() {
        Ranking_Participant empty = RankingCalculatorTestSupport.participant(
                Collections.emptyList());

        assertThat(calculator.pointsForPeriod(empty, Ranking_Period.TOTAL, NOW)).isZero();
        assertThat(calculator.winPercentage(empty, Ranking_Period.TOTAL, NOW)).isZero();
        assertThat(calculator.streak(empty)).isZero();
        assertThat(calculator.hasLive(empty)).isFalse();
    }

    @Example
    void emptyParticipantListRanksToEmptyResult() {
        RankingResult result = calculator.rank(
                Collections.<Ranking_Participant>emptyList(), Ranking_Period.SEMANAL, NOW);
        assertThat(result.getEntries()).isEmpty();
        assertThat(result.getCurrentUserPosition()).isEqualTo(-1);
        assertThat(result.hasLiveAny()).isFalse();
    }

    // --- Single participant with data ------------------------------------

    @Example
    void singleParticipantWithDataIsPositionOne() {
        // 2-1 predicted, 2-1 actual => exact => 19 points.
        ScoredPrediction hit = new ScoredPrediction(2, 1, 2, 1, MatchStatus.FINISHED, NOW - DAY);
        Ranking_Participant only = RankingCalculatorTestSupport.participant(
                "u-1", "Solo", true, RankingCalculatorTestSupport.list(hit));

        RankingResult result = calculator.rank(
                Collections.singletonList(only), Ranking_Period.SEMANAL, NOW);

        assertThat(result.getEntries()).hasSize(1);
        Ranking_Entry entry = result.getEntries().get(0);
        assertThat(entry.getPosition()).isEqualTo(1);
        assertThat(entry.getPoints()).isEqualTo(19);
        assertThat(entry.getWinPercentage()).isEqualTo(100);
        assertThat(entry.getStreak()).isEqualTo(1);
        assertThat(result.getCurrentUserPosition()).isEqualTo(1);
    }

    // --- Total ties: resolved by tie-break -------------------------------

    @Example
    void totalTieBrokenByWinPercentageThenNameThenId() {
        // Two participants with identical points but different win% -> higher wins.
        // A: one exact hit (19 pts, 100%).
        Ranking_Participant a = RankingCalculatorTestSupport.participant(
                "id-a", "Zed", false,
                RankingCalculatorTestSupport.list(
                        new ScoredPrediction(2, 1, 2, 1, MatchStatus.FINISHED, NOW - DAY)));
        // B: an exact hit plus a total miss -> same 19 pts but 50% win rate.
        Ranking_Participant b = RankingCalculatorTestSupport.participant(
                "id-b", "Amy", false,
                RankingCalculatorTestSupport.list(
                        new ScoredPrediction(2, 1, 2, 1, MatchStatus.FINISHED, NOW - DAY),
                        new ScoredPrediction(0, 0, 5, 1, MatchStatus.FINISHED, NOW - 2 * DAY)));

        RankingResult result = calculator.rank(
                Arrays.asList(b, a), Ranking_Period.TOTAL, NOW);

        // A has 19 pts (100%), B has 19 pts (50%) -> A first by win%.
        assertThat(result.getEntries().get(0).getDisplayName()).isEqualTo("Zed");
        assertThat(result.getEntries().get(1).getDisplayName()).isEqualTo("Amy");
    }

    @Example
    void fullTieBrokenByNameCaseInsensitiveThenId() {
        // Identical points AND win% -> break by name case-insensitive, then id.
        ScoredPrediction hit1 = new ScoredPrediction(1, 0, 1, 0, MatchStatus.FINISHED, NOW - DAY);
        Ranking_Participant bob = RankingCalculatorTestSupport.participant(
                "id-2", "bob", false, RankingCalculatorTestSupport.list(hit1));
        Ranking_Participant ana = RankingCalculatorTestSupport.participant(
                "id-1", "Ana", false,
                RankingCalculatorTestSupport.list(
                        new ScoredPrediction(1, 0, 1, 0, MatchStatus.FINISHED, NOW - DAY)));

        RankingResult result = calculator.rank(
                Arrays.asList(bob, ana), Ranking_Period.TOTAL, NOW);

        assertThat(result.getEntries().get(0).getDisplayName()).isEqualTo("Ana");
        assertThat(result.getEntries().get(1).getDisplayName()).isEqualTo("bob");
    }

    // --- Live 0-0 ---------------------------------------------------------

    @Example
    void liveZeroZeroContributesScoreAndSetsLiveFlag() {
        // Predicted 0-0 vs live 0-0 is an exact draw => 19 points, and hasLive true.
        ScoredPrediction liveDraw = new ScoredPrediction(0, 0, 0, 0, MatchStatus.LIVE, NOW);
        Ranking_Participant p = RankingCalculatorTestSupport.participant(
                RankingCalculatorTestSupport.list(liveDraw));

        assertThat(calculator.pointsForPeriod(p, Ranking_Period.SEMANAL, NOW)).isEqualTo(19);
        assertThat(calculator.hasLive(p)).isTrue();
        // But a live prediction is not "resolved" -> win% stays 0.
        assertThat(calculator.winPercentage(p, Ranking_Period.SEMANAL, NOW)).isZero();
    }

    @Example
    void liveWithNullScoreContributesZeroAndNoLiveFlag() {
        ScoredPrediction liveNoScore = new ScoredPrediction(2, 1, null, null, MatchStatus.LIVE, NOW);
        Ranking_Participant p = RankingCalculatorTestSupport.participant(
                RankingCalculatorTestSupport.list(liveNoScore));

        assertThat(calculator.pointsForPeriod(p, Ranking_Period.SEMANAL, NOW)).isZero();
        assertThat(calculator.hasLive(p)).isFalse();
    }

    // --- Win_Percentage Math.round rounding ------------------------------

    @Example
    void winPercentageRoundsHalfUp() {
        // 1 correct out of 3 resolved = 33.33% -> rounds to 33.
        List<ScoredPrediction> preds = new ArrayList<>();
        preds.add(new ScoredPrediction(2, 1, 2, 1, MatchStatus.FINISHED, NOW - DAY)); // exact hit
        preds.add(new ScoredPrediction(0, 0, 4, 1, MatchStatus.FINISHED, NOW - 2 * DAY)); // miss
        preds.add(new ScoredPrediction(0, 0, 5, 2, MatchStatus.FINISHED, NOW - 3 * DAY)); // miss
        Ranking_Participant p = RankingCalculatorTestSupport.participant(preds);
        assertThat(calculator.winPercentage(p, Ranking_Period.SEMANAL, NOW)).isEqualTo(33);
    }

    @Example
    void winPercentageRoundsTwoThirdsTo67() {
        // 2 correct out of 3 = 66.67% -> rounds to 67 (Math.round half-up).
        List<ScoredPrediction> preds = new ArrayList<>();
        preds.add(new ScoredPrediction(2, 1, 2, 1, MatchStatus.FINISHED, NOW - DAY)); // hit
        preds.add(new ScoredPrediction(1, 0, 1, 0, MatchStatus.FINISHED, NOW - 2 * DAY)); // hit
        preds.add(new ScoredPrediction(0, 0, 4, 1, MatchStatus.FINISHED, NOW - 3 * DAY)); // miss
        Ranking_Participant p = RankingCalculatorTestSupport.participant(preds);
        assertThat(calculator.winPercentage(p, Ranking_Period.SEMANAL, NOW)).isEqualTo(67);
    }

    // --- Streak ordered from most recent by timestamp --------------------

    @Example
    void streakCountsFromMostRecentAndStopsAtFirstMiss() {
        // Insert out of chronological order to prove sorting by timestamp desc.
        // Most recent (NOW-DAY): hit; next (NOW-2d): hit; next (NOW-3d): miss;
        // oldest (NOW-4d): hit. Streak from most recent = 2, stops at the miss.
        List<ScoredPrediction> preds = new ArrayList<>();
        preds.add(new ScoredPrediction(0, 0, 4, 1, MatchStatus.FINISHED, NOW - 3 * DAY)); // miss
        preds.add(new ScoredPrediction(2, 1, 2, 1, MatchStatus.FINISHED, NOW - DAY));     // hit (newest)
        preds.add(new ScoredPrediction(1, 0, 1, 0, MatchStatus.FINISHED, NOW - 4 * DAY)); // hit (oldest)
        preds.add(new ScoredPrediction(3, 2, 3, 2, MatchStatus.FINISHED, NOW - 2 * DAY)); // hit
        Ranking_Participant p = RankingCalculatorTestSupport.participant(preds);

        assertThat(calculator.streak(p)).isEqualTo(2);
    }

    @Example
    void streakIsPeriodIndependent() {
        // A hit far in the past (Total-only) still counts toward the global streak.
        List<ScoredPrediction> preds = new ArrayList<>();
        preds.add(new ScoredPrediction(2, 1, 2, 1, MatchStatus.FINISHED, NOW - 100 * DAY));
        preds.add(new ScoredPrediction(1, 0, 1, 0, MatchStatus.FINISHED, NOW - 90 * DAY));
        Ranking_Participant p = RankingCalculatorTestSupport.participant(preds);
        // Even though these fall outside SEMANAL/MENSUAL windows, streak counts them.
        assertThat(calculator.streak(p)).isEqualTo(2);
    }
}
