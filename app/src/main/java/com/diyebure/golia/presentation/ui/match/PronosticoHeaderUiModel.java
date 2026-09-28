package com.diyebure.golia.presentation.ui.match;

import androidx.annotation.Nullable;

/**
 * Immutable presentation model for the header of {@code PronosticoActivity}
 * (R5.1): the two team names, their logos and the preformatted kickoff date. The
 * Activity only renders it.
 */
public final class PronosticoHeaderUiModel {

    public final String homeName;
    public final String awayName;
    @Nullable
    public final String homeLogoUrl;
    @Nullable
    public final String awayLogoUrl;
    public final String kickoffLocal;

    public PronosticoHeaderUiModel(String homeName,
                                   String awayName,
                                   @Nullable String homeLogoUrl,
                                   @Nullable String awayLogoUrl,
                                   String kickoffLocal) {
        this.homeName = homeName;
        this.awayName = awayName;
        this.homeLogoUrl = homeLogoUrl;
        this.awayLogoUrl = awayLogoUrl;
        this.kickoffLocal = kickoffLocal;
    }
}
