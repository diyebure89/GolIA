package com.diyebure.golia.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.diyebure.golia.data.local.entity.UserEntity;

/**
 * Data Access Object for local user accounts.
 *
 * <p>Inserts use {@link OnConflictStrategy#ABORT} so a unique-constraint
 * violation (duplicate email/username) surfaces as an exception rather than
 * silently overwriting an existing account.</p>
 */
@Dao
public interface UserDao {

    /**
     * Insert a new user. Aborts (throws) on a unique-constraint conflict.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(UserEntity user);

    /**
     * Find a user by exact email match.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity findByEmail(String email);

    /**
     * Find a user by exact username match.
     */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserEntity findByUsername(String username);

    /**
     * Find a user whose username or email matches the given identifier.
     */
    @Query("SELECT * FROM users WHERE username = :identifier OR email = :identifier LIMIT 1")
    UserEntity findByUsernameOrEmail(String identifier);

    /**
     * Check whether an account with the given email already exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    boolean existsByEmail(String email);

    /**
     * Check whether an account with the given username already exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    boolean existsByUsername(String username);

    /**
     * Get a user by primary key id.
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getById(String id);
}
