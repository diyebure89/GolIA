package com.diyebure.golia.presentation.ui.partidos;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Estado observable de la Pantalla_Partidos (jerarquía tipo "sealed").
 *
 * <p>Se expone al fragment mediante {@code LiveData<PartidosUiState>} desde el
 * {@code PartidosViewModel}. Es una clase abstracta sellada mediante constructor
 * privado, por lo que solo pueden existir las variantes anidadas
 * {@link Loading}, {@link Content}, {@link Empty} y {@link Error}.</p>
 */
public abstract class PartidosUiState {

    /** Constructor privado: solo las subclases anidadas pueden extender el estado. */
    private PartidosUiState() {
    }

    /**
     * Estado de carga: la pantalla está obteniendo datos por primera vez.
     */
    public static final class Loading extends PartidosUiState {
        public Loading() {
            super();
        }
    }

    /**
     * Estado con contenido: hay una lista de partidos a mostrar.
     */
    public static final class Content extends PartidosUiState {

        /** Lista inmutable de modelos de presentación a renderizar. */
        public final List<MatchUiModel> matches;

        /** True si los datos provienen de caché por fallo remoto (aviso offline, R11.1). */
        public final boolean offlineNotice;

        /** True si se alcanzó el límite diario de peticiones (aviso de cuota, R7.7). */
        public final boolean quotaNotice;

        public Content(List<MatchUiModel> matches, boolean offlineNotice, boolean quotaNotice) {
            super();
            this.matches = matches == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(matches);
            this.offlineNotice = offlineNotice;
            this.quotaNotice = quotaNotice;
        }
    }

    /**
     * Estado vacío: no hay partidos que mostrar para el criterio actual.
     */
    public static final class Empty extends PartidosUiState {

        /**
         * Motivo del estado vacío.
         *
         * <ul>
         *     <li>{@link #FILTER}: no hay partidos en el chip de horario activo (R9.2).</li>
         *     <li>{@link #SEARCH}: la búsqueda no arrojó coincidencias (R10.7).</li>
         * </ul>
         */
        public enum Type {
            FILTER,
            SEARCH
        }

        /** Motivo del estado vacío. */
        public final Type type;

        public Empty(Type type) {
            super();
            this.type = type;
        }
    }

    /**
     * Estado de error: no hay datos que mostrar y ocurrió un fallo.
     */
    public static final class Error extends PartidosUiState {

        /** Mensaje de error legible para el usuario. */
        public final String message;

        /** True si el error admite reintento (se ofrece acción "Reintentar", R9.3/R9.4). */
        public final boolean retryable;

        public Error(String message, boolean retryable) {
            super();
            this.message = message;
            this.retryable = retryable;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
