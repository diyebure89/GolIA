package com.diyebure.golia.presentation.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.diyebure.golia.R;
import com.diyebure.golia.presentation.ui.partidos.MatchUiModel;

/**
 * Adapter de la lista de partidos de la Pantalla_Partidos.
 *
 * <p>Extiende {@link ListAdapter} con {@link MatchDiffCallback} para calcular diffs en un
 * hilo de fondo y animar solo las tarjetas cuyo contenido visible ha cambiado (útil para
 * las actualizaciones de marcador/minuto durante partidos en vivo).</p>
 *
 * <p>En el bind se muestran: liga + jornada, nombres de equipos, hora local formateada,
 * marcador y minuto según el estado, y estadio. Los logos se cargan con Glide, usando un
 * placeholder tanto mientras cargan como ante error de carga (Requisitos 5.3, 5.4).</p>
 *
 * <p><b>IDs de vista esperados en {@code item_match.xml} (rediseño de la tarea 7.1):</b></p>
 * <ul>
 *     <li>{@code card_match_container} — contenedor raíz clicable (ya existe).</li>
 *     <li>{@code text_league} — nombre de la liga (ya existe).</li>
 *     <li>{@code text_league_round} — jornada / {@code league.round} (ya existe).</li>
 *     <li>{@code image_home_logo} — {@link ImageView} del logo del equipo local (NUEVO).</li>
 *     <li>{@code image_away_logo} — {@link ImageView} del logo del equipo visitante (NUEVO).</li>
 *     <li>{@code text_team1} — nombre del equipo local (ya existe).</li>
 *     <li>{@code text_team2} — nombre del equipo visitante (ya existe).</li>
 *     <li>{@code text_score} — marcador "2 - 1"; visible solo si {@code showScore} (NUEVO).</li>
 *     <li>{@code text_elapsed} — minuto de juego "63'"; visible solo si {@code showElapsed} (NUEVO).</li>
 *     <li>{@code text_status} — etiqueta de estado (p.ej. "Disponible"); visible cuando no hay
 *     marcador que mostrar (NUEVO, opcional).</li>
 *     <li>{@code text_date_time} — hora local formateada (ya existe).</li>
 *     <li>{@code text_venue} — estadio "Estadio, Ciudad"; se oculta si está vacío (NUEVO).</li>
 * </ul>
 */
public class MatchesAdapter extends ListAdapter<MatchUiModel, MatchesAdapter.VH> {

    /**
     * Listener opcional para la pulsación sobre una tarjeta de partido.
     */
    public interface OnMatchClickListener {
        void onMatchClick(@NonNull MatchUiModel match);
    }

    @Nullable
    private final OnMatchClickListener clickListener;

    public MatchesAdapter() {
        this(null);
    }

