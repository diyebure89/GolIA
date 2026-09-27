package com.diyebure.golia.data.repository;

import com.diyebure.golia.domain.model.RankingSnapshot;
import com.diyebure.golia.domain.repository.RankingDataSource;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Placeholder implementation of {@link RankingDataSource} that returns fixed metrics
 * until the real ranking is available (R11.5, R11.6).
 *
 * <p>The values are identical, field by field, to the placeholder statistics rendered
 * in the Inicio welcome card ({@code InicioFragment.bindPlaceholderData}), so both
 * screens show the same numbers through the same Hilt binding (R11.3, R11.4):
 * <ul>
 *   <li>pronósticos {@code 48} → {@code predictionsMade = 48}</li>
 *   <li>aciertos {@code 67%} → {@code accuracyPercentage = 67}</li>
 *   <li>puntos {@code 1 250} → {@code totalPoints = 1250}</li>
 *   <li>ranking {@code #142} → {@code rankingPosition = 142}</li>
 * </ul>
 *
 * <p>Injected as an application-scoped singleton via Hilt (constructor injection).
 */
@Singleton
public class PlaceholderRankingDataSource implements RankingDataSource {

    /** Pronósticos placeholder shown in Inicio ("48"). */
    private static final int PREDICTIONS_MADE = 48;

    /** Aciertos placeholder shown in Inicio ("67%"). */
    private static final float ACCURACY_PERCENTAGE = 67f;

    /** Puntos placeholder shown in Inicio ("1 250"). */
    private static final int TOTAL_POINTS = 1250;

    /** Ranking placeholder shown in Inicio ("#142"). */
    private static final int RANKING_POSITION = 142;

    @Inject
    public PlaceholderRankingDataSource() {
        // No dependencies; Hilt constructs the singleton via constructor injection.
    }

    @Override
    public RankingSnapshot getRankingForCurrentUser() {
        return new RankingSnapshot(
                PREDICTIONS_MADE, ACCURACY_PERCENTAGE, TOTAL_POINTS, RANKING_POSITION);
    }
}
