package com.diyebure.golia.data.repository;

import com.diyebure.golia.domain.model.RankingResult;
import com.diyebure.golia.domain.model.RankingSnapshot;
import com.diyebure.golia.domain.model.Ranking_Calculator;
import com.diyebure.golia.domain.model.Ranking_Entry;
import com.diyebure.golia.domain.model.Ranking_Participant;
import com.diyebure.golia.domain.model.Ranking_Period;
import com.diyebure.golia.domain.model.ScoredPrediction;
import com.diyebure.golia.domain.repository.RankingDataSource;
import com.diyebure.golia.domain.usecase.Ranking_Participant_Provider;

import java.util.List;

import javax.inject.Inject;

/**
 * Real {@link RankingDataSource} that derives the current user's ranking metrics
 * from the same data that feeds the ranking screen: the real registered users
 * read from Room and their scored predictions.
 *
 * <p>It reuses {@link Ranking_Participant_Provider} (real users + real
 * predictions) and the pure {@link Ranking_Calculator} to compute the global
 * ({@link Ranking_Period#TOTAL}) ranking, then extracts the current user's row
 * to build the {@link RankingSnapshot} shown in the Inicio welcome card and the
 * Perfil "Historial" section (R11).</p>
 *
 * <p>Returns {@code null} when there is no active session, the user is not found,
 * or the user is not present in the ranking, so both screens fall back to their
 * neutral placeholder state without showing an error.</p>
 */
public class RealRankingDataSource implements RankingDataSource {

    private final Ranking_Participant_Provider participantProvider;
    private final Ranking_Calculator calculator;

    @Inject
    public RealRankingDataSource(Ranking_Participant_Provider participantProvider,
                                 Ranking_Calculator calculator) {
        this.participantProvider = participantProvider;
        this.calculator = calculator;
    }

    @Override
    public RankingSnapshot getRankingForCurrentUser() {
        try {
            long now = System.currentTimeMillis();

            Ranking_Participant currentUser = participantProvider.getCurrentUserParticipant();
            if (currentUser == null) {
                return null;
            }

            List<Ranking_Participant> participants = participantProvider.getParticipants(now);
            RankingResult result = calculator.rank(participants, Ranking_Period.TOTAL, now);

            int position = result.getCurrentUserPosition();
            if (position < 1) {
                // The user is not part of the ranking -> neutral state.
                return null;
            }

            Ranking_Entry currentEntry = findCurrentUserEntry(result);
            int points = currentEntry != null ? currentEntry.getPoints() : 0;
            int accuracy = currentEntry != null ? clampPercentage(currentEntry.getWinPercentage()) : 0;
            int predictionsMade = countPredictions(currentUser);

            return new RankingSnapshot(predictionsMade, accuracy, points, position);
        } catch (Exception e) {
            // Never surface an error to the UI (R11.5): degrade to neutral state.
            return null;
        }
    }

    /** Locate the ranking row flagged as the current user, or {@code null}. */
    private Ranking_Entry findCurrentUserEntry(RankingResult result) {
        for (Ranking_Entry entry : result.getEntries()) {
            if (entry != null && entry.isCurrentUser()) {
                return entry;
            }
        }
        return null;
    }

    /** Number of predictions the current user has made (all resolved statuses). */
    private int countPredictions(Ranking_Participant participant) {
        int count = 0;
        for (ScoredPrediction prediction : participant.getPredictions()) {
            if (prediction != null) {
                count++;
            }
        }
        return count;
    }

    /** Keep the percentage inside the {@link RankingSnapshot} invariant [0,100]. */
    private int clampPercentage(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }
}
