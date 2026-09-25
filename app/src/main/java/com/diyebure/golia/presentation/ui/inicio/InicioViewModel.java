package com.diyebure.golia.presentation.ui.inicio;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.diyebure.golia.BuildConfig;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.usecase.GetMatchesUseCase;
import com.diyebure.golia.domain.usecase.RefreshMatchesUseCase;
import com.diyebure.golia.presentation.ui.partidos.MatchUiMapper;
import com.diyebure.golia.presentation.ui.partidos.MatchUiModel;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel de la sección "Próximos partidos" del Inicio (@HiltViewModel).
 *
 * <p>Consume la misma fuente de datos que la Pantalla_Partidos
 * ({@link GetMatchesUseCase} cache-first + {@link RefreshMatchesUseCase} por
 * fecha de hoy) y expone una lista reducida de próximos encuentros ya mapeados
 * a {@link MatchUiModel} mediante {@link MatchUiMapper}.</p>
 *
 * <p>Al ser una sección secundaria, no expone estados de carga ni de error: si
 * falta la clave de API o falla el refresco, simplemente emite una lista vacía.
 * Los use cases entregan su resultado en un hilo de fondo, por lo que se publica
 * con {@code postValue}.</p>
 */
@HiltViewModel
public class InicioViewModel extends ViewModel {

    /** Número máximo de próximos partidos mostrados en el Inicio. */
    private static final int UPCOMING_LIMIT = 5;

    private final GetMatchesUseCase getMatches;
    private final RefreshMatchesUseCase refreshMatches;

    private final MutableLiveData<List<MatchUiModel>> upcomingMatches = new MutableLiveData<>();

    /** True cuando la clave de API está ausente (bloquea el refresco remoto). */
    private final boolean apiKeyMissing;

    @Inject
    public InicioViewModel(GetMatchesUseCase getMatches, RefreshMatchesUseCase refreshMatches) {
        this.getMatches = getMatches;
        this.refreshMatches = refreshMatches;
        this.apiKeyMissing = isBlank(BuildConfig.API_FOOTBALL_KEY);

        // Emitir una lista vacía inicial para evitar nulls en los observadores.
        upcomingMatches.setValue(Collections.emptyList());

        initLoad();
    }

    /** @return los próximos partidos ya mapeados para pintar en el Inicio. */
    public LiveData<List<MatchUiModel>> getUpcomingMatches() {
        return upcomingMatches;
    }

    /**
     * Carga cache-first: pinta de inmediato desde la caché y luego dispara un
     * refresco por la fecha de hoy. Si falta la clave de API, solo lee la caché.
     */
    private void initLoad() {
        getMatches.execute(result -> {
            if (result.isSuccess()) {
                publish(result.getOrNull());
            }
            requestDateRefresh();
        });
    }

    private void requestDateRefresh() {
        if (apiKeyMissing) {
            return;
        }
        refreshMatches.execute(todayIso(), result -> {
            if (result.isSuccess()) {
                publish(result.getOrNull());
            }
            // Ante error se conserva lo ya publicado (o la lista vacía inicial);
            // el Inicio no muestra estados de error (sección secundaria).
        });
    }

    /**
     * Deriva la lista de próximos partidos: SCHEDULED o LIVE con horario a partir
     * de ahora, ordenados ascendentemente por horario y limitados a
     * {@link #UPCOMING_LIMIT}. Publica con {@code postValue} (hilo de fondo).
     */
    private void publish(List<Match> matches) {
        List<MatchUiModel> models = toUpcomingUiModels(matches);
        upcomingMatches.postValue(models);
    }

    private List<MatchUiModel> toUpcomingUiModels(List<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        List<Match> upcoming = new ArrayList<>();
        for (Match m : matches) {
            MatchStatus status = m.getStatus();
            // Los partidos LIVE siempre son elegibles (aunque su horario ya pasó,
            // están en curso). Los SCHEDULED solo si aún no han empezado.
            boolean eligible = status == MatchStatus.LIVE
                    || (status == MatchStatus.SCHEDULED && m.getScheduledDateTime() >= now);
            if (eligible) {
                upcoming.add(m);
            }
        }

        // LIVE primero, luego por horario ascendente (coherente con Partidos).
        Collections.sort(upcoming, Comparator
                .comparingInt((Match m) -> m.getStatus() == MatchStatus.LIVE ? 0 : 1)
                .thenComparingLong(Match::getScheduledDateTime));

        int limit = Math.min(UPCOMING_LIMIT, upcoming.size());
        List<MatchUiModel> models = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            models.add(MatchUiMapper.toUiModel(upcoming.get(i)));
        }
        return models;
    }

    private static String todayIso() {
        return LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static boolean isBlank(String s) {
        return TextUtils.isEmpty(s) || s.trim().isEmpty();
    }
}
