package com.diyebure.golia.domain.error;

/**
 * Typed domain error for the football news feed feature.
 *
 * <p>{@code Result.Error} already wraps an {@link Exception}, so {@code NewsError}
 * lets a typed news failure travel inside {@code Result.Error} without changing the
 * signature of {@code Result} (same approach as {@link AuthException}). The repository
 * never leaks provider/persistence exceptions: it translates every failure into one of
 * the concrete subclasses below and delivers it as {@code Result.Error(NewsError)}
 * (R11.5). The presentation layer inspects the concrete type to pick the banner shown
 * in {@code Estado_UI}.</p>
 *
 * <p>Banner mapping (per design.md "Error Handling"):</p>
 * <ul>
 *   <li>{@link NetworkError} &rarr; {@code OFFLINE}</li>
 *   <li>{@link ProviderError} &rarr; {@code ERROR}</li>
 *   <li>{@link QuotaExceededError} &rarr; {@code QUOTA}</li>
 *   <li>{@link PersistenceError} &rarr; {@code ERROR}</li>
 *   <li>{@link TimeoutError} &rarr; {@code ERROR}</li>
 * </ul>
 *
 * <p>Requirements: R11.5.</p>
 */
public abstract class NewsError extends Exception {

    protected NewsError(String message) {
        super(message);
    }

    protected NewsError(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * No network connectivity or an {@code IOException} while reaching the provider.
     * Maps to the {@code OFFLINE} banner (R10.1/R10.2).
     */
    public static final class NetworkError extends NewsError {

        public NetworkError(String message) {
            super(message);
        }

        public NetworkError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * The provider answered with a non-429 4xx/5xx status or an invalid body.
     * Maps to the {@code ERROR} banner (R9.4/R10.3).
     */
    public static final class ProviderError extends NewsError {

        public ProviderError(String message) {
            super(message);
        }

        public ProviderError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Daily budget exhausted (&ge;180 credits) or HTTP 429 from the provider,
     * forcing cache-only mode until the UTC reset. Maps to the {@code QUOTA}
     * banner (R6.5/R6.10/R10.3).
     */
    public static final class QuotaExceededError extends NewsError {

        public QuotaExceededError(String message) {
            super(message);
        }

        public QuotaExceededError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * A failure reading from or writing to the local Room cache.
     * Maps to the {@code ERROR} banner.
     */
    public static final class PersistenceError extends NewsError {

        public PersistenceError(String message) {
            super(message);
        }

        public PersistenceError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * The request exceeded the 10s timeout budget (R2.7/R3.6).
     * Maps to the {@code ERROR} banner.
     */
    public static final class TimeoutError extends NewsError {

        public TimeoutError(String message) {
            super(message);
        }

        public TimeoutError(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
