package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.repository.MatchRepository;

import javax.inject.Inject;

/**
 * Retrieves a single {@link Match} by its primary-key id following a cache-first
 * strategy (R2.1).
 *
 * <p>Like {@link GetMatchesUseCase}, this use case is a thin delegation to the
 * already-asynchronous {@link MatchRepository}: the data-layer implementation
 * runs its blocking Room read on the shared IO executor and never performs a
 * network request nor touches the request budget. The outcome is delivered
 * through the supplied {@link Callback} as a {@link Result} that is either the
 * cached match or a typed error (e.g.
 * {@link com.diyebure.golia.domain.error.PredictionError#MATCH_NOT_FOUND} when
 * no match with that id exists in the cache).
 */
public class GetMatchByIdUseCase {

    private final MatchRepository matchRepository;

    @Inject
    public GetMatchByIdUseCase(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /**
     * Delivers the cached match with the given id without hitting the network.
     *
     * @param matchId  the primary-key id of the match to read
     * @param callback receives {@code Result.Success<Match>} with the cached
     *                 match, or {@code Result.Error} when it does not exist
     */
    public void execute(String matchId, Callback<Match> callback) {
        matchRepository.getMatchById(matchId, callback);
    }
}
