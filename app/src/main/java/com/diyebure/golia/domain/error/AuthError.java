package com.diyebure.golia.domain.error;

/**
 * Typed authentication error contract (R15).
 *
 * <p>These values travel inside {@code Result.Error} carried by an
 * {@link AuthException}, and are translated to localized string resources by the UI.
 */
public enum AuthError {
    USERNAME_TAKEN,
    EMAIL_TAKEN,
    INVALID_CREDENTIALS,
    PERSISTENCE_ERROR,
    VALIDATION_ERROR
}
