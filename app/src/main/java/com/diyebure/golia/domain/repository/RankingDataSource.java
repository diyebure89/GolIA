package com.diyebure.golia.domain.repository;

import com.diyebure.golia.domain.model.RankingSnapshot;

/**
 * Domain source for the ranking metrics shown in the "Historial" section of
 * {@code Pantalla_Perfil} and in the Inicio welcome card.
 *
 * <p>Modeled as a domain interface so the same Hilt binding feeds both screens with
 * identical values (R11.3), and the real ranking can be plugged in later without
 * touching {@code Perfil_UI} (R11.5, R11.6).
 */
public interface RankingDataSource {

    /**
     * Returns the ranking snapshot for the current user.
     *
     * @return an immutable {@link RankingSnapshot}
     */
    RankingSnapshot getRankingForCurrentUser(); // R11.4
}
