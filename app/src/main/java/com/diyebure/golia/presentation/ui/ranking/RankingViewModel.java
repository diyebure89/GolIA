package com.diyebure.golia.presentation.ui.ranking;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.RankingResult;
import com.diyebure.golia.domain.model.Ranking_Entry;
import com.diyebure.golia.domain.model.Ranking_Period;
import com.diyebure.golia.domain.usecase.GetRankingUseCase;
import com.diyebure.golia.presentation.util.Event;
import com.diyebure.golia.presentation.viewmodel.BaseViewModel;
import com.diyebure.golia.util.LiveRefreshScheduler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel de la Pantalla_Ranking (@HiltViewModel), sobre {@link BaseViewModel}.
 *
 * <p>Orquesta el cálculo del ranking local: valida la sesión, delega el cómputo
 * a {@link GetRankingUseCase} (que corre en {@code @IoExecutor} y no realiza
 * peticiones a la API) y publica el estado observable como
 * {@code LiveData<RankingUiState>} (Requisitos 1.2, 1.4, 7.1).</p>
 *
 * <h3>Sesión y navegación (R1.7)</h3>
 * <p>Antes de cada cálculo se comprueba {@link PreferencesManager#isLoggedIn()};
 * si no hay sesión no se muestra ranking y se emite un evento de navegación a
 * login mediante {@link #getNavigateToLogin()} (patrón {@link Event} de un solo
 * disparo).</p>
 *
 * <h3>Anti-solapamiento "último gana" (R7.6)</h3>
 * <p>Cada disparo incrementa {@link #generationToken} y captura su valor. Como el
 * {@link com.diyebure.golia.domain.common.Callback} no transporta el token, el
 * callback compara el token capturado con el actual y descarta cualquier
 * resultado obsoleto.</p>
 *
 * <h3>Ciclo en vivo (R7.4, R7.5, R7.6)</h3>
 * <p>{@link #onScreenResumed()} y cada tick del {@link LiveRefreshScheduler}
 * recalculan con el {@link #selectedPeriod} retenido (nuevo token) para refrescar
 * los Live_Points sin peticiones a la API. El scheduler es {@code @MainThread};
 * sus toggles se marshalan al hilo principal con un {@link Handler}.</p>
 *
 * <h3>Threading</h3>
 * <p>El use case entrega su {@link Result} en un hilo de fondo, por lo que este
 * ViewModel publica con {@code postValue}.</p>
 */
@HiltViewModel
public class RankingViewModel extends BaseViewModel {

    /** Intervalo compartido de refresco en vivo: 60 s (Requisitos 7.4, 7.5). */
    @VisibleForTesting
    static final long LIVE_POLL_INTERVAL_MS = 60_000L;

    /** Mensaje genérico cuando el cálculo del ranking falla (R1.8). */
    @VisibleForTesting
    static final String MSG_ERROR = "No se pudo cargar el ranking";

    private final GetRankingUseCase getRanking;
    private final PreferencesManager preferences;
    private final LiveRefreshScheduler scheduler;

    /** Handler del hilo principal para marshalar los toggles del scheduler. */
    private final Handler mainHandler;

    private final MutableLiveData<RankingUiState> uiState = new MutableLiveData<>();

    /** Evento de un solo disparo para navegar a login sin sesión (R1.7). */
    private final MutableLiveData<Event<Boolean>> navigateToLogin = new MutableLiveData<>();

    /** Periodo activo; por defecto {@link Ranking_Period#SEMANAL}. Sobrevive rotación. */
    private Ranking_Period selectedPeriod = Ranking_Period.SEMANAL;

    /** Token de generación para el anti-solapamiento "último gana" (R7.6). */
    private long generationToken = 0L;

    /** True mientras la pantalla debe mantener el ciclo en vivo activo. */
    private boolean liveActive;

    @Inject
    public RankingViewModel(GetRankingUseCase getRanking,
                            PreferencesManager preferences,
                            LiveRefreshScheduler scheduler) {
        this(getRanking, preferences, scheduler, new Handler(Looper.getMainLooper()));
    }

    /**
     * Constructor visible para pruebas que permite inyectar un {@link Handler}
     * controlable para los toggles del scheduler en el hilo principal.
     */
    @VisibleForTesting
    RankingViewModel(GetRankingUseCase getRanking,
                     PreferencesManager preferences,
                     LiveRefreshScheduler scheduler,
                     Handler mainHandler) {
        this.getRanking = getRanking;
        this.preferences = preferences;
        this.scheduler = scheduler;
        this.mainHandler = mainHandler;
    }

    /** @return estado observable de la pantalla (Requisito 7.1). */
    public LiveData<RankingUiState> getUiState() {
        return uiState;
    }

    /** @return evento de un solo disparo para navegar a login (Requisito 1.7). */
    public LiveData<Event<Boolean>> getNavigateToLogin() {
        return navigateToLogin;
    }

    /** @return el periodo activo retenido (Requisito 7.3). */
    public Ranking_Period getSelectedPeriod() {
        return selectedPeriod;
    }

    // ==================== Acciones desde la UI ====================

    /**
     * Carga el ranking para el {@link #selectedPeriod} actual (Requisitos 1.2,
     * 1.4). Valida sesión antes de cualquier cálculo.
     */
    public void load() {
        recompute();
    }

    /**
     * Cambia el periodo activo y recalcula (nuevo token) (Requisitos 3.3, 7.3).
     */
    public void selectPeriod(Ranking_Period period) {
        if (period == null || period == selectedPeriod) {
            if (period != null) {
                selectedPeriod = period;
            }
            recompute();
            return;
        }
        selectedPeriod = period;
        recompute();
    }

    /**
     * Recalcula al volver a primer plano para refrescar Live_Points sin
     * peticiones a la API (Requisitos 7.2, 7.4).
     */
    public void onScreenResumed() {
        liveActive = true;
        recompute();
        maybeToggleScheduler();
    }

    /**
     * Detiene el ciclo en vivo (p. ej. en {@code onPause}) (Requisito 7.6).
     */
    public void onScreenPaused() {
        liveActive = false;
        stopScheduler();
    }

    // ==================== Núcleo de cálculo ====================

    /**
     * Valida la sesión y dispara el cálculo del ranking. Si no hay sesión, emite
     * el evento de navegación a login y no muestra ranking (R1.7). Si la hay,
     * incrementa el token, emite {@link RankingUiState.Loading} y llama al use
     * case.
     */
    private void recompute() {
        if (!preferences.isLoggedIn()) {
            navigateToLogin.postValue(new Event<>(Boolean.TRUE));
            return;
        }

        final long token = ++generationToken;
        final String userId = preferences.getUserId();
        uiState.postValue(new RankingUiState.Loading());

        getRanking.execute(selectedPeriod, userId, token, result -> onRankingResult(token, result));
    }

    /**
     * Maneja el resultado del use case aplicando "último gana": descarta el
     * resultado si el token capturado ya no es el vigente (R7.6). En caso
     * contrario publica Content/Empty/Error con {@code postValue}.
     */
    private void onRankingResult(long token, Result<RankingResult> result) {
        if (token != generationToken) {
            // Resultado obsoleto: un disparo más reciente lo reemplaza (R7.6).
            return;
        }

        if (result != null && result.isSuccess()) {
            RankingResult ranking = result.getOrNull();
            List<Ranking_Entry> entries = ranking == null ? null : ranking.getEntries();
            if (entries == null || entries.isEmpty()) {
                uiState.postValue(new RankingUiState.Empty());
            } else {
                List<Ranking_Entry> podium = topThree(entries);
                uiState.postValue(new RankingUiState.Content(
                        podium,
                        new ArrayList<>(entries),
                        ranking.getCurrentUserPosition(),
                        ranking.hasLiveAny()));
            }
        } else {
            // Fallo técnico: sin datos parciales (R1.8).
            uiState.postValue(new RankingUiState.Error(MSG_ERROR));
        }
    }

    /** @return los primeros 3 elementos (o menos) de la lista ordenada. */
    private static List<Ranking_Entry> topThree(List<Ranking_Entry> entries) {
        int end = Math.min(3, entries.size());
        return new ArrayList<>(entries.subList(0, end));
    }

    // ==================== Ciclo en vivo ====================

    /**
     * Arranca o detiene el scheduler según el estado de visibilidad. El scheduler
     * es {@code @MainThread}, por lo que el toggle se marshala al hilo principal.
     */
    private void maybeToggleScheduler() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            toggleSchedulerOnMain();
        } else {
            mainHandler.post(this::toggleSchedulerOnMain);
        }
    }

    private void toggleSchedulerOnMain() {
        if (liveActive && !scheduler.isRunning()) {
            scheduler.start(LIVE_POLL_INTERVAL_MS, this::onLiveTick);
        } else if (!liveActive && scheduler.isRunning()) {
            scheduler.stop();
        }
    }

    /**
     * Tick del scheduler: recalcula con el periodo retenido (nuevo token) para
     * refrescar Live_Points sin peticiones a la API (Requisitos 7.4, 7.5).
     */
    private void onLiveTick() {
        recompute();
    }

    private void stopScheduler() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (scheduler.isRunning()) {
                scheduler.stop();
            }
        } else {
            mainHandler.post(() -> {
                if (scheduler.isRunning()) {
                    scheduler.stop();
                }
            });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        liveActive = false;
        if (scheduler.isRunning()) {
            scheduler.stop();
        }
    }

    // ==================== Accessors de test ====================

    @VisibleForTesting
    long getGenerationTokenForTest() {
        return generationToken;
    }

    @NonNull
    @VisibleForTesting
    Ranking_Period getSelectedPeriodForTest() {
        return selectedPeriod;
    }
}
