package com.diyebure.golia.presentation.ui.news;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Estado_UI: estado observable compuesto e inmutable de la Pantalla_Noticias.
 *
 * <p>Se expone al fragment mediante {@code LiveData<EstadoUi>} desde el
 * {@code NoticiasViewModel}. A diferencia de una jerarquía sellada, es una clase
 * compuesta única que permite representar "contenido + banner" de forma
 * simultánea: los artículos cacheados ({@link #items}) pueden mostrarse a la vez
 * que un aviso {@link Banner#OFFLINE} o {@link Banner#QUOTA} sin ocultar el
 * contenido.</p>
 *
 * <p>Incluye {@link #equals(Object)}/{@link #hashCode()} por campos para una
 * observación eficiente vía LiveData (evita re-render ante estados equivalentes).</p>
 *
 * <p>Requirements: R9.1, R10.1, R10.3.</p>
 */
public final class EstadoUi {

    /**
     * Tipo de banner de aviso mostrado junto al contenido.
     *
     * <ul>
     *     <li>{@link #NINGUNO}: sin aviso.</li>
     *     <li>{@link #OFFLINE}: datos servidos desde caché por falta de red (R10.1/R10.2).</li>
     *     <li>{@link #QUOTA}: se alcanzó el presupuesto/cuota diario (R6.5/R6.10/R10.3).</li>
     *     <li>{@link #ERROR}: fallo del proveedor/persistencia u otro error (R9.4/R10.3).</li>
     * </ul>
     */
    public enum Banner {
        NINGUNO,
        OFFLINE,
        QUOTA,
        ERROR
    }

    /**
     * Motivo del estado vacío (sin artículos que mostrar).
     *
     * <ul>
     *     <li>{@link #NINGUNO}: hay contenido; no es un estado vacío.</li>
     *     <li>{@link #LEAGUE}: no hay noticias para la liga/chip seleccionado.</li>
     *     <li>{@link #SEARCH}: la búsqueda no arrojó coincidencias (R8.7).</li>
     * </ul>
     */
    public enum EmptyKind {
        NINGUNO,
        LEAGUE,
        SEARCH
    }

    /** Lista inmutable de modelos de presentación a renderizar. */
    public final List<ArticleUiModel> items;

    /** True si la pantalla está cargando datos. */
    public final boolean loading;

    /** Banner de aviso a mostrar junto al contenido. */
    public final Banner banner;

    /** Motivo del estado vacío (o {@link EmptyKind#NINGUNO} si hay contenido). */
    public final EmptyKind empty;

    /**
     * Crea un estado de UI inmutable.
     *
     * @param items   lista de artículos a renderizar (null se trata como vacía)
     * @param loading true si la pantalla está cargando
     * @param banner  banner de aviso (no nulo)
     * @param empty   motivo del estado vacío (no nulo)
     */
    public EstadoUi(List<ArticleUiModel> items, boolean loading, Banner banner, EmptyKind empty) {
        this.items = items == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(items);
        this.loading = loading;
        this.banner = banner;
        this.empty = empty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EstadoUi that = (EstadoUi) o;
        return loading == that.loading
                && Objects.equals(items, that.items)
                && banner == that.banner
                && empty == that.empty;
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, loading, banner, empty);
    }

    @NonNull
    @Override
    public String toString() {
        return "EstadoUi{items=" + items.size() + ", loading=" + loading
                + ", banner=" + banner + ", empty=" + empty + '}';
    }
}
