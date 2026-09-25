package com.diyebure.golia.presentation.ui.partidos;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.diyebure.golia.BuildConfig;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.usecase.GetMatchesUseCase;
import com.diyebure.golia.domain.usecase.RefreshLiveMatchesUseCase;
import com.diyebure.golia.domain.usecase.RefreshMatchesUseCase;
import com.diyebure.golia.util.LiveRefreshScheduler;
import com.diyebure.golia.util.SearchTextNormalizer;
import com.diyebure.golia.util.TimeRangeCalculator;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel de la Pantalla_Partidos (@HiltViewModel).
 *
 * <p>Orquesta la obtención de fixtures (cache-first), el filtrado por
 * {@link ChipHorario} mediante {@link TimeRangeCalculator}, la búsqueda con
 * debounce de 300&nbsp;ms normalizada con {@link SearchTextNormalizer}, el
 * ordenamiento por grupos (LIVE primero) y el ciclo de polling en vivo mediante
 * {@link LiveRefreshScheduler}. El estado observable se expone únicamente como
 * {@code LiveData<PartidosUiState>} (Requisitos 12.1, 12.7).</p>
 *
 * <h3>Estrategia de datos (cache-first)</h3>
 * <ol>
 *     <li>Al inicializarse, si {@code BuildConfig.API_FOOTBALL_KEY} está vacío se
 *     emite {@link PartidosUiState.Error} no reintentable (Requisito 2.3) y no se
 *     realiza ninguna petición.</li>
 *     <li>En caso contrario emite {@link PartidosUiState.Loading}, pide los datos
 *     cacheados con {@link GetMatchesUseCase} y, tras ello, dispara un refresco
 *     por fecha de hoy con {@link RefreshMatchesUseCase} (Requisitos 7.1, 12.2,
 *     12.3).</li>
 * </ol>
 *
 * <h3>Modelo en memoria</h3>
 * <p>Se conserva la lista completa sin filtrar ({@link #allMatches}); cada cambio
 * de chip o de búsqueda re-deriva la lista filtrada+buscada+ordenada y re-emite
 * el estado, sin volver a pedir datos (Requisitos 3.3, 10.6).</p>
 *
 * <h3>Threading</h3>
 * <p>Los use cases entregan su {@link Result} en un hilo de fondo; por eso este
 * ViewModel publica con {@code postValue}. Las acciones que llegan desde la UI
 * (selección de chip, búsqueda, refresco) y los ticks del scheduler ocurren en
 * el hilo principal, por lo que las estructuras en memoria se tocan desde el hilo
 * principal salvo la asignación de resultados, que también se marshala al hilo
 * principal antes de re-derivar.</p>
 */
@HiltViewModel
public class PartidosViewModel extends ViewModel {

    /** Intervalo mínimo compartido de refresco en vivo: 60 s (Requisitos 7.4, 7.5). */
    @VisibleForTesting
    static final long LIVE_POLL_INTERVAL_MS = 60_000L;

    /** Ventana de debounce para la búsqueda: 300 ms (Requisito 10.3). */
    @VisibleForTesting
    static final long SEARCH_DEBOUNCE_MS = 300L;

    /** Mensaje mostrado cuando falta la clave de API (Requisito 2.3). */
    @VisibleForTesting
    static final String MSG_API_KEY_MISSING = "Falta configurar la clave de API";

    /** Mensaje genérico cuando no hay datos que mostrar y falla el refresco. */
    @VisibleForTesting
    static final String MSG_NO_DATA = "No se pudieron cargar los partidos";

    private final GetMatchesUseCase getMatches;
    private final RefreshMatchesUseCase refreshMatches;
    private final RefreshLiveMatchesUseCase refreshLive;
    private final TimeRangeCalculator timeRanges;
    private final SearchTextNormalizer normalizer;
    private final LiveRefreshScheduler scheduler;

    /** Handler para el debounce de búsqueda (hilo principal). */
    private final Handler mainHandler;

    private final MutableLiveData<PartidosUiState> uiState = new MutableLiveData<>();

    /** Lista completa de partidos sin filtrar mantenida en memoria. */
    private final List<Match> allMatches = new ArrayList<>();

    /** Chip activo; por defecto {@link ChipHorario#HOY} (Requisito 3.2). */
    private ChipHorario activeChip = ChipHorario.HOY;

    /** Consulta de búsqueda actual (texto crudo, sin normalizar). */
    private String currentQuery = "";

    /** Runnable de debounce en vuelo, o {@code null}. */
    @Nullable
    private Runnable pendingSearch;

    /** True cuando la clave de API está ausente (bloquea cualquier petición). */
    private final boolean apiKeyMissing;

    /** True si el último refresco degradó a caché por fallo remoto (aviso offline). */
    private boolean offlineNotice;

    /** True si la cuota diaria está agotada (aviso de cuota). */
    private boolean quotaNotice;

    /** True mientras la pantalla debe mantener el polling en vivo activo. */
    private boolean pollingActive;

    @Inject
    public PartidosViewModel(
            GetMatchesUseCase getMatches,
            RefreshMatchesUseCase refreshMatches,
            RefreshLiveMatchesUseCase refreshLive,
            TimeRangeCalculator timeRanges,
            SearchTextNormalizer normalizer,
            LiveRefreshScheduler scheduler) {
        this(getMatches, refreshMatches, refreshLive, timeRanges, normalizer, scheduler,
                new Handler(Looper.getMainLooper()));
    }

    /**
     * Constructor visible para pruebas que permite inyectar un {@link Handler}
     * controlable para el debounce de búsqueda.
     */
    @VisibleForTesting
    PartidosViewModel(
            GetMatchesUseCase getMatches,
            RefreshMatchesUseCase refreshMatches,
            RefreshLiveMatchesUseCase refreshLive,
            TimeRangeCalculator timeRanges,
            SearchTextNormalizer normalizer,
            LiveRefreshScheduler scheduler,
            Handler mainHandler) {
        this.getMatches = getMatches;
        this.refreshMatches = refreshMatches;
        this.refreshLive = refreshLive;
        this.timeRanges = timeRanges;
        this.normalizer = normalizer;
        this.scheduler = scheduler;
        this.mainHandler = mainHandler;

        this.apiKeyMissing = isBlank(BuildConfig.API_FOOTBALL_KEY);

        if (apiKeyMissing) {
            // Estado Error no reintentable: falta configurar la clave (R2.3).
            uiState.setValue(new PartidosUiState.Error(MSG_API_KEY_MISSING, false));
        } else {
            initLoad();
        }
    }

    /** @return estado observable de la pantalla (Requisitos 12.1, 12.7). */
    public LiveData<PartidosUiState> getUiState() {
        return uiState;
    }

    // ==================== Carga inicial (cache-first) ====================

    /**
     * Carga inicial cache-first: emite Loading, lee la caché y luego refresca por
     * la fecha de hoy (Requisitos 7.1, 12.2, 12.3, 7.9).
     */
    private void initLoad() {
        uiState.setValue(new PartidosUiState.Loading());

        getMatches.execute(result -> {
            if (result.isSuccess()) {
                List<Match> cached = safeList(result.getOrNull());
                setMatches(cached);
            }
            // Tras entregar la caché (o si no hay), disparar el refresco remoto.
            requestDateRefresh();
        });
    }

    /**
     * Solicita un refresco por la fecha de hoy respetando el intervalo compartido.
     * El control real de cuota/intervalo de 60 s vive en el repositorio
     * ({@code RequestBudgetManager}); aquí sólo se delega (Requisitos 7.4, 7.7).
     */
    private void requestDateRefresh() {
        if (apiKeyMissing) {
            return;
        }
        String today = todayIso();
        refreshMatches.execute(today, result -> applyRemoteResult(result));
    }

    /**
     * Aplica el resultado de un refresco remoto (por fecha o en vivo) a la lista
     * en memoria y re-emite el estado con los avisos correspondientes.
     */
    private void applyRemoteResult(Result<List<Match>> result) {
        if (result.isSuccess()) {
            offlineNotice = false;
            setMatches(safeList(result.getOrNull()));
        } else {
            // Fallo remoto: si hay caché, degradar a Content con aviso offline;
            // si no hay datos, emitir Error reintentable (R11.1, R9.3).
            if (hasCachedData()) {
                offlineNotice = true;
                reEmit();
            } else {
                postState(new PartidosUiState.Error(MSG_NO_DATA, true));
            }
        }
    }

    // ==================== Acciones desde la UI ====================

    /**
     * Cambia el chip activo, recalcula el rango y re-filtra sin volver a pedir
     * datos (Requisitos 3.3, 3.7, 10.6).
     */
    public void onChipSelected(ChipHorario chip) {
        if (chip == null || chip == activeChip) {
            if (chip != null) {
                activeChip = chip;
            }
            reEmit();
            return;
        }
        activeChip = chip;
        reEmit();
    }

    /**
     * Recibe el texto de búsqueda y aplica un debounce de 300 ms antes de
     * re-filtrar dentro del chip activo (Requisitos 10.3, 10.5).
     */
    public void onSearchQueryChanged(String query) {
        final String q = query == null ? "" : query;
        if (pendingSearch != null) {
            mainHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> {
            currentQuery = q;
            pendingSearch = null;
            reEmit();
        };
        mainHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    /**
     * Pull-to-refresh: solicita un refresco por fecha respetando el intervalo
     * compartido (Requisitos 7.2, 7.4).
     */
    public void onRefresh() {
        requestDateRefresh();
    }

    /**
     * Reintento tras un estado de error reintentable: reinicia la carga
     * cache-first (Requisitos 9.4, 11.3).
     */
    public void onRetry() {
        if (apiKeyMissing) {
            uiState.setValue(new PartidosUiState.Error(MSG_API_KEY_MISSING, false));
            return;
        }
        initLoad();
    }

    // ==================== Polling en vivo ====================

    /**
     * Arranca el polling en vivo sólo si hay partidos LIVE en la lista filtrada
     * actual; en caso contrario no hace nada (Requisitos 7.5, 7B.2).
     */
    public void startLivePolling() {
        if (apiKeyMissing) {
            return;
        }
        pollingActive = true;
        maybeToggleScheduler();
    }

    /**
     * Detiene el polling en vivo (p. ej. en {@code onPause}) (Requisitos 7.6, 7B.1).
     */
    public void stopLivePolling() {
        pollingActive = false;
        if (scheduler.isRunning()) {
            scheduler.stop();
        }
    }

    /**
     * Arranca o detiene el scheduler según haya o no partidos LIVE en la lista
     * filtrada y según el estado de pausa. Delega el intervalo de 60 s al
     * scheduler compartido (Requisitos 7.4, 7.5, 7.6).
     */
    private void maybeToggleScheduler() {
        // El scheduler es @MainThread; los resultados de los use cases pueden
        // llegar en un hilo de fondo, por lo que marshalamos al hilo principal.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            toggleSchedulerOnMain();
        } else {
            mainHandler.post(this::toggleSchedulerOnMain);
        }
    }

    private void toggleSchedulerOnMain() {
        boolean shouldRun = pollingActive && hasLiveInFilteredList();
        if (shouldRun && !scheduler.isRunning()) {
            scheduler.start(LIVE_POLL_INTERVAL_MS, this::onLiveTick);
        } else if (!shouldRun && scheduler.isRunning()) {
            scheduler.stop();
        }
    }

    /**
     * Tick del scheduler: solicita un refresco en vivo. El control de cuota e
     * intervalo compartido de 60 s se aplica en el repositorio (Requisito 7.7).
     */
    private void onLiveTick() {
        refreshLive.execute(result -> {
            applyRemoteResult(result);
            // Tras cada tick, reevaluar si aún hay LIVE para seguir o detenerse.
            maybeToggleScheduler();
        });
    }

    /** @return true si en la lista filtrada (sin búsqueda) hay algún LIVE. */
    private boolean hasLiveInFilteredList() {
        for (Match m : filterByRange(allMatches, activeChip)) {
            if (m.getStatus() == MatchStatus.LIVE) {
                return true;
            }
        }
        return false;
    }

    // ==================== Derivación de estado ====================

    /** Reemplaza la lista completa en memoria y re-emite el estado derivado. */
    private void setMatches(List<Match> matches) {
        allMatches.clear();
        allMatches.addAll(matches);
        reEmit();
        // Un cambio de datos puede introducir/quitar LIVE: reevaluar el scheduler.
        maybeToggleScheduler();
    }

    /**
     * Re-deriva la lista filtrada + buscada + ordenada y emite el
     * {@link PartidosUiState} resultante.
     */
    private void reEmit() {
        List<Match> filtered = filterByRange(allMatches, activeChip);

        String normalizedQuery = normalizer.normalize(currentQuery);
        boolean searching = normalizedQuery != null && !normalizedQuery.trim().isEmpty();

        List<Match> visible = searching ? applySearch(filtered, normalizedQuery) : filtered;
        List<Match> sorted = sortMatches(visible);

        final PartidosUiState state;
        if (sorted.isEmpty()) {
            if (searching) {
                state = new PartidosUiState.Empty(PartidosUiState.Empty.Type.SEARCH);
            } else {
                state = new PartidosUiState.Empty(PartidosUiState.Empty.Type.FILTER);
            }
        } else {
            List<MatchUiModel> models = new ArrayList<>(sorted.size());
            for (Match m : sorted) {
                models.add(toUiModel(m));
            }
            state = new PartidosUiState.Content(models, offlineNotice, quotaNotice);
        }
        postState(state);
    }

    // ==================== Filtrado ====================

    /**
     * Filtra por el rango temporal del chip usando la Zona_Local del dispositivo
     * (Requisitos 3.4, 3.5, 3.6, 3.7, 4.1, 4.2).
     */
    private List<Match> filterByRange(List<Match> all, ChipHorario chip) {
        ZoneId zone = ZoneId.systemDefault();
        TimeRangeCalculator.Range range;
        switch (chip) {
            case AYER:
                range = timeRanges.yesterday(zone);
                break;
            case MANANA:
                range = timeRanges.tomorrow(zone);
                break;
            case ESTA_SEMANA:
                range = timeRanges.thisWeek(zone);
                break;
            case HOY:
            default:
                range = timeRanges.today(zone);
                break;
        }
        List<Match> out = new ArrayList<>();
        for (Match m : all) {
            if (timeRanges.isWithin(m.getScheduledDateTime(), range)) {
                out.add(m);
            }
        }
        return out;
    }

    // ==================== Búsqueda ====================

    /**
     * Filtra la lista por coincidencia normalizada en liga, equipo local o
     * visitante (Requisitos 10.2, 10.4).
     */
    private List<Match> applySearch(List<Match> list, String normalizedQuery) {
        List<Match> out = new ArrayList<>();
        for (Match m : list) {
            if (matchesQuery(m, normalizedQuery)) {
                out.add(m);
            }
        }
        return out;
    }

    private boolean matchesQuery(Match m, String normalizedQuery) {
        return containsNormalized(m.getCompetitionName(), normalizedQuery)
                || containsNormalized(m.getHomeTeamName(), normalizedQuery)
                || containsNormalized(m.getAwayTeamName(), normalizedQuery);
    }

    private boolean containsNormalized(String value, String normalizedQuery) {
        if (value == null) {
            return false;
        }
        String normalizedValue = normalizer.normalize(value);
        return normalizedValue != null && normalizedValue.contains(normalizedQuery);
    }

    // ==================== Ordenamiento ====================

    /**
     * Ordena con un comparador compuesto: grupo (LIVE=0, resto=1) →
     * {@code scheduledDateTime} ascendente → liga alfabética → equipo local
     * alfabético. Determinista y estable (Requisitos 8.1, 8.2, 8.3, 8.4, 8.5).
     */
    @VisibleForTesting
    List<Match> sortMatches(List<Match> list) {
        List<Match> copy = new ArrayList<>(list);
        Collections.sort(copy, MATCH_COMPARATOR);
        return copy;
    }

    /** Comparador compuesto y estable usado por {@link #sortMatches(List)}. */
    @VisibleForTesting
    static final Comparator<Match> MATCH_COMPARATOR =
            Comparator
                    .comparingInt((Match m) -> liveGroup(m.getStatus()))
                    .thenComparingLong(Match::getScheduledDateTime)
                    .thenComparing(m -> nullSafe(m.getCompetitionName()),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(m -> nullSafe(m.getHomeTeamName()),
                            String.CASE_INSENSITIVE_ORDER);

    /** Grupo primario del ordenamiento: LIVE=0, cualquier otro=1 (Requisito 8.1). */
    private static int liveGroup(MatchStatus status) {
        return status == MatchStatus.LIVE ? 0 : 1;
    }

    // ==================== Mapeo a modelo de presentación ====================

    /**
     * Convierte un {@link Match} de dominio en su {@link MatchUiModel} de
     * presentación delegando en el mapeador compartido {@link MatchUiMapper}
     * (Requisitos 4.3, 5.x, 6.x, 10.7).
     */
    @VisibleForTesting
    MatchUiModel toUiModel(Match m) {
        return MatchUiMapper.toUiModel(m);
    }

    // ==================== Helpers ====================

    private void postState(PartidosUiState state) {
        uiState.postValue(state);
    }

    private boolean hasCachedData() {
        return !allMatches.isEmpty();
    }

    private static List<Match> safeList(List<Match> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /** @return la fecha de hoy en Zona_Local con formato ISO {@code yyyy-MM-dd}. */
    private static String todayIso() {
        return java.time.LocalDate.now(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static boolean isBlank(String s) {
        return TextUtils.isEmpty(s) || s.trim().isEmpty();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (pendingSearch != null) {
            mainHandler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
        if (scheduler.isRunning()) {
            scheduler.stop();
        }
    }

    // ==================== Accessors de test ====================

    @VisibleForTesting
    ChipHorario getActiveChipForTest() {
        return activeChip;
    }
}
