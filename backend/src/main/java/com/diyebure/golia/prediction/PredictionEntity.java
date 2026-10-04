package com.diyebure.golia.prediction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Pronostico de un usuario para un partido, persistido en MySQL.
 *
 * <p>Replica el modelo {@code PredictionEntity} de Room de la app: mismas
 * columnas y la misma regla de negocio de "un pronostico por (user_id,
 * match_id)", aqui garantizada con un indice UNICO real de MySQL.</p>
 *
 * <p>Nota de diseno: {@code match_id} se guarda como texto porque en la app el
 * id de partido no siempre es un UUID canonico (puede ser "match-1" o un id
 * externo). No se declara clave foranea a una tabla de partidos porque en el
 * enfoque minimo los partidos siguen viniendo de la API externa, no de este
 * MySQL.</p>
 */
@Entity
@Table(
        name = "predictions",
        indexes = {
                @Index(name = "uk_predictions_user_match",
                        columnList = "user_id, match_id", unique = true)
        }
)
public class PredictionEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Column(name = "predicted_outcome")
    private String predictedOutcome;

    @Column(name = "predicted_home_score")
    private Integer predictedHomeScore;

    @Column(name = "predicted_away_score")
    private Integer predictedAwayScore;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    /** null = sin resolver; true/false una vez el partido termino. */
    @Column(name = "is_correct")
    private Boolean isCorrect;

    /** Epoch UTC en milisegundos. */
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    /** Epoch UTC en milisegundos cuando se resolvio; null si sigue pendiente. */
    @Column(name = "finalized_at")
    private Long finalizedAt;

    protected PredictionEntity() {
        // Requerido por JPA.
    }

    public PredictionEntity(String id, String userId, String matchId, String predictedOutcome,
                            Integer predictedHomeScore, Integer predictedAwayScore,
                            int pointsEarned, Boolean isCorrect, long createdAt, Long finalizedAt) {
        this.id = id;
        this.userId = userId;
        this.matchId = matchId;
        this.predictedOutcome = predictedOutcome;
        this.predictedHomeScore = predictedHomeScore;
        this.predictedAwayScore = predictedAwayScore;
        this.pointsEarned = pointsEarned;
        this.isCorrect = isCorrect;
        this.createdAt = createdAt;
        this.finalizedAt = finalizedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getPredictedOutcome() {
        return predictedOutcome;
    }

    public void setPredictedOutcome(String predictedOutcome) {
        this.predictedOutcome = predictedOutcome;
    }

    public Integer getPredictedHomeScore() {
        return predictedHomeScore;
    }

    public void setPredictedHomeScore(Integer predictedHomeScore) {
        this.predictedHomeScore = predictedHomeScore;
    }

    public Integer getPredictedAwayScore() {
        return predictedAwayScore;
    }

    public void setPredictedAwayScore(Integer predictedAwayScore) {
        this.predictedAwayScore = predictedAwayScore;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(Long finalizedAt) {
        this.finalizedAt = finalizedAt;
    }
}
