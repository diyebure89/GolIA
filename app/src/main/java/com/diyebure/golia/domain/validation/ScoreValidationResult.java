package com.diyebure.golia.domain.validation;

import java.util.Objects;

/**
 * Immutable value object holding the outcome of validating an exact-score input,
 * with errors reported <em>per field</em> (home and away separately) (R5.3, R5.4).
 *
 * <p>A field is valid when its associated {@link ScoreFieldError} is {@code null}.
 * The whole result {@link #isValid() is valid} only when both fields are valid.</p>
 */
public final class ScoreValidationResult {

    private final ScoreFieldError homeError;
    private final ScoreFieldError awayError;

    /**
     * @param homeError the error for the home-goals field, or {@code null} when valid
     * @param awayError the error for the away-goals field, or {@code null} when valid
     */
    public ScoreValidationResult(ScoreFieldError homeError, ScoreFieldError awayError) {
        this.homeError = homeError;
        this.awayError = awayError;
    }

    /**
     * @return {@code true} when neither field has an error
     */
    public boolean isValid() {
        return homeError == null && awayError == null;
    }

    /**
     * @return the error for the home-goals field, or {@code null} when it is valid
     */
    public ScoreFieldError getHomeError() {
        return homeError;
    }

    /**
     * @return the error for the away-goals field, or {@code null} when it is valid
     */
    public ScoreFieldError getAwayError() {
        return awayError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScoreValidationResult)) {
            return false;
        }
        ScoreValidationResult that = (ScoreValidationResult) o;
        return homeError == that.homeError && awayError == that.awayError;
    }

    @Override
    public int hashCode() {
        return Objects.hash(homeError, awayError);
    }

    @Override
    public String toString() {
        return "ScoreValidationResult{homeError=" + homeError + ", awayError=" + awayError + '}';
    }
}
