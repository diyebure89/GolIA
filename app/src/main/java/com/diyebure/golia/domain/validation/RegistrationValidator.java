package com.diyebure.golia.domain.validation;

import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * Centralized, framework-agnostic validation for the local registration flow.
 * Each per-field method returns the first {@link ValidationError} detected or
 * {@code null} when the field is valid, and {@link #validateRegistration} composes
 * them into an immutable {@link ValidationResult}.
 *
 * <p>Lives in the domain layer so it stays independent of the UI. The only
 * Android touch point is the email pattern check, isolated behind the
 * {@code protected} {@link #isEmailPattern(String)} hook so it can be overridden
 * in pure-JVM tests.
 */
public class RegistrationValidator {

    private static final int FULL_NAME_MIN = 3;
    private static final int FULL_NAME_MAX = 50;
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 30;
    private static final int PASSWORD_MIN = 8;

    /** Allowed character set for a username (without the optional leading '@'). */
    private static final Pattern USERNAME_CHARSET = Pattern.compile("^[A-Za-z0-9_.]+$");

    private static final Pattern HAS_UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*[0-9].*");

    @Inject
    public RegistrationValidator() {
    }

    /**
     * Validates the full name (R1). Length is measured on the trimmed value.
     *
     * @param fullName raw full name, may be {@code null}
     * @return the detected error, or {@code null} when valid
     */
    public ValidationError validateFullName(String fullName) {
        String value = fullName == null ? "" : fullName.trim();
        if (value.isEmpty()) {
            return ValidationError.FULL_NAME_REQUIRED;
        }
        if (value.length() < FULL_NAME_MIN) {
            return ValidationError.FULL_NAME_TOO_SHORT;
        }
        if (value.length() > FULL_NAME_MAX) {
            return ValidationError.FULL_NAME_TOO_LONG;
        }
        return null;
    }

    /**
     * Validates the optional username (R2). The value is trimmed and a single
     * leading '@' is stripped; an empty effective value means "not provided"
     * and is therefore valid. When provided it must match
     * {@code ^[A-Za-z0-9_.]{3,30}$}: format is checked before length.
     *
     * @param username raw username, may be {@code null}
     * @return the detected error, or {@code null} when valid or not provided
     */
    public ValidationError validateUsername(String username) {
        String value = username == null ? "" : username.trim();
        if (value.startsWith("@")) {
            value = value.substring(1);
        }
        if (value.isEmpty()) {
            // Optional field, not provided.
            return null;
        }
        if (!USERNAME_CHARSET.matcher(value).matches()) {
            return ValidationError.USERNAME_FORMAT;
        }
        if (value.length() < USERNAME_MIN) {
            return ValidationError.USERNAME_TOO_SHORT;
        }
        if (value.length() > USERNAME_MAX) {
            return ValidationError.USERNAME_TOO_LONG;
        }
        return null;
    }

    /**
     * Validates the email (R3). Required; the format check is delegated to
     * {@link #isEmailPattern(String)}.
     *
     * @param email raw email, may be {@code null}
     * @return the detected error, or {@code null} when valid
     */
    public ValidationError validateEmail(String email) {
        String value = email == null ? "" : email.trim();
        if (value.isEmpty()) {
            return ValidationError.EMAIL_REQUIRED;
        }
        if (!isEmailPattern(email)) {
            return ValidationError.EMAIL_INVALID;
        }
        return null;
    }

    /**
     * Email pattern hook. In production this uses the Android
     * {@code Patterns.EMAIL_ADDRESS} matcher. Declared {@code protected} so
     * pure-JVM tests can override it without pulling in Android APIs.
     *
     * @param email the raw email
     * @return {@code true} when it matches the platform email pattern
     */
    protected boolean isEmailPattern(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validates the password (R4). Not trimmed. Required and at least
     * {@value #PASSWORD_MIN} characters, then must contain at least one
     * uppercase letter, one lowercase letter and one digit; the first failing
     * condition (in that order) is returned.
     *
     * @param password raw password, may be {@code null}
     * @return the detected error, or {@code null} when valid
     */
    public ValidationError validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationError.PASSWORD_REQUIRED;
        }
        if (password.length() < PASSWORD_MIN) {
            return ValidationError.PASSWORD_TOO_SHORT;
        }
        if (!HAS_UPPER.matcher(password).matches()) {
            return ValidationError.PASSWORD_NO_UPPER;
        }
        if (!HAS_LOWER.matcher(password).matches()) {
            return ValidationError.PASSWORD_NO_LOWER;
        }
        if (!HAS_DIGIT.matcher(password).matches()) {
            return ValidationError.PASSWORD_NO_DIGIT;
        }
        return null;
    }

    /**
     * Validates the password confirmation (R5). Not trimmed. Required and must
     * match {@code password} character by character.
     *
     * @param password the original password
     * @param confirm  the confirmation value, may be {@code null}
     * @return the detected error, or {@code null} when valid
     */
    public ValidationError validateConfirm(String password, String confirm) {
        if (confirm == null || confirm.isEmpty()) {
            return ValidationError.CONFIRM_REQUIRED;
        }
        if (!confirm.equals(password)) {
            return ValidationError.CONFIRM_MISMATCH;
        }
        return null;
    }

    /**
     * Composes the five per-field validations into a single immutable result.
     * {@code null} (valid) entries are ignored by the builder.
     *
     * @return an aggregate {@link ValidationResult} for the whole form
     */
    public ValidationResult validateRegistration(String fullName, String username, String email,
                                                 String password, String confirmPassword) {
        return ValidationResult.builder()
                .put(ValidationResult.Field.FULL_NAME, validateFullName(fullName))
                .put(ValidationResult.Field.USERNAME, validateUsername(username))
                .put(ValidationResult.Field.EMAIL, validateEmail(email))
                .put(ValidationResult.Field.PASSWORD, validatePassword(password))
                .put(ValidationResult.Field.CONFIRM_PASSWORD, validateConfirm(password, confirmPassword))
                .build();
    }
}
