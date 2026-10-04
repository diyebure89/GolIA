package com.diyebure.golia.data.repository;

import com.diyebure.golia.data.remote.api.MatchApiService;
import com.diyebure.golia.data.remote.api.PredictionApiService;
import com.diyebure.golia.data.remote.dto.MatchResolveDto.ResolveRequest;
import com.diyebure.golia.data.remote.dto.PredictionDto.PredictionResponse;
import com.diyebure.golia.data.remote.dto.PredictionDto.SavePredictionRequest;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.PredictionError;
import com.diyebure.golia.domain.error.PredictionException;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.model.PredictionOutcome;
import com.diyebure.golia.domain.repository.PredictionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Implementacion remota de {@link PredictionRepository}: los pronosticos se
 * guardan y leen del backend GolIA (MySQL), de modo que se comparten entre
 * dispositivos. Sustituye a la anterior {@code PredictionRepositoryImpl} (Room).
 *
 * <p>Los partidos NO viven en este backend (vienen de la API externa de futbol
 * cacheada en Room), por eso cada pronostico conserva su {@code match_id} en
 * {@code rawMatchId} del dominio: los use cases resuelven el {@link Match} contra
 * la cache local para mostrar nombres/marcadores.</p>
 *
 * <p>Estilo sincrono que devuelve {@link Result}, igual que el resto de
 * repositorios; los use cases despachan estas llamadas al {@code @IoExecutor}.
 * La resolucion de puntajes ({@link #resolveForMatch}) notifica al backend para
 * que calcule puntos y actualice los contadores de ranking en MySQL.</p>
 */
@Singleton
public class RemotePredictionRepository implements PredictionRepository {

    private final PredictionApiService api;
    private final MatchApiService matchApi;

    @Inject
    public RemotePredictionRepository(PredictionApiService api, MatchApiService matchApi) {
        this.api = api;
        this.matchApi = matchApi;
    }

    @Override
    public Result<Prediction> getUserPrediction(String userId, String matchId) {
        // El backend no tiene un endpoint "mi pronostico para este partido", asi
        // que se filtra del historial del usuario autenticado.
        try {
            Response<List<PredictionResponse>> response = api.getMyPredictions().execute();
            if (!response.isSuccessful() || response.body() == null) {
                return error(PredictionError.PERSISTENCE_ERROR);
            }
            for (PredictionResponse dto : response.body()) {
                if (dto.getMatchId() != null && dto.getMatchId().equals(matchId)) {
                    return new Result.Success<>(toDomain(dto));
                }
            }
            // Sin pronostico para ese partido.
            return error(PredictionError.MATCH_NOT_FOUND);
        } catch (Exception e) {
            return persistenceError(e);
        }
    }

    @Override
    public Result<Prediction> savePrediction(String userId, String matchId,
                                             int homeScore, int awayScore) {
        try {
            SavePredictionRequest body = new SavePredictionRequest(matchId, homeScore, awayScore);
            Response<PredictionResponse> response = api.savePrediction(body).execute();
            if (response.isSuccessful() && response.body() != null) {
                return new Result.Success<>(toDomain(response.body()));
            }
            if (response.code() == 401 || response.code() == 403) {
                return error(PredictionError.SESSION_UNAVAILABLE);
            }
            if (response.code() == 422) {
                return error(PredictionError.VALIDATION_ERROR);
            }
            return error(PredictionError.PERSISTENCE_ERROR);
        } catch (Exception e) {
            return persistenceError(e);
        }
    }

    @Override
    public Result<List<Prediction>> getUserPredictions(String userId) {
        try {
            Response<List<PredictionResponse>> response = api.getMyPredictions().execute();
            if (!response.isSuccessful() || response.body() == null) {
                return persistenceErrorList();
            }
            return new Result.Success<>(toDomainList(response.body()));
        } catch (Exception e) {
            return persistenceErrorListCause(e);
        }
    }

    @Override
    public Result<List<Prediction>> getPredictionsForMatch(String matchId) {
        try {
            Response<List<PredictionResponse>> response = api.getByMatch(matchId).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return persistenceErrorList();
            }
            return new Result.Success<>(toDomainList(response.body()));
        } catch (Exception e) {
            return persistenceErrorListCause(e);
        }
    }

    @Override
    public Result<Void> resolveForMatch(Match finishedMatch) {
        // Notifica al backend para que resuelva los pronosticos pendientes del
        // partido y actualice total_points / predictions_correct en MySQL.
        // Si no hay red o el backend falla, degradamos a exito silencioso:
        // la resolucion se reintentara la proxima vez que la app refresque ese partido.
        if (finishedMatch == null
                || finishedMatch.getHomeScore() == null
                || finishedMatch.getAwayScore() == null) {
            return new Result.Success<>(null);
        }

        String matchId = finishedMatch.getRawId() != null
                ? finishedMatch.getRawId()
                : (finishedMatch.getId() != null ? finishedMatch.getId().toString() : null);
        if (matchId == null) {
            return new Result.Success<>(null);
        }

        try {
            ResolveRequest body = new ResolveRequest(
                    finishedMatch.getHomeScore(),
                    finishedMatch.getAwayScore());
            matchApi.resolveMatch(matchId, body).execute();
            // Tratamos cualquier respuesta (incluso errores de servidor) como exito
            // silencioso en el cliente: el backend es idempotente y lo intentara
            // de nuevo en el siguiente refresco si algo fallo.
        } catch (Exception ignored) {
            // Sin red: no rompemos el flujo de refresco de partidos.
        }

        return new Result.Success<>(null);
    }

    // ==================== Mapeo DTO -> dominio ====================

    /**
     * Convierte la respuesta del backend al modelo de dominio, replicando el
     * comportamiento de {@code PredictionEntity.toDomainModel()}: el
     * {@code match_id} se preserva SIEMPRE en {@code rawMatchId} (identidad de
     * busqueda del partido en la cache local), y solo se intenta parsear a UUID
     * para {@code matchId} cuando es un UUID canonico.
     */
    private Prediction toDomain(PredictionResponse dto) {
        Prediction prediction = new Prediction();

        prediction.setId(parseUuidOrRandom(dto.getId()));
        prediction.setUserId(parseUuidOrNull(dto.getUserId()));

        String matchId = dto.getMatchId();
        if (matchId != null && !matchId.isEmpty()) {
            prediction.setRawMatchId(matchId);
            UUID matchUuid = parseUuidOrNull(matchId);
            if (matchUuid != null) {
                prediction.setMatchId(matchUuid);
            }
        }

        if (dto.getPredictedOutcome() != null) {
            try {
                prediction.setPredictedOutcome(
                        PredictionOutcome.valueOf(dto.getPredictedOutcome()));
            } catch (IllegalArgumentException ignored) {
                // Outcome desconocido: se deja null, no rompe el historial.
            }
        }

        prediction.setPredictedHomeScore(dto.getPredictedHomeScore());
        prediction.setPredictedAwayScore(dto.getPredictedAwayScore());
        prediction.setPointsEarned(dto.getPointsEarned());
        prediction.setIsCorrect(dto.getIsCorrect());
        prediction.setCreatedAt(dto.getCreatedAt());
        prediction.setFinalizedAt(dto.getFinalizedAt());
        return prediction;
    }

    private List<Prediction> toDomainList(List<PredictionResponse> dtos) {
        List<Prediction> result = new ArrayList<>();
        if (dtos != null) {
            for (PredictionResponse dto : dtos) {
                if (dto != null) {
                    result.add(toDomain(dto));
                }
            }
        }
        return result;
    }

    private static UUID parseUuidOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UUID parseUuidOrRandom(String value) {
        UUID parsed = parseUuidOrNull(value);
        return parsed != null ? parsed : UUID.randomUUID();
    }

    // ==================== Helpers de error ====================

    private static Result<Prediction> error(PredictionError predictionError) {
        return new Result.Error(new PredictionException(predictionError));
    }

    private static Result<Prediction> persistenceError(Exception cause) {
        return new Result.Error(new PredictionException(PredictionError.PERSISTENCE_ERROR, cause));
    }

    private static Result<List<Prediction>> persistenceErrorList() {
        return new Result.Error(new PredictionException(PredictionError.PERSISTENCE_ERROR));
    }

    private static Result<List<Prediction>> persistenceErrorListCause(Exception cause) {
        return new Result.Error(new PredictionException(PredictionError.PERSISTENCE_ERROR, cause));
    }
}
