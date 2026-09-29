package com.diyebure.golia.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

/**
 * Shared jqwik generators and helpers for the {@code Ranking_Calculator}
 * property tests. Keeps the individual property files focused on the assertion
 * while centralising the intelligent generators that constrain inputs to the
 * relevant space (scores in a small range, statuses, timestamps around a fixed
 * {@code now}).
 *
 * <p>All tests inject a fixed literal {@code now} for determinism.</p>
 */
final class RankingCalculatorTestSupport {

    /** Fixed reference instant used across property tests (2024-01-01T00:00:00Z). */
    static final long NOW = 1_704_067_200_000L;

    /** Milliseconds in a day. */
    static final long DAY = 24L * 60L * 60L * 1000L;

    private RankingCalculatorTestSupport() {
    }

    /** Small goal counts keep the score space realistic while covering hits and misses. */
    static Arbitrary<Integer> goals() {
        return Arbitraries.integers().between(0, 5);
    }

    /** Nullable actual goals: mostly present, occasionally null (live-without-score). */
    static Arbitrary<Integer> nullableGoals() {
        return Arbitraries.oneOf(
                goals(),
                Arbitraries.just(null));
    }

    static Arbitrary<MatchStatus> anyStatus() {
        return Arbitraries.of(MatchStatus.values());
    }

    /** Timestamps spread from ~120 days in the past to a little into the future. */
    static Arbitrary<Long> timestamps() {
        return Arbitraries.longs().between(NOW - 120L * DAY, NOW + 2L * DAY);
    }

    /**
     * A fully arbitrary {@link ScoredPrediction}: any status, nullable actual
     * score, timestamp around {@code now}.
     */
    static Arbitrary<ScoredPrediction> scoredPredictions() {
        return Combinators.combine(
                        goals(), goals(), nullableGoals(), nullableGoals(),
                        anyStatus(), timestamps())
                .as(ScoredPrediction::new);
    }

    /** A FINISHED prediction that always carries a score (a "resolved" prediction). */
    static Arbitrary<ScoredPrediction> finishedResolved() {
        return Combinators.combine(goals(), goals(), goals(), goals(), timestamps())
                .as((ph, pa, ah, aa, ts) ->
                        new ScoredPrediction(ph, pa, ah, aa, MatchStatus.FINISHED, ts));
    }

    static Arbitrary<List<ScoredPrediction>> predictionLists() {
        return scoredPredictions().list().ofMaxSize(20);
    }

    static Ranking_Participant participant(String id, String name, boolean isCurrentUser,
                                           List<ScoredPrediction> predictions) {
        return new Ranking_Participant(id, name, null, isCurrentUser, predictions);
    }

    static Ranking_Participant participant(List<ScoredPrediction> predictions) {
        return participant("p-1", "Player", false, predictions);
    }

    static List<ScoredPrediction> list(ScoredPrediction... predictions) {
        return new ArrayList<>(Arrays.asList(predictions));
    }

    static Prediction_Scorer scorer() {
        return new Prediction_Scorer();
    }

    static Ranking_Calculator calculator() {
        return new Ranking_Calculator(scorer());
    }
}
