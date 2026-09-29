package com.diyebure.golia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property 2: Live sin marcador aporta cero y no falla.
 *
 * <p>Any LIVE {@link ScoredPrediction} with a null {@code actualHome} or
 * {@code actualAway} contributes 0 Live_Points and never throws.</p>
 *
 * <b>Validates: Requirements 6.3</b>
 */
class RankingCalculatorProperty2Test {

    private final Ranking_Calculator calculator = RankingCalculatorTestSupport.calculator();

    /** LIVE predictions where at least one of the actual scores is null. */
    @Provide
    Arbitrary<ScoredPrediction> liveWithoutScore() {
        Arbitrary<Integer> present = RankingCalculatorTestSupport.goals();
        Arbitrary<Integer> nullOnly = Arbitraries.just(null);
        Arbitrary<Integer> maybe = Arbitraries.oneOf(present, nullOnly);

        // Ensure at least one of home/away is null by pairing a null with a maybe
        // in both orders.
        Arbitrary<ScoredPrediction> homeNull = Combinators.combine(
                        RankingCalculatorTestSupport.goals(),
                        RankingCalculatorTestSupport.goals(),
                        nullOnly, maybe,
                        RankingCalculatorTestSupport.timestamps())
                .as((ph, pa, ah, aa, ts) ->
                        new ScoredPrediction(ph, pa, ah, aa, MatchStatus.LIVE, ts));
        Arbitrary<ScoredPrediction> awayNull = Combinators.combine(
                        RankingCalculatorTestSupport.goals(),
                        RankingCalculatorTestSupport.goals(),
                        maybe, nullOnly,
                        RankingCalculatorTestSupport.timestamps())
                .as((ph, pa, ah, aa, ts) ->
                        new ScoredPrediction(ph, pa, ah, aa, MatchStatus.LIVE, ts));
        return Arbitraries.oneOf(homeNull, awayNull);
    }

    @Property(tries = 200)
    void liveWithoutScoreContributesZeroAndNeverThrows(
            @ForAll("liveWithoutScore") ScoredPrediction live,
            @ForAll Ranking_Period period) {

        long now = RankingCalculatorTestSupport.NOW;

        Ranking_Participant onlyLive = RankingCalculatorTestSupport.participant(
                RankingCalculatorTestSupport.list(live));

        // Never throws.
        assertThatCode(() -> calculator.pointsForPeriod(onlyLive, period, now))
                .doesNotThrowAnyException();

        // Contributes exactly 0.
        assertThat(calculator.pointsForPeriod(onlyLive, period, now)).isZero();
        assertThat(calculator.hasLive(onlyLive)).isFalse();
    }

    @Property(tries = 200)
    void liveWithoutScoreAddsNothingToOtherPredictions(
            @ForAll("liveWithoutScore") ScoredPrediction live,
            @ForAll("resolvedList") List<ScoredPrediction> others,
            @ForAll Ranking_Period period) {

        long now = RankingCalculatorTestSupport.NOW;

        Ranking_Participant withoutLive = RankingCalculatorTestSupport.participant(others);
        List<ScoredPrediction> withLiveList = new java.util.ArrayList<>(others);
        withLiveList.add(live);
        Ranking_Participant withLive = RankingCalculatorTestSupport.participant(withLiveList);

        assertThat(calculator.pointsForPeriod(withLive, period, now))
                .isEqualTo(calculator.pointsForPeriod(withoutLive, period, now));
    }

    @Provide
    Arbitrary<List<ScoredPrediction>> resolvedList() {
        return RankingCalculatorTestSupport.finishedResolved().list().ofMaxSize(10);
    }
}
