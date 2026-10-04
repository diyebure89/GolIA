package com.diyebure.golia.prediction;

import com.diyebure.golia.common.ApiException;
import com.diyebure.golia.prediction.dto.PredictionDtos.PredictionDto;
import com.diyebure.golia.prediction.dto.PredictionDtos.SavePredictionRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints de pronosticos (requieren access token valido):
 * <ul>
 *   <li>POST /predictions: crea o actualiza el pronostico del usuario autenticado.</li>
 *   <li>GET  /predictions: historial del usuario autenticado.</li>
 *   <li>GET  /predictions/by-match?matchId=...: todos los pronosticos de un partido.</li>
 * </ul>
 */
@RestController
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/predictions")
    public PredictionDto save(Authentication authentication,
                              @Valid @RequestBody SavePredictionRequest request) {
        String userId = requireUserId(authentication);
        PredictionEntity saved = predictionService.savePrediction(
                userId, request.matchId, request.homeScore, request.awayScore);
        return PredictionMapper.toDto(saved);
    }

    @GetMapping("/predictions")
    public List<PredictionDto> myPredictions(Authentication authentication) {
        String userId = requireUserId(authentication);
        return predictionService.getUserPredictions(userId).stream()
                .map(PredictionMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/predictions/by-match")
    public List<PredictionDto> byMatch(@RequestParam("matchId") String matchId) {
        return predictionService.getPredictionsForMatch(matchId).stream()
                .map(PredictionMapper::toDto)
                .collect(Collectors.toList());
    }

    private String requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw ApiException.unauthorized("No autenticado.");
        }
        return authentication.getPrincipal().toString();
    }
}
