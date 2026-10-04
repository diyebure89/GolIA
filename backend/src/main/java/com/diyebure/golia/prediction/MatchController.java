package com.diyebure.golia.prediction;

import com.diyebure.golia.prediction.dto.MatchResolveDtos.ResolveRequest;
import com.diyebure.golia.prediction.dto.MatchResolveDtos.ResolveResponse;
import com.diyebure.golia.prediction.dto.PredictionDtos.PredictionDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints relacionados con un partido concreto:
 *
 * <ul>
 *   <li>{@code POST /matches/{matchId}/resolve} — la app llama este endpoint
 *       cuando detecta que un partido termino; el backend resuelve todos los
 *       pronosticos pendientes, calcula puntos con la misma regla que la app y
 *       actualiza {@code total_points}/{@code predictions_correct} en
 *       {@code users}. <strong>Requiere autenticacion</strong> (Bearer token),
 *       por lo que solo un dispositivo con sesion activa puede dispararlo.</li>
 *   <li>{@code GET /matches/{matchId}/predictions} — lista todos los pronosticos
 *       almacenados para un partido. Util para depuracion y para que la app
 *       compruebe el estado de los pronosticos de otros usuarios.</li>
 * </ul>
 */
@RestController
@RequestMapping("/matches")
public class MatchController {

    private final ResolutionService resolutionService;
    private final PredictionService predictionService;

    public MatchController(ResolutionService resolutionService,
                           PredictionService predictionService) {
        this.resolutionService = resolutionService;
        this.predictionService = predictionService;
    }

    /**
     * Resuelve los pronosticos pendientes del partido usando el marcador final.
     *
     * <p>La operacion es <strong>idempotente</strong>: si se llama varias veces
     * con el mismo marcador los pronosticos ya resueltos se omiten y los
     * contadores de usuario no se duplican.</p>
     *
     * @param matchId id del partido (el mismo que usa la app en {@code match_id})
     * @param request marcador final: {@code home_score} y {@code away_score}
     * @return resumen de pronosticos procesados, acertados y ya-resueltos
     */
    @PostMapping("/{matchId}/resolve")
    public ResponseEntity<ResolveResponse> resolve(
            @PathVariable String matchId,
            @Valid @RequestBody ResolveRequest request) {

        ResolveResponse response = resolutionService.resolve(
                matchId, request.homeScore, request.awayScore);
        return ResponseEntity.ok(response);
    }

    /**
     * Devuelve todos los pronosticos almacenados para el partido.
     * Util para ver el estado de resolucion desde cualquier dispositivo.
     */
    @GetMapping("/{matchId}/predictions")
    public List<PredictionDto> predictionsForMatch(@PathVariable String matchId) {
        return predictionService.getPredictionsForMatch(matchId)
                .stream()
                .map(PredictionMapper::toDto)
                .collect(Collectors.toList());
    }
}
