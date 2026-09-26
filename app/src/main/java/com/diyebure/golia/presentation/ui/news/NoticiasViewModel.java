package com.diyebure.golia.presentation.ui.news;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.diyebure.golia.BuildConfig;
import com.diyebure.golia.data.mapper.FixtureMapper;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.NewsError;
import com.diyebure.golia.domain.model.ArticuloNoticia;
import com.diyebure.golia.domain.news.LeagueNewsQuery;
import com.diyebure.golia.domain.usecase.GetNewsUseCase;
import com.diyebure.golia.domain.usecase.RefreshNewsUseCase;
import com.diyebure.golia.util.SearchTextNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel_Noticias (@HiltViewModel) de la Pantalla_Noticias.
 *
 * <p>Deriva la fila de {@link ChipLiga} de la misma fuente de verdad que Partidos
 * ({@link FixtureMapper#TARGET_LEAGUE_IDS} y sus constantes {@code LEAGUE_*}), agrupando
 * los siete identificadores de clasificación al Mundial
 * ({@link LeagueNewsQuery#WC_QUALIFICATION_IDS}) en un único {@code Chip_Eliminatorias} y
 * anteponiendo el {@code Chip_Todos} (R1.1, R1.2). Si la fuente está vacía/irresoluble,
 * expone únicamente el {@code Chip_Todos} sin error y permanece operativa (R1.5).</p>
 *
 * <p>Gestiona selección única con reposición al primer elemento al cambiar de chip (R2.2,
 * R2.3, R2.5), consume exclusivamente los casos de uso de dominio
 * ({@link GetNewsUseCase}, {@link RefreshNewsUseCase}) sin referenciar el proveedor (R4.1),
 * y expone el estado observable como {@code LiveData<EstadoUi>} (R9.x). Traduce el
 * {@link Result}/{@link NewsError} a banners (OFFLINE/QUOTA/ERROR) conservando el contenido
 * cacheado cuando existe (R9.4, R10.1-R10.4). Aplica la guarda de reintento mientras
 * {@code loading=true} (R9.6) y, si {@code BuildConfig.NEWSDATA_API_KEY} está ausente/vacía
 * al iniciar, emite {@code banner=ERROR} sin realizar peticiones (R5.5).</p>
 *
 * <h3>Modelo en memoria</h3>
 * <p>Se conserva la lista completa del chip seleccionado ({@link #currentLeagueArticles})
 * separada de la lista mostrada, de modo que el filtro de búsqueda (tarea 11.4) pueda
 * añadirse limpiamente como una transformación en memoria sobre esta lista sin volver a
 * pedir datos. En esta tarea, la lista mostrada coincide con la lista completa del chip.</p>
 *
 * <h3>Threading</h3>
 * <p>Los casos de uso ejecutan su E/S en el executor de fondo del repositorio (@IoExecutor)
 * y entregan el {@link Result} mediante {@code Callback}; por eso las emisiones que parten de
 * un resultado usan {@code postValue}. Las acciones desde la UI (selección de chip,
 * pull-to-refresh, reintento) ocurren en el hilo principal.</p>
 */
@HiltViewModel
public class NoticiasViewModel extends ViewModel {

    /** Clave textual del {@code Chip_Todos} (feed agregado). */
    @VisibleForTesting
    static final String KEY_ALL = LeagueNewsQuery.KEY_ALL;

    /** Clave textual del {@code Chip_Eliminatorias} (agregado de clasificación al Mundial). */
    @VisibleForTesting
    static final String KEY_WC_QUALIFICATION = LeagueNewsQuery.KEY_WC_QUALIFICATION;

    /** Etiqueta visible del {@code Chip_Todos}. */
    @VisibleForTesting
    static final String LABEL_ALL = "Todos";

    /** Etiqueta visible del {@code Chip_Eliminatorias}. */
    @VisibleForTesting
    static final String LABEL_WC_QUALIFICATION = "Eliminatorias";

    /** Longitud máxima considerada del texto de búsqueda; el exceso se ignora (R8.6). */
    @VisibleForTesting
    static final int MAX_SEARCH_LENGTH = 256;

    /** Retardo del debounce de búsqueda antes de recalcular la lista filtrada (R8.3). */
    @VisibleForTesting
    static final long SEARCH_DEBOUNCE_MS = 300L;

    private final GetNewsUseCase getNews;
    private final RefreshNewsUseCase refreshNews;
    private final LeagueNewsQuery leagueNewsQuery;

    /** Normalizador reutilizado (minúsculas + sin diacríticos) para el filtro (R8.4). */
    private final SearchTextNormalizer searchTextNormalizer;

    /** Handler del hilo principal para marshalar emisiones (chips, key ausente). */
    private final Handler mainHandler;

    /** Estado observable de la pantalla (R9.x). */
    private final MutableLiveData<EstadoUi> uiState = new MutableLiveData<>();

    /** Lista observable de chips de liga (selección única). */
    private final MutableLiveData<List<ChipLiga>> chips = new MutableLiveData<>();

    /**
     * Lista completa de artículos del chip actualmente seleccionado, mantenida en memoria.
     * Se mantiene separada de la lista mostrada para permitir que el filtro de búsqueda
     * (tarea 11.4) opere sobre ella sin volver a pedir datos.
     */
    private final List<ArticuloNoticia> currentLeagueArticles = new ArrayList<>();

    /** Clave del chip seleccionado; por defecto {@link #KEY_ALL} (R2.3). */
    private String selectedLeagueKey = KEY_ALL;

    /** True cuando la clave de API está ausente/vacía: bloquea toda petición (R5.5). */
    private final boolean apiKeyMissing;

    /** True mientras hay una solicitud de refresco en curso (guarda de reintento, R9.6). */
    private boolean loading;

    /**
     * Texto de búsqueda activo ya normalizado (minúsculas, sin diacríticos) y recortado a
     * {@link #MAX_SEARCH_LENGTH} caracteres, o cadena vacía si no hay filtro de texto activo
     * (vacío/whitespace/cerrado). Cuando es no vacío, la lista mostrada es el subconjunto de
     * {@link #currentLeagueArticles} que coincide (R8.2, R8.4-R8.6, R8.8).
     */
    private String activeSearchQuery = "";

    /** Callback pendiente del debounce de búsqueda; se cancela ante cada cambio (R8.3). */
    private Runnable pendingSearchRunnable;

    @Inject
    public NoticiasViewModel(GetNewsUseCase getNews,
                             RefreshNewsUseCase refreshNews,
                             LeagueNewsQuery leagueNewsQuery,
                             SearchTextNormalizer searchTextNormalizer) {
        this(getNews, refreshNews, leagueNewsQuery, searchTextNormalizer,
                new Handler(Looper.getMainLooper()));
    }

    /**
     * Constructor visible para pruebas que permite inyectar un {@link Handler} controlable.
     */
    @VisibleForTesting
    NoticiasViewModel(GetNewsUseCase getNews,
                      RefreshNewsUseCase refreshNews,
                      LeagueNewsQuery leagueNewsQuery,
                      SearchTextNormalizer searchTextNormalizer,
                      Handler mainHandler) {
        this.getNews = getNews;
        this.refreshNews = refreshNews;
        this.leagueNewsQuery = leagueNewsQuery;
        this.searchTextNormalizer = searchTextNormalizer;
        this.mainHandler = mainHandler;

        // Construir la fila de chips desde la fuente de verdad de ligas (R1.1, R1.2, R1.5).
        chips.setValue(buildChips(selectedLeagueKey));

        this.apiKeyMissing = isBlank(BuildConfig.NEWSDATA_API_KEY);

        if (apiKeyMissing) {
            // Falta de clave: banner=ERROR sin peticiones al proveedor (R5.5).
            uiState.setValue(new EstadoUi(
                    Collections.emptyList(), false,
                    EstadoUi.Banner.ERROR, EstadoUi.EmptyKind.NINGUNO));
        } else {
            loadSelectedLeague();
        }
    }

    /** @return estado observable de la pantalla (R9.x). */
    public LiveData<EstadoUi> getUiState() {
        return uiState;
    }

    /** @return lista observable de chips de liga con el chip seleccionado marcado. */
    public LiveData<List<ChipLiga>> getChips() {
        return chips;
    }

    // ==================== Construcción de chips ====================

    /**
     * Construye la lista de {@link ChipLiga} desde {@link FixtureMapper#TARGET_LEAGUE_IDS}:
     * {@code Chip_Todos} primero, un chip por cada id no perteneciente a la clasificación al
     * Mundial y un único {@code Chip_Eliminatorias} si el conjunto incluye al menos uno de
     * {@link LeagueNewsQuery#WC_QUALIFICATION_IDS} (R1.2). Si el conjunto está vacío, sólo
     * emite el {@code Chip_Todos} (R1.5). El chip cuyo {@code leagueKey} coincide con
     * {@code selectedKey} se marca como seleccionado (selección única, R2.2).
     */
    @VisibleForTesting
    List<ChipLiga> buildChips(String selectedKey) {
        List<ChipLiga> result = new ArrayList<>();

        // Chip_Todos siempre primero (R2.1).
        result.add(new ChipLiga(KEY_ALL, LABEL_ALL, KEY_ALL.equals(selectedKey)));

        // Orden determinista de ids para un renderizado estable.
        List<Integer> ids = new ArrayList<>(FixtureMapper.TARGET_LEAGUE_IDS);
        Collections.sort(ids);

        boolean hasWcQualification = false;
        for (Integer id : ids) {
            if (LeagueNewsQuery.WC_QUALIFICATION_IDS.contains(id)) {
                hasWcQualification = true;
                continue;
            }
            String key = String.valueOf(id);
            result.add(new ChipLiga(key, labelForLeague(id), key.equals(selectedKey)));
        }

        // Un único Chip_Eliminatorias si el conjunto incluye alguna clasificación (R1.2).
        if (hasWcQualification) {
            result.add(new ChipLiga(
                    KEY_WC_QUALIFICATION,
                    LABEL_WC_QUALIFICATION,
                    KEY_WC_QUALIFICATION.equals(selectedKey)));
        }

        return result;
    }

    /**
     * Deriva la etiqueta de un chip de liga individual del primer término (de mayor
     * prioridad) de {@link LeagueNewsQuery}, evitando duplicar la lista de ligas.
     */
    private String labelForLeague(int leagueId) {
        List<String> terms = leagueNewsQuery.termsFor(leagueId);
        if (terms != null && !terms.isEmpty() && terms.get(0) != null && !terms.get(0).isEmpty()) {
            return terms.get(0);
        }
        return String.valueOf(leagueId);
    }

    // ==================== Acciones desde la UI ====================

    /**
     * Selecciona un chip de liga. Con selección única, si la clave es distinta a la actual
     * reemplaza el feed y repone la lista al primer elemento recargando el chip (R2.4, R2.5).
     * Si el chip ya estaba seleccionado, no recarga.
     *
     * @param leagueKey clave textual del chip ({@code "39"}, {@code "wc_qualification"},
     *                  {@code "all"})
     */
    public void onChipSelected(String leagueKey) {
        if (leagueKey == null || leagueKey.equals(selectedLeagueKey)) {
            return;
        }
        selectedLeagueKey = leagueKey;
        // Reposición al primer elemento: se vacía la lista en memoria del chip anterior.
        currentLeagueArticles.clear();
        chips.setValue(buildChips(selectedLeagueKey));
        if (apiKeyMissing) {
            uiState.setValue(new EstadoUi(
                    Collections.emptyList(), false,
                    EstadoUi.Banner.ERROR, EstadoUi.EmptyKind.NINGUNO));
            return;
        }
        loadSelectedLeague();
    }

    /**
     * Pull-to-refresh: vuelve a solicitar el refresco del chip seleccionado respetando la
     * guarda de reintento mientras {@code loading=true} (R6.2, R9.6).
     */
    public void onRefresh() {
        requestRefresh();
    }

    /**
     * Reintento tras un error: re-solicita el refresco del chip seleccionado y fija
     * {@code loading=true} mientras está en curso (R9.5). Si ya hay una solicitud en curso
     * ({@code loading=true}), la nueva activación se ignora (R9.6).
     */
    public void onRetry() {
        requestRefresh();
    }

    // ==================== Búsqueda de texto ====================

    /**
     * Cambio de texto de búsqueda desde la UI. Aplica un debounce de 300 ms cancelando el
     * cálculo pendiente anterior antes de recalcular la lista filtrada (R8.3). El texto se
     * recorta a {@link #MAX_SEARCH_LENGTH} caracteres antes de normalizar y comparar (R8.6);
     * un texto vacío o solo con espacios equivale a no filtrar y restaura la lista completa
     * del chip seleccionado (R8.5, R8.8). El resultado se aplica sobre
     * {@link #currentLeagueArticles} como transformación en memoria (R8.2).
     *
     * @param query texto introducido por el usuario; puede ser {@code null}
     */
    public void onSearchQueryChanged(String query) {
        final String normalizedQuery = normalizeQuery(query);
        // Cancelar el cálculo pendiente anterior (R8.3).
        cancelPendingSearch();
        pendingSearchRunnable = () -> {
            pendingSearchRunnable = null;
            applySearchQuery(normalizedQuery);
        };
        mainHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
    }

    /**
     * Cierre/limpieza del campo de búsqueda: cancela cualquier cálculo pendiente y restaura
     * de inmediato la lista completa del chip seleccionado sin debounce (R8.8).
     */
    public void onSearchClosed() {
        cancelPendingSearch();
        applySearchQuery("");
    }

    /**
     * Recorta el texto a {@link #MAX_SEARCH_LENGTH} caracteres (R8.6) y lo normaliza
     * (minúsculas, sin diacríticos) con {@link SearchTextNormalizer} (R8.4). Un texto
     * vacío o solo con espacios se colapsa a cadena vacía (sin filtro, R8.5).
     */
    private String normalizeQuery(String query) {
        if (isBlank(query)) {
            return "";
        }
        String trimmedToLimit = query.length() > MAX_SEARCH_LENGTH
                ? query.substring(0, MAX_SEARCH_LENGTH)
                : query;
        String normalized = searchTextNormalizer.normalize(trimmedToLimit);
        if (normalized == null) {
            return "";
        }
        // Tras normalizar, un texto que solo contenía espacios equivale a no filtrar (R8.5).
        return normalized.trim().isEmpty() ? "" : normalized;
    }

    /**
     * Fija el texto de búsqueda activo (ya normalizado y recortado) y re-emite el estado
     * aplicando el filtro sobre la lista completa del chip actual (R8.2, R8.5, R8.7, R8.8).
     */
    private void applySearchQuery(String normalizedQuery) {
        activeSearchQuery = normalizedQuery == null ? "" : normalizedQuery;
        emitState(EstadoUi.Banner.NINGUNO);
    }

    /** Cancela el callback de debounce pendiente, si lo hay (R8.3). */
    private void cancelPendingSearch() {
        if (pendingSearchRunnable != null) {
            mainHandler.removeCallbacks(pendingSearchRunnable);
            pendingSearchRunnable = null;
        }
    }

    // ==================== Carga y refresco ====================

    /**
     * Carga cache-first del chip seleccionado: emite {@code loading=true} si no hay
     * contenido, entrega la caché inmediata con {@link GetNewsUseCase} y dispara el refresco
     * remoto con {@link RefreshNewsUseCase} (R6.7, R9.1).
     */
    private void loadSelectedLeague() {
        if (apiKeyMissing) {
            return;
        }
        // Sin contenido aún: estado de carga con lista vacía (R9.1).
        emitLoadingIfEmpty();

        final String key = selectedLeagueKey;
        getNews.execute(key, result -> {
            // Ignorar resultados de un chip que ya no está seleccionado.
            if (!key.equals(selectedLeagueKey)) {
                return;
            }
            if (result != null && result.isSuccess()) {
                setCurrentLeagueArticles(safeList(result.getOrNull()));
            }
            // Tras la caché (o si no hay), disparar el refresco remoto.
            requestRefresh();
        });
    }

    /**
     * Solicita un refresco remoto del chip seleccionado. Aplica la guarda de reintento:
     * mientras {@code loading=true} descarta la nueva solicitud (R9.6). Al aceptar, fija
     * {@code loading=true} y emite el estado de carga cuando no hay contenido (R9.5).
     */
    private void requestRefresh() {
        if (apiKeyMissing) {
            return;
        }
        if (loading) {
            // Guarda de reintento: ya hay una solicitud en curso (R9.6).
            return;
        }
        loading = true;
        emitLoadingIfEmpty();

        final String key = selectedLeagueKey;
        refreshNews.execute(key, result -> {
            loading = false;
            if (!key.equals(selectedLeagueKey)) {
                return;
            }
            applyRefreshResult(result);
        });
    }

    /**
     * Traduce el {@link Result} del refresco al {@link EstadoUi}: en éxito reemplaza la lista
     * y limpia el banner; en error mapea el {@link NewsError} a un banner conservando el
     * contenido cacheado si existe (R9.4, R10.1-R10.4).
     */
    private void applyRefreshResult(Result<List<ArticuloNoticia>> result) {
        if (result != null && result.isSuccess()) {
            setCurrentLeagueArticles(safeList(result.getOrNull()));
            return;
        }
        Exception error = result != null ? result.getErrorOrNull() : null;
        EstadoUi.Banner banner = bannerFor(error);
        emitState(banner);
    }

    /** Reemplaza la lista completa del chip actual en memoria y re-emite sin banner. */
    private void setCurrentLeagueArticles(List<ArticuloNoticia> articles) {
        currentLeagueArticles.clear();
        currentLeagueArticles.addAll(articles);
        emitState(EstadoUi.Banner.NINGUNO);
    }

    /**
     * Emite un {@link EstadoUi} derivado de la lista completa del chip actual, aplicando el
     * filtro de búsqueda activo (si lo hay) como transformación en memoria, y el banner
     * indicado.
     *
     * <p>Comportamiento del estado vacío:</p>
     * <ul>
     *   <li>Con contenido a mostrar → {@code empty=NINGUNO} + banner.</li>
     *   <li>Sin contenido y hay una búsqueda activa (query no vacío) que descartó todo →
     *       {@code empty=SEARCH} (R8.7), distinto del vacío de liga.</li>
     *   <li>Sin contenido, sin búsqueda activa y sin banner → estado vacío de liga
     *       {@code empty=LEAGUE} (R9.3).</li>
     *   <li>Sin contenido con banner (error/aviso) → {@code empty=NINGUNO}.</li>
     * </ul>
     */
    private void emitState(EstadoUi.Banner banner) {
        boolean searchActive = !activeSearchQuery.isEmpty();
        List<ArticuloNoticia> visible = searchActive
                ? filterBySearch(currentLeagueArticles, activeSearchQuery)
                : currentLeagueArticles;
        List<ArticleUiModel> items = toUiModels(visible);

        EstadoUi.EmptyKind empty;
        if (!items.isEmpty()) {
            empty = EstadoUi.EmptyKind.NINGUNO;
        } else if (searchActive && !currentLeagueArticles.isEmpty()) {
            // Búsqueda no vacía que filtró todo → estado vacío de búsqueda (R8.7).
            empty = EstadoUi.EmptyKind.SEARCH;
        } else if (banner == EstadoUi.Banner.NINGUNO) {
            // Carga completada sin datos y sin error → estado vacío de liga (R9.3).
            empty = EstadoUi.EmptyKind.LEAGUE;
        } else {
            empty = EstadoUi.EmptyKind.NINGUNO;
        }
        postState(new EstadoUi(items, false, banner, empty));
    }

    /**
     * Filtra la lista conservando los artículos cuyo título o descripción, tras normalizar
     * con {@link SearchTextNormalizer}, contienen como subcadena el texto de búsqueda ya
     * normalizado (R8.2, R8.4).
     */
    private List<ArticuloNoticia> filterBySearch(List<ArticuloNoticia> articles, String normalizedQuery) {
        List<ArticuloNoticia> filtered = new ArrayList<>();
        for (ArticuloNoticia a : articles) {
            if (matchesQuery(a, normalizedQuery)) {
                filtered.add(a);
            }
        }
        return filtered;
    }

    /** True si el título o la descripción normalizados contienen la consulta como subcadena. */
    private boolean matchesQuery(ArticuloNoticia article, String normalizedQuery) {
        return normalizedContains(article.getTitle(), normalizedQuery)
                || normalizedContains(article.getDescription(), normalizedQuery);
    }

    private boolean normalizedContains(String field, String normalizedQuery) {
        if (field == null) {
            return false;
        }
        String normalizedField = searchTextNormalizer.normalize(field);
        return normalizedField != null && normalizedField.contains(normalizedQuery);
    }

    /**
     * Emite el estado de carga sólo cuando aún no hay contenido para mostrar: {@code
     * loading=true}, {@code items} vacío, {@code banner=NINGUNO}, {@code empty=NINGUNO}
     * (R9.1). Si ya hay contenido cacheado, no se oculta (se mantiene el estado actual).
     */
    private void emitLoadingIfEmpty() {
        if (currentLeagueArticles.isEmpty()) {
            postState(new EstadoUi(
                    Collections.emptyList(), true,
                    EstadoUi.Banner.NINGUNO, EstadoUi.EmptyKind.NINGUNO));
        }
    }

    // ==================== Mapeo de errores a banners ====================

    /**
     * Mapea un error a su banner: {@link NewsError.NetworkError} &rarr; OFFLINE,
     * {@link NewsError.QuotaExceededError} &rarr; QUOTA, cualquier otro &rarr; ERROR
     * (design.md "Error Handling", R10.1-R10.4).
     */
    @VisibleForTesting
    static EstadoUi.Banner bannerFor(Exception error) {
        if (error instanceof NewsError.NetworkError) {
            return EstadoUi.Banner.OFFLINE;
        }
        if (error instanceof NewsError.QuotaExceededError) {
            return EstadoUi.Banner.QUOTA;
        }
        return EstadoUi.Banner.ERROR;
    }

    // ==================== Mapeo a modelo de presentación ====================

    private List<ArticleUiModel> toUiModels(List<ArticuloNoticia> articles) {
        List<ArticleUiModel> models = new ArrayList<>(articles.size());
        for (ArticuloNoticia a : articles) {
            models.add(toUiModel(a));
        }
        return models;
    }

    private ArticleUiModel toUiModel(ArticuloNoticia a) {
        return new ArticleUiModel(
                a.getArticleId(),
                a.getTitle(),
                a.getDescription(),
                a.getImageUrl(),
                a.getSourceName(),
                a.getArticleUrl(),
                a.getPublishedAtEpochUtc(),
                badgeLabelForSelectedChip());
    }

    /** Etiqueta de badge de liga/categoría para el chip seleccionado (R7.5). */
    private String badgeLabelForSelectedChip() {
        if (KEY_ALL.equals(selectedLeagueKey)) {
            return LABEL_ALL;
        }
        if (KEY_WC_QUALIFICATION.equals(selectedLeagueKey)) {
            return LABEL_WC_QUALIFICATION;
        }
        try {
            return labelForLeague(Integer.parseInt(selectedLeagueKey));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== Helpers ====================

    /** Publica un estado en el hilo principal desde cualquier hilo (resultados de fondo). */
    private void postState(EstadoUi state) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            uiState.setValue(state);
        } else {
            uiState.postValue(state);
        }
    }

    private static List<ArticuloNoticia> safeList(List<ArticuloNoticia> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private static boolean isBlank(String s) {
        return TextUtils.isEmpty(s) || s.trim().isEmpty();
    }

    // ==================== Accessors de test ====================

    @VisibleForTesting
    String getSelectedLeagueKeyForTest() {
        return selectedLeagueKey;
    }

    @VisibleForTesting
    boolean isLoadingForTest() {
        return loading;
    }

    @NonNull
    @VisibleForTesting
    List<ArticuloNoticia> getCurrentLeagueArticlesForTest() {
        return new ArrayList<>(currentLeagueArticles);
    }

    @NonNull
    @VisibleForTesting
    String getActiveSearchQueryForTest() {
        return activeSearchQuery;
    }
}
