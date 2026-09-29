package com.diyebure.golia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property 7: Un pronóstico sin Match en caché no participa.
 *
 * <p>{@code Ranking_Calculator} operates on {@link ScoredPrediction} lists. The
 * provider ({@code Ranking_Participant_Provider}) discards a prediction whose
 * match is not resolved (absent from cache) by leaving it out of the
 * participant's list. This property models that case as the prediction being
 * absent and asserts it contributes nothing to points, percentage, or streak:
 * a participant built from a base list is indistinguishable from one where the
 * unresolved prediction was dropped.</p>
 *
 * <b>Validates: Requirements 4.5</b>
 */
class RankingCalculatorProperty7Test {

    private final Ranking_Calculator calculator = RankingCalculatorTestSupport.calculator();

    @Provide
    Arbitrary<List<ScoredPrediction>> baseLists() {
        return RankingCalculatorTestSupport.scoredPredictions().list().ofMaxSize(12);
    }

    /**
     * A prediction whose match was not resolved: no actual score. The provider
     * would never emit such a prediction (it is dropped). We assert that adding
     * it changes nothing, which is the calculator-side guarantee of the
     * provider's discard behaviour.
     */
    @Provide
    Arbitrary<ScoredPrediction> unresolvedPrediction() {
        // No score available, regardless of status (match absent from cache).
        return Combinators.combine(
                        RankingCalculatorTestSupport.goals(),
                        RankingCalculatorTestSupport.goals(),
                        RankingCalculatorTestSupport.timestamps(),
                        RankingCalculatorTestSupport.anyStatus())
                .as((ph, pa, ts, status) ->
                        new ScoredPrediction(ph, pa, null, null, status, ts));
    }

    @Property(tries = 200)
    void unresolvedPredictionContributesNothing(
            @ForAll("baseLists") List<ScoredPrediction> base,
            @ForAll("unresolvedPrediction") ScoredPrediction unresolved,
            @ForAll Ranking_Period period) {

        long now = RankingCalculatorTestSupport.NOW;

        Ranking_Participant withoutIt = RankingCalculatorTestSupport.participant(base);

        List<ScoredPrediction> withList = new ArrayList<>(base);
        withList.add(unresolved);
        Ranking_Participant withIt = RankingCalculatorTestSupport.participant(withList);

        assertThat(calculator.pointsForPeriod(withIt, period, now))
                .isEqualTo(calculator.pointsForPeriod(withoutIt, period, now));
        assertThat(calculator.winPercentage(withIt, period, now))
                .isEqualTo(calculator.winPercentage(withoutIt, period, now));
        assertThat(calculator.streak(withIt))
                .isEqualTo(calculator.streak(withoutIt));
    }
}
