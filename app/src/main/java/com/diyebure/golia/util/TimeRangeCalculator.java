package com.diyebure.golia.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Utilidad_Zona_Local: single source of truth for temporal range calculation.
 *
 * <p>Computes Rango_Hoy, Rango_Manana and Rango_Esta_Semana in a given {@link ZoneId},
 * exposing inclusive millisecond boundaries where the start of a day is
 * {@code 00:00:00.000} and the end of a day is {@code 23:59:59.999}.</p>
 *
 * <p>This is a plain Java class with no Android dependencies, so it is unit-testable
 * on the JVM. It uses the {@code java.time} APIs to derive the boundaries.</p>
 *
 * <p>Requirements: 3.7, 4.1, 4.2.</p>
 */
public class TimeRangeCalculator {

    /** Inclusive start-of-day time: {@code 00:00:00.000}. */
    private static final LocalTime START_OF_DAY = LocalTime.MIN; // 00:00:00.000

    /** Inclusive end-of-day time: {@code 23:59:59.999}. */
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59, 999_000_000);

    /**
     * Range with inclusive millisecond boundaries.
     *
     * <p>{@code startMs} is the epoch-millis of {@code 00:00:00.000} of the start day
     * in the target zone; {@code endMs} is the epoch-millis of {@code 23:59:59.999}
     * of the end day in the target zone. Both ends are inclusive.</p>
     */
    public static final class Range {
        public final long startMs;
        public final long endMs;

        public Range(long startMs, long endMs) {
            this.startMs = startMs;
            this.endMs = endMs;
        }

        public long getStartMs() {
            return startMs;
        }

        public long getEndMs() {
            return endMs;
        }
    }

    /**
     * Rango_Ayer: from yesterday {@code 00:00:00.000} to yesterday {@code 23:59:59.999}
     * in the given zone.
     */
    public Range yesterday(ZoneId zone) {
        LocalDate yesterday = LocalDate.now(zone).minusDays(1);
        return rangeForDays(yesterday, yesterday, zone);
    }

    /**
     * Rango_Hoy: from today {@code 00:00:00.000} to today {@code 23:59:59.999}
     * in the given zone.
     */
    public Range today(ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        return rangeForDays(today, today, zone);
    }

    /**
     * Rango_Manana: from tomorrow {@code 00:00:00.000} to tomorrow {@code 23:59:59.999}
     * in the given zone.
     */
    public Range tomorrow(ZoneId zone) {
        LocalDate tomorrow = LocalDate.now(zone).plusDays(1);
        return rangeForDays(tomorrow, tomorrow, zone);
    }

    /**
     * Rango_Esta_Semana: from today {@code 00:00:00.000} to the Sunday of the current
     * week at {@code 23:59:59.999} in the given zone. Uses the ISO week, where Sunday is
     * the last day of the week, so Rango_Hoy is always a subset of Rango_Esta_Semana.
     */
    public Range thisWeek(ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return rangeForDays(today, sunday, zone);
    }

    /**
     * Checks whether the given instant (epoch millis) falls within the range,
     * inclusive on both ends.
     */
    public boolean isWithin(long epochMillis, Range range) {
        return epochMillis >= range.startMs && epochMillis <= range.endMs;
    }

    /**
     * Builds a range spanning from {@code 00:00:00.000} of {@code startDay} to
     * {@code 23:59:59.999} of {@code endDay} in the given zone.
     */
    private Range rangeForDays(LocalDate startDay, LocalDate endDay, ZoneId zone) {
        ZonedDateTime start = ZonedDateTime.of(startDay, START_OF_DAY, zone);
        ZonedDateTime end = ZonedDateTime.of(endDay, END_OF_DAY, zone);
        return new Range(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
    }
}
