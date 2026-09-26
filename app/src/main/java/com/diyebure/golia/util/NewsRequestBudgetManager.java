package com.diyebure.golia.util;

import com.diyebure.golia.data.local.PreferencesManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Persistent daily request budget manager for NewsData.io
 * (Presupuesto_Peticiones_Noticias).
 *
 * <p>Analogous to {@link RequestBudgetManager} but tailored to NewsData.io's free
 * plan, which resets its quota at 00:00 UTC. Differences with the API-Football
 * manager:</p>
 * <ul>
 *   <li><b>Reset at 00:00 UTC</b> (not the local day): {@link #today()} derives the
 *       day from {@link #currentTimeMillis()} as the UTC epoch-day
 *       ({@code currentTimeMillis() / 86_400_000L}).</li>
 *   <li><b>Operational limit {@value #OPERATIONAL_LIMIT}</b> — a safety margin below
 *       the provider's ~200/day allowance (R6.4).</li>
 *   <li><b>Per-(league, query) minimum interval of {@value #MIN_INTERVAL_MS} ms</b>
 *       (30 min = 1800 s), not a global one: {@link #isMinIntervalElapsed(long)}
 *       receives the {@code lastFetchedAt} of the requested league, which lives in
 *       Room ({@code news_league_meta}), not in this manager (R6.5).</li>
 *   <li><b>Forced quota mode on 429</b> (R6.10): {@link #markQuotaExhausted()} sets a
 *       flag that forces cache-only mode until the next 00:00 UTC reset, even if the
 *       local counter has not reached {@link #OPERATIONAL_LIMIT}.</li>
 * </ul>
 *
 * <p>State is persisted through {@link PreferencesManager} (the daily counter and
 * UTC day marker, plus the forced-quota day marker), in plain (non-encrypted) keys
 * because they are counters/flags, not secrets. The {@code lastFetchedAt} per league
 * is <em>not</em> stored here; it is passed in by the caller from Room.</p>
 *
 * <p>{@link #currentTimeMillis()} and {@link #today()} are {@code protected} and
 * overridable so unit tests can supply a controllable clock (task 6.4).</p>
 *
 * <p>Requirements: 6.3, 6.4, 6.5, 6.9, 6.10.</p>
 */
@Singleton
public class NewsRequestBudgetManager {

    /** Number of milliseconds in a UTC day, used to derive the UTC epoch-day. */
    private static final long MILLIS_PER_DAY = 86_400_000L;

    /**
     * Operational daily request limit for NewsData.io. Kept below the provider's
     * ~200/day allowance to leave a safety margin (R6.4).
     */
    public static final int OPERATIONAL_LIMIT = 180;

    /**
     * Minimum interval between consecutive refreshes of the same (league, query), in
     * milliseconds (30 min = 1800 s). Applied per league, not globally (R6.5).
     */
    public static final long MIN_INTERVAL_MS = 1_800_000L;

    private final PreferencesManager preferencesManager;

    @Inject
    public NewsRequestBudgetManager(PreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;
    }

    /**
     * Atomically evaluates the budget, forced-quota flag and per-league interval and
     * returns the refresh decision (R6.9). The check-and-increment is performed inside
     * this {@code synchronized} critical section so that, under concurrent calls for
     * the same league, at most one caller receives {@link RefreshDecision#PROCEED}
     * (and therefore at most one increment happens).
     *
     * @param leagueKey    stable league key of the requested chip/query (used for
     *                     traceability; the interval is keyed by {@code lastFetchedAt}).
     * @param lastFetchedAt epoch-millis timestamp of the last successful fetch for this
     *                      league (from Room {@code news_league_meta}), or {@code 0} if
     *                      the league has never been fetched.
     * @return the decision the caller must honor.
     */
    public synchronized RefreshDecision decide(String leagueKey, long lastFetchedAt) {
        resetIfNewDayUtc();
        if (isQuotaExhausted()) {
            return RefreshDecision.SERVE_CACHE_QUOTA;
        }
        if (!isMinIntervalElapsed(lastFetchedAt)) {
            return RefreshDecision.SERVE_CACHE;
        }
        if (!canRequest()) {
            return RefreshDecision.SERVE_CACHE_QUOTA;
        }
        recordRequest();
        return RefreshDecision.PROCEED;
    }

    /**
     * Returns {@code true} when the daily budget is not exhausted for the current UTC
     * day, i.e. the recorded request count is below {@link #OPERATIONAL_LIMIT}. Performs
     * a UTC daily reset first so the check reflects the current day.
     */
    public synchronized boolean canRequest() {
        resetIfNewDayUtc();
        return preferencesManager.getNewsRequestCount() < OPERATIONAL_LIMIT;
    }

    /**
     * Returns {@code true} when the news quota must be treated as exhausted for the
     * current UTC day, either because the operational limit has been reached or because
     * the provider forced quota mode via a 429 (R6.10). Performs a UTC daily reset first.
     */
    public synchronized boolean isQuotaExhausted() {
        resetIfNewDayUtc();
        if (preferencesManager.getNewsRequestCount() >= OPERATIONAL_LIMIT) {
            return true;
        }
        return preferencesManager.getNewsQuotaExhaustedDay() == today();
    }

    /**
     * Returns {@code true} when at least {@link #MIN_INTERVAL_MS} ms have elapsed since
     * the given last-fetch timestamp for the league. When the league has never been
     * fetched ({@code lastFetchedAt <= 0}), the interval is considered elapsed (R6.5).
     *
     * @param lastFetchedAt epoch-millis of the last successful fetch for the league.
     */
    public boolean isMinIntervalElapsed(long lastFetchedAt) {
        if (lastFetchedAt <= 0L) {
            return true;
        }
        return (currentTimeMillis() - lastFetchedAt) >= MIN_INTERVAL_MS;
    }

    /**
     * Records a performed request: resets the counter first if the UTC day changed, then
     * increments the daily counter. Invoked inside {@link #decide(String, long)} within
     * the lock so the increment is atomic with the budget check.
     */
    public synchronized void recordRequest() {
        resetIfNewDayUtc();
        int next = preferencesManager.getNewsRequestCount() + 1;
        preferencesManager.setNewsRequestCount(next);
    }

    /**
     * Marks the news quota as exhausted for the remainder of the current UTC day after a
     * provider 429 (R6.10). This forces cache-only mode even if the local counter has not
     * reached {@link #OPERATIONAL_LIMIT}. The flag is cleared automatically when the UTC
     * day changes via {@link #resetIfNewDayUtc()}.
     */
    public synchronized void markQuotaExhausted() {
        resetIfNewDayUtc();
        preferencesManager.setNewsQuotaExhaustedDay(today());
    }

    /**
     * Resets the daily counter, updates the stored UTC day marker and clears the
     * forced-quota flag when the UTC calendar day has changed since the last recorded
     * budget day. Safe to call before any budget check.
     */
    public synchronized void resetIfNewDayUtc() {
        int currentDay = today();
        int storedDay = preferencesManager.getNewsBudgetDay();
        if (storedDay != currentDay) {
            preferencesManager.setNewsRequestCount(0);
            preferencesManager.setNewsBudgetDay(currentDay);
            preferencesManager.setNewsQuotaExhaustedDay(0);
        }
    }

    /**
     * Current wall-clock time in epoch milliseconds. Overridable for testing.
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Returns an integer marker identifying the current UTC calendar day, computed as the
     * UTC epoch-day ({@code currentTimeMillis() / 86_400_000}). Because it divides by whole
     * UTC days, the marker rolls over exactly at 00:00 UTC. Overridable for testing with a
     * controllable clock.
     */
    protected int today() {
        return (int) (currentTimeMillis() / MILLIS_PER_DAY);
    }
}
