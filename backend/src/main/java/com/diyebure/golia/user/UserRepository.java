package com.diyebure.golia.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de usuarios (Spring Data JPA genera la implementacion).
 */
public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    /** Login por username O email en una sola consulta. */
    Optional<UserEntity> findByUsernameOrEmail(String username, String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    /** Ranking global: usuarios ordenados por puntos totales descendente. */
    List<UserEntity> findAllByOrderByTotalPointsDesc();
}
