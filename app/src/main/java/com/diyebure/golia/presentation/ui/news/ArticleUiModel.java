package com.diyebure.golia.presentation.ui.news;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.diyebure.golia.domain.model.ArticuloNoticia;

import java.util.Objects;

/**
 * Modelo de presentación inmutable de un artículo de noticias, enlazado por el
 * {@code NewsAdapter} y renderizado en {@code item_news.xml}.
 *
 * <p>Se deriva del modelo de dominio {@link ArticuloNoticia} conservando sus
 * campos ({@code articleId}, {@code title}, {@code description}, {@code imageUrl},
 * {@code sourceName}, {@code articleUrl}, {@code publishedAtEpochUtc}) y añade
 * campos derivados de UI como la etiqueta de badge de liga/categoría
 * ({@link #badgeLabel}).</p>
 *
 * <p>{@link #articleId} es el identificador estable usado por
 * {@code DiffUtil.areItemsTheSame}; {@link #equals(Object)}/{@link #hashCode()}
 * comparan todos los campos visibles para {@code areContentsTheSame}.</p>
 *
 * <p>Requirements: R9.1, R10.1, R10.3.</p>
 */
public final class ArticleUiModel {

    /** Identificador estable del artículo (hash de URL canónica); usado por DiffUtil.areItemsTheSame. */
    public final String articleId;

    /** Título del artículo (obligatorio). */
    public final String title;

    /** Descripción/resumen corto (opcional, puede ser null). */
    @Nullable
    public final String description;

    /** URL de la imagen, esperada https (opcional, puede ser null). */
    @Nullable
    public final String imageUrl;

    /** Nombre legible de la fuente (opcional, puede ser null). */
    @Nullable
    public final String sourceName;

    /** URL canónica del artículo, esperada https (obligatorio). */
    public final String articleUrl;

    /** Instante de publicación como epoch UTC (segundos). */
    public final long publishedAtEpochUtc;

    /** Etiqueta del badge de liga/categoría a mostrar en la tarjeta (opcional, puede ser null). */
    @Nullable
    public final String badgeLabel;

    /**
     * Crea un modelo de presentación inmutable de un artículo.
     *
     * @param articleId           hash de la URL canónica (obligatorio)
     * @param title               título del artículo (obligatorio)
     * @param description         descripción corta (opcional, nullable)
     * @param imageUrl            URL de la imagen (opcional, nullable, https)
     * @param sourceName          nombre de la fuente (opcional, nullable)
     * @param articleUrl          URL canónica del artículo (obligatorio, https)
     * @param publishedAtEpochUtc instante de publicación en epoch UTC
     * @param badgeLabel          etiqueta del badge de liga/categoría (opcional, nullable)
     */
    public ArticleUiModel(String articleId,
                          String title,
                          @Nullable String description,
                          @Nullable String imageUrl,
                          @Nullable String sourceName,
                          String articleUrl,
                          long publishedAtEpochUtc,
                          @Nullable String badgeLabel) {
        this.articleId = articleId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sourceName = sourceName;
        this.articleUrl = articleUrl;
        this.publishedAtEpochUtc = publishedAtEpochUtc;
        this.badgeLabel = badgeLabel;
    }

    /**
     * Igualdad por todos los campos visibles, útil para DiffUtil.areContentsTheSame.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArticleUiModel that = (ArticleUiModel) o;
        return publishedAtEpochUtc == that.publishedAtEpochUtc
                && Objects.equals(articleId, that.articleId)
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description)
                && Objects.equals(imageUrl, that.imageUrl)
                && Objects.equals(sourceName, that.sourceName)
                && Objects.equals(articleUrl, that.articleUrl)
                && Objects.equals(badgeLabel, that.badgeLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, title, description, imageUrl, sourceName,
                articleUrl, publishedAtEpochUtc, badgeLabel);
    }

    @NonNull
    @Override
    public String toString() {
        return "ArticleUiModel{articleId='" + articleId + "', title='" + title
                + "', sourceName='" + sourceName + "', badgeLabel='" + badgeLabel + "'}";
    }
}
