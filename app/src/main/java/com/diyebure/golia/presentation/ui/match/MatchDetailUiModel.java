package com.diyebure.golia.presentation.ui.match;

import androidx.annotation.Nullable;

/**
 * Immutable presentation model describing the header/detail of a match on the
 * {@code DetallePartidoActivity} (R2). Derived from a domain
 * {@link com.diyebure.golia.domain.model.Match} in the ViewModel; the Activity
 * only renders it.
 *
 * <p>The status representation adapts to the {@code MatchStatus} (R2.4/R2.5):
 * SCHEDULED shows only the kickoff time (no score), LIVE shows the running score
 * plus the elapsed minute, FINISHED shows the final score, and
 * POSTPONED/CANCELLED show a localized status label. Fields that do not apply to
 * the current status are {@code null} so the Activity can hide them.</p>
 */
public final class MatchDetailUiModel {

    public final String leagueName;
    public final String round;
    public final String homeName;
    public final String awayName;
    @Nullable
    public final String homeLogoUrl;
    @Nullable
    public final String awayLogoUrl;
    public final String kickoffLocal;
    public final String statusLabel;
    @Nullable
    public final String scoreText;
    @Nullable
    public final String elapsedText;

    public MatchDetailUiModel(String leagueName,
                              String round,
                              String homeName,
                              String awayName,
                              @Nullable String homeLogoUrl,
                              @Nullable String awayLogoUrl,
                              String kickoffLocal,
                              String statusLabel,
                              @Nullable String scoreText,
                              @Nullable String elapsedText) {
        this.leagueName = leagueName;
        this.round = round;
        this.homeName = homeName;
        this.awayName = awayName;
        this.homeLogoUrl = homeLogoUrl;
        this.awayLogoUrl = awayLogoUrl;
        this.kickoffLocal = kickoffLocal;
        this.statusLabel = statusLabel;
        this.scoreText = scoreText;
        this.elapsedText = elapsedText;
    }
}
