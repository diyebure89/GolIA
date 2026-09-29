package com.diyebure.golia.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable value object describing a single participant fed into the
 * {@code Ranking_Calculator}.
 *
 * <p>A participant may be the real {@code Current_User} (built from Room) or a
 * deterministic seed profile held in memory. The {@code avatarRef} is a String
 * reference to the avatar: for the real user it is the stored avatar path
 * ({@code UserEntity.avatarUri}); for seed profiles it is a reference to a local
 * drawable resource. Using a single {@code String} keeps the unified model
 * consistent with the real user's {@code avatarUri} column and allows the
 * presentation layer to degrade gracefully to a placeholder when absent.</p>
 */
public final class Ranking_Participant {

    private final String id;
    private final String displayName;
    private final String avatarRef;
    private final boolean isCurrentUser;
    private final List<ScoredPrediction> predictions;

    /**
     * @param id            stable identifier of the participant
     * @param displayName   name shown in the ranking
     * @param avatarRef     avatar reference (path or resource name); may be {@code null}
     * @param isCurrentUser {@code true} when this participant is the real logged-in user
     * @param predictions   the participant's scored predictions (defensively copied)
     */
    public Ranking_Participant(String id, String displayName, String avatarRef,
                               boolean isCurrentUser, List<ScoredPrediction> predictions) {
        this.id = id;
        this.displayName = displayName;
        this.avatarRef = avatarRef;
        this.isCurrentUser = isCurrentUser;
        this.predictions = predictions == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new java.util.ArrayList<>(predictions));
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarRef() {
        return avatarRef;
    }

    public boolean isCurrentUser() {
        return isCurrentUser;
    }

    public List<ScoredPrediction> getPredictions() {
        return predictions;
    }
}
