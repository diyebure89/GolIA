package com.diyebure.golia.presentation.ui.match;

/**
 * Data the {@code PronosticoActivity} carries to the confirmation screen after a
 * successful save (R5.11): the match id, both team names, the predicted scores
 * and the preformatted match date. The Activity turns this into the Intent
 * extras {@code ConfirmacionActivity} expects.
 */
public final class PronosticoConfirmationArgs {

    public final String matchId;
    public final String homeName;
    public final String awayName;
    public final int homeScore;
    public final int awayScore;
    public final String matchDate;

    public PronosticoConfirmationArgs(String matchId,
                                      String homeName,
                                      String awayName,
                                      int homeScore,
                                      int awayScore,
                                      String matchDate) {
        this.matchId = matchId;
        this.homeName = homeName;
        this.awayName = awayName;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.matchDate = matchDate;
    }
}
