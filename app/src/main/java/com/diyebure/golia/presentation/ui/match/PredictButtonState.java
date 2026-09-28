package com.diyebure.golia.presentation.ui.match;

/**
 * State of the "Hacer Pronóstico" button on the match detail (R4), derived from
 * the {@code Prediction_Lock_Threshold} and whether the user already has a
 * prediction for the match:
 * <ul>
 *   <li>{@link #ENABLED_NEW}: match open and no prediction yet — create one.</li>
 *   <li>{@link #ENABLED_EDIT}: match open and a prediction exists — edit it.</li>
 *   <li>{@link #VIEW_ONLY}: match closed but a prediction exists — read only.</li>
 *   <li>{@link #CLOSED}: match closed and no prediction — disabled.</li>
 * </ul>
 */
public enum PredictButtonState {
    ENABLED_NEW,
    ENABLED_EDIT,
    VIEW_ONLY,
    CLOSED
}
