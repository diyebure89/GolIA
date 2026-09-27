package com.diyebure.golia.data.local.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.room.migration.AutoMigrationSpec;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Room migration test for {@link GolIADatabase}.
 *
 * <p>Verifies that {@link GolIADatabase#MIGRATION_4_5} correctly upgrades the
 * {@code users} table from schema version 4 to 5 by adding the
 * {@code avatar_uri} column, while preserving the data that existed before the
 * migration. Accounts created before the profile screen feature must keep a
 * {@code NULL} {@code avatar_uri} after the migration.
 *
 * <p>This is an instrumented test: it must run on an Android device or
 * emulator (e.g. {@code ./gradlew connectedAndroidTest}). It relies on the
 * exported Room schema JSON files (version 4 and 5) produced when
 * {@code exportSchema = true} and {@code room.schemaLocation} are configured,
 * which are made available to this test via the androidTest assets source set.
 */
@RunWith(AndroidJUnit4.class)
public class UserMigrationTest {

    private static final String TEST_DB = "migration-test-golia";

    /**
     * SQL that recreates the {@code users} table exactly as it existed at
     * schema version 4 (before the {@code avatar_uri} column was added).
     */
    private static final String CREATE_USERS_V4 =
            "CREATE TABLE IF NOT EXISTS `users` ("
                    + "`id` TEXT NOT NULL, "
                    + "`full_name` TEXT, "
                    + "`username` TEXT, "
                    + "`email` TEXT NOT NULL, "
                    + "`password_algorithm` TEXT, "
                    + "`password_iterations` INTEGER NOT NULL, "
                    + "`password_salt` TEXT, "
                    + "`password_hash` TEXT, "
                    + "`created_at` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`id`))";

    /**
     * Unique indices that exist on the {@code users} table at schema version 4.
     * They must be recreated so the migrated schema validates against the
     * exported version-5 schema.
     */
    private static final String CREATE_INDEX_USERS_EMAIL_V4 =
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)";

    private static final String CREATE_INDEX_USERS_USERNAME_V4 =
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)";

    /** No auto-migrations are declared on {@link GolIADatabase}. */
    private static final List<AutoMigrationSpec> NO_AUTO_MIGRATIONS =
            Collections.emptyList();

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            GolIADatabase.class,
            NO_AUTO_MIGRATIONS);

    @Test
    public void migrate4To5_addsAvatarUriColumnAndPreservesData() throws IOException {
        // 1) Create the database at version 4 and seed a row using the v4 schema.
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 4);
        db.execSQL(CREATE_USERS_V4);
        db.execSQL(CREATE_INDEX_USERS_EMAIL_V4);
        db.execSQL(CREATE_INDEX_USERS_USERNAME_V4);

        ContentValues seed = new ContentValues();
        seed.put("id", "user-1");
        seed.put("full_name", "Ada Lovelace");
        seed.put("username", "ada");
        seed.put("email", "ada@example.com");
        seed.put("password_algorithm", "PBKDF2WithHmacSHA256");
        seed.put("password_iterations", 120_000);
        seed.put("password_salt", "c2FsdA==");
        seed.put("password_hash", "aGFzaA==");
        seed.put("created_at", 1_700_000_000_000L);
        db.insert("users", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, seed);

        // MigrationTestHelper requires the database to be closed before running
        // the migration.
        db.close();

        // 2) Run MIGRATION_4_5 and validate the resulting schema against the
        // exported version-5 schema.
        db = helper.runMigrationsAndValidate(
                TEST_DB, 5, true, GolIADatabase.MIGRATION_4_5);

        // 3a) The new column must exist in the migrated table.
        Set<String> columns = tableColumns(db, "users");
        assertTrue("avatar_uri column should exist after migration",
                columns.contains("avatar_uri"));

        // 3b) The row inserted before the migration must be preserved unchanged,
        // and the new column must default to NULL for pre-existing accounts.
        Cursor cursor = db.query("SELECT * FROM users WHERE id = 'user-1'");
        try {
            assertEquals("previously inserted row should be preserved",
                    1, cursor.getCount());
            assertTrue(cursor.moveToFirst());

            assertEquals("Ada Lovelace",
                    cursor.getString(cursor.getColumnIndexOrThrow("full_name")));
            assertEquals("ada",
                    cursor.getString(cursor.getColumnIndexOrThrow("username")));
            assertEquals("ada@example.com",
                    cursor.getString(cursor.getColumnIndexOrThrow("email")));
            assertEquals("PBKDF2WithHmacSHA256",
                    cursor.getString(cursor.getColumnIndexOrThrow("password_algorithm")));
            assertEquals(120_000,
                    cursor.getInt(cursor.getColumnIndexOrThrow("password_iterations")));
            assertEquals("c2FsdA==",
                    cursor.getString(cursor.getColumnIndexOrThrow("password_salt")));
            assertEquals("aGFzaA==",
                    cursor.getString(cursor.getColumnIndexOrThrow("password_hash")));
            assertEquals(1_700_000_000_000L,
                    cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));

            assertTrue("avatar_uri should be NULL for pre-existing accounts",
                    cursor.isNull(cursor.getColumnIndexOrThrow("avatar_uri")));
        } finally {
            cursor.close();
        }

        // 3c) The unique indices that existed at v4 must still be present after
        // the migration (they are part of the validated v5 schema).
        Set<String> indices = tableIndices(db, "users");
        assertTrue("index_users_email should exist after migration",
                indices.contains("index_users_email"));
        assertTrue("index_users_username should exist after migration",
                indices.contains("index_users_username"));
        assertFalse("users table should not be empty after migration",
                indices.isEmpty() && columns.isEmpty());
    }

    /**
     * Returns the set of column names for the given table using PRAGMA.
     */
    private static Set<String> tableColumns(SupportSQLiteDatabase db, String table) {
        Set<String> columns = new HashSet<>();
        Cursor cursor = db.query("PRAGMA table_info(`" + table + "`)");
        try {
            int nameIndex = cursor.getColumnIndex("name");
            if (nameIndex < 0) {
                fail("PRAGMA table_info did not return a 'name' column");
            }
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex));
            }
        } finally {
            cursor.close();
        }
        return columns;
    }

    /**
     * Returns the set of index names for the given table using PRAGMA.
     */
    private static Set<String> tableIndices(SupportSQLiteDatabase db, String table) {
        Set<String> indices = new HashSet<>();
        Cursor cursor = db.query("PRAGMA index_list(`" + table + "`)");
        try {
            int nameIndex = cursor.getColumnIndex("name");
            if (nameIndex < 0) {
                fail("PRAGMA index_list did not return a 'name' column");
            }
            while (cursor.moveToNext()) {
                indices.add(cursor.getString(nameIndex));
            }
        } finally {
            cursor.close();
        }
        return indices;
    }
}
