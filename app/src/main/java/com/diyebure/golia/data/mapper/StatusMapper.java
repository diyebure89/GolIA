package com.diyebure.golia.data.mapper;

import android.util.Log;

import com.diyebure.golia.domain.model.MatchStatus;

import javax.inject.Inject;

/**
 * Maps API-Football fixture status short codes to the domain {@link MatchStatus}.
 *
 * <p>Implements the normalization table defined in Requirement 6B:</p>
 * <ul>
 *     <li>NS &rarr; SCHEDULED</li>
 *     <li>1H, HT, 2H, ET, BT, P, LIVE &rarr; LIVE</li>
 *     <li>FT, AET, PEN &rarr; FINISHED</li>
 *     <li>PST &rarr; POSTPONED</li>
 *     <li>CANC, ABD &rarr; CANCELLED</li>
 * </ul>
 *
 * <p>The {@link #map(String)} operation is total and deterministic: it never throws
 * and never returns {@code null}. Null, empty, or unrecognized codes fall back to
 * {@link MatchStatus#SCHEDULED} and the unrecognized code is logged.</p>
 */
public class StatusMapper {

    private static final String TAG = "StatusMapper";

    @Inject
    public StatusMapper() {
    }

    /**
     * Normalizes an API-Football status short code to a domain {@link MatchStatus}.
     *
     * @param apiShortCode the short status code from the API (may be null/empty)
     * @return the corresponding {@link MatchStatus}; {@link MatchStatus#SCHEDULED}
     * for null, empty, or unrecognized codes
     */
    public MatchStatus map(String apiShortCode) {
        if (apiShortCode == null || apiShortCode.trim().isEmpty()) {
            Log.w(TAG, "Received null or empty status code; defaulting to SCHEDULED");
            return MatchStatus.SCHEDULED;
        }

        String code = apiShortCode.trim().toUpperCase();

        switch (code) {
            case "NS":
                return MatchStatus.SCHEDULED;
            case "1H":
            case "HT":
            case "2H":
            case "ET":
            case "BT":
            case "P":
            case "LIVE":
                return MatchStatus.LIVE;
            case "FT":
            case "AET":
            case "PEN":
                return MatchStatus.FINISHED;
            case "PST":
                return MatchStatus.POSTPONED;
            case "CANC":
            case "ABD":
                return MatchStatus.CANCELLED;
            default:
                Log.w(TAG, "Unrecognized status code '" + apiShortCode + "'; defaulting to SCHEDULED");
                return MatchStatus.SCHEDULED;
        }
    }
}
