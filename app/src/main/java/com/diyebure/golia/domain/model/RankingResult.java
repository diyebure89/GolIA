package com.diyebure.golia.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable result of a ranking computation.
 *
 * <p>Holds the ordered list of {@link Ranking_Entry} (position 1..N), the
 * current user's position ({@code -1} when the real user is not present in the
 * ranking) and a flag indicating whether any participant has a live
 * contribution.</p>
 */
public final class RankingResult {

    private final List<Ranking_Entry> entries;
    private final int currentUserPosition;
    private final boolean hasLiveAny;

    /**
     * @param entries             ordered ranking rows (defensively copied)
     * @param currentUserPosition 1-based position of the real user, or {@code -1} when absent
     * @param hasLiveAny          {@code true} when at least one participant has a live contribution
     */
    public RankingResult(List<Ranking_Entry> entries, int currentUserPosition, boolean hasLiveAny) {
        this.entries = entries == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new java.util.ArrayList<>(entries));
        this.currentUserPosition = currentUserPosition;
        this.hasLiveAny = hasLiveAny;
    }

    public List<Ranking_Entry> getEntries() {
        return entries;
    }

    public int getCurrentUserPosition() {
        return currentUserPosition;
    }

    public boolean hasLiveAny() {
        return hasLiveAny;
    }
}
