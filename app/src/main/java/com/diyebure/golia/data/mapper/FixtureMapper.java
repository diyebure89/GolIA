package com.diyebure.golia.data.mapper;

import androidx.annotation.Nullable;

import com.diyebure.golia.data.remote.dto.FixtureDto;
import com.diyebure.golia.data.remote.dto.FixtureItemDto;
import com.diyebure.golia.data.remote.dto.GoalsDto;
import com.diyebure.golia.data.remote.dto.LeagueDto;
import com.diyebure.golia.data.remote.dto.TeamSideDto;
import com.diyebure.golia.data.remote.dto.TeamsDto;
import com.diyebure.golia.data.remote.dto.VenueDto;
import com.diyebure.golia.domain.model.Competition;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.model.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * Maps API-Football {@link FixtureItemDto} objects to the domain models
 * {@link Match}, {@link Team} and {@link Competition}.
 *
 * <p>The status is normalized through {@link StatusMapper} (Requirement 6B). When the
 * normalized status is {@link MatchStatus#LIVE}, the elapsed minute (Minuto_Juego) is
 * taken from {@code fixture.status.elapsed}; for other statuses the elapsed minute is
 * left {@code null} (Requirement 6B.3).</p>
 *
 * <p>Only the six Ligas_Objetivo are kept; fixtures whose {@code league.id} is not in
 * {@link #TARGET_LEAGUE_IDS} are discarded (Requirements 1.5, 1.6).</p>
 *
 * <p>The domain {@link Match} exposes {@code elapsedMinute}, {@code venueName} and
 * {@code venueCity} (task 2.1). This mapper sets the elapsed minute only when the
 * normalized status is {@link MatchStatus#LIVE} (Requirement 6B.3) and copies the
 * venue name/city from {@code fixture.venue} (Requirement 5.5).</p>
 */
public class FixtureMapper {

    /** Premier League (England). */
    public static final int LEAGUE_PREMIER_LEAGUE = 39;
    /** LaLiga / Primera División (Spain). */
    public static final int LEAGUE_LA_LIGA = 140;
    /** Serie A (Italy). */
    public static final int LEAGUE_SERIE_A = 135;
    /** Bundesliga (Germany). */
    public static final int LEAGUE_BUNDESLIGA = 78;
    /** Ligue 1 (France). */
    public static final int LEAGUE_LIGUE_1 = 61;
    /** Colombia Primera A. */
    public static final int LEAGUE_COLOMBIA_PRIMERA_A = 239;
    /** UEFA Champions League. */
    public static final int LEAGUE_CHAMPIONS_LEAGUE = 2;
    /** UEFA Nations League. */
    public static final int LEAGUE_UEFA_NATIONS_LEAGUE = 5;
    /** World Cup Qualification - Europe (rumbo al Mundial). */
    public static final int LEAGUE_WC_QUAL_EUROPE = 32;
    /** World Cup Qualification - South America (Conmebol; incluye a Colombia). */
    public static final int LEAGUE_WC_QUAL_SOUTH_AMERICA = 34;
    /** World Cup Qualification - Africa. */
    public static final int LEAGUE_WC_QUAL_AFRICA = 29;
    /** World Cup Qualification - Asia. */
    public static final int LEAGUE_WC_QUAL_ASIA = 30;
    /** World Cup Qualification - CONCACAF. */
    public static final int LEAGUE_WC_QUAL_CONCACAF = 31;
    /** World Cup Qualification - Oceania. */
    public static final int LEAGUE_WC_QUAL_OCEANIA = 33;
    /** World Cup Qualification - Intercontinental Play-offs. */
    public static final int LEAGUE_WC_QUAL_PLAYOFFS = 37;

    /**
     * The six Ligas_Objetivo identified by their API-Football numeric league id.
     * Fixtures from any other league are discarded (Requirements 1.3, 1.5, 1.6).
     */
    public static final Set<Integer> TARGET_LEAGUE_IDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    LEAGUE_PREMIER_LEAGUE,
                    LEAGUE_LA_LIGA,
                    LEAGUE_SERIE_A,
                    LEAGUE_BUNDESLIGA,
                    LEAGUE_LIGUE_1,
                    LEAGUE_COLOMBIA_PRIMERA_A,
                    LEAGUE_CHAMPIONS_LEAGUE,
                    LEAGUE_UEFA_NATIONS_LEAGUE,
                    LEAGUE_WC_QUAL_EUROPE,
                    LEAGUE_WC_QUAL_SOUTH_AMERICA,
                    LEAGUE_WC_QUAL_AFRICA,
                    LEAGUE_WC_QUAL_ASIA,
                    LEAGUE_WC_QUAL_CONCACAF,
                    LEAGUE_WC_QUAL_OCEANIA,
                    LEAGUE_WC_QUAL_PLAYOFFS
            )));

    private static final long MILLIS_PER_SECOND = 1000L;

    /** Extracts a numeric matchday (e.g. "5" from "Regular Season - 5"). */
    private static final Pattern MATCHDAY_PATTERN = Pattern.compile("(\\d+)");

    private final StatusMapper statusMapper;

    @Inject
    public FixtureMapper(StatusMapper statusMapper) {
        this.statusMapper = statusMapper;
    }

    /**
     * Maps a single {@link FixtureItemDto} to a domain {@link Match}.
     *
     * @param item the fixture item from API-Football (may be null)
     * @return the mapped {@link Match}, or {@code null} if the item is null, has no
     * league, or its league is not one of the {@link #TARGET_LEAGUE_IDS}
     */
    @Nullable
    public Match toMatch(@Nullable FixtureItemDto item) {
        if (item == null) {
            return null;
        }

        LeagueDto league = item.getLeague();
        if (league == null || !TARGET_LEAGUE_IDS.contains(league.getId())) {
            // Requirement 1.6: discard fixtures outside the six Ligas_Objetivo.
            return null;
        }

        FixtureDto fixture = item.getFixture();
        TeamsDto teams = item.getTeams();
        GoalsDto goals = item.getGoals();

        MatchStatus status = statusMapper.map(fixture != null && fixture.getStatus() != null
                ? fixture.getStatus().getShortCode()
                : null);

        Match match = new Match();

        // Identity and competition
        if (fixture != null) {
            match.setExternalId(String.valueOf(fixture.getId()));
        }
        match.setCompetitionId(String.valueOf(league.getId()));
        match.setCompetitionName(league.getName());
        match.setMatchday(parseMatchday(league.getRound()));

        // Teams
        TeamSideDto home = teams != null ? teams.getHome() : null;
        TeamSideDto away = teams != null ? teams.getAway() : null;
        if (home != null) {
            match.setHomeTeamId(String.valueOf(home.getId()));
            match.setHomeTeamName(home.getName());
            match.setHomeTeamLogoUrl(home.getLogo());
        }
        if (away != null) {
            match.setAwayTeamId(String.valueOf(away.getId()));
            match.setAwayTeamName(away.getName());
            match.setAwayTeamLogoUrl(away.getLogo());
        }

        // Kickoff: fixture.timestamp is epoch seconds; Match.scheduledDateTime is epoch millis.
        if (fixture != null) {
            match.setScheduledDateTime(fixture.getTimestamp() * MILLIS_PER_SECOND);
        }

        // Status and score
        match.setStatus(status);
        if (goals != null) {
            match.setHomeScore(goals.getHome());
            match.setAwayScore(goals.getAway());
        }

        // Minuto_Juego (Requirement 6B.3): only populated when status is LIVE.
        match.setElapsedMinute(resolveElapsedMinute(fixture, status));

        // Venue (Requirement 5.5): venue name/city from fixture.venue.
        VenueDto venue = fixture != null ? fixture.getVenue() : null;
        if (venue != null) {
            match.setVenueName(venue.getName());
            match.setVenueCity(venue.getCity());
        }

        return match;
    }

    /**
     * Maps a list of {@link FixtureItemDto} to a list of domain {@link Match}, applying
     * the target-league filter: fixtures from leagues outside {@link #TARGET_LEAGUE_IDS}
     * are dropped (Requirements 1.5, 1.6).
     *
     * @param items the fixture items (may be null)
     * @return a non-null list of mapped matches limited to the Ligas_Objetivo
     */
    public List<Match> toMatches(@Nullable List<FixtureItemDto> items) {
        List<Match> matches = new ArrayList<>();
        if (items == null) {
            return matches;
        }
        for (FixtureItemDto item : items) {
            Match match = toMatch(item);
            if (match != null) {
                matches.add(match);
            }
        }
        return matches;
    }

    /**
     * Maps the home/away teams of a fixture item to domain {@link Team} objects.
     *
     * @param item the fixture item (may be null)
     * @return a list with 0, 1 or 2 teams (home first, then away) that are present
     */
    public List<Team> toTeams(@Nullable FixtureItemDto item) {
        List<Team> teams = new ArrayList<>();
        if (item == null || item.getTeams() == null) {
            return teams;
        }
        Team home = toTeam(item.getTeams().getHome());
        if (home != null) {
            teams.add(home);
        }
        Team away = toTeam(item.getTeams().getAway());
        if (away != null) {
            teams.add(away);
        }
        return teams;
    }

    /**
     * Maps the {@code league} node of a fixture item to a domain {@link Competition}.
     *
     * @param item the fixture item (may be null)
     * @return the mapped {@link Competition}, or {@code null} if there is no league
     */
    @Nullable
    public Competition toCompetition(@Nullable FixtureItemDto item) {
        if (item == null || item.getLeague() == null) {
            return null;
        }
        LeagueDto league = item.getLeague();
        return new Competition(
                String.valueOf(league.getId()),
                league.getName(),
                null,               // country not provided in the fixtures league node
                league.getLogo(),
                null                // season not provided at this node
        );
    }

    @Nullable
    private Team toTeam(@Nullable TeamSideDto side) {
        if (side == null) {
            return null;
        }
        Team team = new Team();
        team.setName(side.getName());
        team.setLogoUrl(side.getLogo());
        return team;
    }

    /**
     * Resolves the Minuto_Juego to persist on the domain model. Returns the elapsed
     * minute from {@code fixture.status.elapsed} only when the normalized status is
     * {@link MatchStatus#LIVE}; otherwise {@code null} (Requirement 6B.3).
     *
     * <p>Kept for task 2.1 to wire onto {@code Match.setElapsedMinute}.</p>
     */
    @Nullable
    static Integer resolveElapsedMinute(@Nullable FixtureDto fixture, MatchStatus status) {
        if (status != MatchStatus.LIVE || fixture == null || fixture.getStatus() == null) {
            return null;
        }
        return fixture.getStatus().getElapsed();
    }

    /**
     * Parses a numeric matchday from the API-Football {@code league.round} string
     * (e.g. "Regular Season - 5" &rarr; 5). Returns 0 when no number is present.
     */
    private static int parseMatchday(@Nullable String round) {
        if (round == null) {
            return 0;
        }
        Matcher matcher = MATCHDAY_PATTERN.matcher(round);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
