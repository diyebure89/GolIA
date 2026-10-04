package com.diyebure.golia.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Cuenta de usuario persistida en MySQL.
 *
 * <p>Los nombres de columna coinciden con los del modelo de la app Android
 * ({@code UserDto} / la antigua {@code UserEntity} de Room) para mantener un
 * contrato de datos consistente. El {@code id} es un UUID en texto, igual que en
 * la app. Email y username son unicos a nivel de base de datos (constraints
 * reales, no solo validacion de aplicacion).</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_username", columnNames = "username")
        }
)
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "username")
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    /** Hash BCrypt de la contrasena. NUNCA se expone en las respuestas. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "country")
    private String country;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Column(name = "predictions_made", nullable = false)
    private int predictionsMade;

    @Column(name = "predictions_correct", nullable = false)
    private int predictionsCorrect;

    /** Epoch UTC en milisegundos (mismo formato que la app). */
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected UserEntity() {
        // Requerido por JPA.
    }

    public UserEntity(String id, String fullName, String username, String email,
                      String passwordHash, String country, String avatarUrl,
                      int totalPoints, int predictionsMade, int predictionsCorrect,
                      long createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.country = country;
        this.avatarUrl = avatarUrl;
        this.totalPoints = totalPoints;
        this.predictionsMade = predictionsMade;
        this.predictionsCorrect = predictionsCorrect;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getPredictionsMade() {
        return predictionsMade;
    }

    public void setPredictionsMade(int predictionsMade) {
        this.predictionsMade = predictionsMade;
    }

    public int getPredictionsCorrect() {
        return predictionsCorrect;
    }

    public void setPredictionsCorrect(int predictionsCorrect) {
        this.predictionsCorrect = predictionsCorrect;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
