package com.diyebure.golia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.diyebure.golia.data.local.dao.CompetitionDao;
import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.dao.TeamDao;
import com.diyebure.golia.data.local.entity.CompetitionEntity;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.data.local.entity.TeamEntity;
import com.diyebure.golia.data.remote.api.FootballApiService;
import com.diyebure.golia.data.remote.dto.MatchDto;
import com.diyebure.golia.data.remote.dto.MatchDto.CompetitionDto;
import com.diyebure.golia.data.remote.dto.MatchDto.MatchResponseDto;
import com.diyebure.golia.data.remote.dto.MatchDto.TeamDto;
import com.diyebure.golia.domain.model.Competition;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.Team;
import com.diyebure.golia.domain.repository.MatchRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Implementation of MatchRepository that provides data from both
 * remote API and local database with network-first, cache-back strategy.
 * Supports offline functionality by caching data locally.
 */
@Singleton
public class MatchRepositoryImpl extends BaseRepository implements MatchRepository {

    private static final String TAG = "MatchRepository";
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    private final FootballApiService apiService;
    private final MatchDao matchDao;
    private final TeamDao teamDao;
    private final CompetitionDao competitionDao;
    private final ExecutorService executor;

    @Inject
    public MatchRepositoryImpl(
            FootballApiService apiService,
            MatchDao matchDao,
            TeamDao teamDao,
            CompetitionDao competitionDao) {
        this.apiService = apiService;
        this.matchDao = matchDao;
        this.teamDao = teamDao;
        this.competitionDao = competitionDao;
        this.executor = Executors.newFixedThreadPool(4);
    }

    @Override
    public List<Match> getUpcomingMatches() {
        List<MatchEntity> cached = matchDao.getUpcomingMatches();
        if (!cached.isEmpty()) {
            return toMatchList(cached);
        }

        // Try to fetch from network
        refreshMatches();

        // Return cached data (may still be empty if network failed)
        return toMatchList(matchDao.getUpcomingMatches());
    }

    @Override
    public List<Match> getLiveMatches() {
        List<MatchEntity> cached = matchDao.getLiveMatches();
        if (!cached.isEmpty()) {
            return toMatchList(cached);
        }

        // Try to fetch from network
        refreshLiveMatches();

        return toMatchList(matchDao.getLiveMatches());
    }

