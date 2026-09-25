package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.repository.MatchRepository;

import java.util.List;

import javax.inject.Inject;

/**
 * Refreshes match data for a specific date from the remote API.
 *
 * <p>A single request covers all target leagues for that date; the freshly
 * fetched matches are persisted to the cache by the data layer and delivered
 * through the callback.
 *
 * <p>The {@link MatchRepository} methods are already asynchronous (the
 * data-layer implementation runs its blocking work on the shared IO executor),
 * so this use case is a thin, threading-free delegation to
 * {@link MatchRepository#refreshMatchesByDate(String, Callback)}.
 */
public class RefreshMatchesUseCase {

    private final MatchRepository matchRepository;

    @Inject
    public RefreshMatchesUseCase(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /**
     * Refreshes the matches for the given date.
     *
     * @param isoDate  the target date in ISO-8601 format ({@code yyyy-MM-dd})
     * @param callback receives {@code Result.Success<List<Match>>} with the
     *                 refreshed matches, or {@code Result.Error} on failure
     */
    public void execute(String isoDate, Callback<List<Match>> callback) {
        matchRepository.refreshMatchesByDate(isoDate, callback);
    }
}
