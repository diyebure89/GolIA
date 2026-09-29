package com.diyebure.golia.domain.usecase;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.dao.PredictionDao;
import com.diyebure.golia.data.local.dao.UserDao;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.data.local.entity.PredictionEntity;
import com.diyebure.golia.data.local.entity.UserEntity;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.model.Ranking_Participant;
import com.diyebure.golia.domain.model.ScoredPrediction;
import com.diyebure.golia.util.DisplayName;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Builds the list of {@link Ranking_Participant} that feeds the
 * {@code Ranking_Calculator} from the <strong>real registered users</strong>
 * read from Room (no seed/mock profiles).
 *
 * <p>Every account is loaded via {@link UserDao#getAll()}. For each user the
 * display name is resolved with
 * {@link DisplayName#resolve(String, String, String)} (username preferred over
 * full name), the avatar comes from its {@code avatar_uri} column, and the
 * participant matching {@link PreferencesManager#getUserId()} is flagged as the
 * current user. Predictions are read through the bounded query
 * {@link PredictionDao#getPredictionsByUser(String)} (R8.1). For every
 * prediction the backing {@code Match} is resolved via
 * {@link MatchDao#getMatchById(String)} to obtain the score and status; when the
 * match is <em>not</em> in the cache the prediction is discarded (R4.5), so it
 * never participates in points, percentage or streak.</p>
 *
 * <p>Each resolved prediction is mapped to a {@link ScoredPrediction}
 * (predicted score + the match's actual score/status + timestamp). The timestamp
 * uses the prediction's {@code finalizedAt} when present, falling back to
 * {@code createdAt}.</p>
 *
 * <p>The class performs local reads only — no API call — and is Hilt-injectable
 * through its {@link Inject} constructor.</p>
 */
public class Ranking_Participant_Provider {

    /** Default display name used when the user has neither username nor full name. */
    private static final String DEFAULT_USER_NAME = "Tú";

    private final PreferencesManager preferencesManager;
    private final UserDao userDao;
    private final PredictionDao predictionDao;
    private final MatchDao matchDao;

    @Inject
    public Ranking_Participant_Provider(PreferencesManager preferencesManager,
                                        UserDao userDao,
                                        PredictionDao predictionDao,
                                        MatchDao matchDao) {
        this.preferencesManager = preferencesManager;
        this.userDao = userDao;
        this.predictionDao = predictionDao;
        this.matchDao = matchDao;
    }

    /**
     * Build the full participant list: the real current user (when a session
     * exists and its predictions resolve) followed by the seed profiles.
     *
     * @param now current instant as epoch millis (injected for determinism)
     * @return the participants for the ranking (real user + seed profiles)
     */
    public List<Ranking_Participant> getParticipants(long now) {
        List<Ranking_Participant> participants = new ArrayList<>();

        String currentUserId = preferencesManager.getUserId();

        List<UserEntity> users = userDao.getAll();
        if (users == null) {
            return participants;
        }

        for (UserEntity user : users) {
            if (user == null) {
                continue;
            }
            Ranking_Participant participant = buildParticipant(user, currentUserId);
            if (participant != null) {
                participants.add(participant);
            }
        }

        return participants;
    }

    /**
     * Build the {@link Ranking_Participant} for the active session (the current
     * user) with its real scored predictions, or {@code null} when there is no
     * active session or the user is not found in Room. Useful to derive the
     * current user's own metrics (points, accuracy, predictions made).
     */
    public Ranking_Participant getCurrentUserParticipant() {
        String currentUserId = preferencesManager.getUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            return null;
        }
        UserEntity user = userDao.getById(currentUserId);
        if (user == null) {
            return null;
        }
        return buildParticipant(user, currentUserId);
    }

    /**
     * Build a {@link Ranking_Participant} from a real {@link UserEntity} read
     * from Room, resolving its display name/avatar and its scored predictions.
     * The participant is flagged {@code isCurrentUser} when its id matches the
     * active session.
     */
    private Ranking_Participant buildParticipant(UserEntity user, String currentUserId) {
        String userId = user.getId();
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        String displayName = DisplayName.resolve(
                user.getUsername(), user.getFullName(), DEFAULT_USER_NAME);
        String avatarRef = user.getAvatarUri();
        boolean isCurrentUser = userId.equals(currentUserId);

        List<ScoredPrediction> scored = buildScoredPredictions(userId);

        return new Ranking_Participant(userId, displayName, avatarRef, isCurrentUser, scored);
    }

    /**
     * Resolve every prediction of the user against its match, mapping the pair to
     * a {@link ScoredPrediction}. Predictions whose match is missing from the
     * cache are discarded (R4.5).
     */
    private List<ScoredPrediction> buildScoredPredictions(String userId) {
        List<PredictionEntity> predictions = predictionDao.getPredictionsByUser(userId);
        List<ScoredPrediction> scored = new ArrayList<>();
        if (predictions == null) {
            return scored;
        }

        for (PredictionEntity prediction : predictions) {
            if (prediction == null) {
                continue;
            }
            String matchId = prediction.getMatchId();
            if (matchId == null || matchId.isEmpty()) {
                // No match reference -> cannot resolve score/status; discard.
                continue;
            }
            MatchEntity match = matchDao.getMatchById(matchId);
            if (match == null) {
                // Match not in cache -> discard the prediction (R4.5).
                continue;
            }

            ScoredPrediction scoredPrediction = toScoredPrediction(prediction, match);
            if (scoredPrediction != null) {
                scored.add(scoredPrediction);
            }
        }
        return scored;
    }

    /**
     * Map a resolved prediction/match pair to a {@link ScoredPrediction}. The
     * predicted score comes from the prediction, while the actual score and
     * status come from the match. Returns {@code null} when the prediction has no
     * predicted score to compare.
     */
    private ScoredPrediction toScoredPrediction(PredictionEntity prediction, MatchEntity match) {
        Integer predictedHome = prediction.getPredictedHomeScore();
        Integer predictedAway = prediction.getPredictedAwayScore();
        if (predictedHome == null || predictedAway == null) {
            // Without a predicted exact score there is nothing to score.
            return null;
        }

        MatchStatus status = parseStatus(match.getStatus());
        long timestamp = resolveTimestamp(prediction);

        return new ScoredPrediction(
                predictedHome,
                predictedAway,
                match.getHomeScore(),
                match.getAwayScore(),
                status,
                timestamp);
    }

    /**
     * Resolve the timestamp used for period filtering and streak ordering:
     * {@code finalizedAt} when present, otherwise {@code createdAt}.
     */
    private long resolveTimestamp(PredictionEntity prediction) {
        Long finalizedAt = prediction.getFinalizedAt();
        if (finalizedAt != null) {
            return finalizedAt;
        }
        return prediction.getCreatedAt();
    }

    /**
     * Parse the stored status string into a {@link MatchStatus}, defaulting to
     * {@link MatchStatus#SCHEDULED} when it is missing or unrecognized so an
     * unfinished/unstarted match neither scores nor counts as live.
     */
    private MatchStatus parseStatus(String status) {
        if (status == null) {
            return MatchStatus.SCHEDULED;
        }
        try {
            return MatchStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return MatchStatus.SCHEDULED;
        }
    }
}
