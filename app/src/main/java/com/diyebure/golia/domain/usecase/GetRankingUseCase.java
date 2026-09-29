package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.RankingResult;
import com.diyebure.golia.domain.model.Ranking_Calculator;
import com.diyebure.golia.domain.model.Ranking_Participant;
import com.diyebure.golia.domain.model.Ranking_Period;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Computes the ranking for a given {@link Ranking_Period} off the main thread
 * (R1.2, R1.3, R7.5).
 *
 * <p>The blocking local reads run on the shared {@link IoExecutor}
 * {@link ExecutorService}: on that background thread it captures {@code now},
 * gathers the participants from {@link Ranking_Participant_Provider} (the real
 * {@code Current_User} from Room plus the in-memory seed profiles) and delegates
 * the pure computation to {@link Ranking_Calculator#rank(List, Ranking_Period, long)},
 * delivering a {@link RankingResult} through the supplied {@link Callback}.</p>
 *
 * <p>No API call is made — only local reads through the provider (R1.6). Any
 * technical failure is surfaced as a {@link Result.Error} with no partial data
 * (R1.8).</p>
 *
 * <p>The {@code token} is a generation token owned by the ViewModel for its
 * anti-overlap ("last wins") logic (R7.6). The use case accepts it per the
 * design contract; because the shared {@link Callback}/{@link Result} types do
 * not carry a token, the ViewModel correlates the callback with the token it
 * captured when dispatching, so the value is not echoed back here.</p>
 */
public class GetRankingUseCase {

    private final Ranking_Participant_Provider participantProvider;
    private final Ranking_Calculator calculator;
    private final ExecutorService executor;

    @Inject
    public GetRankingUseCase(Ranking_Participant_Provider participantProvider,
                             Ranking_Calculator calculator,
                             @IoExecutor ExecutorService executor) {
        this.participantProvider = participantProvider;
        this.calculator = calculator;
        this.executor = executor;
    }

    /**
     * Computes the ranking for the given period off the main thread.
     *
     * @param period   the selected ranking period
     * @param userId   the current user id (available for the ViewModel's flow;
     *                 the participant list is resolved by the provider)
     * @param token    the ViewModel generation token for anti-overlap ("last
     *                 wins"); accepted per the design contract
     * @param callback receives {@code Result.Success<RankingResult>} with the
     *                 ordered ranking, or {@code Result.Error} on failure (no
     *                 partial data)
     */
    public void execute(Ranking_Period period,
                        String userId,
                        long token,
                        Callback<RankingResult> callback) {
        executor.execute(() -> callback.onResult(compute(period)));
    }

    private Result<RankingResult> compute(Ranking_Period period) {
        try {
            long now = System.currentTimeMillis();
            List<Ranking_Participant> participants = participantProvider.getParticipants(now);
            RankingResult result = calculator.rank(participants, period, now);
            return new Result.Success<>(result);
        } catch (Exception e) {
            return new Result.Error(e);
        }
    }
}
