package com.diyebure.golia.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.security.PasswordCredential;

/**
 * Room entity for storing user data locally in the {@code users} table.
 *
 * <p>Credentials (algorithm, iterations, salt and hash) live only in this data
 * layer entity and are never exposed to the domain {@link User} model. The
 * {@code email} column is {@link NonNull} and unique; {@code username} is
 * nullable and unique among non-null values (SQLite treats NULLs as distinct),
 * so multiple users without a username can coexist.
 */
@Entity(
        tableName = "users",
        indices = {
                @Index(value = "email", unique = true),
                @Index(value = "username", unique = true)
        }
)
public class UserEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;                     // UUID v4

    @ColumnInfo(name = "full_name")
    private String fullName;

    @ColumnInfo(name = "username")
    private String username;               // NULLABLE (NULL when not provided)

    @NonNull
    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "password_algorithm")
    private String passwordAlgorithm;

    @ColumnInfo(name = "password_iterations")
    private int passwordIterations;

    @ColumnInfo(name = "password_salt")    // Base64
    private String passwordSalt;

    @ColumnInfo(name = "password_hash")    // Base64
    private String passwordHash;

    @ColumnInfo(name = "created_at")
    private long createdAt;                 // epoch UTC millis

    @ColumnInfo(name = "avatar_uri")
    private String avatarUri;               // NULLABLE (absolute path to stored avatar; NULL when none)

    public UserEntity() {}

    public UserEntity(@NonNull String id, String fullName, String username, @NonNull String email,
                      String passwordAlgorithm, int passwordIterations,
                      String passwordSalt, String passwordHash, long createdAt,
                      String avatarUri) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordAlgorithm = passwordAlgorithm;
        this.passwordIterations = passwordIterations;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.avatarUri = avatarUri;
    }

    /**
     * Factory that builds a {@code UserEntity} from a {@link PasswordCredential},
     * keeping credential composition inside the data layer.
     */
    public static UserEntity newUser(@NonNull String id, String fullName, String username,
                                     @NonNull String email, PasswordCredential credential,
                                     long createdAt) {
        return new UserEntity(
                id,
                fullName,
                username,
                email,
                credential != null ? credential.getAlgorithm() : null,
                credential != null ? credential.getIterations() : 0,
                credential != null ? credential.getSaltBase64() : null,
                credential != null ? credential.getHashBase64() : null,
                createdAt,
                null    // avatarUri: newly registered accounts have no avatar yet
        );
    }

    /**
     * Reconstruct the {@link PasswordCredential} from the stored columns for
     * verification. Never leaves the data layer.
     */
    public PasswordCredential toCredential() {
        return new PasswordCredential(passwordAlgorithm, passwordIterations,
                passwordSalt, passwordHash);
    }

    /**
     * Convert to the domain model without exposing credentials.
     * {@code country} and statistics use default values; {@code avatarUrl} is
     * populated from the stored {@code avatar_uri} column.
     */
    public User toDomainModel() {
        return new User(
                id,
                fullName,
                username,
                email,
                null,       // country
                avatarUri,  // avatarUrl (from avatar_uri column, may be null)
                0,          // totalPoints
                0,      // predictionsMade
                0,      // predictionsCorrect
                String.valueOf(createdAt)
        );
    }

    // Getters
    @NonNull
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    @NonNull
    public String getEmail() { return email; }
    public String getPasswordAlgorithm() { return passwordAlgorithm; }
    public int getPasswordIterations() { return passwordIterations; }
    public String getPasswordSalt() { return passwordSalt; }
    public String getPasswordHash() { return passwordHash; }
    public long getCreatedAt() { return createdAt; }
    public String getAvatarUri() { return avatarUri; }

    // Setters
    public void setId(@NonNull String id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(@NonNull String email) { this.email = email; }
    public void setPasswordAlgorithm(String passwordAlgorithm) { this.passwordAlgorithm = passwordAlgorithm; }
    public void setPasswordIterations(int passwordIterations) { this.passwordIterations = passwordIterations; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setAvatarUri(String avatarUri) { this.avatarUri = avatarUri; }
}
