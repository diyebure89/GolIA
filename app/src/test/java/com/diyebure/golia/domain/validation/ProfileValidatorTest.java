package com.diyebure.golia.domain.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

/**
 * Pure-JVM unit tests for {@link ProfileValidator} (task 3.3).
 *
 * <p>{@link ProfileValidator} delegates every field check to
 * {@link RegistrationValidator}, whose only Android touch point is the email
 * pattern hook {@link RegistrationValidator#isEmailPattern(String)}. To keep
 * these tests free of Android APIs, the validator under test is built on top of
 * a test-only subclass that overrides that hook with a simple regex.
 *
 * <p>Validates: Requisitos R16.1, R3, R4, R15.2.
 */
public class ProfileValidatorTest {

    /**
     * Test double for the base validator that replaces the Android email
     * matcher with a plain regex, so the whole suite runs on the JVM.
     */
    private static final class TestRegistrationValidator extends RegistrationValidator {
        private static final Pattern SIMPLE_EMAIL =
                Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

        @Override
        protected boolean isEmailPattern(String email) {
            return email != null && SIMPLE_EMAIL.matcher(email).matches();
        }
    }

    private ProfileValidator validator;

    @Before
    public void setUp() {
        validator = new ProfileValidator(new TestRegistrationValidator());
    }

    // ---------------------------------------------------------------------
    // Username
    // ---------------------------------------------------------------------

    @Test
    public void username_formatCheckedBeforeLength_tooShortInvalidChar_returnsFormat() {
        // "a!" is both too short (< 3) and has an invalid char: FORMAT wins.
        assertEquals(ValidationError.USERNAME_FORMAT, validator.validateUsername("a!"));
    }

