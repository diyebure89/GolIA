package com.diyebure.golia.presentation;

import com.diyebure.golia.domain.validation.ValidationError;

/**
 * Immutable snapshot of the editable state of the profile form, retained inside
 * {@code PerfilViewModel} so it survives configuration changes such as screen
 * rotation (R14.4).
 *
 * <p>The full name is read-only (the profile only edits username and email) and,
 * per the design, is normalized to an empty string when the source value is
 * {@code null} so the UI never renders the literal {@code "null"} (R1.5).
 *
 * <p>Per-field validation errors are carried as typed {@link ValidationError}
 * values (or {@code null} when the field is valid); the presentation layer maps
 * each one to a localized string resource shown via {@code TextInputLayout.setError}.
 */
public final class ProfileFormState {

    private final String fullName;      // read-only, never "null"
    private final String email;         // editable
    private final String username;      // editable ("" == no username)
    private final ValidationError emailError;    // null == valid
    private final ValidationError usernameError; // null == valid

    /**
     * @param fullName      the read-only full name; {@code null} is coerced to {@code ""}
     * @param email         the editable email value
     * @param username      the editable username value
     * @param emailError    the email field error, or {@code null} when valid
     * @param usernameError the username field error, or {@code null} when valid
     */
    public ProfileFormState(String fullName,
                            String email,
                            String username,
                            ValidationError emailError,
                            ValidationError usernameError) {
        // Never expose the literal "null": coerce a null full name to "" (R1.5).
        this.fullName = fullName == null ? "" : fullName;
        this.email = email;
        this.username = username;
        this.emailError = emailError;
        this.usernameError = usernameError;
    }

    /**
     * Builds an initial form state from the loaded values, without any errors.
     *
     * @param fullName the read-only full name (may be {@code null})
     * @param email    the current email
     * @param username the current username
     * @return a fresh {@link ProfileFormState} with no per-field errors
     */
    public static ProfileFormState of(String fullName, String email, String username) {
        return new ProfileFormState(fullName, email, username, null, null);
    }

    /**
     * Returns a copy of this state with the given per-field errors, keeping the
     * currently edited values so the user can fix them in place (R6.7, R13.4).
     *
     * @param emailError    the email field error, or {@code null}
     * @param usernameError the username field error, or {@code null}
     * @return a new {@link ProfileFormState} carrying the errors
     */
    public ProfileFormState withErrors(ValidationError emailError, ValidationError usernameError) {
        return new ProfileFormState(fullName, email, username, emailError, usernameError);
    }

    /**
     * Returns a copy of this state with new edited values and cleared errors.
     *
     * @param email    the edited email
     * @param username the edited username
     * @return a new {@link ProfileFormState} with the given values and no errors
     */
    public ProfileFormState withValues(String email, String username) {
        return new ProfileFormState(fullName, email, username, null, null);
    }

    /** @return the read-only full name; never {@code null} (empty string when absent). */
    public String getFullName() {
        return fullName;
    }

    /** @return the editable email value. */
    public String getEmail() {
        return email;
    }

    /** @return the editable username value ({@code ""} means no username). */
    public String getUsername() {
        return username;
    }

    /** @return the email field error, or {@code null} when the field is valid. */
    public ValidationError getEmailError() {
        return emailError;
    }

    /** @return the username field error, or {@code null} when the field is valid. */
    public ValidationError getUsernameError() {
        return usernameError;
    }
}
