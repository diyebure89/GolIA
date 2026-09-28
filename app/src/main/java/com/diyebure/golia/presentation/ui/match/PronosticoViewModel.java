package com.diyebure.golia.presentation.ui.match;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.PredictionError;
import com.diyebure.golia.domain.error.PredictionException;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.usecase.GetMatchByIdUseCase;
import com.diyebure.golia.domain.usecase.GetUserPredictionUseCase;
import com.diyebure.golia.domain.usecase.SavePredictionUseCase;
import com.diyebure.golia.domain.validation.ScoreFieldError;
import com.diyebure.golia.domain.validation.ScoreValidationResult;
import com.diyebure.golia.domain.validation.ScoreValidator;
import com.diyebure.golia.presentation.util.Event;
import com.diyebure.golia.presentation.viewmodel.BaseViewModel;

import java.text.DateFormat;
import java.util.Date;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for {@code PronosticoActivity} (R5).
 *
 * <p>{@link #load(String)} validates the session, reads the cached match so the
 * screen can render its header (teams + kickoff, R5.1) and preloads the user's
 * existing prediction into the two score fields when one exists (R5.2).</p>
 *
 * <p>{@link #submit(String, String)} validates the two raw score inputs with the
 * pure {@link ScoreValidator} (0..99, required &mdash; R5.3, R5.4) reporting a
 * per-field error, then delegates the upsert to {@link SavePredictionUseCase},
 * which re-validates the {@code Prediction_Lock_Threshold} authoritatively
 * (R5.7). The 1X2 outcome derivation happens inside the repository, so the
 * ViewModel only passes the two integer scores (R5.5). While the save is in
 * flight {@link #getIsLoading()} is {@code true} so the Activity can disable the
 * submit control (anti double-submit, R5.10). On success it emits a one-shot
 * navigation event carrying the data the confirmation screen needs (R5.11); on a
 * typed failure it surfaces the {@link PredictionError} name (mapped to a string
 * by the Activity) while keeping the entered values (R5.6, R5.12).</p>
 */
@HiltViewModel
public class PronosticoViewModel extends BaseViewModel {

    private final GetMatchByIdUseCase getMatchByIdUseCase;
    private final GetUserPredictionUseCase getUserPredictionUseCase;
    private final SavePredictionUseCase savePredictionUseCase;
    private final ScoreValidator scoreValidator;
    private final PreferencesManager preferencesManager;

    private final MutableLiveData<PronosticoHeaderUiModel> header = new MutableLiveData<>();
    private final MutableLiveData<PronosticoPrefill> prefill = new MutableLiveData<>();
    private final MutableLiveData<ScoreFieldError> homeFieldError = new MutableLiveData<>();
    private final MutableLiveData<ScoreFieldError> awayFieldError = new MutableLiveData<>();
    private final MutableLiveData<Event<PronosticoConfirmationArgs>> navigateToConfirmation =
            new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> navigateToLogin = new MutableLiveData<>();

    private Match loadedMatch;
    private String matchId;

    @Inject
    public PronosticoViewModel(GetMatchByIdUseCase getMatchByIdUseCase,
                               GetUserPredictionUseCase getUserPredictionUseCase,
                               SavePredictionUseCase savePredictionUseCase,
                               ScoreValidator scoreValidator,
                               PreferencesManager preferencesManager) {
        this.getMatchByIdUseCase = getMatchByIdUseCase;
        this.getUserPredictionUseCase = getUserPredictionUseCase;
        this.savePredictionUseCase = savePredictionUseCase;
        this.scoreValidator = scoreValidator;
        this.preferencesManager = preferencesManager;
    }

    public LiveData<PronosticoHeaderUiModel> getHeader() {
        return header;
    }

    public LiveData<PronosticoPrefill> getPrefill() {
        return prefill;
    }

    public LiveData<ScoreFieldError> getHomeFieldError() {
        return homeFieldError;
    }

    public LiveData<ScoreFieldError> getAwayFieldError() {
        return awayFieldError;
    }

    public LiveData<Event<PronosticoConfirmationArgs>> getNavigateToConfirmation() {
        return navigateToConfirmation;
    }

    public LiveData<Event<Boolean>> getNavigateToLogin() {
        return navigateToLogin;
    }

    /**
     * Loads the match for the header and preloads any existing prediction into
     * the score fields.
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
        this.matchId = matchId;

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
            onMatchLoaded(match);
        });
    }

    private void onMatchLoaded(Match match) {
        this.loadedMatch = match;
        header.postValue(toHeaderUiModel(match));
        loadExistingPrediction(match);
    }

    private void loadExistingPrediction(Match match) {
        String userId = preferencesManager.getUserId();
        getUserPredictionUseCase.execute(userId, matchId, predictionResult -> {
            Prediction prediction = predictionResult.isSuccess() ? predictionResult.getOrNull() : null;
            if (prediction != null
                    && prediction.getPredictedHomeScore() != null
                    && prediction.getPredictedAwayScore() != null) {
                prefill.postValue(new PronosticoPrefill(
                        prediction.getPredictedHomeScore(),
                        prediction.getPredictedAwayScore()));
            }
            setLoading(false);
        });
    }

    /**
     * Validates and saves the prediction. Rejects invalid input with a per-field
     * error (R5.3, R5.4) and does not write; on valid input delegates the upsert
     * to {@link SavePredictionUseCase} (R5.5, R5.7) while showing the loading
     * state (R5.10).
     *
     * @param homeInput the raw home-goals input from the form
     * @param awayInput the raw away-goals input from the form
     */
    public void submit(String homeInput, String awayInput) {
        // Anti double-submit: ignore taps while a save is already in flight.
        if (isLoadingState()) {
            return;
        }
        if (loadedMatch == null || matchId == null) {
            setError(PredictionError.MATCH_NOT_FOUND.name());
            return;
        }
        if (!preferencesManager.isLoggedIn()) {
            navigateToLogin.postValue(new Event<>(true));
            return;
        }

        ScoreValidationResult validation = scoreValidator.validate(homeInput, awayInput);
        // Publish per-field errors (null clears a previously shown error).
        homeFieldError.postValue(validation.getHomeError());
        awayFieldError.postValue(validation.getAwayError());
        if (!validation.isValid()) {
            return;
        }

        int homeScore = Integer.parseInt(homeInput.trim());
        int awayScore = Integer.parseInt(awayInput.trim());
        String userId = preferencesManager.getUserId();

        setLoading(true);
        savePredictionUseCase.execute(userId, matchId, homeScore, awayScore, result -> {
            setLoading(false);
            if (result.isSuccess()) {
                navigateToConfirmation.postValue(new Event<>(
                        buildConfirmationArgs(loadedMatch, homeScore, awayScore)));
                return;
            }
            // Keep the entered values on the screen (R5.12); only report the error.
            setError(errorName(result.getErrorOrNull(), PredictionError.PERSISTENCE_ERROR));
        });
    }

    // --- Mapping helpers --------------------------------------------------

    private PronosticoHeaderUiModel toHeaderUiModel(Match match) {
        return new PronosticoHeaderUiModel(
                safe(match.getHomeTeamName()),
                safe(match.getAwayTeamName()),
                match.getHomeTeamLogoUrl(),
                match.getAwayTeamLogoUrl(),
                formatKickoff(match.getScheduledDateTime()));
    }

    private PronosticoConfirmationArgs buildConfirmationArgs(Match match, int homeScore, int awayScore) {
        return new PronosticoConfirmationArgs(
                match.getId().toString(),
                safe(match.getHomeTeamName()),
                safe(match.getAwayTeamName()),
                homeScore,
                awayScore,
                formatKickoff(match.getScheduledDateTime()));
    }

    private static String formatKickoff(long epochMillis) {
        return DateFormat
                .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(epochMillis));
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
