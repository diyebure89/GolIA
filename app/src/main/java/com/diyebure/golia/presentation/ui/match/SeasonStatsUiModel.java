package com.diyebure.golia.presentation.ui.match;

/**
 * Immutable presentation model for the "Estadísticas de temporada" section of
 * the match detail (R3): three comparative metrics between the home and away
 * team — wins, goals average and recent form — each accompanied by its
 * normalized bar ratio (home share in {@code [0, 1]}) so the Activity can size
 * two opposing bars without doing any math itself.
 */
public final class SeasonStatsUiModel {

    public final int homeWins;
    public final int awayWins;
    public final double homeAvg;
    public final double awayAvg;
    public final int homeFormV;
    public final int awayFormV;

    /** Home share of the wins bar, in {@code [0, 1]} (away share is 1 - this). */
    public final double winsRatio;
    /** Home share of the goals-average bar, in {@code [0, 1]}. */
    public final double avgRatio;
    /** Home share of the recent-form bar, in {@code [0, 1]}. */
    public final double formRatio;

    public SeasonStatsUiModel(int homeWins,
                              int awayWins,
                              double homeAvg,
                              double awayAvg,
                              int homeFormV,
                              int awayFormV,
                              double winsRatio,
                              double avgRatio,
                              double formRatio) {
        this.homeWins = homeWins;
        this.awayWins = awayWins;
        this.homeAvg = homeAvg;
        this.awayAvg = awayAvg;
        this.homeFormV = homeFormV;
        this.awayFormV = awayFormV;
        this.winsRatio = winsRatio;
        this.avgRatio = avgRatio;
        this.formRatio = formRatio;
    }
}
