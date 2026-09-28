package com.diyebure.golia.domain.error;

/**
 * Domain exception carrying a typed {@link PredictionError}.
 *
 * <p>{@code Result.Error} already wraps an {@link Exception}, so this exception lets the
 * typed error travel inside {@code Result.Error} without changing the signature of
 * {@code Result}. The UI extracts the error via
 * {@code ((PredictionException) ex).getPredictionError()} and maps it to a string resource.
 */
public class PredictionException extends Exception {

    private final PredictionError error;

    public PredictionException(PredictionError error) {
        super(error.name());
        this.error = error;
    }

    public PredictionException(PredictionError error, Throwable cause) {
        super(error.name(), cause);
        this.error = error;
    }

    public PredictionError getPredictionError() {
        return error;
    }
}
