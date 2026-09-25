package com.diyebure.golia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.data.mapper.FixtureMapper;
import com.diyebure.golia.data.remote.api.FootballApiService;
import com.diyebure.golia.data.remote.dto.FixtureItemDto;
import com.diyebure.golia.data.remote.dto.FixtureResponseDto;
import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.repository.MatchRepository;
import com.diyebure.golia.util.RequestBudgetManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Asynchronous, cache-first implementation of {@link MatchRepository} for
 * API-Football fixtures.
 *
 * <p>All network (Retrofit) and persistence (Room) I/O runs on the injected
 * {@link IoExecutor} {@link ExecutorService}; nothing blocks the main thread.
 * Outcomes are published through the supplied {@link Callback} as a typed
 * {@link Result} ({@link Result.Success} carrying the matches, or
 * {@link Result.Error} carrying the failure).</p>
 *
 * <p>Refresh methods integrate the {@link RequestBudgetManager}
 * (Presupuesto_Peticiones). The shared 60&nbsp;s minimum interval gates the START
 * of a full refresh only; once the refresh proceeds, a window of
 * {@link #REFRESH_WINDOW_DAYS} days is fetched with one request per day. Before
 * each per-day request the daily quota is re-evaluated
 * ({@link RequestBudgetManager#isExhausted()} / {@link RequestBudgetManager#canRequest()})
 * and each performed request is recorded. If the budget runs out mid-window, the
 * remaining days are skipped and whatever was accumulated (plus cache) is
 * delivered (Requirements 7.1, 7.2, 7.3, 7.7, 7.8).</p>
 */
@Singleton
public class MatchRepositoryImpl extends BaseRepository implements MatchRepository {

    private static final String TAG = "MatchRepository";

    /**
     * Number of days fetched BEFORE the requested date, so the "Ayer" chip is
     * populated with yesterday's matches.
     */
    private static final int REFRESH_WINDOW_DAYS_BACK = 1;

    /**
     * Number of consecutive days fetched by a full refresh: yesterday + today +
     * the next six days. Eight single-day requests fit comfortably in the 100/day
     * quota and populate the "Ayer", "Hoy", "Mañana" and "Esta semana" chips.
     */
    private static final int REFRESH_WINDOW_DAYS = 8;

    private final FootballApiService apiService;
    private final MatchDao matchDao;
    private final FixtureMapper fixtureMapper;
    private final RequestBudgetManager requestBudgetManager;
    private final ExecutorService executor;

    @Inject
    public MatchRepositoryImpl(
            FootballApiService apiService,
            MatchDao matchDao,
            FixtureMapper fixtureMapper,
            RequestBudgetManager requestBudgetManager,
            @IoExecutor ExecutorService executor) {
        this.apiService = apiService;
        this.matchDao = matchDao;
        this.fixtureMapper = fixtureMapper;
        this.requestBudgetManager = requestBudgetManager;
        this.executor = executor;
    }

    @Override
    public void getMatches(Callback<List<Match>> callback) {
        executor.execute(() -> {
            try {
                List<Match> matches = readCachedMatches();
                callback.onResult(new Result.Success<>(matches));
            } catch (Exception e) {
                Log.e(TAG, "Error reading cached matches", e);
                callback.onResult(new Result.Error(e));
            }
        });
    }

    @Override
    public void refreshMatchesByDate(String isoDate, Callback<List<Match>> callback) {
        // Pull-to-refresh / initial load pass today's date. The window starts one
        // day earlier (yesterday) and spans REFRESH_WINDOW_DAYS days so the "Ayer",
        // "Hoy", "Mañana" and "Esta semana" chips are all populated.
        executor.execute(() -> {
            String windowStart = shiftIsoDate(isoDate, -REFRESH_WINDOW_DAYS_BACK);
            refreshWindowInternal(windowStart, REFRESH_WINDOW_DAYS, callback);
        });
    }

    @Override
    public void refreshLiveMatches(Callback<List<Match>> callback) {
        // Live matches are always for today, so a single-day window is enough.
        executor.execute(() -> refreshWindowInternal(todayIsoDate(), 1, callback));
    }

    @Override
    public void clearCache() {
        executor.execute(() -> {
            try {
                matchDao.deleteAllMatches();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing match cache", e);
            }
        });
    }

    /**
     * Core refresh routine, always invoked on the IO executor.
     *
     * <p>Precedence at the start of the refresh:</p>
     * <ol>
     *   <li>If the daily budget is exhausted or no request is allowed, no network
     *       call is made and the cached matches are delivered as a success
     *       (degrade to cache-only).</li>
     *   <li>Otherwise the shared minimum interval is checked ONCE. If it has not
     *       elapsed the cached matches are delivered so the shared 60&nbsp;s
     *       throttle is respected.</li>
     * </ol>
     *
     * <p>When the refresh proceeds it fetches {@code windowDays} consecutive days
     * starting at {@code startIsoDate}. Each day is one request: the budget is
     * re-checked before every request and every performed request is recorded.
     * Per-day failures (non-successful HTTP or {@link IOException}) are logged and
     * skipped without aborting the whole window. All mapped matches are accumulated
     * and upserted into Room with a single insert. The accumulated matches are
     * returned on success; {@link Result.Error} is only returned when nothing could
     * be accumulated and an exception occurred.</p>
     *
     * @param startIsoDate the first day of the window ({@code yyyy-MM-dd})
     * @param windowDays   number of consecutive days to fetch (inclusive of start)
     * @param callback     receives the accumulated matches or the failure
     */
    private void refreshWindowInternal(String startIsoDate, int windowDays, Callback<List<Match>> callback) {
        try {
            // 1) Budget/quota first: if exhausted, never hit the network.
            //    2) Shared 60s interval gates the START of the refresh only.
            if (requestBudgetManager.isExhausted()
                    || !requestBudgetManager.canRequest()
                    || !requestBudgetManager.isMinIntervalElapsed()) {
                callback.onResult(new Result.Success<>(readCachedMatches()));
                return;
            }

            LocalDate start = LocalDate.parse(startIsoDate, DateTimeFormatter.ISO_LOCAL_DATE);

            List<Match> accumulatedMatches = new ArrayList<>();
            List<MatchEntity> accumulatedEntities = new ArrayList<>();
            IOException lastError = null;

            for (int i = 0; i < windowDays; i++) {
                // Re-evaluate the budget before EACH per-day request. The 60s interval
                // is intentionally NOT checked here so days 2..7 are not discarded.
                if (requestBudgetManager.isExhausted() || !requestBudgetManager.canRequest()) {
                    Log.w(TAG, "Budget exhausted mid-window; stopping after " + i + " day(s)");
                    break;
                }

                String isoDate = start.plusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);

                try {
                    requestBudgetManager.recordRequest();
                    Response<FixtureResponseDto> response =
                            apiService.getFixturesByDate(isoDate).execute();

                    if (!response.isSuccessful() || response.body() == null) {
                        Log.e(TAG, "Unsuccessful fixtures response for " + isoDate
                                + ": HTTP " + response.code());
                        continue;
                    }

                    List<FixtureItemDto> items = response.body().getResponse();
                    List<Match> matches = fixtureMapper.toMatches(items);
                    for (Match match : matches) {
                        // Deterministic id keeps upserts stable across refreshes.
                        match.setId(stableId(match));
                        accumulatedEntities.add(MatchEntity.fromDomainModel(match));
                        accumulatedMatches.add(match);
                    }
                } catch (IOException e) {
                    // Do not abort the whole window on a single-day network error.
                    Log.e(TAG, "Network error refreshing matches for " + isoDate, e);
                    lastError = e;
                }
            }

            // 3) Single upsert of everything accumulated across the window.
            if (!accumulatedEntities.isEmpty()) {
                matchDao.insertMatches(accumulatedEntities);
            }

            // Only surface an error when nothing at all could be obtained and a
            // network failure actually occurred; otherwise return what we have.
            if (accumulatedMatches.isEmpty() && lastError != null) {
                callback.onResult(new Result.Error(lastError));
                return;
            }

            callback.onResult(new Result.Success<>(accumulatedMatches));
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing matches starting " + startIsoDate, e);
            callback.onResult(new Result.Error(e));
        }
    }

    /**
     * Reads all cached matches from Room and converts them to domain models.
     * Runs on the IO executor (callers already execute on it).
     */
    private List<Match> readCachedMatches() {
        List<MatchEntity> cached = matchDao.getAllMatches();
        List<Match> matches = new ArrayList<>(cached.size());
        for (MatchEntity entity : cached) {
            matches.add(entity.toDomainModel());
        }
        return matches;
    }

    /**
     * Derives a deterministic {@link UUID} from a match's external id so that
     * re-fetching the same fixture replaces the existing Room row instead of
     * inserting a duplicate. Falls back to a random UUID when no external id is
     * available.
     */
    private static UUID stableId(Match match) {
        String externalId = match.getExternalId();
        if (externalId == null || externalId.isEmpty()) {
            return UUID.randomUUID();
        }
        return UUID.nameUUIDFromBytes(externalId.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Today's date as a {@code yyyy-MM-dd} ISO string in the device's default zone,
     * matching the local-zone dates used by the ViewModel.
     */
    @NonNull
    private static String todayIsoDate() {
        return LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Shifts an ISO {@code yyyy-MM-dd} date by {@code deltaDays} (negative to move
     * to the past) and returns the resulting ISO date string.
     */
    @NonNull
    private static String shiftIsoDate(String isoDate, int deltaDays) {
        return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
                .plusDays(deltaDays)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
