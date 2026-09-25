package com.diyebure.golia.presentation.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.diyebure.golia.presentation.ui.partidos.MatchUiModel;

import java.util.Objects;

/**
 * DiffUtil callback para {@link MatchUiModel} usado por {@link MatchesAdapter}.
 *
 * <ul>
 *     <li>{@link #areItemsTheSame}: dos elementos representan el mismo partido cuando
 *     comparten identificador ({@code id}).</li>
 *     <li>{@link #areContentsTheSame}: el contenido visible es idéntico cuando coinciden
 *     todos los campos que la tarjeta muestra (liga, jornada, nombres, logos, hora local,
 *     marcador/minuto, estadio y estado). Así RecyclerView solo re-liga las tarjetas
 *     cuyo contenido visible ha cambiado (por ejemplo, al actualizarse el marcador o el
 *     minuto durante un partido en vivo).</li>
 * </ul>
 */
public class MatchDiffCallback extends DiffUtil.ItemCallback<MatchUiModel> {

    @Override
    public boolean areItemsTheSame(@NonNull MatchUiModel oldItem, @NonNull MatchUiModel newItem) {
        return Objects.equals(oldItem.id, newItem.id);
    }

    @Override
    public boolean areContentsTheSame(@NonNull MatchUiModel oldItem, @NonNull MatchUiModel newItem) {
        return Objects.equals(oldItem.leagueName, newItem.leagueName)
                && Objects.equals(oldItem.round, newItem.round)
                && Objects.equals(oldItem.homeName, newItem.homeName)
                && Objects.equals(oldItem.awayName, newItem.awayName)
                && Objects.equals(oldItem.homeLogoUrl, newItem.homeLogoUrl)
                && Objects.equals(oldItem.awayLogoUrl, newItem.awayLogoUrl)
                && Objects.equals(oldItem.kickoffFormatted, newItem.kickoffFormatted)
                && Objects.equals(oldItem.statusLabel, newItem.statusLabel)
                && oldItem.showScore == newItem.showScore
                && oldItem.showElapsed == newItem.showElapsed
                && Objects.equals(oldItem.scoreText, newItem.scoreText)
                && Objects.equals(oldItem.elapsedText, newItem.elapsedText)
                && Objects.equals(oldItem.venueText, newItem.venueText)
                && oldItem.status == newItem.status;
    }
}
