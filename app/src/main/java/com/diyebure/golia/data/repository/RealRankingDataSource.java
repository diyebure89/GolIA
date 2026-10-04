package com.diyebure.golia.data.repository;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.remote.api.RankingApiService;
import com.diyebure.golia.data.remote.dto.RankingEntryDto;
import com.diyebure.golia.domain.model.RankingSnapshot;
import com.diyebure.golia.domain.repository.RankingDataSource;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Response;

/**
 * {@link RankingDataSource} real que obtiene las metricas del usuario actual
 * desde el backend GolIA (MySQL), donde viven los datos compartidos entre
 * dispositivos.
 *
 * <p>Llama a {@code GET /ranking}, localiza la fila del usuario actual por su
 * {@code userId} y construye el {@link RankingSnapshot} (posicion, puntos,
 * aciertos %, pronosticos). Si el backend no responde (sin red, servidor caido)
 * devuelve {@code null} para que las pantallas (Inicio, Perfil) muestren el
 * estado neutro sin error (R11.5).</p>
 */
public class RealRankingDataSource implements RankingDataSource {

    private final RankingApiService rankingApiService;
    private final PreferencesManager preferencesManager;

    @Inject
    public RealRankingDataSource(RankingApiService rankingApiService,
                                 PreferencesManager preferencesManager) {
        this.rankingApiService = rankingApiService;
        this.preferencesManager = preferencesManager;
    }

    @Override
    public RankingSnapshot getRankingForCurrentUser() {
        try {
            String currentUserId = preferencesManager.getUserId();
            if (currentUserId == null || currentUserId.isEmpty()) {
                return null;
            }

            Response<List<RankingEntryDto>> response =
                    rankingApiService.getGlobalRanking().execute();
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }

            List<RankingEntryDto> entries = response.body();
            for (RankingEntryDto entry : entries) {
                if (currentUserId.equals(entry.getUserId())) {
                    int position = entry.getPosition();
                    if (position < 1) {
                        return null;
                    }
                    int predictionsMade = entry.getPredictionsMade();
                    int accuracy = winPercentage(
                            entry.getPredictionsCorrect(), entry.getPredictionsMade());
                    int points = entry.getTotalPoints();
                    return new RankingSnapshot(
                            predictionsMade,
                            clamp(accuracy),
                            points,
                            position);
                }
            }
            // El usuario aun no tiene posicion en el ranking (sin pronosticos).
            return null;
        } catch (Exception e) {
            // Sin red o error: estado neutro, sin mensaje de error (R11.5).
            return null;
        }
    }

    private static int winPercentage(int correct, int made) {
        if (made == 0) {
            return 0;
        }
        return (int) Math.round((double) correct / made * 100.0);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
