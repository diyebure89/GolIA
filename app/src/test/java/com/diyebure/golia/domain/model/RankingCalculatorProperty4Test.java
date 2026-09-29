package com.diyebure.golia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property 4: La racha es global, no negativa y se corta con el primer fallo.
 *
 * <p>{@code streak} counts exactly the consecutive correct (score &gt;= 1)
 * FINISHED predictions from the most recent by timestamp, stops at the first 0,
 * is non-negative, and is independent of the selected {@link Ranking_Period}.</p>
 *
 * <b>Validates: Requirements 5.3</b>
 */
class RankingCalculatorProperty4Test {

    private final Ranking_Calculator calculator = RankingCalculatorTestSupport.calculator();
    private final Prediction_Scorer scorer = RankingCalculatorTestSupport.scorer();

    @Provide
    Arbitrary<List<ScoredPrediction>> predictionLists() {
        return RankingCalculatorTestSupport.predictionLists();
    }

    @Property(tries = 200)
    void streakMatchesReferenceAndIsNonNegativeAndPeriodIndependent(
            @ForAll("predictionLists") List<ScoredPrediction> predictions,
            @ForAll Ranking_Period period) {

        Ranking_Participant participant = RankingCalculatorTestSupport.participant(predictions);

        int actual = calculator.streak(participant);

        // Non-negative.
        assertThat(actual).isGreaterThanOrEqualTo(0);

        // Reference implementation: resolved FINISHED-with-score predictions,
        // sorted most-recent-first, counting consecutive correct until first miss.
        List<ScoredPrediction> resolved = new ArrayList<>();
        for (ScoredPrediction p : predictions) {
            if (p.getStatus() == MatchStatus.FINISHED
                    && p.getActualHome() != null && p.getActualAway() != null) {
                resolved.add(p);
            }
        }
        resolved.sort(Comparator.comparingLong(ScoredPrediction::getTimestamp).reversed());

        int expected = 0;
        for (ScoredPrediction p : resolved) {
            int score = scorer.score(p.getPredictedHome(), p.getPredictedAway(),
                    p.getActualHome(), p.getActualAway());
            if (scorer.isCorrect(score)) {
                expected++;
            } else {
                break;
            }
        }
        assertThat(actual).isEqualTo(expected);

        // Streak is period-independent: the calculator takes no period, so the
        // value is identical regardless of the period we would use elsewhere.
        // (Confirms the API contract: streak(participant) has no period arg.)
        assertThat(calculator.streak(participant)).isEqualTo(actual);
    }
}
