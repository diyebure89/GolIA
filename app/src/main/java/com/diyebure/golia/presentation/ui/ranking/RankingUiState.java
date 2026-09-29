package com.diyebure.golia.presentation.ui.ranking;

import androidx.annotation.NonNull;

import com.diyebure.golia.domain.model.Ranking_Entry;

import java.util.Collections;
import java.util.List;

/**
 * Estado observable de la Pantalla_Ranking (jerarquía tipo "sealed").
 *
 * <p>Se expone al fragment mediante {@code LiveData<RankingUiState>} desde el
 * {@code RankingViewModel}. Es una clase abstracta sellada mediante constructor
 * privado, por lo que solo pueden existir las variantes anidadas
 * {@link Loading}, {@link Content}, {@link Empty} y {@link Error} (Requisito
 * 7.1).</p>
 */
public abstract class RankingUiState {

    /** Constructor privado: solo las subclases anidadas pueden extender el estado. */
    private RankingUiState() {
    }

    /**
     * Estado de carga: la pantalla está calculando el ranking (Requisito 1.4).
     */
    public static final class Loading extends RankingUiState {
        public Loading() {
            super();
        }
    }

    /**
     * Estado con contenido: hay un ranking calculado que mostrar.
     *
     * <p>Separa el podio (los tres primeros puestos disponibles) de la lista
     * completa ordenada para simplificar el render del fragment (Requisitos 3.1,
     * 4.1, 6.7).</p>
     */
    public static final class Content extends RankingUiState {

        /** Top 3 (o menos si el ranking tiene menos participantes) para el podio. */
        public final List<Ranking_Entry> podium;

        /** Lista completa ordenada de posiciones (1..N). */
        public final List<Ranking_Entry> list;

        /** Posición 1-based del usuario real, o {@code -1} si no participa. */
        public final int currentUserPosition;

        /** True si algún participante tiene aporte en vivo (Requisito 6.7). */
        public final boolean hasLive;

        public Content(List<Ranking_Entry> podium,
                       List<Ranking_Entry> list,
                       int currentUserPosition,
                       boolean hasLive) {
            super();
            this.podium = podium == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(podium);
            this.list = list == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(list);
            this.currentUserPosition = currentUserPosition;
            this.hasLive = hasLive;
        }
    }

    /**
     * Estado vacío: no hay participantes con datos que mostrar.
     */
    public static final class Empty extends RankingUiState {
        public Empty() {
            super();
        }
    }

    /**
     * Estado de error: no se pudo calcular el ranking; sin datos parciales
     * (Requisito 1.8).
     */
    public static final class Error extends RankingUiState {

        /** Mensaje de error legible para el usuario. */
        public final String message;

        public Error(String message) {
            super();
            this.message = message;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
