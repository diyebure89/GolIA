package com.diyebure.golia.presentation.ui.match;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.PredictionError;
import com.diyebure.golia.domain.error.PredictionException;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchSeasonStats;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.model.SeasonStats;
import com.diyebure.golia.domain.model.Season_Stats_Calculator;
import com.diyebure.golia.domain.usecase.GetMatchByIdUseCase;
import com.diyebure.golia.domain.usecase.GetSeasonStatsUseCase;
import com.diyebure.golia.domain.usecase.GetUserPredictionUseCase;
import com.diyebure.golia.domain.usecase.SavePredictionUseCase;
import com.diyebure.golia.presentation.util.Event;
import com.diyebure.golia.presentation.viewmodel.BaseViewModel;

import java.text.DateFormat;
import java.util.Date;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for {@code DetallePartidoActivity} (R2, R3, R4).
 *
 * <p>{@link #load(String)} validates the session, then reads the cached match by
 * id, derives its season statistics and the user's existing prediction, and
 * publishes the resulting UI models. If the match is missing from cache
 * ({@link PredictionError#MATCH_NOT_FOUND}) it surfaces a typed error and does
 * <strong>not</strong> emit any partial data (R2.6). If the session is invalid it
 * emits a one-shot navigation-to-login event and loads nothing (R4.6).</p>
 *
 * <p>The "Hacer Pronóstico" button state is recomputed both after load and on
 * {@link #reevaluateButtonState()} (called from the Activity's {@code onResume},
 * R4.3) so a match that crossed the {@code Prediction_Lock_Threshold} while the
 * screen was open transitions to {@link PredictButtonState#CLOSED} or
 * {@link PredictButtonState#VIEW_ONLY}.</p>
 */
@HiltViewModel
public class DetallePartidoViewModel extends BaseViewModel {

    private final GetMatchByIdUseCase getMatchByIdUseCase;
    private final GetSeasonStatsUseCase getSeasonStatsUseCase;
    private final GetUserPredictionUseCase getUserPredictionUseCase;
    private final Season_Stats_Calculator calculator;
    private final PreferencesManager preferencesManager;

    private final MutableLiveData<MatchDetailUiModel> detail = new MutableLiveData<>();
    private final MutableLiveData<SeasonStatsUiModel> seasonStats = new MutableLiveData<>();
    private final MutableLiveData<PredictButtonState> buttonState = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> navigateToPrediction = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> navigateToLogin = new MutableLiveData<>();

    // Snapshot of the loaded state, needed to recompute the button on resume.
    private Match loadedMatch;
    private boolean hasPrediction;
    private boolean loaded;

    @Inject
    public DetallePartidoViewModel(GetMatchByIdUseCase getMatchByIdUseCase,
                                   GetSeasonStatsUseCase getSeasonStatsUseCase,
                                   GetUserPredictionUseCase getUserPredictionUseCase,
                                   Season_Stats_Calculator calculator,
                                   PreferencesManager preferencesManager) {
        this.getMatchByIdUseCase = getMatchByIdUseCase;
        this.getSeasonStatsUseCase = getSeasonStatsUseCase;
        this.getUserPredictionUseCase = getUserPredictionUseCase;
        this.calculator = calculator;
        this.preferencesManager = preferencesManager;
    }

    public LiveData<MatchDetailUiModel> getDetail() {
        return detail;
    }

    public LiveData<SeasonStatsUiModel> getSeasonStats() {
        return seasonStats;
    }

    public LiveData<PredictButtonState> getButtonState() {
        return buttonState;
    }

    public LiveData<Event<String>> getNavigateToPrediction() {
        return navigateToPrediction;
    }

    public LiveData<Event<Boolean>> getNavigateToLogin() {
        return navigateToLogin;
    }

    /**
     * Loads the match detail, its season statistics and the user's prediction
     * for the given match id.
     *
     * @param matchId the primary-key id of the match (may be null/empty)
     */
    public void load(String matchId) {
        if (matchId == null || matchId.trim().isEmpty()) {
            setError(PredictionError.MATCH_NOT_FOUND.name());
            return;
        }
        if (!preferencesManager.isLoggedIn()) {
            navigateToLogin.postValue(new Event<>(true));
            return;
        }

        setLoading(true);
        // GetMatchByIdUseCase entrega un único Result<Match>: éxito con el partido
        // en caché o error tipado (p. ej. MATCH_NOT_FOUND) cuando no existe.
        getMatchByIdUseCase.execute(matchId, result -> {
            if (result == null || result.isError()) {
                setLoading(false);
                setError(errorName(result != null ? result.getErrorOrNull() : null,
                        PredictionError.MATCH_NOT_FOUND));
                return;
            }
            Match match = result.getOrNull();
            if (match == null) {
                setLoading(false);
                setError(PredictionError.MATCH_NOT_FOUND.name());
                return;
            }
            onMatchLoaded(match, matchId);
        });
    }

    private void onMatchLoaded(Match match, String matchId) {
        this.loadedMatch = match;
        detail.postValue(toDetailUiModel(match));

        getSeasonStatsUseCase.execute(match, statsResult -> {
            if (statsResult.isSuccess()) {
                MatchSeasonStats stats = statsResult.getOrNull();
                if (stats != null) {
                    seasonStats.postValue(toSeasonStatsUiModel(stats));
                }
            }
            // Season stats are non-critical: a failure leaves the section empty
            // but must not block the detail from showing.
            loadUserPrediction(match, matchId);
        });
    }

    private void loadUserPrediction(Match match, String matchId) {
        String userId = preferencesManager.getUserId();
        getUserPredictionUseCase.execute(userId, matchId, predictionResult -> {
            // Result.Success carries null when the user has no prediction yet.
            Prediction prediction = predictionResult.isSuccess() ? predictionResult.getOrNull() : null;
            hasPrediction = prediction != null;
            loaded = true;
            buttonState.postValue(computeButtonState(match, hasPrediction));
            setLoading(false);
        });
    }

    /**
     * Recomputes the button state against the current time (R4.3). Safe to call
     * before a successful load: it is a no-op until the match is available.
     */
    public void reevaluateButtonState() {
        if (loaded && loadedMatch != null) {
            buttonState.postValue(computeButtonState(loadedMatch, hasPrediction));
        }
    }

    /**
     * Handles a tap on "Hacer Pronóstico": if there is no valid session, emits a
     * login-navigation event and does not open the prediction screen (R4.6);
     * otherwise emits a one-shot navigation event carrying the match id.
     */
    public void onPredictClicked() {
        if (loadedMatch == null) {
            return;
        }
        if (!preferencesManager.isLoggedIn()) {
            navigateToLogin.postValue(new Event<>(true));
            return;
        }
        navigateToPrediction.postValue(new Event<>(loadedMatch.getId().toString()));
    }

    // --- Mapping helpers --------------------------------------------------

    private MatchDetailUiModel toDetailUiModel(Match match) {
        String kickoff = DateFormat
                .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(match.getScheduledDateTime()));

        String round = "";
        if (match.getMatchday() > 0) {
            round = "Jornada " + match.getMatchday();
        }

        MatchStatus status = match.getStatus();
        String scoreText = null;
        String elapsedText = null;
        if (status == MatchStatus.LIVE || status == MatchStatus.FINISHED) {
            if (match.getHomeScore() != null && match.getAwayScore() != null) {
                scoreText = match.getHomeScore() + " - " + match.getAwayScore();
            }
            if (status == MatchStatus.LIVE && match.getElapsedMinute() != null) {
                elapsedText = match.getElapsedMinute() + "'";
            }
        }

        return new MatchDetailUiModel(
                safe(match.getCompetitionName()),
                round,
                safe(match.getHomeTeamName()),
                safe(match.getAwayTeamName()),
                match.getHomeTeamLogoUrl(),
                match.getAwayTeamLogoUrl(),
                kickoff,
                statusLabel(status),
                scoreText,
                elapsedText);
    }

    private SeasonStatsUiModel toSeasonStatsUiModel(MatchSeasonStats stats) {
        SeasonStats home = stats.getHome();
        SeasonStats away = stats.getAway();
        double winsRatio = calculator.barRatio(home.getWins(), away.getWins());
        double avgRatio = calculator.barRatio(home.getGoalsAverage(), away.getGoalsAverage());
        double formRatio = calculator.barRatio(home.getRecentWins(), away.getRecentWins());
        return new SeasonStatsUiModel(
                home.getWins(), away.getWins(),
                home.getGoalsAverage(), away.getGoalsAverage(),
                home.getRecentWins(), away.getRecentWins(),
                winsRatio, avgRatio, formRatio);
    }

    /**
     * Derives the button state from the lock threshold and whether a prediction
     * exists. Uses the same authoritative rule as {@link SavePredictionUseCase}:
     * a match is open only while SCHEDULED and more than the lock window before
     * kickoff.
     */
    private PredictButtonState computeButtonState(Match match, boolean predictionExists) {
        boolean open = isOpenForPrediction(match);
        if (open) {
            return predictionExists ? PredictButtonState.ENABLED_EDIT : PredictButtonState.ENABLED_NEW;
        }
        return predictionExists ? PredictButtonState.VIEW_ONLY : PredictButtonState.CLOSED;
    }

    private boolean isOpenForPrediction(Match match) {
        if (match == null || match.getStatus() != MatchStatus.SCHEDULED) {
            return false;
        }
        long lockTime = match.getScheduledDateTime() - SavePredictionUseCase.PREDICTION_LOCK_MILLIS;
        return System.currentTimeMillis() < lockTime;
    }

    private static String statusLabel(MatchStatus status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case SCHEDULED:
                return "Programado";
            case LIVE:
                return "En vivo";
            case FINISHED:
                return "Finalizado";
            case POSTPONED:
                return "Aplazado";
            case CANCELLED:
                return "Cancelado";
            default:
                return "";
        }
    }

    private static String errorName(Exception error, PredictionError fallback) {
        if (error instanceof PredictionException) {
            return ((PredictionException) error).getPredictionError().name();
        }
        return fallback.name();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
