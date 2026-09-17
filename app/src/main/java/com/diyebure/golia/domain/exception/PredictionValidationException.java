package com.diyebure.golia.domain.exception;

/**
 * Exception thrown when a prediction validation fails.
 */
public class PredictionValidationException extends RuntimeException {

    private final String errorCode;

    public PredictionValidationException(String message) {
        super(message);
        this.errorCode = "PREDICTION_VALIDATION_ERROR";
    }

    public PredictionValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}