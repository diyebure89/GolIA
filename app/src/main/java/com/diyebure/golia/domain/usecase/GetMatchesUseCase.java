package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.repository.MatchRepository;

import java.util.List;

import javax.inject.Inject;

/**
 * Retrieves the currently cached matches following a cache-first strategy.
 *
 * <p>A use case represents one business action. It depends only on the domain
 * {@link MatchRepository} interface (never on a concrete data class), which
 * keeps business rules independent of networking/persistence details.
 *
 * <p>Unlike the auth use cases, the {@link MatchRepository} methods are already
 * asynchronous: the data-layer implementation runs its blocking network/Room
 * work on the shared IO executor and publishes the outcome through a
 * {@link Callback}. This use case is therefore a thin, threading-free
 * delegation to {@link MatchRepository#getMatches(Callback)}.
 */
public class GetMatchesUseCase {

    private final MatchRepository matchRepository;

    @Inject
    public GetMatchesUseCase(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /**
     * Delivers the cached matches without hitting the network.
     *
     * @param callback receives {@code Result.Success<List<Match>>} with the
     *                 cached matches, or {@code Result.Error} on failure
     */
    public void execute(Callback<List<Match>> callback) {
        matchRepository.getMatches(callback);
    }
}
