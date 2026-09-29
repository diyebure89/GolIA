package com.diyebure.golia.domain.model;

import java.util.concurrent.TimeUnit;

/**
 * Time window used to scope the ranking computation.
 *
 * <p>Each period exposes {@link #windowStartMillis(long)} which returns the lower
 * bound (inclusive) of the window relative to a caller-supplied {@code now}
 * timestamp (epoch millis). Injecting {@code now} keeps the calculation
 * deterministic and testable.</p>
 *
 * <ul>
 *   <li>{@link #SEMANAL}: the last 7 days.</li>
 *   <li>{@link #MENSUAL}: the last 30 days.</li>
 *   <li>{@link #TOTAL}: no lower bound; {@link #windowStartMillis(long)} returns {@code 0}.</li>
 * </ul>
 */
public enum Ranking_Period {

    /** Rolling 7-day window. */
    SEMANAL(7),
    /** Rolling 30-day window. */
    MENSUAL(30),
    /** No temporal limit; includes everything. */
    TOTAL(-1);

    /** Window length in days, or {@code -1} for an unbounded window. */
    private final int windowDays;

    Ranking_Period(int windowDays) {
        this.windowDays = windowDays;
    }

    /**
     * Compute the inclusive lower bound of this period's window.
     *
     * @param now the current instant as epoch millis
     * @return {@code now - windowDays} (in millis) for {@link #SEMANAL} and
     *         {@link #MENSUAL}; {@code 0} for {@link #TOTAL}
     */
    public long windowStartMillis(long now) {
        if (windowDays < 0) {
            return 0L;
        }
        return now - TimeUnit.DAYS.toMillis(windowDays);
    }
}
