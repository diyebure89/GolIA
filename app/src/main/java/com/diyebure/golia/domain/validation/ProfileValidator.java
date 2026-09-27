package com.diyebure.golia.domain.validation;

import javax.inject.Inject;

/**
 * Framework-agnostic validation for the profile-editing flow. It reuses the
 * exact same field rules as registration by delegating every check to
 * {@link RegistrationValidator}, guaranteeing that a value considered valid on
 * sign-up stays valid when edited from the profile screen (R15).
 *
 * <p>Lives in the domain layer so it stays independent of the UI. Unlike
 * registration, the profile form only edits the username and email; the full
 * name is read-only and the password is handled through a dedicated change
 * flow, so {@link #validateProfile(String, String)} composes only the editable
 * fields.
 */
public class ProfileValidator {

    private final RegistrationValidator base;

    @Inject
    public ProfileValidator(RegistrationValidator base) {
        this.base = base;
    }

    /**
     * Validates the username (R3) by delegating to the shared base validator.
     *
     * @param username raw username, may be {@code null}
     * @return the detected error, or {@code null} when valid or not provided
     */
    public ValidationError validateUsername(String username) {
        return base.validateUsername(username);
    }

    /**
     * Validates the email (R4) by delegating to the shared base validator.
     *
     * @param email raw email, may be {@code null}
     * @return the detected error, or {@code null} when valid
     */
    public ValidationError validateEmail(String email) {
        return base.validateEmail(email);
    }

    /**
     * Validates a new password (R15.2) by delegating to the shared base
     * validator, applying the identical complexity and length rules used on
     * registration.
     *
     * @param pwd raw new password, may be {@code null}
     * @return the detected error, or {@code null} when valid
     */
    public ValidationError validateNewPassword(String pwd) {
        return base.validatePassword(pwd);
    }

    /**
     * Validates the password confirmation (R15.3) by delegating to the shared
     * base validator.
     *
     * @param pwd     the new password
     * @param confirm the confirmation value, may be {@code null}
     * @return the detected error, or {@code null} when valid
     */
    public ValidationError validateConfirm(String pwd, String confirm) {
        return base.validateConfirm(pwd, confirm);
    }

    /**
     * Composes the validations for the editable profile fields into a single
     * immutable result. Only {@link ValidationResult.Field#USERNAME} and
     * {@link ValidationResult.Field#EMAIL} are considered; the full name is
     * read-only and the password is validated through the dedicated change
     * flow. {@code null} (valid) entries are ignored by the builder.
     *
     * @param username raw username, may be {@code null}
     * @param email    raw email, may be {@code null}
     * @return an aggregate {@link ValidationResult} for the editable fields
     */
    public ValidationResult validateProfile(String username, String email) {
        return ValidationResult.builder()
                .put(ValidationResult.Field.USERNAME, validateUsername(username))
                .put(ValidationResult.Field.EMAIL, validateEmail(email))
                .build();
    }
}
