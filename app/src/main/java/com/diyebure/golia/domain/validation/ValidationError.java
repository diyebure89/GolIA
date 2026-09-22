package com.diyebure.golia.domain.validation;

/**
 * Typed, per-field validation errors produced by the registration validator.
 * Lives in the domain layer so it stays independent of Android/UI concerns;
 * the presentation layer maps each value to a localized string resource.
 */
public enum ValidationError {

    // Full name (R1)
    FULL_NAME_REQUIRED,
    FULL_NAME_TOO_SHORT,
    FULL_NAME_TOO_LONG,

    // Username (R2) - optional field
    USERNAME_TOO_SHORT,
    USERNAME_TOO_LONG,
    USERNAME_FORMAT,

    // Email (R3)
    EMAIL_REQUIRED,
    EMAIL_INVALID,

    // Password (R4)
    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT,
    PASSWORD_NO_UPPER,
    PASSWORD_NO_LOWER,
    PASSWORD_NO_DIGIT,

    // Confirm password (R5)
    CONFIRM_REQUIRED,
    CONFIRM_MISMATCH
}
