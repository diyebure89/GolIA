package com.diyebure.golia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

/**
 * Pure domain calculator that derives the ranking from a list of
 * {@link Ranking_Participant} using the exact-score rule implemented by
 * {@link Prediction_Scorer} (R3, R4, R5, R6, R9).
 *
 * <p>The calculator is intentionally free of any Android/Room dependency so it
 * can be exercised directly from plain JVM unit tests. It is Hilt-injectable via
 * a public {@link Inject} constructor that receives the shared
 * {@link Prediction_Scorer}. All methods are pure: they do not mutate their
 * inputs nor keep any state, and {@code now} is always supplied by the caller so
 * the computation is deterministic.</p>
 *
 * <p>Point derivation is split into two <em>disjoint</em> sets by match status
 * (R6.1): consolidated points come from {@link MatchStatus#FINISHED} predictions
 * whose {@code timestamp} falls inside the period window, while
 * {@code Live_Points} come from {@link MatchStatus#LIVE} predictions whose actual
 * score is present. A live match with a {@code null} score contributes {@code 0}
 * without failing (R6.3).</p>
 */
public class Ranking_Calculator {

    private final Prediction_Scorer scorer;

    @Inject
    public Ranking_Calculator(Prediction_Scorer scorer) {
        this.scorer = scorer;
    }

    /**
     * Total points for a participant in a given period: consolidated points of
     * the {@link MatchStatus#FINISHED} predictions inside the window plus the
     * {@code Live_Points} of the {@link MatchStatus#LIVE} predictions (R6.1, R6.4).
     *
     * @param participant the participant whose points are summed
     * @param period      the selected period
     * @param now         current instant as epoch millis
     * @return total points in the period (never negative)
     */
    public int pointsForPeriod(Ranking_Participant participant, Ranking_Period period, long now) {
        if (participant == null || period == null) {
            return 0;
        }
        long windowStart = period.windowStartMillis(now);
        int total = 0;
        for (ScoredPrediction prediction : participant.getPredictions()) {
            if (prediction == null) {
                continue;
            }
            MatchStatus status = prediction.getStatus();
            if (status == MatchStatus.FINISHED) {
                if (isWithinWindow(prediction.getTimestamp(), windowStart)
                        && hasScore(prediction)) {
                    total += scoreOf(prediction);
                }
            } else if (status == MatchStatus.LIVE) {
                // Live_Points ignore the period window and contribute only when a
                // score is available; a null score contributes 0 (R6.2, R6.3).
                if (hasScore(prediction)) {
                    total += scoreOf(prediction);
                }
            }
        }
        return total;
    }

    /**
     * Win percentage in the period computed over the resolved
     * ({@link MatchStatus#FINISHED}) predictions inside the window:
     * {@code Math.round(correct / resolved * 100)}. Returns {@code 0} when there
     * are no resolved predictions in the period (R5.2, R4.6).
     *
     * @param participant the participant whose percentage is computed
     * @param period      the selected period
     * @param now         current instant as epoch millis
     * @return win percentage in the range {@code 0..100}
     */
    public int winPercentage(Ranking_Participant participant, Ranking_Period period, long now) {
        if (participant == null || period == null) {
            return 0;
        }
        long windowStart = period.windowStartMillis(now);
        int resolved = 0;
        int correct = 0;
        for (ScoredPrediction prediction : participant.getPredictions()) {
            if (prediction == null
                    || prediction.getStatus() != MatchStatus.FINISHED
                    || !hasScore(prediction)
                    || !isWithinWindow(prediction.getTimestamp(), windowStart)) {
                continue;
            }
            resolved++;
            if (scorer.isCorrect(scoreOf(prediction))) {
                correct++;
            }
        }
        if (resolved == 0) {
            return 0;
        }
        return (int) Math.round((double) correct / resolved * 100.0);
    }

