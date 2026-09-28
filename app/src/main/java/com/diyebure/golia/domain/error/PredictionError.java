package com.diyebure.golia.domain.error;

/**
 * Typed prediction error contract (R2.7, R5.6, R5.7, R10.5).
 *
 * <p>These values travel inside {@code Result.Error} carried by a
 * {@link PredictionException}, and are translated to localized string resources by the UI.
 */
public enum PredictionError {
    SESSION_UNAVAILABLE,   // no hay sesión válida al operar
    PREDICTION_CLOSED,     // el pronóstico está cerrado (fuera de plazo)
    MATCH_NOT_FOUND,       // el partido no existe
    PERSISTENCE_ERROR,     // fallo al persistir el pronóstico
    VALIDATION_ERROR       // datos de pronóstico inválidos
}
