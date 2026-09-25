package com.diyebure.golia.presentation.ui.partidos;

import android.text.TextUtils;

import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Mapeador compartido y sin estado de {@link Match} de dominio a
 * {@link MatchUiModel} de presentación.
 *
 * <p>Centraliza el formateo de hora local, marcador, minuto de juego, estadio y
 * etiqueta de estado para reutilizarlo desde la Pantalla_Partidos
 * ({@code PartidosViewModel}) y la sección "Próximos partidos" del Inicio
 * ({@code InicioViewModel}), evitando duplicación de lógica.</p>
 */
public final class MatchUiMapper {

    /** Formateador de hora local "d MMM · HH:mm" (ejemplo: "14 Jun · 20:00"). */
    private static final DateTimeFormatter KICKOFF_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM · HH:mm", new Locale("es", "ES"));

    private MatchUiMapper() {
        // No instanciable: utilidad estática.
    }

    /**
     * Convierte un {@link Match} de dominio en su {@link MatchUiModel} de
     * presentación, formateando la hora a Zona_Local y derivando marcador/minuto/
     * estadio según el estado.
     */
    public static MatchUiModel toUiModel(Match m) {
        MatchStatus status = m.getStatus();
        boolean isLive = status == MatchStatus.LIVE;
        boolean isFinished = status == MatchStatus.FINISHED;
        boolean showScore = isLive || isFinished;

        String kickoff = formatKickoff(m.getScheduledDateTime());
        String scoreText = showScore ? formatScore(m.getHomeScore(), m.getAwayScore()) : "";
        boolean showElapsed = isLive && m.getElapsedMinute() != null;
        String elapsedText = showElapsed ? (m.getElapsedMinute() + "'") : "";
        String venueText = formatVenue(m.getVenueName(), m.getVenueCity());
        String statusLabel = buildStatusLabel(status, scoreText, showScore, kickoff);
        String round = m.getMatchday() > 0 ? ("Jornada " + m.getMatchday()) : "";

        return new MatchUiModel(
                m.getId() != null ? m.getId().toString() : "",
                nullSafe(m.getCompetitionName()),
                round,
                nullSafe(m.getHomeTeamName()),
                nullSafe(m.getAwayTeamName()),
                m.getHomeTeamLogoUrl(),
                m.getAwayTeamLogoUrl(),
                kickoff,
                statusLabel,
                showScore,
                showElapsed,
                scoreText,
                elapsedText,
                venueText,
                status,
                m.getScheduledDateTime());
    }

    private static String formatKickoff(long epochMillis) {
        ZonedDateTime zdt = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault());
        return KICKOFF_FORMATTER.format(zdt);
    }

    private static String formatScore(Integer home, Integer away) {
        int h = home == null ? 0 : home;
        int a = away == null ? 0 : away;
        return h + " - " + a;
    }

    private static String formatVenue(String name, String city) {
        boolean hasName = !isBlank(name);
        boolean hasCity = !isBlank(city);
        if (hasName && hasCity) {
            return name + ", " + city;
        }
        if (hasName) {
            return name;
        }
        if (hasCity) {
            return city;
        }
        return "";
    }

    private static String buildStatusLabel(MatchStatus status, String scoreText,
                                           boolean showScore, String kickoff) {
        if (status == null) {
            return kickoff;
        }
        switch (status) {
            case LIVE:
                return scoreText;
            case FINISHED:
                return "Final " + scoreText;
            case POSTPONED:
                return "Aplazado";
            case CANCELLED:
                return "Cancelado";
            case SCHEDULED:
            default:
                return "Disponible";
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return TextUtils.isEmpty(s) || s.trim().isEmpty();
    }
}
