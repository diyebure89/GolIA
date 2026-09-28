package com.diyebure.golia.domain.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Required JVM unit tests for {@link ScoreValidator} (R11.1, R11.2, R11.3).
 *
 * <p>Pure JVM tests (no Android/Robolectric) covering the exact-score input rules:
 * integers within {@code 0..99} are valid; empty/blank/null fields are
 * {@link ScoreFieldError#REQUIRED}; non-integer or out-of-range values are
 * {@link ScoreFieldError#OUT_OF_RANGE}; errors are reported per field.</p>
 */
public class ScoreValidatorTest {

    private ScoreValidator validator;

    @Before
    public void setUp() {
        validator = new ScoreValidator();
    }

    // --- Valid values (boundaries) --------------------------------------------

    @Test
    public void validLowerBoundZero_isValidNoErrors() {
        ScoreValidationResult result = validator.validate("0", "0");
        assertTrue(result.isValid());
        assertNull(result.getHomeError());
        assertNull(result.getAwayError());
    }

    @Test
    public void validUpperBound99_isValidNoErrors() {
        ScoreValidationResult result = validator.validate("99", "99");
        assertTrue(result.isValid());
        assertNull(result.getHomeError());
        assertNull(result.getAwayError());
    }

    @Test
    public void validMidRange_isValid() {
        ScoreValidationResult result = validator.validate("2", "1");
        assertTrue(result.isValid());
    }

    // --- Required (empty / blank / null) --------------------------------------

    @Test
    public void emptyFields_areRequired() {
        ScoreValidationResult result = validator.validate("", "");
        assertFalse(result.isValid());
        assertEquals(ScoreFieldError.REQUIRED, result.getHomeError());
        assertEquals(ScoreFieldError.REQUIRED, result.getAwayError());
    }

    @Test
    public void blankFields_areRequired() {
        ScoreValidationResult result = validator.validate("   ", "\t");
        assertEquals(ScoreFieldError.REQUIRED, result.getHomeError());
        assertEquals(ScoreFieldError.REQUIRED, result.getAwayError());
    }

    @Test
    public void nullFields_areRequired() {
        ScoreValidationResult result = validator.validate(null, null);
        assertEquals(ScoreFieldError.REQUIRED, result.getHomeError());
        assertEquals(ScoreFieldError.REQUIRED, result.getAwayError());
    }

    // --- Out of range / non-integer -------------------------------------------

    @Test
    public void negativeValue_isOutOfRange() {
        ScoreValidationResult result = validator.validate("-1", "0");
        assertEquals(ScoreFieldError.OUT_OF_RANGE, result.getHomeError());
        assertNull(result.getAwayError());
    }

    @Test
    public void aboveMax100_isOutOfRange() {
        ScoreValidationResult result = validator.validate("0", "100");
        assertNull(result.getHomeError());
        assertEquals(ScoreFieldError.OUT_OF_RANGE, result.getAwayError());
    }

    @Test
    public void nonInteger_isOutOfRange() {
        ScoreValidationResult result = validator.validate("abc", "1");
        assertEquals(ScoreFieldError.OUT_OF_RANGE, result.getHomeError());
        assertNull(result.getAwayError());
    }

    // --- Per-field independence -----------------------------------------------

    @Test
    public void errorsAreReportedPerField_validHomeInvalidAway() {
        ScoreValidationResult result = validator.validate("3", "abc");
        assertFalse(result.isValid());
        assertNull(result.getHomeError());
        assertEquals(ScoreFieldError.OUT_OF_RANGE, result.getAwayError());
    }

    @Test
    public void errorsAreReportedPerField_requiredHomeOutOfRangeAway() {
        ScoreValidationResult result = validator.validate("", "150");
        assertEquals(ScoreFieldError.REQUIRED, result.getHomeError());
        assertEquals(ScoreFieldError.OUT_OF_RANGE, result.getAwayError());
    }
}
