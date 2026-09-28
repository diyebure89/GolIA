package com.diyebure.golia.presentation.ui.history;

import androidx.annotation.Nullable;

/**
 * Immutable presentation model for one row of the prediction history
 * ({@code HistorialPronosticosActivity}, R8).
 *
 * <p>Derived from a {@link com.diyebure.golia.domain.model.PredictionWithMatch}
 * in the {@link HistorialViewModel}; the adapter only renders it. When the
 * referenced match is no longer in the local cache, {@link #matchLabel} carries
 * the "Partido no disponible" placeholder while the predicted score is still
 * shown, so a missing match never breaks the row (R8.3).</p>
 *
 * <p>The {@link #status} discriminates the three visual states (R8.4):
 * {@link Status#PENDING} while the match is not resolved ({@code isCorrect} is
 * null), {@link Status#CORRECT} when {@code isCorrect} is true and
 * {@link Status#WRONG} when false. {@link #pointsText} is non-null only once the
 * prediction is resolved so the adapter can hide the points chip while pending
 * (R8.4, R8.5).</p>
 */
public final class PredictionHistoryUiModel {

    /** Visual status of a prediction row (R8.4). */
    public enum Status {
        PENDING,
        CORRECT,
        WRONG
    }

    /** Team names ("Local vs Visitante") or the "Partido no disponible" placeholder. */
    public final String matchLabel;
    /** Predicted score, e.g. "2 - 1" (always shown, even for a missing match). */
    public final String scoreText;
    /** Resolution status used to pick the label/colour. */
    public final Status status;
    /** Points earned, only present once the prediction is resolved; {@code null} while pending. */
    @Nullable
    public final String pointsText;

    public PredictionHistoryUiModel(String matchLabel,
                                    String scoreText,
                                    Status status,
                                    @Nullable String pointsText) {
        this.matchLabel = matchLabel;
        this.scoreText = scoreText;
        this.status = status;
        this.pointsText = pointsText;
    }
}