    @Test
    public void username_formatCheckedBeforeLength_tooLongInvalidChar_returnsFormat() {
        // 40 chars containing a space (invalid char) is too long: FORMAT wins.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append("a");
        }
        sb.setCharAt(10, ' ');
        assertEquals(ValidationError.USERNAME_FORMAT, validator.validateUsername(sb.toString()));
    }

    @Test
    public void username_validCharsetBelowMin_returnsTooShort() {
        assertEquals(ValidationError.USERNAME_TOO_SHORT, validator.validateUsername("ab"));
    }

    @Test
    public void username_validCharsetAboveMax_returnsTooLong() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 31; i++) {
            sb.append("a");
        }
        assertEquals(ValidationError.USERNAME_TOO_LONG, validator.validateUsername(sb.toString()));
    }

    @Test
    public void username_boundaryMinLength_isValid() {
        assertNull(validator.validateUsername("abc"));
    }

    @Test
    public void username_boundaryMaxLength_isValid() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("a");
        }
        assertNull(validator.validateUsername(sb.toString()));
    }

    @Test
    public void username_empty_isValidBecauseOptional() {
        assertNull(validator.validateUsername(""));
    }

    @Test
    public void username_onlyAtSign_isValidBecauseOptional() {
        assertNull(validator.validateUsername("@"));
    }

    @Test
    public void username_leadingAtStripped_thenValidated() {
        assertNull(validator.validateUsername("@john_doe"));
    }

    // ---------------------------------------------------------------------
    // Email
    // ---------------------------------------------------------------------

    @Test
    public void email_empty_returnsRequired() {
        assertEquals(ValidationError.EMAIL_REQUIRED, validator.validateEmail(""));
    }

    @Test
    public void email_null_returnsRequired() {
        assertEquals(ValidationError.EMAIL_REQUIRED, validator.validateEmail(null));
    }

    @Test
    public void email_blankWhitespace_returnsRequired() {
        assertEquals(ValidationError.EMAIL_REQUIRED, validator.validateEmail("   "));
    }

    @Test
    public void email_badFormat_returnsInvalid() {
        assertEquals(ValidationError.EMAIL_INVALID, validator.validateEmail("not-an-email"));
    }

    @Test
    public void email_valid_isValid() {
        assertNull(validator.validateEmail("user@example.com"));
    }

    // ---------------------------------------------------------------------
    // New password (validateNewPassword -> base.validatePassword)
    // ---------------------------------------------------------------------

    @Test
    public void newPassword_nullOrEmpty_returnsRequired() {
        assertEquals(ValidationError.PASSWORD_REQUIRED, validator.validateNewPassword(null));
        assertEquals(ValidationError.PASSWORD_REQUIRED, validator.validateNewPassword(""));
    }

    @Test
    public void newPassword_belowMin_returnsTooShort() {
        // 7 chars, otherwise complete complexity.
        assertEquals(ValidationError.PASSWORD_TOO_SHORT, validator.validateNewPassword("Abcde1x"));
    }

    @Test
    public void newPassword_aboveMax_returnsTooLong() {
        // 65 chars with full complexity.
        StringBuilder sb = new StringBuilder("Aa1");
        while (sb.length() < 65) {
            sb.append("a");
        }
        assertEquals(65, sb.length());
        assertEquals(ValidationError.PASSWORD_TOO_LONG, validator.validateNewPassword(sb.toString()));
    }

    @Test
    public void newPassword_boundaryMinLength_isValid() {
        // 8 chars with upper, lower and digit.
        assertNull(validator.validateNewPassword("Abcdef1g"));
    }

    @Test
    public void newPassword_boundaryMaxLength_isValid() {
        // Exactly 64 chars with full complexity.
        StringBuilder sb = new StringBuilder("Aa1");
        while (sb.length() < 64) {
            sb.append("a");
        }
        assertEquals(64, sb.length());
        assertNull(validator.validateNewPassword(sb.toString()));
    }

    @Test
    public void newPassword_missingUpper_returnsNoUpper() {
        assertEquals(ValidationError.PASSWORD_NO_UPPER, validator.validateNewPassword("abcdef1g"));
    }

    @Test
    public void newPassword_missingLower_returnsNoLower() {
        assertEquals(ValidationError.PASSWORD_NO_LOWER, validator.validateNewPassword("ABCDEF1G"));
    }

    @Test
    public void newPassword_missingDigit_returnsNoDigit() {
        assertEquals(ValidationError.PASSWORD_NO_DIGIT, validator.validateNewPassword("Abcdefgh"));
    }

    @Test
    public void newPassword_fullyValid_isValid() {
        assertNull(validator.validateNewPassword("Str0ngPass"));
    }

    // ---------------------------------------------------------------------
    // Confirm password
    // ---------------------------------------------------------------------

    @Test
    public void confirm_null_returnsRequired() {
        assertEquals(ValidationError.CONFIRM_REQUIRED, validator.validateConfirm("Str0ngPass", null));
    }

    @Test
    public void confirm_empty_returnsRequired() {
        assertEquals(ValidationError.CONFIRM_REQUIRED, validator.validateConfirm("Str0ngPass", ""));
    }

    @Test
    public void confirm_mismatch_returnsMismatch() {
        assertEquals(ValidationError.CONFIRM_MISMATCH,
                validator.validateConfirm("Str0ngPass", "Other0ne"));
    }

    @Test
    public void confirm_exactMatch_isValid() {
        assertNull(validator.validateConfirm("Str0ngPass", "Str0ngPass"));
    }

    // ---------------------------------------------------------------------
    // validateProfile: composes only USERNAME and EMAIL
    // ---------------------------------------------------------------------

    @Test
    public void validateProfile_validPair_isValid() {
        ValidationResult result = validator.validateProfile("john_doe", "user@example.com");
        assertTrue(result.isValid());
    }

    @Test
    public void validateProfile_invalidPair_reportsUsernameAndEmail() {
        ValidationResult result = validator.validateProfile("ab", "bad-email");
        assertFalse(result.isValid());
        assertEquals(ValidationError.USERNAME_TOO_SHORT,
                result.errorFor(ValidationResult.Field.USERNAME));
        assertEquals(ValidationError.EMAIL_INVALID,
                result.errorFor(ValidationResult.Field.EMAIL));
    }

    @Test
    public void validateProfile_neverIncludesFullNameOrPassword() {
        // Even with values that would be invalid elsewhere, only USERNAME and
        // EMAIL are considered by the profile form.
        ValidationResult result = validator.validateProfile("ab", "bad-email");
        assertNull(result.errorFor(ValidationResult.Field.FULL_NAME));
        assertNull(result.errorFor(ValidationResult.Field.PASSWORD));
        assertNull(result.errorFor(ValidationResult.Field.CONFIRM_PASSWORD));
    }

    @Test
    public void validateProfile_emptyUsernameStillValid_emailRequired() {
        // Username is optional (empty -> valid); missing email -> EMAIL_REQUIRED.
        ValidationResult result = validator.validateProfile("", "");
        assertFalse(result.isValid());
        assertNull(result.errorFor(ValidationResult.Field.USERNAME));
        assertEquals(ValidationError.EMAIL_REQUIRED,
                result.errorFor(ValidationResult.Field.EMAIL));
    }
}
