package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.repository.MatchRepository;

import java.util.List;

import javax.inject.Inject;

/**
 * Refreshes live match data (scores and elapsed minute) from the remote API.
 *
 * <p>Intended to be called more frequently than a full refresh while there are
 * live matches visible.
 *
 * <p>The {@link MatchRepository} methods are already asynchronous (the
 * data-layer implementation runs its blocking work on the shared IO executor),
 * so this use case is a thin, threading-free delegation to
 * {@link MatchRepository#refreshLiveMatches(Callback)}.
 */
public class RefreshLiveMatchesUseCase {

    private final MatchRepository matchRepository;

    @Inject
    public RefreshLiveMatchesUseCase(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /**
     * Refreshes the live match data.
     *
     * @param callback receives {@code Result.Success<List<Match>>} with the
     *                 updated matches, or {@code Result.Error} on failure
     */
    public void execute(Callback<List<Match>> callback) {
        matchRepository.refreshLiveMatches(callback);
    }
}
