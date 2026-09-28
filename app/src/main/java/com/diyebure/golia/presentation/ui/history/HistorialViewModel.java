package com.diyebure.golia.presentation.ui.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.PredictionError;
import com.diyebure.golia.domain.error.PredictionException;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.model.PredictionWithMatch;
import com.diyebure.golia.domain.usecase.GetUserPredictionsUseCase;
import com.diyebure.golia.presentation.util.Event;
import com.diyebure.golia.presentation.viewmodel.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for {@code HistorialPronosticosActivity} (R8).
 *
 * <p>{@link #load()} first validates the local session: when there is none it
 * emits a one-shot navigation-to-login event and reads nothing (R8.7).
 * Otherwise it calls {@link GetUserPredictionsUseCase}, which reads the user's
 * predictions sorted and resolves each against its {@link Match} from the cache
 * (R8.1, R8.2). The results are mapped to {@link PredictionHistoryUiModel}: a
 * missing match yields the "Partido no disponible" placeholder without breaking
 * the list (R8.3), a still-unresolved prediction is shown as pending with no
 * points (R8.4, R8.5), and an empty list surfaces the empty state without an
 * error (R8.6). A technical failure emits an error and no partial data
 * (R8.8).</p>
 */
@HiltViewModel
public class HistorialViewModel extends BaseViewModel {

    private final GetUserPredictionsUseCase getUserPredictionsUseCase;
    private final PreferencesManager preferencesManager;

    private final MutableLiveData<List<PredictionHistoryUiModel>> predictions =
            new MutableLiveData<>();
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> navigateToLogin = new MutableLiveData<>();

    @Inject
    public HistorialViewModel(GetUserPredictionsUseCase getUserPredictionsUseCase,
                              PreferencesManager preferencesManager) {
        this.getUserPredictionsUseCase = getUserPredictionsUseCase;
        this.preferencesManager = preferencesManager;
    }

    /** The mapped history rows, published once loading succeeds. */
    public LiveData<List<PredictionHistoryUiModel>> getPredictions() {
        return predictions;
    }

    /** {@code true} when the user has no predictions, driving the empty state (R8.6). */
    public LiveData<Boolean> getIsEmpty() {
        return isEmpty;
    }

    /** One-shot navigation event to login when there is no valid session (R8.7). */
    public LiveData<Event<Boolean>> getNavigateToLogin() {
        return navigateToLogin;
    }

    /**
     * Loads the current user's prediction history. Requires a valid session; if
     * absent it navigates to login and reads nothing (R8.7).
     */
    public void load() {
        if (!preferencesManager.isLoggedIn()) {
            navigateToLogin.postValue(new Event<>(true));
            return;
        }
        String userId = preferencesManager.getUserId();
        if (userId == null) {
            navigateToLogin.postValue(new Event<>(true));
            return;
        }

        setLoading(true);
        getUserPredictionsUseCase.execute(userId, result -> {
            setLoading(false);
            if (result.isError()) {
                // No partial data on a technical error (R8.8).
                setError(errorName(result.getErrorOrNull()));
                return;
            }
            List<PredictionWithMatch> combined = result.getOrNull();
            List<PredictionHistoryUiModel> rows = map(combined);
            predictions.postValue(rows);
            isEmpty.postValue(rows.isEmpty());
        });
    }

    private List<PredictionHistoryUiModel> map(List<PredictionWithMatch> combined) {
        List<PredictionHistoryUiModel> rows = new ArrayList<>();
        if (combined == null) {
            return rows;
        }
        for (PredictionWithMatch item : combined) {
            rows.add(toUiModel(item));
        }
        return rows;
    }

    private PredictionHistoryUiModel toUiModel(PredictionWithMatch item) {
        Prediction prediction = item.getPrediction();
        Match match = item.getMatch();

        String matchLabel = buildMatchLabel(match);
        String scoreText = buildScoreText(prediction);
        PredictionHistoryUiModel.Status status = statusOf(prediction);
        String pointsText = status == PredictionHistoryUiModel.Status.PENDING
                ? null
                : prediction.getPointsEarned() + " pts";

        return new PredictionHistoryUiModel(matchLabel, scoreText, status, pointsText);
    }

    /**
     * Team names as "Local vs Visitante". When the match is not in cache, returns
     * the "Partido no disponible" placeholder so a missing match still renders a
     * row (R8.3). The literal label is built here, mirroring the existing
     * {@code DetallePartidoViewModel} convention of composing display strings in
     * the ViewModel.
     */
    private String buildMatchLabel(Match match) {
        if (match == null) {
            return MATCH_UNAVAILABLE_LABEL;
        }
        String home = safe(match.getHomeTeamName());
        String away = safe(match.getAwayTeamName());
        if (home.isEmpty() && away.isEmpty()) {
            return MATCH_UNAVAILABLE_LABEL;
        }
        return home + " vs " + away;
    }

    private String buildScoreText(Prediction prediction) {
        Integer home = prediction.getPredictedHomeScore();
        Integer away = prediction.getPredictedAwayScore();
        int h = home != null ? home : 0;
        int a = away != null ? away : 0;
        return h + " - " + a;
    }

    private PredictionHistoryUiModel.Status statusOf(Prediction prediction) {
        Boolean correct = prediction.getIsCorrect();
        if (correct == null) {
            return PredictionHistoryUiModel.Status.PENDING;
        }
        return correct
                ? PredictionHistoryUiModel.Status.CORRECT
                : PredictionHistoryUiModel.Status.WRONG;
    }

    private static String errorName(Exception error) {
        if (error instanceof PredictionException) {
            return ((PredictionException) error).getPredictionError().name();
        }
        return PredictionError.PERSISTENCE_ERROR.name();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /** Placeholder shown when a prediction's match is no longer in the cache (R8.3). */
    private static final String MATCH_UNAVAILABLE_LABEL = "Partido no disponible";
}
