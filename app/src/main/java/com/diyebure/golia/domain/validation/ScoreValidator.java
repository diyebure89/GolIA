package com.diyebure.golia.domain.validation;

import javax.inject.Inject;

/**
 * Pure domain validator for the exact-score prediction form (R5.3, R5.4).
 *
 * <p>Validates the home and away goal inputs independently and reports errors
 * per field via {@link ScoreValidationResult}. Inputs are received as
 * {@link String} (as they come from a text form) so the validator can tell an
 * empty field ({@link ScoreFieldError#REQUIRED}) apart from a non-integer or
 * out-of-range value ({@link ScoreFieldError#OUT_OF_RANGE}).</p>
 *
 * <p>The validator has no Android dependency so it can be exercised directly from
 * plain JVM unit tests, and it exposes a public no-arg {@link Inject} constructor
 * so Hilt can build it.</p>
 *
 * <p>Rules applied to each field:</p>
 * <ul>
 *   <li>{@code null} or blank (only whitespace) &rarr; {@link ScoreFieldError#REQUIRED}</li>
 *   <li>not an integer, or an integer outside {@code 0..99} &rarr; {@link ScoreFieldError#OUT_OF_RANGE}</li>
 *   <li>an integer within {@code 0..99} (inclusive) &rarr; valid ({@code null} error)</li>
 * </ul>
 */
public class ScoreValidator {

    /** Minimum accepted number of goals (inclusive). */
    public static final int MIN_GOALS = 0;
    /** Maximum accepted number of goals (inclusive). */
    public static final int MAX_GOALS = 99;

    @Inject
    public ScoreValidator() {
        // No dependencies: pure domain component.
    }

    /**
     * Validate the raw home and away goal inputs from the prediction form.
     *
     * @param homeInput the raw home-goals input (may be {@code null})
     * @param awayInput the raw away-goals input (may be {@code null})
     * @return a {@link ScoreValidationResult} carrying a per-field error (or {@code null})
     */
    public ScoreValidationResult validate(String homeInput, String awayInput) {
        return new ScoreValidationResult(validateField(homeInput), validateField(awayInput));
    }

    /**
     * Validate a single raw goal input.
     *
     * @param input the raw input (may be {@code null})
     * @return the {@link ScoreFieldError} describing why the value was rejected,
     *         or {@code null} when the value is a valid integer in {@code 0..99}
     */
    private ScoreFieldError validateField(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ScoreFieldError.REQUIRED;
        }

        int value;
        try {
            value = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return ScoreFieldError.OUT_OF_RANGE;
        }

        if (value < MIN_GOALS || value > MAX_GOALS) {
            return ScoreFieldError.OUT_OF_RANGE;
        }

        return null;
    }
}
