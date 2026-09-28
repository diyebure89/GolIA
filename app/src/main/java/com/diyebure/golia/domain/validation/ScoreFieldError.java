package com.diyebure.golia.domain.validation;

/**
 * Per-field validation error for an exact-score prediction input (R5.3, R5.4).
 *
 * <p>Used by {@link ScoreValidationResult} to report, independently for the home
 * and away fields, why a value was rejected. A {@code null} value (rather than an
 * enum constant) signals that the corresponding field is valid.</p>
 *
 * <p>The UI maps each constant to a localized string resource so it can render a
 * per-field message.</p>
 */
public enum ScoreFieldError {
    /** The field was empty, blank or {@code null} (R5.3). */
    REQUIRED,
    /** The field was not an integer within the inclusive range {@code 0..99} (R5.4). */
    OUT_OF_RANGE
}
