package com.diyebure.golia.presentation.ui.partidos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.diyebure.golia.domain.model.MatchStatus;

import java.util.Objects;

/**
 * Modelo de presentación inmutable de un partido para la Pantalla_Partidos.
 *
 * <p>Contiene únicamente los datos ya formateados listos para pintarse en la
 * tarjeta ({@code item_match.xml}). No expone cuotas: aunque el modelo de
 * dominio {@code Match} conserva {@code homeOdds}/{@code drawOdds}/{@code awayOdds},
 * la tarjeta no las renderiza.</p>
 *
 * <p>Los campos {@link #status} y {@link #scheduledDateTime} se conservan para el
 * ordenamiento por grupos (LIVE primero) y por horario ascendente.</p>
 */
public final class MatchUiModel {

    /** Identificador estable del partido (usado por DiffUtil.areItemsTheSame). */
    public final String id;

    /** Nombre de la liga (league.name). */
    public final String leagueName;

    /** Jornada (league.round). */
    public final String round;

    /** Nombre del equipo local. */
    public final String homeName;

    /** Nombre del equipo visitante. */
    public final String awayName;

    /** URL del logo del equipo local (puede ser null). */
    @Nullable
    public final String homeLogoUrl;

    /** URL del logo del equipo visitante (puede ser null). */
    @Nullable
    public final String awayLogoUrl;

    /** Hora de inicio formateada en Zona_Local, p.ej. "14 Jun · 20:00". */
    public final String kickoffFormatted;

    /** Etiqueta de estado: "Disponible" / marcador / final. */
    public final String statusLabel;

    /** True si debe mostrarse el marcador (estado LIVE o FINISHED). */
    public final boolean showScore;

    /** True si debe mostrarse el minuto de juego (solo LIVE). */
    public final boolean showElapsed;

    /** Texto del marcador, p.ej. "2 - 1". */
    public final String scoreText;

    /** Texto del minuto de juego, p.ej. "63'". */
    public final String elapsedText;

    /** Texto del estadio, p.ej. "Estadio, Ciudad" o cadena vacía. */
    public final String venueText;

    /** Estado del partido, usado para el ordenamiento por grupo. */
    public final MatchStatus status;

    /** Instante de inicio (epoch millis), usado para el ordenamiento. */
    public final long scheduledDateTime;

    public MatchUiModel(
            String id,
            String leagueName,
            String round,
            String homeName,
            String awayName,
            @Nullable String homeLogoUrl,
            @Nullable String awayLogoUrl,
            String kickoffFormatted,
            String statusLabel,
            boolean showScore,
            boolean showElapsed,
            String scoreText,
            String elapsedText,
            String venueText,
            MatchStatus status,
            long scheduledDateTime) {
        this.id = id;
        this.leagueName = leagueName;
        this.round = round;
        this.homeName = homeName;
        this.awayName = awayName;
        this.homeLogoUrl = homeLogoUrl;
        this.awayLogoUrl = awayLogoUrl;
        this.kickoffFormatted = kickoffFormatted;
        this.statusLabel = statusLabel;
        this.showScore = showScore;
        this.showElapsed = showElapsed;
        this.scoreText = scoreText;
        this.elapsedText = elapsedText;
        this.venueText = venueText;
        this.status = status;
        this.scheduledDateTime = scheduledDateTime;
    }

    /**
     * Igualdad por campos visibles, útil para DiffUtil.areContentsTheSame.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MatchUiModel that = (MatchUiModel) o;
        return showScore == that.showScore
                && showElapsed == that.showElapsed
                && scheduledDateTime == that.scheduledDateTime
                && Objects.equals(id, that.id)
                && Objects.equals(leagueName, that.leagueName)
                && Objects.equals(round, that.round)
                && Objects.equals(homeName, that.homeName)
                && Objects.equals(awayName, that.awayName)
                && Objects.equals(homeLogoUrl, that.homeLogoUrl)
                && Objects.equals(awayLogoUrl, that.awayLogoUrl)
                && Objects.equals(kickoffFormatted, that.kickoffFormatted)
                && Objects.equals(statusLabel, that.statusLabel)
                && Objects.equals(scoreText, that.scoreText)
                && Objects.equals(elapsedText, that.elapsedText)
                && Objects.equals(venueText, that.venueText)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, leagueName, round, homeName, awayName, homeLogoUrl,
                awayLogoUrl, kickoffFormatted, statusLabel, showScore, showElapsed,
                scoreText, elapsedText, venueText, status, scheduledDateTime);
    }

    @NonNull
    @Override
    public String toString() {
        return "MatchUiModel{id='" + id + "', leagueName='" + leagueName
                + "', homeName='" + homeName + "', awayName='" + awayName
                + "', status=" + status + '}';
    }
}
