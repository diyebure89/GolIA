package com.diyebure.golia.domain.error;

/**
 * Domain exception carrying a typed {@link AuthError}.
 *
 * <p>{@code Result.Error} already wraps an {@link Exception}, so this exception lets the
 * typed error travel inside {@code Result.Error} without changing the signature of
 * {@code Result}. The UI extracts the error via
 * {@code ((AuthException) ex).getAuthError()} and maps it to a string resource.
 */
public class AuthException extends Exception {

    private final AuthError error;

    public AuthException(AuthError error) {
        super(error.name());
        this.error = error;
    }

    public AuthException(AuthError error, Throwable cause) {
        super(error.name(), cause);
        this.error = error;
    }

    public AuthError getAuthError() {
        return error;
    }
}
