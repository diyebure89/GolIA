package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.PredictionError;
import com.diyebure.golia.domain.error.PredictionException;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchSeasonStats;
import com.diyebure.golia.domain.model.SeasonStats;
import com.diyebure.golia.domain.model.Season_Stats_Calculator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Derives the per-team season statistics of a match's home and away teams from
 * the finished matches available in the local cache (R3.1).
 *
 * <p>The blocking Room reads ({@link MatchDao#getFinishedMatchesByTeam(String)})
 * run on the shared {@link IoExecutor} {@link ExecutorService}; no network
 * request is made and the request budget is never touched. The pure
 * {@link Season_Stats_Calculator} then computes the {@link SeasonStats} for each
 * team and the outcome is delivered through the supplied {@link Callback} as a
 * {@link Result} carrying a {@link MatchSeasonStats}, or a typed error.
 */
public class GetSeasonStatsUseCase {

    private final MatchDao matchDao;
    private final Season_Stats_Calculator calculator;
    private final ExecutorService executor;

    @Inject
    public GetSeasonStatsUseCase(MatchDao matchDao,
                                 Season_Stats_Calculator calculator,
                                 @IoExecutor ExecutorService executor) {
        this.matchDao = matchDao;
        this.calculator = calculator;
        this.executor = executor;
    }

    /**
     * Computes the season statistics of both teams of the given match off the
     * main thread.
     *
     * @param match    the match whose teams' statistics are computed
     * @param callback receives {@code Result.Success<MatchSeasonStats>} with the
     *                 home/away statistics, or {@code Result.Error} on failure
     */
    public void execute(Match match, Callback<MatchSeasonStats> callback) {
        executor.execute(() -> callback.onResult(compute(match)));
    }

    private Result<MatchSeasonStats> compute(Match match) {
        if (match == null) {
            return new Result.Error(new PredictionException(PredictionError.MATCH_NOT_FOUND));
        }
        try {
            SeasonStats home = statsForTeam(match.getHomeTeamId());
            SeasonStats away = statsForTeam(match.getAwayTeamId());
            return new Result.Success<>(new MatchSeasonStats(home, away));
        } catch (Exception e) {
            return new Result.Error(new PredictionException(PredictionError.PERSISTENCE_ERROR, e));
        }
    }

    private SeasonStats statsForTeam(String teamId) {
        List<Match> matches = toDomainMatches(matchDao.getFinishedMatchesByTeam(teamId));
        int wins = calculator.wins(teamId, matches);
        double goalsAverage = calculator.goalsAverage(teamId, matches);
        int recentWins = calculator.recentForm(teamId, matches);
        return new SeasonStats(wins, goalsAverage, recentWins);
    }

    private static List<Match> toDomainMatches(List<MatchEntity> entities) {
        List<Match> result = new ArrayList<>();
        if (entities != null) {
            for (MatchEntity entity : entities) {
                if (entity != null) {
                    result.add(entity.toDomainModel());
                }
            }
        }
        return result;
    }
}
