package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.remote.api.RankingApiService;
import com.diyebure.golia.data.remote.dto.RankingEntryDto;
import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.RankingResult;
import com.diyebure.golia.domain.model.Ranking_Calculator;
import com.diyebure.golia.domain.model.Ranking_Entry;
import com.diyebure.golia.domain.model.Ranking_Participant;
import com.diyebure.golia.domain.model.Ranking_Period;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import retrofit2.Response;

/**
 * Computes the ranking off the main thread (R1.2, R1.3, R7.5).
 *
 * <p><strong>Estrategia remoto-primero con fallback local.</strong> Cuando el
 * backend GolIA responde correctamente, el ranking se construye directamente
 * desde los {@link RankingEntryDto} (datos compartidos entre dispositivos desde
 * MySQL). Si el backend no responde (sin red, servidor caído, etc.) se cae al
 * cálculo local: {@link Ranking_Participant_Provider} + {@link Ranking_Calculator}
 * con los datos de Room del dispositivo.</p>
 *
 * <p>El ranking del backend es global (todos los usuarios de MySQL); el local
 * es el usuario actual con los partidos cacheados. Ambos modos respetan el
 * {@link Ranking_Period} seleccionado, aunque el backend actualmente devuelve
 * solo el total (el filtro por periodo se aplica en el cálculo local).</p>
 *
 * <p>El {@code token} sigue siendo el mecanismo anti-overlap del ViewModel
 * (R7.6). No hay llamada a la API de fútbol.</p>
 */
public class GetRankingUseCase {

    private final Ranking_Participant_Provider participantProvider;
    private final Ranking_Calculator calculator;
    private final RankingApiService rankingApiService;
    private final PreferencesManager preferencesManager;
    private final ExecutorService executor;

    @Inject
    public GetRankingUseCase(Ranking_Participant_Provider participantProvider,
                             Ranking_Calculator calculator,
                             RankingApiService rankingApiService,
                             PreferencesManager preferencesManager,
                             @IoExecutor ExecutorService executor) {
        this.participantProvider = participantProvider;
        this.calculator = calculator;
        this.rankingApiService = rankingApiService;
        this.preferencesManager = preferencesManager;
        this.executor = executor;
    }

    /**
     * Computes the ranking for the given period off the main thread.
     *
     * @param period   the selected ranking period
     * @param userId   the current user id (used to flag the current user)
     * @param token    the ViewModel generation token for anti-overlap ("last wins")
     * @param callback receives {@code Result.Success<RankingResult>} or
     *                 {@code Result.Error} on failure
     */
    public void execute(Ranking_Period period,
                        String userId,
                        long token,
                        Callback<RankingResult> callback) {
        executor.execute(() -> callback.onResult(compute(period, userId)));
    }

    private Result<RankingResult> compute(Ranking_Period period, String userId) {
        // 1) Intenta el backend (ranking global compartido entre dispositivos).
        Result<RankingResult> remote = computeFromBackend(userId);
        if (remote != null && remote.isSuccess()) {
            return remote;
        }

        // 2) Fallback: cálculo local con los datos cacheados en Room.
        return computeLocal(period);
    }

    // ==================== Rama remota ====================

    /**
     * Llama a GET /ranking y construye un {@link RankingResult} con los datos
     * del backend. El periodo no se filtra aquí (el backend devuelve totales);
     * el ViewModel puede mostrar el resultado igual. Devuelve {@code null} si
     * la llamada falla para señalar al llamador que use el fallback local.
     */
    private Result<RankingResult> computeFromBackend(String currentUserId) {
        try {
            Response<List<RankingEntryDto>> response =
                    rankingApiService.getGlobalRanking().execute();
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            List<RankingEntryDto> dtos = response.body();
            if (dtos.isEmpty()) {
                return new Result.Success<>(
                        new RankingResult(new ArrayList<>(), -1, false));
            }

            List<Ranking_Entry> entries = new ArrayList<>(dtos.size());
            int currentUserPosition = -1;
            for (RankingEntryDto dto : dtos) {
                boolean isCurrent = dto.getUserId() != null
                        && dto.getUserId().equals(currentUserId);
                if (isCurrent) {
                    currentUserPosition = dto.getPosition();
                }
                entries.add(new Ranking_Entry(
                        dto.getPosition(),
                        dto.getUsername() != null ? dto.getUsername() : "Usuario",
                        dto.getAvatarUrl(),
                        dto.getTotalPoints(),
                        // Calcula el porcentaje de aciertos a partir de los contadores.
                        winPercentage(dto.getPredictionsCorrect(), dto.getPredictionsMade()),
                        0,         // streak: no disponible en el backend todavia
                        isCurrent,
                        false));   // hasLive: no disponible en el backend todavia
            }
            return new Result.Success<>(
                    new RankingResult(entries, currentUserPosition, false));
        } catch (Exception e) {
            // Sin red, timeout, etc. → señal de "usa el fallback local".
            return null;
        }
    }

    /** Porcentaje de aciertos redondeado, o 0 si no hay predicciones. */
    private static int winPercentage(int correct, int made) {
        if (made == 0) {
            return 0;
        }
        return (int) Math.round((double) correct / made * 100.0);
    }

    // ==================== Rama local (fallback) ====================

    private Result<RankingResult> computeLocal(Ranking_Period period) {
        try {
            long now = System.currentTimeMillis();
            List<Ranking_Participant> participants =
                    participantProvider.getParticipants(now);
            RankingResult result = calculator.rank(participants, period, now);
            return new Result.Success<>(result);
        } catch (Exception e) {
            return new Result.Error(e);
        }
    }
}
