package com.diyebure.golia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property 3: Win_Percentage está en 0..100 y es 0 sin resueltos.
 *
 * <p>For any list and period, {@code winPercentage} is in {@code 0..100}, and is
 * exactly 0 when there are no FINISHED-with-score predictions inside the period
 * window.</p>
 *
 * <b>Validates: Requirements 5.2, 4.6</b>
 */
class RankingCalculatorProperty3Test {

    private final Ranking_Calculator calculator = RankingCalculatorTestSupport.calculator();

    @Provide
    Arbitrary<List<ScoredPrediction>> predictionLists() {
        return RankingCalculatorTestSupport.predictionLists();
    }

    @Property(tries = 200)
    void winPercentageWithinBounds(
            @ForAll("predictionLists") List<ScoredPrediction> predictions,
            @ForAll Ranking_Period period) {

        long now = RankingCalculatorTestSupport.NOW;
        Ranking_Participant participant = RankingCalculatorTestSupport.participant(predictions);

        int pct = calculator.winPercentage(participant, period, now);
        assertThat(pct).isBetween(0, 100);
    }

    @Property(tries = 200)
    void zeroWhenNoResolvedInPeriod(
            @ForAll("predictionLists") List<ScoredPrediction> predictions,
            @ForAll Ranking_Period period) {

        long now = RankingCalculatorTestSupport.NOW;
        long windowStart = period.windowStartMillis(now);
        Ranking_Participant participant = RankingCalculatorTestSupport.participant(predictions);

        long resolvedInPeriod = predictions.stream()
                .filter(p -> p.getStatus() == MatchStatus.FINISHED
                        && p.getActualHome() != null && p.getActualAway() != null
                        && p.getTimestamp() >= windowStart)
                .count();

        int pct = calculator.winPercentage(participant, period, now);
        if (resolvedInPeriod == 0) {
            assertThat(pct).isZero();
        }
    }
}