    @Override
    public Match getMatchById(UUID id) {
        MatchEntity cached = matchDao.getMatchById(id.toString());
        if (cached != null) {
            return cached.toDomainModel();
        }

        // Try to fetch from network
        try {
            Response<MatchResponseDto> response = apiService.getMatchById(id.toString()).execute();
            if (response.isSuccessful() && response.body() != null) {
                List<MatchDto.MatchItemDto> matches = response.body().getMatches();
                if (!matches.isEmpty()) {
                    MatchEntity entity = matches.get(0).toEntity();
                    matchDao.insertMatch(entity);
                    return entity.toDomainModel();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching match by ID", e);
        }

        return null;
    }

    @Override
    public List<Match> getMatchesByCompetition(String competitionId) {
        List<MatchEntity> cached = matchDao.getMatchesByCompetition(competitionId);
        if (!cached.isEmpty()) {
            return toMatchList(cached);
        }

        refreshMatchesByCompetition(competitionId);

        return toMatchList(matchDao.getMatchesByCompetition(competitionId));
    }

    @Override
    public List<Match> getMatchesByDateRange(long start, long end) {
        return toMatchList(matchDao.getMatchesByDateRange(start, end));
    }

    @Override
    public List<Competition> getCompetitions() {
        List<CompetitionEntity> cached = competitionDao.getAllCompetitions();
        if (!cached.isEmpty()) {
            return toCompetitionList(cached);
        }

        refreshCompetitions();

        return toCompetitionList(competitionDao.getAllCompetitions());
    }

    @Override
    public Team getTeamById(String teamId) {
        TeamEntity cached = teamDao.getTeamById(teamId);
        if (cached != null) {
            return cached.toDomainModel();
        }

        try {
            Response<MatchDto.TeamResponseDto> response = apiService.getTeamById(teamId).execute();
            if (response.isSuccessful() && response.body() != null) {
                TeamDto teamDto = response.body().getTeam();
                if (teamDto != null) {
                    TeamEntity entity = teamDto.toEntity();
                    teamDao.insertTeam(entity);
                    return entity.toDomainModel();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching team by ID", e);
        }

        return null;
    }

    @Override
    public List<Match> getTeamMatches(String teamId) {
        try {
            Response<MatchResponseDto> response = apiService.getTeamMatches(teamId).execute();
            if (response.isSuccessful() && response.body() != null) {
                List<MatchDto.MatchItemDto> dtos = response.body().getMatches();
                List<MatchEntity> entities = MatchDto.MatchMapper.toEntityList(dtos);
                matchDao.insertMatches(entities);
                return toMatchList(entities);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching team matches", e);
        }

        // Return cached matches for this team
        List<MatchEntity> allMatches = matchDao.getAllMatches();
        List<MatchEntity> teamMatches = new ArrayList<>();
        for (MatchEntity match : allMatches) {
            if (teamId.equals(match.getHomeTeamId()) || teamId.equals(match.getAwayTeamId())) {
                teamMatches.add(match);
            }
        }
        return toMatchList(teamMatches);
    }

    @Override
    public boolean refreshMatches() {
        try {
            // Fetch upcoming matches
            Response<MatchResponseDto> upcomingResponse = apiService.getMatchesByDate(
                    getTodayDateString()
            ).execute();

            if (upcomingResponse.isSuccessful() && upcomingResponse.body() != null) {
                List<MatchDto.MatchItemDto> upcomingMatches = upcomingResponse.body().getMatches();
                List<MatchEntity> upcomingEntities = MatchDto.MatchMapper.toEntityList(upcomingMatches);
                matchDao.insertMatches(upcomingEntities);
            }

            // Fetch matches for next 7 days
            String startDate = getDateString(0);
            String endDate = getDateString(7);
            Response<MatchResponseDto> dateRangeResponse = apiService.getMatchesByDateRange(startDate, endDate).execute();

            if (dateRangeResponse.isSuccessful() && dateRangeResponse.body() != null) {
                List<MatchDto.MatchItemDto> rangeMatches = dateRangeResponse.body().getMatches();
                List<MatchEntity> rangeEntities = MatchDto.MatchMapper.toEntityList(rangeMatches);
                matchDao.insertMatches(rangeEntities);
            }

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error refreshing matches", e);
            return false;
        }
    }

    @Override
    public boolean refreshLiveMatches() {
        try {
            Response<MatchResponseDto> response = apiService.getMatchesByDate(
                    getTodayDateString()
            ).execute();

            if (response.isSuccessful() && response.body() != null) {
                List<MatchDto.MatchItemDto> matches = response.body().getMatches();
                List<MatchEntity> liveMatches = new ArrayList<>();

                for (MatchDto.MatchItemDto dto : matches) {
                    if ("LIVE".equalsIgnoreCase(dto.getStatus())) {
                        liveMatches.add(dto.toEntity());
                    }
                }

                if (!liveMatches.isEmpty()) {
                    matchDao.insertMatches(liveMatches);
                }
                return true;
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error refreshing live matches", e);
            return false;
        }
    }

    @Override
    public void clearCache() {
        executor.execute(() -> {
            matchDao.deleteAllMatches();
            teamDao.deleteAllTeams();
            competitionDao.deleteAllCompetitions();
        });
    }

    /**
     * Refresh matches for a specific competition.
     */
    private void refreshMatchesByCompetition(String competitionId) {
        executor.execute(() -> {
            try {
                Response<MatchResponseDto> response = apiService.getMatchesByCompetition(competitionId).execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<MatchDto.MatchItemDto> matches = response.body().getMatches();
                    List<MatchEntity> entities = MatchDto.MatchMapper.toEntityList(matches);
                    matchDao.insertMatches(entities);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error refreshing competition matches", e);
            }
        });
    }

    /**
     * Refresh competitions from the API.
     */
    private void refreshCompetitions() {
        executor.execute(() -> {
            try {
                Response<MatchDto.CompetitionResponseDto> response = apiService.getCompetitions().execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<CompetitionDto> competitions = response.body().getCompetitions();
                    List<CompetitionEntity> entities = MatchDto.MatchMapper.competitionToEntityList(competitions);
                    competitionDao.insertCompetitions(entities);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error refreshing competitions", e);
            }
        });
    }

    /**
     * Convert list of entities to domain models.
     */
    private List<Match> toMatchList(List<MatchEntity> entities) {
        List<Match> matches = new ArrayList<>();
        for (MatchEntity entity : entities) {
            matches.add(entity.toDomainModel());
        }
        return matches;
    }

    /**
     * Convert list of competition entities to domain models.
     */
    private List<Competition> toCompetitionList(List<CompetitionEntity> entities) {
        List<Competition> competitions = new ArrayList<>();
        for (CompetitionEntity entity : entities) {
            competitions.add(entity.toDomainModel());
        }
        return competitions;
    }

    /**
     * Get today's date as YYYY-MM-DD string.
     */
    @NonNull
    private String getTodayDateString() {
        return getDateString(0);
    }

    /**
     * Get date as YYYY-MM-DD string for offset days from today.
     */
    @NonNull
    private String getDateString(int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, offsetDays);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        return sdf.format(cal.getTime());
    }
}