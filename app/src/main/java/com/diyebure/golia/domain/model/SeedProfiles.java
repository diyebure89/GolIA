package com.diyebure.golia.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Deterministic in-memory domain constants that populate the ranking alongside
 * the real {@code Current_User}.
 *
 * <p>Each {@link SeedProfile} defines a stable name, an avatar reference and a
 * fixed list of {@link Seed_Result}. A {@code Seed_Result} carries a
 * {@code timestampOffsetDays} relative to a caller-supplied {@code now}, so the
 * same profiles fall into the Semanal (last 7 days), Mensual (last 30 days) and
 * Total windows regardless of when they are evaluated. Because the offsets and
 * scores are fixed, {@link #all(long)} yields the same points, win percentage
 * and streak on every run (Requirement 2.2).</p>
 *
 * <p>These are pure domain constants: they never touch Room or the network
 * (Requirement 2.2). The {@code avatarRef} is a String reference to a local
 * drawable resource name, matching the unified {@link Ranking_Participant}
 * avatar convention; the presentation layer resolves it (or degrades to a
 * placeholder) when rendering.</p>
 *
 * <p>At least three profiles are always defined to guarantee a full podium
 * (Requirement 2.6). The set includes a mix of {@link MatchStatus#FINISHED}
 * results spread across all three periods and at least one
 * {@link MatchStatus#LIVE} result so the "en vivo" badge path is exercised.</p>
 */
public final class SeedProfiles {

    /** Milliseconds in a single day, used to convert day offsets to timestamps. */
    private static final long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);

    /** Shared avatar placeholder resource name for seed participants. */
    private static final String AVATAR_PLACEHOLDER = "ic_avatar_placeholder";

    private SeedProfiles() {
        // Utility holder; not instantiable.
    }

    /**
     * A single sample prediction result for a {@link SeedProfile}.
     *
     * <p>{@code actualHome}/{@code actualAway} are nullable: a {@link MatchStatus#LIVE}
     * match may not yet carry a score. {@code timestampOffsetDays} is relative to
     * {@code now} (negative = days in the past) and determines which
     * {@link Ranking_Period} window the result falls into.</p>
     */
    public static final class Seed_Result {

        private final int predictedHome;
        private final int predictedAway;
        private final Integer actualHome;
        private final Integer actualAway;
        private final MatchStatus status;
        private final int timestampOffsetDays;

        /**
         * @param predictedHome       predicted goals for the home team
         * @param predictedAway       predicted goals for the away team
         * @param actualHome          actual home goals, or {@code null} when unavailable
         * @param actualAway          actual away goals, or {@code null} when unavailable
         * @param status              the match status backing this result
         * @param timestampOffsetDays offset in days relative to {@code now}
         *                            (negative = in the past)
         */
        public Seed_Result(int predictedHome, int predictedAway,
                           Integer actualHome, Integer actualAway,
                           MatchStatus status, int timestampOffsetDays) {
            this.predictedHome = predictedHome;
            this.predictedAway = predictedAway;
            this.actualHome = actualHome;
            this.actualAway = actualAway;
            this.status = status;
            this.timestampOffsetDays = timestampOffsetDays;
        }

        public int getPredictedHome() {
            return predictedHome;
        }

        public int getPredictedAway() {
            return predictedAway;
        }

        public Integer getActualHome() {
            return actualHome;
        }

        public Integer getActualAway() {
            return actualAway;
        }

        public MatchStatus getStatus() {
            return status;
        }

        public int getTimestampOffsetDays() {
            return timestampOffsetDays;
        }

        /**
         * Convert this seed result to a {@link ScoredPrediction} anchored to
         * {@code now}.
         *
         * @param now the current instant as epoch millis
         * @return the equivalent {@link ScoredPrediction} with an absolute timestamp
         */
        ScoredPrediction toScoredPrediction(long now) {
            long timestamp = now + (timestampOffsetDays * MILLIS_PER_DAY);
            return new ScoredPrediction(predictedHome, predictedAway,
                    actualHome, actualAway, status, timestamp);
        }
    }

    /**
     * A deterministic sample participant: a name, an avatar reference and a fixed
     * list of {@link Seed_Result}.
     */
    public static final class SeedProfile {

        private final String id;
        private final String name;
        private final String avatarDrawable;
        private final List<Seed_Result> results;

        /**
         * @param id             stable identifier used for tie-breaking and keys
         * @param name           display name shown in the ranking
         * @param avatarDrawable avatar reference (local drawable resource name)
         * @param results        fixed sample results (defensively copied)
         */
        public SeedProfile(String id, String name, String avatarDrawable,
                           List<Seed_Result> results) {
            this.id = id;
            this.name = name;
            this.avatarDrawable = avatarDrawable;
            this.results = results == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(results));
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getAvatarDrawable() {
            return avatarDrawable;
        }

        public List<Seed_Result> getResults() {
            return results;
        }

        /**
         * Convert this profile to a {@link Ranking_Participant} anchored to
         * {@code now}. Seed participants are never the current user.
         *
         * @param now the current instant as epoch millis
         * @return the equivalent {@link Ranking_Participant}
         */
        Ranking_Participant toParticipant(long now) {
            List<ScoredPrediction> predictions = new ArrayList<>(results.size());
            for (Seed_Result result : results) {
                predictions.add(result.toScoredPrediction(now));
            }
            return new Ranking_Participant(id, name, avatarDrawable, false, predictions);
        }
    }

    /**
     * The fixed, ordered set of seed profiles. Offsets are chosen so that every
     * profile has FINISHED results in the Semanal window (>= -6 days), additional
     * FINISHED results in the Mensual-only window (-7..-30 days) and Total-only
     * window (< -30 days), guaranteeing meaningful metrics across all periods.
     * At least one LIVE result (offset 0) exercises the live badge path.
     */
    private static final List<SeedProfile> PROFILES = Collections.unmodifiableList(Arrays.asList(
            new SeedProfile("seed-carlosgol", "CarlosGol", AVATAR_PLACEHOLDER, Arrays.asList(
                    // Semanal (last 7 days): two exact hits, one miss.
                    new Seed_Result(2, 1, 2, 1, MatchStatus.FINISHED, -1),
                    new Seed_Result(1, 0, 1, 0, MatchStatus.FINISHED, -3),
                    new Seed_Result(3, 2, 0, 0, MatchStatus.FINISHED, -5),
                    // Mensual-only window.
                    new Seed_Result(2, 2, 2, 2, MatchStatus.FINISHED, -12),
                    new Seed_Result(1, 1, 3, 0, MatchStatus.FINISHED, -20),
                    // Total-only (older than 30 days).
                    new Seed_Result(0, 1, 0, 1, MatchStatus.FINISHED, -45),
                    // Live provisional contribution.
                    new Seed_Result(1, 0, 1, 0, MatchStatus.LIVE, 0)
            )),
            new SeedProfile("seed-pronosticopro", "PronosticoPro", AVATAR_PLACEHOLDER, Arrays.asList(
                    new Seed_Result(3, 1, 3, 1, MatchStatus.FINISHED, -2),
                    new Seed_Result(2, 0, 2, 1, MatchStatus.FINISHED, -4),
                    new Seed_Result(0, 0, 0, 0, MatchStatus.FINISHED, -6),
                    new Seed_Result(1, 2, 1, 2, MatchStatus.FINISHED, -15),
                    new Seed_Result(2, 1, 0, 3, MatchStatus.FINISHED, -25),
                    new Seed_Result(4, 2, 4, 2, MatchStatus.FINISHED, -40)
            )),
            new SeedProfile("seed-betmaster99", "BetMaster99", AVATAR_PLACEHOLDER, Arrays.asList(
                    new Seed_Result(1, 1, 1, 1, MatchStatus.FINISHED, -1),
                    new Seed_Result(2, 3, 1, 0, MatchStatus.FINISHED, -4),
                    new Seed_Result(0, 2, 0, 2, MatchStatus.FINISHED, -18),
                    new Seed_Result(3, 0, 3, 0, MatchStatus.FINISHED, -28),
                    new Seed_Result(1, 1, 2, 2, MatchStatus.FINISHED, -50),
                    // Live match without a score yet -> contributes 0 live points.
                    new Seed_Result(2, 1, null, null, MatchStatus.LIVE, 0)
            )),
            new SeedProfile("seed-golazoana", "GolazoAna", AVATAR_PLACEHOLDER, Arrays.asList(
                    new Seed_Result(2, 2, 2, 2, MatchStatus.FINISHED, -2),
                    new Seed_Result(1, 3, 1, 3, MatchStatus.FINISHED, -5),
                    new Seed_Result(0, 1, 2, 1, MatchStatus.FINISHED, -10),
                    new Seed_Result(3, 3, 3, 3, MatchStatus.FINISHED, -22),
                    new Seed_Result(1, 0, 0, 0, MatchStatus.FINISHED, -38)
            )),
            new SeedProfile("seed-defensacentral", "DefensaCentral", AVATAR_PLACEHOLDER, Arrays.asList(
                    new Seed_Result(0, 0, 1, 1, MatchStatus.FINISHED, -3),
                    new Seed_Result(1, 2, 1, 2, MatchStatus.FINISHED, -6),
                    new Seed_Result(2, 1, 2, 0, MatchStatus.FINISHED, -14),
                    new Seed_Result(1, 1, 1, 1, MatchStatus.FINISHED, -33),
                    // Live provisional contribution.
                    new Seed_Result(0, 1, 0, 1, MatchStatus.LIVE, 0)
            ))
    ));

    /**
     * Return the seed profiles as {@link Ranking_Participant} instances anchored
     * to the supplied {@code now}. Each {@link Seed_Result} becomes a
     * {@link ScoredPrediction} whose timestamp is
     * {@code now + timestampOffsetDays * MILLIS_PER_DAY}, and every participant is
     * flagged {@code isCurrentUser = false}.
     *
     * <p>The result is deterministic for a fixed {@code now}: identical input
     * yields identical output (Requirement 2.2). The list always contains at
     * least three participants (Requirement 2.6).</p>
     *
     * @param now the current instant as epoch millis (injected for determinism)
     * @return the seed participants for the ranking
     */
    public static List<Ranking_Participant> all(long now) {
        List<Ranking_Participant> participants = new ArrayList<>(PROFILES.size());
        for (SeedProfile profile : PROFILES) {
            participants.add(profile.toParticipant(now));
        }
        return Collections.unmodifiableList(participants);
    }

    /**
     * @return the immutable list of raw seed profile definitions
     */
    public static List<SeedProfile> profiles() {
        return PROFILES;
    }
}