    /**
     * Global (period-independent) streak: the number of consecutive
     * {@link MatchStatus#FINISHED} predictions with {@code score >= 1} counted
     * from the most recent one (by {@code timestamp} descending) backwards; it
     * stops at the first resolved prediction scoring {@code 0} (R5.3).
     *
     * @param participant the participant whose streak is computed
     * @return the current global streak (never negative)
     */
    public int streak(Ranking_Participant participant) {
        if (participant == null) {
            return 0;
        }
        List<ScoredPrediction> resolved = new ArrayList<>();
        for (ScoredPrediction prediction : participant.getPredictions()) {
            if (prediction != null
                    && prediction.getStatus() == MatchStatus.FINISHED
                    && hasScore(prediction)) {
                resolved.add(prediction);
            }
        }
        resolved.sort(new Comparator<ScoredPrediction>() {
            @Override
            public int compare(ScoredPrediction a, ScoredPrediction b) {
                return Long.compare(b.getTimestamp(), a.getTimestamp());
            }
        });
        int streak = 0;
        for (ScoredPrediction prediction : resolved) {
            if (scorer.isCorrect(scoreOf(prediction))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Whether the participant has at least one {@link MatchStatus#LIVE}
     * prediction with a score that contributes {@code Live_Points} (R6.5).
     *
     * @param participant the participant to inspect
     * @return {@code true} when a live contribution exists
     */
    public boolean hasLive(Ranking_Participant participant) {
        if (participant == null) {
            return false;
        }
        for (ScoredPrediction prediction : participant.getPredictions()) {
            if (prediction != null
                    && prediction.getStatus() == MatchStatus.LIVE
                    && hasScore(prediction)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compute the full ranking: per-participant metrics, ordered by points
     * descending with the binding tie-break — (a) {@code winPercentage}
     * descending, (b) {@code displayName} ascending case-insensitive, (c)
     * {@code id} ascending — assigning positions {@code 1..N} and building the
     * {@link RankingResult} (R3.4, R5.5, R8.2).
     *
     * @param participants the participants to rank
     * @param period       the selected period
     * @param now          current instant as epoch millis
     * @return an immutable {@link RankingResult}
     */
    public RankingResult rank(List<Ranking_Participant> participants, Ranking_Period period, long now) {
        if (participants == null || participants.isEmpty()) {
            return new RankingResult(Collections.<Ranking_Entry>emptyList(), -1, false);
        }

        List<Computed> computed = new ArrayList<>(participants.size());
        for (Ranking_Participant participant : participants) {
            if (participant == null) {
                continue;
            }
            int points = pointsForPeriod(participant, period, now);
            int winPct = winPercentage(participant, period, now);
            int streak = streak(participant);
            boolean live = hasLive(participant);
            computed.add(new Computed(participant, points, winPct, streak, live));
        }

        computed.sort(new Comparator<Computed>() {
            @Override
            public int compare(Computed a, Computed b) {
                int byPoints = Integer.compare(b.points, a.points);
                if (byPoints != 0) {
                    return byPoints;
                }
                int byWin = Integer.compare(b.winPercentage, a.winPercentage);
                if (byWin != 0) {
                    return byWin;
                }
                int byName = compareNames(a.participant.getDisplayName(),
                        b.participant.getDisplayName());
                if (byName != 0) {
                    return byName;
                }
                return compareIds(a.participant.getId(), b.participant.getId());
            }
        });

        List<Ranking_Entry> entries = new ArrayList<>(computed.size());
        int currentUserPosition = -1;
        boolean hasLiveAny = false;
        for (int i = 0; i < computed.size(); i++) {
            Computed c = computed.get(i);
            int position = i + 1;
            Ranking_Participant p = c.participant;
            entries.add(new Ranking_Entry(
                    position,
                    p.getDisplayName(),
                    p.getAvatarRef(),
                    c.points,
                    c.winPercentage,
                    c.streak,
                    p.isCurrentUser(),
                    c.hasLive));
            if (p.isCurrentUser()) {
                currentUserPosition = position;
            }
            if (c.hasLive) {
                hasLiveAny = true;
            }
        }

        return new RankingResult(entries, currentUserPosition, hasLiveAny);
    }

    // --- Internal helpers -------------------------------------------------

    private int scoreOf(ScoredPrediction prediction) {
        return scorer.score(
                prediction.getPredictedHome(),
                prediction.getPredictedAway(),
                prediction.getActualHome(),
                prediction.getActualAway());
    }

    private boolean hasScore(ScoredPrediction prediction) {
        return prediction.getActualHome() != null && prediction.getActualAway() != null;
    }

    private boolean isWithinWindow(long timestamp, long windowStart) {
        return timestamp >= windowStart;
    }

    private static int compareNames(String a, String b) {
        String left = a == null ? "" : a;
        String right = b == null ? "" : b;
        return left.compareToIgnoreCase(right);
    }

    private static int compareIds(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    /** Small holder for the per-participant computed metrics used while sorting. */
    private static final class Computed {
        final Ranking_Participant participant;
        final int points;
        final int winPercentage;
        final int streak;
        final boolean hasLive;

        Computed(Ranking_Participant participant, int points, int winPercentage,
                 int streak, boolean hasLive) {
            this.participant = participant;
            this.points = points;
            this.winPercentage = winPercentage;
            this.streak = streak;
            this.hasLive = hasLive;
        }
    }
}
