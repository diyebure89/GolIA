package com.diyebure.golia.util;

/**
 * Outcome of the atomic budget/interval evaluation performed by
 * {@link NewsRequestBudgetManager#decide(String, long)} (R6.9).
 *
 * <p>The repository uses this to decide whether to hit NewsData.io or serve the
 * cached news for the requested league:</p>
 * <ul>
 *   <li>{@link #PROCEED}: the caller may perform a network refresh; the daily
 *       counter has already been incremented inside the critical section.</li>
 *   <li>{@link #SERVE_CACHE}: skip the network call and serve cache because the
 *       per-league minimum interval (1800 s) has not elapsed yet (R6.5).</li>
 *   <li>{@link #SERVE_CACHE_QUOTA}: skip the network call and serve cache because
 *       the daily budget is exhausted or the provider forced quota mode via a 429
 *       (R6.4, R6.10). Maps to the {@code QUOTA} banner.</li>
 * </ul>
 */
public enum RefreshDecision {
    /** Perform the network refresh; the request has been counted. */
    PROCEED,

    /** Serve cached news: the per-league minimum interval has not elapsed. */
    SERVE_CACHE,

    /** Serve cached news: the daily budget is exhausted or quota is forced by 429. */
    SERVE_CACHE_QUOTA
}
