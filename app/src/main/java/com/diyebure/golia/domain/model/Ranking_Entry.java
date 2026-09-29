package com.diyebure.golia.domain.model;

/**
 * Immutable value object representing one row of the computed ranking.
 *
 * <p>Produced by the {@code Ranking_Calculator} with its final {@code position}
 * (1-based), display metrics and flags consumed directly by the presentation
 * layer. The {@code avatarRef} follows the same convention as
 * {@link Ranking_Participant#getAvatarRef()}.</p>
 */
public final class Ranking_Entry {

    private final int position;
    private final String displayName;
    private final String avatarRef;
    private final int points;
    private final int winPercentage;
    private final int streak;
    private final boolean isCurrentUser;
    private final boolean hasLive;

    /**
     * @param position      1-based position in the ranking
     * @param displayName   name shown in the ranking
     * @param avatarRef     avatar reference (path or resource name); may be {@code null}
     * @param points        total points accumulated in the period
     * @param winPercentage win percentage in the range {@code 0..100}
     * @param streak        current global winning streak
     * @param isCurrentUser {@code true} when this row is the real logged-in user
     * @param hasLive       {@code true} when the participant has a live contribution
     */
    public Ranking_Entry(int position, String displayName, String avatarRef,
                         int points, int winPercentage, int streak,
                         boolean isCurrentUser, boolean hasLive) {
        this.position = position;
        this.displayName = displayName;
        this.avatarRef = avatarRef;
        this.points = points;
        this.winPercentage = winPercentage;
        this.streak = streak;
        this.isCurrentUser = isCurrentUser;
        this.hasLive = hasLive;
    }

    public int getPosition() {
        return position;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarRef() {
        return avatarRef;
    }

    public int getPoints() {
        return points;
    }

    public int getWinPercentage() {
        return winPercentage;
    }

    public int getStreak() {
        return streak;
    }

    public boolean isCurrentUser() {
        return isCurrentUser;
    }

    public boolean hasLive() {
        return hasLive;
    }
}
