package com.diyebure.golia.presentation.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.diyebure.golia.domain.model.Ranking_Entry;

import java.util.Objects;

/**
 * DiffUtil callback para {@link Ranking_Entry} usado por {@link RankingAdapter}.
 *
 * <ul>
 *     <li>{@link #areItemsTheSame}: dos filas representan al mismo participante cuando
 *     comparten el nombre a mostrar ({@code displayName}), que es estable entre
 *     recálculos (el ranking no tiene otro identificador en la entrada). Así, al
 *     refrescar los puntos en vivo o cambiar de periodo, RecyclerView reconoce al mismo
 *     participante aunque cambie de posición.</li>
 *     <li>{@link #areContentsTheSame}: el contenido visible es idéntico cuando coinciden
 *     todos los campos que la fila muestra (posición, nombre, avatar, puntos, porcentaje
 *     de acierto, racha, distintivo en vivo y marca de usuario actual). De esta forma
 *     RecyclerView solo re-liga las filas cuyo contenido visible ha cambiado (por ejemplo,
 *     al actualizarse los puntos en vivo).</li>
 * </ul>
 */
public class RankingDiffCallback extends DiffUtil.ItemCallback<Ranking_Entry> {

    @Override
    public boolean areItemsTheSame(@NonNull Ranking_Entry oldItem, @NonNull Ranking_Entry newItem) {
        return Objects.equals(oldItem.getDisplayName(), newItem.getDisplayName());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Ranking_Entry oldItem, @NonNull Ranking_Entry newItem) {
        return oldItem.getPosition() == newItem.getPosition()
                && oldItem.getPoints() == newItem.getPoints()
                && oldItem.getWinPercentage() == newItem.getWinPercentage()
                && oldItem.getStreak() == newItem.getStreak()
                && oldItem.isCurrentUser() == newItem.isCurrentUser()
                && oldItem.hasLive() == newItem.hasLive()
                && Objects.equals(oldItem.getDisplayName(), newItem.getDisplayName())
                && Objects.equals(oldItem.getAvatarRef(), newItem.getAvatarRef());
    }
}
