package com.diyebure.golia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property 6: El filtro de periodo respeta la ventana temporal.
 *
 * <p>For any timestamp and now, a FINISHED {@link ScoredPrediction} counts for
 * SEMANAL iff its timestamp is within the last 7 days of now (30 for MENSUAL),
 * and always for TOTAL. Verified through {@code pointsForPeriod}: a single
 * scoring FINISHED prediction contributes iff it is inside the window.</p>
 *
 * <b>Validates: Requirements 4.2, 4.4</b>
 */
class RankingCalculatorProperty6Test {

    private final Ranking_Calculator calculator = RankingCalculatorTestSupport.calculator();
    private final Prediction_Scorer scorer = RankingCalculatorTestSupport.scorer();

    /** Timestamps from 60 days before now up to now. */
    @Provide
    Arbitrary<Long> pastTimestamps() {
        long now = RankingCalculatorTestSupport.NOW;
        long day = RankingCalculatorTestSupport.DAY;
        return Arbitraries.longs().between(now - 60L * day, now);
    }

    @Property(tries = 300)
    void finishedCountsForPeriodIffInsideWindow(
            @ForAll("pastTimestamps") long timestamp,
            @ForAll Ranking_Period period) {

        long now = RankingCalculatorTestSupport.NOW;

        // A guaranteed-scoring exact prediction (2-1 predicted, 2-1 actual => 19 pts).
        ScoredPrediction p = new ScoredPrediction(2, 1, 2, 1, MatchStatus.FINISHED, timestamp);
        int predictionScore = scorer.score(2, 1, 2, 1);
        assertThat(predictionScore).isGreaterThan(0);

        Ranking_Participant participant =
                RankingCalculatorTestSupport.participant(
                        RankingCalculatorTestSupport.list(p));

        int points = calculator.pointsForPeriod(participant, period, now);

        boolean insideWindow;
        switch (period) {
            case SEMANAL:
                insideWindow = timestamp >= now - 7L * RankingCalculatorTestSupport.DAY;
                break;
            case MENSUAL:
                insideWindow = timestamp >= now - 30L * RankingCalculatorTestSupport.DAY;
                break;
            case TOTAL:
            default:
                insideWindow = true;
                break;
        }

        if (insideWindow) {
            assertThat(points).isEqualTo(predictionScore);
        } else {
            assertThat(points).isZero();
        }
    }

    @Property(tries = 100)
    void totalAlwaysCounts(@ForAll("pastTimestamps") long timestamp) {
        long now = RankingCalculatorTestSupport.NOW;
        ScoredPrediction p = new ScoredPrediction(2, 1, 2, 1, MatchStatus.FINISHED, timestamp);
        Ranking_Participant participant =
                RankingCalculatorTestSupport.participant(
                        RankingCalculatorTestSupport.list(p));
        assertThat(calculator.pointsForPeriod(participant, Ranking_Period.TOTAL, now))
                .isEqualTo(scorer.score(2, 1, 2, 1));
    }
}
