package com.diyebure.golia.util;

import com.diyebure.golia.data.local.PreferencesManager;

import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Persistent daily request budget manager for API-Football (Presupuesto_Peticiones).
 *
 * <p>Tracks the number of requests performed against the API-Football free plan,
 * which allows at most {@value #DAILY_LIMIT} requests per day, and enforces a
 * minimum shared interval of {@value #MIN_INTERVAL_MS} ms between consecutive
 * requests. State is persisted through {@link PreferencesManager}, which owns the
 * app's {@code SharedPreferences}; the counter, last-request timestamp and day
 * marker are stored in plain (non-encrypted) keys because they are not secrets.</p>
 *
 * <p>The counter resets automatically when the calendar day changes. Callers can
 * safely invoke {@link #resetIfNewDay()} directly, but it is also invoked
 * internally at the start of {@link #canRequest()}, {@link #isExhausted()} and
 * {@link #recordRequest()} so the daily reset is transparent.</p>
 *
 * <p>The clock ({@link #currentTimeMillis()}) and the day determination
 * ({@link #today()}) are {@code protected} and overridable so unit and property
 * tests can supply a controllable clock (tasks 3.5/3.6).</p>
 *
 * <p>Requirements: 7.3, 7.4, 7.7.</p>
 */
@Singleton
public class RequestBudgetManager {

    /** Daily request limit of the API-Football free plan. */
    public static final int DAILY_LIMIT = 100;

    /** Minimum shared interval between consecutive requests, in milliseconds (60 s). */
    public static final long MIN_INTERVAL_MS = 60_000L;

    private final PreferencesManager preferencesManager;

    @Inject
    public RequestBudgetManager(PreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;
    }

    /**
     * Returns {@code true} when the budget is not exhausted for the current day,
     * i.e. the recorded request count is below {@link #DAILY_LIMIT}. Performs a
     * daily reset first so the check reflects the current calendar day.
     */
    public boolean canRequest() {
        resetIfNewDay();
        return preferencesManager.getRequestCount() < DAILY_LIMIT;
    }

    /**
     * Returns {@code true} when at least {@link #MIN_INTERVAL_MS} ms have elapsed
     * since the last recorded request. When no request has ever been recorded, the
     * interval is considered elapsed.
     */
    public boolean isMinIntervalElapsed() {
        long last = preferencesManager.getLastRequestTimestamp();
        if (last <= 0L) {
            return true;
        }
        return (currentTimeMillis() - last) >= MIN_INTERVAL_MS;
    }

    /**
     * Records a performed request: resets the counter first if the day changed,
     * increments the daily counter and stamps the last-request timestamp with the
     * current time.
     */
    public void recordRequest() {
        resetIfNewDay();
        int next = preferencesManager.getRequestCount() + 1;
        preferencesManager.setRequestCount(next);
        preferencesManager.setLastRequestTimestamp(currentTimeMillis());
    }

    /**
     * Returns {@code true} when the daily budget has been exhausted, i.e. the
     * recorded request count has reached {@link #DAILY_LIMIT}. Performs a daily
     * reset first so the check reflects the current calendar day.
     */
    public boolean isExhausted() {
        resetIfNewDay();
        return preferencesManager.getRequestCount() >= DAILY_LIMIT;
    }

    /**
     * Resets the daily counter and updates the stored day marker when the calendar
     * day has changed since the last recorded budget day. Safe to call before
     * {@link #canRequest()} / {@link #recordRequest()}.
     */
    public void resetIfNewDay() {
        int currentDay = today();
        int storedDay = preferencesManager.getBudgetDay();
        if (storedDay != currentDay) {
            preferencesManager.setRequestCount(0);
            preferencesManager.setBudgetDay(currentDay);
        }
    }

    /**
     * Current wall-clock time in epoch milliseconds. Overridable for testing.
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Returns an integer marker identifying the current calendar day, derived from
     * the current time in the default zone as {@code year * 1000 + dayOfYear}.
     * Overridable for testing with a controllable clock.
     */
    protected int today() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis());
        int year = calendar.get(Calendar.YEAR);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        return year * 1000 + dayOfYear;
    }
}