    public MatchesAdapter(@Nullable OnMatchClickListener clickListener) {
        super(new MatchDiffCallback());
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    /**
     * ViewHolder de una tarjeta de partido.
     */
    static class VH extends RecyclerView.ViewHolder {

        private final View cardContainer;
        private final TextView leagueText;
        private final TextView leagueRoundText;
        private final ImageView homeLogo;
        private final ImageView awayLogo;
        private final TextView homeNameText;
        private final TextView awayNameText;
        private final TextView scoreText;
        private final TextView elapsedText;
        private final TextView statusText;
        private final TextView dateTimeText;
        private final TextView venueText;

        VH(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.card_match_container);
            leagueText = itemView.findViewById(R.id.text_league);
            leagueRoundText = itemView.findViewById(R.id.text_league_round);
            homeLogo = itemView.findViewById(R.id.image_home_logo);
            awayLogo = itemView.findViewById(R.id.image_away_logo);
            homeNameText = itemView.findViewById(R.id.text_team1);
            awayNameText = itemView.findViewById(R.id.text_team2);
            scoreText = itemView.findViewById(R.id.text_score);
            elapsedText = itemView.findViewById(R.id.text_elapsed);
            statusText = itemView.findViewById(R.id.text_status);
            dateTimeText = itemView.findViewById(R.id.text_date_time);
            venueText = itemView.findViewById(R.id.text_venue);
        }

        void bind(@NonNull MatchUiModel match, @Nullable OnMatchClickListener listener) {
            // Liga + jornada (Requisito 5.2)
            setText(leagueText, match.leagueName);
            setText(leagueRoundText, match.round);

            // Nombres de equipos (Requisito 5.2)
            setText(homeNameText, match.homeName);
            setText(awayNameText, match.awayName);

            // Hora local formateada, ya en Zona_Local (Requisitos 4.3, 5.2)
            setText(dateTimeText, match.kickoffFormatted);

            // Estadio, se oculta si no hay dato (Requisito 5.5)
            if (venueText != null) {
                if (TextUtils.isEmpty(match.venueText)) {
                    venueText.setVisibility(View.GONE);
                } else {
                    venueText.setVisibility(View.VISIBLE);
                    venueText.setText(match.venueText);
                }
            }

            // Marcador / minuto / etiqueta de estado según el estado (Requisitos 6.1, 6.2, 6.3)
            bindStatus(match);

            // Logos con Glide + placeholder ante error o mientras carga (Requisitos 5.3, 5.4)
            loadLogo(homeLogo, match.homeLogoUrl);
            loadLogo(awayLogo, match.awayLogoUrl);

            // Navegación al detalle/predicción (Requisito 12.4)
            if (cardContainer != null) {
                if (listener != null) {
                    cardContainer.setOnClickListener(v -> listener.onMatchClick(match));
                } else {
                    cardContainer.setOnClickListener(null);
                    cardContainer.setClickable(false);
                }
            }
        }

        /**
         * Aplica la visibilidad y el texto de marcador, minuto y etiqueta de estado según
         * {@link MatchUiModel#showScore} y {@link MatchUiModel#showElapsed}:
         * <ul>
         *     <li>LIVE: marcador + minuto ("63'").</li>
         *     <li>FINISHED: marcador (sin minuto).</li>
         *     <li>Resto (SCHEDULED, POSTPONED, ...): etiqueta de estado ("Disponible", etc.).</li>
         * </ul>
         */
        private void bindStatus(@NonNull MatchUiModel match) {
            if (scoreText != null) {
                if (match.showScore) {
                    scoreText.setVisibility(View.VISIBLE);
                    scoreText.setText(match.scoreText);
                } else {
                    scoreText.setVisibility(View.GONE);
                }
            }

            if (elapsedText != null) {
                if (match.showElapsed && !TextUtils.isEmpty(match.elapsedText)) {
                    elapsedText.setVisibility(View.VISIBLE);
                    elapsedText.setText(match.elapsedText);
                } else {
                    elapsedText.setVisibility(View.GONE);
                }
            }

            // La etiqueta de estado se muestra cuando no hay marcador (partido no iniciado o
            // sin resultado): p.ej. "Disponible". Si hay marcador, se oculta para dar
            // protagonismo al resultado.
            if (statusText != null) {
                if (!match.showScore && !TextUtils.isEmpty(match.statusLabel)) {
                    statusText.setVisibility(View.VISIBLE);
                    statusText.setText(match.statusLabel);
                } else {
                    statusText.setVisibility(View.GONE);
                }
            }
        }

        private void loadLogo(@Nullable ImageView target, @Nullable String url) {
            if (target == null) {
                return;
            }
            Glide.with(target.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_team_placeholder)
                    .error(R.drawable.ic_team_placeholder)
                    .fallback(R.drawable.ic_team_placeholder)
                    .into(target);
        }

        private static void setText(@Nullable TextView view, @Nullable String value) {
            if (view != null) {
                view.setText(value == null ? "" : value);
            }
        }
    }
}
