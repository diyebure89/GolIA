package com.diyebure.golia.presentation.ui.news;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Modelo de presentación de un chip de filtro por liga de la Pantalla_Noticias.
 *
 * <p>{@link #leagueKey} es una clave textual estable: el identificador numérico
 * para ligas individuales ({@code "39"}, {@code "140"}, ...), {@code "wc_qualification"}
 * para el chip agregado de eliminatorias y {@code "all"} para el {@code Chip_Todos}.
 * Esta clave es la que se persiste en la caché (relación N:M y {@code lastFetchedAt}),
 * evitando colisiones entre chips agregados e individuales.</p>
 *
 * <p>Es una clase inmutable con {@link #equals(Object)}/{@link #hashCode()} por
 * campos para una observación eficiente vía LiveData.</p>
 *
 * <p>Requirements: R10.1, R10.3.</p>
 */
public final class ChipLiga {

    /** Clave textual estable de la liga ("39", "wc_qualification", "all"). */
    public final String leagueKey;

    /** Etiqueta visible del chip (p.ej. "Premier League", "Eliminatorias", "Todos"). */
    public final String label;

    /** True si el chip está seleccionado actualmente (selección única). */
    public final boolean selected;

    /**
     * Crea un chip de liga inmutable.
     *
     * @param leagueKey clave textual estable de la liga
     * @param label     etiqueta visible del chip
     * @param selected  true si el chip está seleccionado
     */
    public ChipLiga(String leagueKey, String label, boolean selected) {
        this.leagueKey = leagueKey;
        this.label = label;
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChipLiga that = (ChipLiga) o;
        return selected == that.selected
                && Objects.equals(leagueKey, that.leagueKey)
                && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leagueKey, label, selected);
    }

    @NonNull
    @Override
    public String toString() {
        return "ChipLiga{leagueKey='" + leagueKey + "', label='" + label
                + "', selected=" + selected + '}';
    }
}
