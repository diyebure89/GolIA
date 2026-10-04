package com.diyebure.golia.prediction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de pronosticos (Spring Data JPA genera la implementacion).
 */
public interface PredictionRepository extends JpaRepository<PredictionEntity, String> {

    /** Historial del usuario, mas reciente primero. */
    List<PredictionEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    /** Todos los pronosticos de un partido. */
    List<PredictionEntity> findByMatchId(String matchId);

    /** El pronostico del usuario para un partido concreto (clave logica unica). */
    Optional<PredictionEntity> findByUserIdAndMatchId(String userId, String matchId);

    long countByUserId(String userId);
}
