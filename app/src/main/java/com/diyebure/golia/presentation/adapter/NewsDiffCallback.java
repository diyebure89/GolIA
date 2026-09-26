package com.diyebure.golia.presentation.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.diyebure.golia.presentation.ui.news.ArticleUiModel;

import java.util.Objects;

/**
 * DiffUtil callback para {@link ArticleUiModel} usado por {@link NewsAdapter}.
 *
 * <ul>
 *     <li>{@link #areItemsTheSame}: dos elementos representan el mismo artículo cuando
 *     comparten identificador estable ({@code articleId}, hash de la URL canónica).</li>
 *     <li>{@link #areContentsTheSame}: el contenido visible es idéntico cuando coinciden
 *     todos los campos que la tarjeta muestra (título, descripción, imagen, fuente, URL,
 *     fecha y badge). Se apoya en {@link ArticleUiModel#equals(Object)}, que compara todos
 *     esos campos, para que RecyclerView solo re-ligue las tarjetas cuyo contenido visible
 *     cambió.</li>
 * </ul>
 *
 * <p>Requirements: R11.3 (DiffUtil), R7.1..R7.6 (campos visibles de la tarjeta).</p>
 */
public class NewsDiffCallback extends DiffUtil.ItemCallback<ArticleUiModel> {

    @Override
    public boolean areItemsTheSame(@NonNull ArticleUiModel oldItem, @NonNull ArticleUiModel newItem) {
        return Objects.equals(oldItem.articleId, newItem.articleId);
    }

    @Override
    public boolean areContentsTheSame(@NonNull ArticleUiModel oldItem, @NonNull ArticleUiModel newItem) {
        // ArticleUiModel.equals compara todos los campos visibles de la tarjeta.
        return Objects.equals(oldItem, newItem);
    }
}
