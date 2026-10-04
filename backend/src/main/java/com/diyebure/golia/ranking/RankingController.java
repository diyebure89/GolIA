package com.diyebure.golia.ranking;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.diyebure.golia.user.UserEntity;
import com.diyebure.golia.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Ranking global (publico): todos los usuarios ordenados por puntos totales
 * descendente, con su posicion 1..N. Esto es lo que permite que el ranking sea
 * el mismo en todos los dispositivos, porque se calcula sobre los datos
 * compartidos en MySQL.
 */
@RestController
public class RankingController {

    private final UserRepository userRepository;

    public RankingController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/ranking")
    public List<RankingEntryDto> globalRanking() {
        List<UserEntity> users = userRepository.findAllByOrderByTotalPointsDesc();
        List<RankingEntryDto> result = new ArrayList<>(users.size());
        int position = 1;
        for (UserEntity user : users) {
            RankingEntryDto dto = new RankingEntryDto();
            dto.position = position++;
            dto.userId = user.getId();
            dto.username = user.getUsername();
            dto.avatarUrl = user.getAvatarUrl();
            dto.totalPoints = user.getTotalPoints();
            dto.predictionsMade = user.getPredictionsMade();
            dto.predictionsCorrect = user.getPredictionsCorrect();
            result.add(dto);
        }
        return result;
    }

    /** Una fila del ranking global. */
    public static class RankingEntryDto {
        @JsonProperty("position")
        public int position;

        @JsonProperty("user_id")
        public String userId;

        @JsonProperty("username")
        public String username;

        @JsonProperty("avatar_url")
        public String avatarUrl;

        @JsonProperty("total_points")
        public int totalPoints;

        @JsonProperty("predictions_made")
        public int predictionsMade;

        @JsonProperty("predictions_correct")
        public int predictionsCorrect;
    }
}
