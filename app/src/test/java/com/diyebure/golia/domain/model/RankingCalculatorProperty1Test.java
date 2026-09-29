package com.diyebure.golia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;

/**
 * Property 1: El total del periodo es la suma disjunta de consolidado y live.
 *
 * <p>For any list of {@link ScoredPrediction}, {@code pointsForPeriod} equals the
 * sum of the FINISHED-in-window points plus the LIVE Live_Points, with no
 * prediction contributing to both sets.</p>
 *
 * <b>Validates: Requirements 6.1, 6.4</b>
 */
class RankingCalculatorProperty1Test {

    private final Ranking_Calculator calculator = RankingCalculatorTestSupport.calculator();
    private final Prediction_Scorer scorer = RankingCalculatorTestSupport.scorer();

    @Provide
    Arbitrary<List<ScoredPrediction>> predictionLists() {
        return RankingCalculatorTestSupport.predictionLists();
    }

    @Property(tries = 200)
    void totalEqualsDisjointSumOfConsolidatedAndLive(
            @ForAll("predictionLists") List<ScoredPrediction> predictions,
            @ForAll Ranking_Period period) {

        long now = RankingCalculatorTestSupport.NOW;
        Ranking_Participant participant = RankingCalculatorTestSupport.participant(predictions);
        long windowStart = period.windowStartMillis(now);

        int consolidated = 0;
        int live = 0;
        int contributingPredictions = 0;

        for (ScoredPrediction p : predictions) {
            boolean hasScore = p.getActualHome() != null && p.getActualAway() != null;
            if (p.getStatus() == MatchStatus.FINISHED
                    && hasScore
                    && p.getTimestamp() >= windowStart) {
                consolidated += score(p);
                contributingPredictions++;
            } else if (p.getStatus() == MatchStatus.LIVE && hasScore) {
                live += score(p);
                contributingPredictions++;
            }
        }

        int actual = calculator.pointsForPeriod(participant, period, now);

        // The total is exactly the disjoint sum of the two sets.
        assertThat(actual).isEqualTo(consolidated + live);

        // Disjointness: each contributing prediction was counted in exactly one
        // of the two sets (a prediction is either FINISHED or LIVE, never both).
        long finishedContrib = predictions.stream()
                .filter(p -> p.getStatus() == MatchStatus.FINISHED
                        && p.getActualHome() != null && p.getActualAway() != null
                        && p.getTimestamp() >= windowStart)
                .count();
        long liveContrib = predictions.stream()
                .filter(p -> p.getStatus() == MatchStatus.LIVE
                        && p.getActualHome() != null && p.getActualAway() != null)
                .count();
        assertThat(finishedContrib + liveContrib).isEqualTo(contributingPredictions);
    }

    private int score(ScoredPrediction p) {
        return scorer.score(p.getPredictedHome(), p.getPredictedAway(),
                p.getActualHome(), p.getActualAway());
    }
}
